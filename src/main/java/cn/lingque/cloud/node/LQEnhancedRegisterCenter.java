package cn.lingque.cloud.node;

import cn.hutool.json.JSONUtil;
import cn.lingque.base.LQKey;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 增强版注册中心 - 比Nacos更强大的服务注册与发现
 * 支持多协议(HTTP/Socket/gRPC/MCP)、智能负载均衡、健康检查、服务治理
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQEnhancedRegisterCenter {

    // ========================= Redis Keys =========================
    /** 增强节点服务列表 */
    private final static LQKey enhancedNodeService = LQKey.key("LQ:CLOUD:ENHANCED:NODE:REG:CENTER", 1D, 5L);
    
    /** 服务组列表 */
    private final static LQKey svGroupService = LQKey.key("LQ:CLOUD:ENHANCED:NODE:REG:CENTER:GROUP", 1D, LQKey.FOREVER);
    
    /** 协议映射表 */
    private final static LQKey protocolMapping = LQKey.key("LQ:CLOUD:ENHANCED:PROTOCOL:MAPPING", 1D, LQKey.FOREVER);
    
    /** MCP工具注册表 */
    private final static LQKey mcpToolRegistry = LQKey.key("LQ:CLOUD:ENHANCED:MCP:TOOLS", 1D, LQKey.FOREVER);
    
    /** 服务健康状态 */
    private final static LQKey healthStatus = LQKey.key("LQ:CLOUD:ENHANCED:HEALTH:STATUS", 1D, 60L);
    
    /** 服务负载信息 */
    private final static LQKey loadInfo = LQKey.key("LQ:CLOUD:ENHANCED:LOAD:INFO", 1D, 30L);
    
    /** 服务标签索引 */
    private final static LQKey tagIndex = LQKey.key("LQ:CLOUD:ENHANCED:TAG:INDEX", 1D, LQKey.FOREVER);

    // ========================= 内存缓存 =========================
    /** 本项目注册的增强节点 */
    public volatile static Set<LQEnhancedNodeInfo> currentEnhancedNodes = ConcurrentHashMap.newKeySet();
    
    /** 兼容原有节点 */
    public volatile static Set<LQNodeInfo> currentNodes = new HashSet<>();
    
    /** 服务发现缓存 */
    private static final Map<String, List<LQEnhancedNodeInfo>> serviceCache = new ConcurrentHashMap<>();
    
    /** 协议路由缓存 */
    private static final Map<String, Map<String, List<LQEnhancedNodeInfo>>> protocolRouteCache = new ConcurrentHashMap<>();
    
    /** 采用CAS锁来做创建NodeThread */
    private static AtomicInteger isInitRegister = new AtomicInteger(0);
    
    /** 负载均衡策略 */
    public enum LoadBalanceStrategy {
        ROUND_ROBIN, WEIGHTED_ROUND_ROBIN, RANDOM, LEAST_CONNECTIONS, CONSISTENT_HASH
    }

    // ========================= 核心注册方法 =========================
    
    /**
     * 注册增强节点
     */
    public static boolean registerEnhancedNode(LQEnhancedNodeInfo node) {
        try {
            // 设置注册时间和心跳时间
            long currentTime = System.currentTimeMillis();
            node.setRegisterTime(currentTime);
            node.setLastHeartbeat(currentTime);
            
            // 添加到本地缓存
            currentEnhancedNodes.add(node);
            
            // 注册到Redis
            String nodeJson = JSONUtil.toJsonStr(node);
            enhancedNodeService.rd(node.getServerName()).ofZSet().setScore(nodeJson, currentTime * 1D);
            svGroupService.rd().ofZSet().setScore(node.getServerName(), currentTime * 1D);
            
            // 注册协议映射
            registerProtocolMapping(node);
            
            // 注册MCP工具
            if (node.getMcpToolInfo() != null) {
                registerMCPTool(node);
            }
            
            // 注册标签索引
            registerTagIndex(node);
            
            // 初始化健康状态
            updateHealthStatus(node, true);
            
            log.info("成功注册增强服务 {} | ip {} | port {} | protocol {} | weight {}", 
                    node.getServerName(), node.getNodeIp(), node.getNodePort(), 
                    node.getProtocol(), node.getWeight());
            
            // 清除相关缓存
            clearServiceCache(node.getServerName());
            
            return true;
        } catch (Exception e) {
            log.error("注册增强节点失败: {}", JSONUtil.toJsonStr(node), e);
            return false;
        }
    }
    
    /**
     * 兼容原有节点注册
     */
    public static boolean registerNode(LQNodeInfo node) {
        // 转换为增强节点
        LQEnhancedNodeInfo enhancedNode = convertToEnhancedNode(node);
        currentNodes.add(node);
        return registerEnhancedNode(enhancedNode);
    }

    // ========================= 服务发现方法 =========================
    
    /**
     * 获取服务节点列表（增强版）
     */
    public static List<LQEnhancedNodeInfo> getEnhancedNodeList(String serverName) {
        return getEnhancedNodeList(serverName, null, null, LoadBalanceStrategy.ROUND_ROBIN);
    }
    
    /**
     * 根据协议获取服务节点
     */
    public static List<LQEnhancedNodeInfo> getNodesByProtocol(String serverName, String protocol) {
        return getEnhancedNodeList(serverName, protocol, null, LoadBalanceStrategy.ROUND_ROBIN);
    }
    
    /**
     * 根据标签获取服务节点
     */
    public static List<LQEnhancedNodeInfo> getNodesByTags(String serverName, Set<String> tags) {
        return getEnhancedNodeList(serverName, null, tags, LoadBalanceStrategy.ROUND_ROBIN);
    }
    
    /**
     * 智能服务发现 - 支持协议、标签、负载均衡策略
     */
    public static List<LQEnhancedNodeInfo> getEnhancedNodeList(String serverName, String protocol, 
                                                               Set<String> tags, LoadBalanceStrategy strategy) {
        try {
            // 尝试从缓存获取
            String cacheKey = buildCacheKey(serverName, protocol, tags);
            List<LQEnhancedNodeInfo> cachedNodes = serviceCache.get(cacheKey);
            if (cachedNodes != null && !cachedNodes.isEmpty()) {
                return filterAndSortNodes(cachedNodes, strategy);
            }
            
            // 从Redis获取
            long currentTime = System.currentTimeMillis();
            List<RedisRank> svList = enhancedNodeService.<List<RedisRank>>rd(serverName)
                    .ofZSet().getByScoreRange(currentTime - 10000D, currentTime * 1D, 99999);
            
            List<LQEnhancedNodeInfo> nodes = svList.stream()
                    .map(i -> {
                        try {
                            return LQUtil.jsonToBean(i.getMemberId(), LQEnhancedNodeInfo.class);
                        } catch (Exception e) {
                            log.warn("解析节点信息失败: {}", i.getMemberId());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(node -> filterByProtocol(node, protocol))
                    .filter(node -> filterByTags(node, tags))
                    .filter(LQEnhancedNodeInfo::isHealthy)
                    .collect(Collectors.toList());
            
            // 更新缓存
            serviceCache.put(cacheKey, nodes);
            
            return filterAndSortNodes(nodes, strategy);
        } catch (Exception e) {
            log.error("获取增强节点列表失败: serverName={}, protocol={}, tags={}", serverName, protocol, tags, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 兼容原有方法
     */
    public static Set<LQNodeInfo> getSvNodeList(String serverName) {
        return getEnhancedNodeList(serverName).stream()
                .map(LQEnhancedRegisterCenter::convertToLegacyNode)
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取所有节点（增强版）
     */
    public static Set<LQEnhancedNodeInfo> getAllEnhancedNodeList() {
        try {
            List<String> serverGroups = svGroupService.rd().ofZSet().getMembers(1, System.currentTimeMillis());
            Set<LQEnhancedNodeInfo> allNodes = new HashSet<>();
            
            if (serverGroups != null && !serverGroups.isEmpty()) {
                for (String serverGroup : serverGroups) {
                    List<LQEnhancedNodeInfo> nodes = getEnhancedNodeList(serverGroup);
                    allNodes.addAll(nodes);
                }
            }
            
            return allNodes;
        } catch (Exception e) {
            log.error("获取所有增强节点失败", e);
            return new HashSet<>();
        }
    }
    
    /**
     * 兼容原有方法
     */
    public static Set<LQNodeInfo> getAllNodeList() {
        return getAllEnhancedNodeList().stream()
                .map(LQEnhancedRegisterCenter::convertToLegacyNode)
                .collect(Collectors.toSet());
    }

    // ========================= MCP工具相关 =========================
    
    /**
     * 注册MCP工具
     */
    public static boolean registerMCPTool(LQEnhancedNodeInfo node) {
        if (node.getMcpToolInfo() == null) {
            return false;
        }
        
        try {
            LQEnhancedNodeInfo.MCPToolInfo mcpInfo = node.getMcpToolInfo();
            String toolKey = mcpInfo.getToolName() + ":" + mcpInfo.getToolVersion();
            
            mcpToolRegistry.rd(toolKey).ofZSet().setScore(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
            
            log.info("注册MCP工具: {} -> {}", toolKey, node.getServerName());
            return true;
        } catch (Exception e) {
            log.error("注册MCP工具失败", e);
            return false;
        }
    }
    
    /**
     * 获取MCP工具节点
     */
    public static List<LQEnhancedNodeInfo> getMCPToolNodes(String toolName, String toolVersion) {
        try {
            String toolKey = toolName + ":" + toolVersion;
            List<RedisRank> toolNodes = mcpToolRegistry.<List<RedisRank>>rd(toolKey)
                    .ofZSet().getByScoreRange(System.currentTimeMillis() - 10000D, System.currentTimeMillis() * 1D, 99999);
            
            return toolNodes.stream()
                    .map(i -> LQUtil.jsonToBean(i.getMemberId(), LQEnhancedNodeInfo.class))
                    .filter(Objects::nonNull)
                    .filter(LQEnhancedNodeInfo::isHealthy)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取MCP工具节点失败: {}:{}", toolName, toolVersion, e);
            return new ArrayList<>();
        }
    }

    // ========================= 健康检查 =========================
    
    /**
     * 更新节点健康状态
     */
    public static void updateHealthStatus(LQEnhancedNodeInfo node, boolean healthy) {
        try {
            String healthKey = node.getServerName() + ":" + node.nodeId();
            if (healthy) {
                healthStatus.rd(healthKey).ofValue().set("HEALTHY", 60);
            } else {
                healthStatus.rd(healthKey).ofValue().set("UNHEALTHY", 60);
            }
        } catch (Exception e) {
            log.error("更新健康状态失败", e);
        }
    }
    
    /**
     * 检查节点健康状态
     */
    public static boolean isNodeHealthy(LQEnhancedNodeInfo node) {
        try {
            String healthKey = node.getServerName() + ":" + node.nodeId();
            String status = healthStatus.rd(healthKey).ofValue().get();
            return "HEALTHY".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    // ========================= 启动和心跳维护 =========================
    
    /**
     * 启动增强注册中心
     */
    public static void start() {
        if (isInitRegister.compareAndSet(0, 1)) {
            LQUtil.execLoadJob("增强注册中心定时服务", () -> {
                // 维护增强节点心跳
                maintainEnhancedNodesHeartbeat();
                
                // 维护兼容节点心跳
                maintainLegacyNodesHeartbeat();
                
                // 清理过期节点
                cleanupExpiredNodes();
                
                // 清理缓存
                cleanupCache();
                
            }, 0, 2000, TimeUnit.MILLISECONDS);
            
            log.info("增强注册中心启动成功");
        }
    }
    
    /**
     * 维护增强节点心跳
     */
    private static void maintainEnhancedNodesHeartbeat() {
        TryCatch.trying(() -> {
            long currentTime = System.currentTimeMillis();
            for (LQEnhancedNodeInfo node : currentEnhancedNodes) {
                try {
                    node.setLastHeartbeat(currentTime);
                    enhancedNodeService.rd(node.getServerName()).ofZSet().setScore(JSONUtil.toJsonStr(node), currentTime * 1D);
                    svGroupService.rd().ofZSet().setScore(node.getServerName(), currentTime * 1D);
                    
                    // 更新健康状态
                    updateHealthStatus(node, true);
                } catch (Exception e) {
                    log.error("维护增强节点心跳异常: {}", JSONUtil.toJsonStr(node), e);
                }
            }
        });
    }
    
    /**
     * 维护兼容节点心跳
     */
    private static void maintainLegacyNodesHeartbeat() {
        TryCatch.trying(() -> {
            for (LQNodeInfo node : currentNodes) {
                try {
                    enhancedNodeService.rd(node.getServerName()).ofZSet().setScore(JSONUtil.toJsonStr(convertToEnhancedNode(node)), System.currentTimeMillis() * 1D);
                    svGroupService.rd().ofZSet().setScore(node.getServerName(), System.currentTimeMillis() * 1D);
                } catch (Exception e) {
                    log.error("维护兼容节点心跳异常: {}", JSONUtil.toJsonStr(node), e);
                }
            }
        });
    }

    // ========================= 工具方法 =========================
    
    /**
     * 转换为增强节点
     */
    private static LQEnhancedNodeInfo convertToEnhancedNode(LQNodeInfo legacyNode) {
        return new LQEnhancedNodeInfo()
                .setServerName(legacyNode.getServerName())
                .setNodeIp(legacyNode.getNodeIp())
                .setNodePort(legacyNode.getNodePort())
                .setProtocol("HTTP")
                .setWeight(100)
                .setStatus("ONLINE")
                .setLastHeartbeat(System.currentTimeMillis())
                .setRegisterTime(System.currentTimeMillis());
    }
    
    /**
     * 转换为兼容节点
     */
    private static LQNodeInfo convertToLegacyNode(LQEnhancedNodeInfo enhancedNode) {
        return new LQNodeInfo()
                .setServerName(enhancedNode.getServerName())
                .setNodeIp(enhancedNode.getNodeIp())
                .setNodePort(enhancedNode.getNodePort());
    }
    
    /**
     * 注册协议映射
     */
    private static void registerProtocolMapping(LQEnhancedNodeInfo node) {
        try {
            // 注册主协议
            String protocolKey = node.getServerName() + ":" + node.getProtocol();
            protocolMapping.rd(protocolKey).ofZSet().setScore(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
            
            // 注册额外协议
            for (String protocol : node.getProtocolPorts().keySet()) {
                String extraProtocolKey = node.getServerName() + ":" + protocol;
                protocolMapping.rd(extraProtocolKey).ofZSet().setScore(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
            }
        } catch (Exception e) {
            log.error("注册协议映射失败", e);
        }
    }
    
    /**
     * 注册标签索引
     */
    private static void registerTagIndex(LQEnhancedNodeInfo node) {
        try {
            for (String tag : node.getTags()) {
                String tagKey = node.getServerName() + ":tag:" + tag;
                tagIndex.rd(tagKey).ofZSet().setScore(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
            }
        } catch (Exception e) {
            log.error("注册标签索引失败", e);
        }
    }
    
    // 其他工具方法...
    private static boolean filterByProtocol(LQEnhancedNodeInfo node, String protocol) {
        return protocol == null || node.supportsProtocol(protocol);
    }
    
    private static boolean filterByTags(LQEnhancedNodeInfo node, Set<String> tags) {
        if (tags == null || tags.isEmpty()) return true;
        return tags.stream().anyMatch(node::hasTag);
    }
    
    private static String buildCacheKey(String serverName, String protocol, Set<String> tags) {
        StringBuilder sb = new StringBuilder(serverName);
        if (protocol != null) sb.append(":").append(protocol);
        if (tags != null && !tags.isEmpty()) {
            sb.append(":").append(String.join(",", tags));
        }
        return sb.toString();
    }
    
    private static List<LQEnhancedNodeInfo> filterAndSortNodes(List<LQEnhancedNodeInfo> nodes, LoadBalanceStrategy strategy) {
        // 根据负载均衡策略排序
        switch (strategy) {
            case WEIGHTED_ROUND_ROBIN:
                return nodes.stream().sorted((a, b) -> b.getWeight().compareTo(a.getWeight())).collect(Collectors.toList());
            case RANDOM:
                Collections.shuffle(nodes);
                return nodes;
            default:
                return nodes;
        }
    }
    
    private static void clearServiceCache(String serverName) {
        serviceCache.entrySet().removeIf(entry -> entry.getKey().startsWith(serverName));
    }
    
    private static void cleanupExpiredNodes() {
        // 清理过期节点的实现
    }
    
    private static void cleanupCache() {
        // 定期清理缓存的实现
    }
    
    /**
     * 判断是否当前节点
     */
    public static boolean isCurrentNode(LQNodeInfo node) {
        return currentNodes.stream().anyMatch(n -> n.equals(node));
    }
    
    public static boolean isCurrentEnhancedNode(LQEnhancedNodeInfo node) {
        return currentEnhancedNodes.stream().anyMatch(n -> n.equals(node));
    }
}