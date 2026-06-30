package cn.lingque.cloud.node.mcp;

import cn.hutool.json.JSONUtil;
import cn.lingque.base.LQKey;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.util.LQUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP工具管理器 - 专门处理MCP(Model Context Protocol)工具的注册和发现
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQMCPToolManager {
    
    /** MCP工具注册表 */
    private final static LQKey mcpToolRegistry = LQKey.key("LQ:CLOUD:MCP:TOOLS", 1D, LQKey.FOREVER);
    
    /** MCP工具能力索引 */
    private final static LQKey mcpCapabilityIndex = LQKey.key("LQ:CLOUD:MCP:CAPABILITIES", 1D, LQKey.FOREVER);
    
    /** MCP工具版本管理 */
    private final static LQKey mcpVersionRegistry = LQKey.key("LQ:CLOUD:MCP:VERSIONS", 1D, LQKey.FOREVER);
    
    /** MCP工具使用统计 */
    private final static LQKey mcpUsageStats = LQKey.key("LQ:CLOUD:MCP:USAGE:STATS", 1D, LQKey.ONE_DAY);
    
    /** 本地MCP工具缓存 */
    private static final Map<String, MCPToolDefinition> localToolCache = new ConcurrentHashMap<>();
    
    /**
     * 注册MCP工具
     */
    public static boolean registerMCPTool(MCPToolDefinition toolDef, LQEnhancedNodeInfo nodeInfo) {
        try {
            // 验证工具定义
            if (!validateToolDefinition(toolDef)) {
                log.error("MCP工具定义验证失败: {}", toolDef.getToolName());
                return false;
            }
            
            // 设置节点信息
            toolDef.setProviderNode(nodeInfo);
            toolDef.setRegisterTime(System.currentTimeMillis());
            toolDef.setLastUpdateTime(System.currentTimeMillis());
            
            String toolKey = buildToolKey(toolDef.getToolName(), toolDef.getToolVersion());
            String toolJson = JSONUtil.toJsonStr(toolDef);
            
            // 注册到工具注册表
            mcpToolRegistry.rd(toolKey).ofZSet().setScore(toolJson, System.currentTimeMillis() * 1D);
            
            // 注册能力索引
            registerCapabilityIndex(toolDef);
            
            // 注册版本信息
            registerVersionInfo(toolDef);
            
            // 更新本地缓存
            localToolCache.put(toolKey, toolDef);
            
            log.info("成功注册MCP工具: {} v{} 提供者: {}:{}", 
                    toolDef.getToolName(), toolDef.getToolVersion(), 
                    nodeInfo.getNodeIp(), nodeInfo.getNodePort());
            
            return true;
        } catch (Exception e) {
            log.error("注册MCP工具失败", e);
            return false;
        }
    }
    
    /**
     * 发现MCP工具
     */
    public static List<MCPToolDefinition> discoverTools(String toolName) {
        return discoverTools(toolName, null, null);
    }
    
    /**
     * 按版本发现MCP工具
     */
    public static List<MCPToolDefinition> discoverTools(String toolName, String version) {
        return discoverTools(toolName, version, null);
    }
    
    /**
     * 按能力发现MCP工具
     */
    public static List<MCPToolDefinition> discoverToolsByCapability(String capability) {
        return discoverTools(null, null, Collections.singleton(capability));
    }
    
    /**
     * 智能工具发现
     */
    public static List<MCPToolDefinition> discoverTools(String toolName, String version, Set<String> requiredCapabilities) {
        try {
            List<MCPToolDefinition> tools = new ArrayList<>();
            
            if (toolName != null) {
                // 按工具名查找
                String toolKey = version != null ? buildToolKey(toolName, version) : toolName + ":*";
                
                if (version != null) {
                    // 精确版本匹配
                    List<RedisRank> toolRanks = mcpToolRegistry.<List<RedisRank>>rd(toolKey)
                            .ofZSet().getByScoreRange(System.currentTimeMillis() - 86400000D, System.currentTimeMillis() * 1D, 999);
                    
                    tools.addAll(toolRanks.stream()
                            .map(rank -> LQUtil.jsonToBean(rank.getMemberId(), MCPToolDefinition.class))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                } else {
                    // 查找所有版本
                    List<String> versions = getToolVersions(toolName);
                    for (String ver : versions) {
                        String versionKey = buildToolKey(toolName, ver);
                        List<RedisRank> toolRanks = mcpToolRegistry.<List<RedisRank>>rd(versionKey)
                                .ofZSet().getByScoreRange(System.currentTimeMillis() - 86400000D, System.currentTimeMillis() * 1D, 999);
                        
                        tools.addAll(toolRanks.stream()
                                .map(rank -> LQUtil.jsonToBean(rank.getMemberId(), MCPToolDefinition.class))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                    }
                }
            } else if (requiredCapabilities != null && !requiredCapabilities.isEmpty()) {
                // 按能力查找
                for (String capability : requiredCapabilities) {
                    Set<String> capabilityTools = mcpCapabilityIndex.rd(capability).ofSet().members();
                    for (String toolKey : capabilityTools) {
                        MCPToolDefinition tool = localToolCache.get(toolKey);
                        if (tool != null && !tools.contains(tool)) {
                            tools.add(tool);
                        }
                    }
                }
            }
            
            // 过滤能力匹配
            if (requiredCapabilities != null && !requiredCapabilities.isEmpty()) {
                tools = tools.stream()
                        .filter(tool -> tool.getCapabilities().containsAll(requiredCapabilities))
                        .collect(Collectors.toList());
            }
            
            // 过滤健康的节点
            tools = tools.stream()
                    .filter(tool -> tool.getProviderNode() != null && tool.getProviderNode().isHealthy())
                    .collect(Collectors.toList());
            
            // 按优先级排序
            tools.sort((a, b) -> {
                // 优先级高的在前
                int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
                if (priorityCompare != 0) return priorityCompare;
                
                // 版本新的在前
                return b.toolVersion.compareTo(a.toolVersion);
            });
            
            return tools;
        } catch (Exception e) {
            System.err.println("发现MCP工具失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 调用MCP工具
     */
    public static MCPToolResponse invokeTool(String toolName, String version, MCPToolRequest request) {
        try {
            List<MCPToolDefinition> tools = discoverTools(toolName, version);
            if (tools.isEmpty()) {
                return MCPToolResponse.error("工具未找到: " + toolName + " v" + version);
            }
            
            MCPToolDefinition tool = tools.get(0); // 选择第一个可用工具
            
            // 记录使用统计
            recordUsageStats(tool);
            
            // 这里应该实际调用工具，暂时返回模拟响应
            return MCPToolResponse.success("工具调用成功", Map.of(
                    "toolName", tool.toolName,
                    "version", tool.toolVersion,
                    "provider", tool.providerNode.nodeId(),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            System.err.println("调用MCP工具失败: " + e.getMessage());
            return MCPToolResponse.error("工具调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取工具使用统计
     */
    public static Map<String, Object> getToolUsageStats(String toolName) {
        try {
            String statsKey = toolName + ":usage";
            Map<String, String> stats = mcpUsageStats.rd(statsKey).ofHash().getAllValue(String.class);
            
            Map<String, Object> result = new HashMap<>();
            stats.forEach((k, v) -> {
                try {
                    result.put(k, Long.parseLong(v));
                } catch (NumberFormatException e) {
                    result.put(k, v);
                }
            });
            
            return result;
        } catch (Exception e) {
            System.err.println("获取工具使用统计失败: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 注销MCP工具
     */
    public static boolean unregisterMCPTool(String toolName, String version, LQEnhancedNodeInfo nodeInfo) {
        try {
            String toolKey = buildToolKey(toolName, version);
            
            // 从注册表移除
            List<RedisRank> tools = mcpToolRegistry.<List<RedisRank>>rd(toolKey).ofZSet().pageRankLimit(0, 999);
            for (RedisRank rank : tools) {
                MCPToolDefinition tool = LQUtil.jsonToBean(rank.getMemberId(), MCPToolDefinition.class);
                if (tool != null && tool.providerNode.equals(nodeInfo)) {
                    mcpToolRegistry.rd(toolKey).ofZSet().zrem(rank.getMemberId());
                    break;
                }
            }
            
            // 清理能力索引
            cleanupCapabilityIndex(toolName, version);
            
            // 清理本地缓存
            localToolCache.remove(toolKey);
            
            System.out.println("成功注销MCP工具: " + toolName + " v" + version);
            return true;
        } catch (Exception e) {
            System.err.println("注销MCP工具失败: " + e.getMessage());
            return false;
        }
    }
    
    // ========================= 私有方法 =========================
    
    private static boolean validateToolDefinition(MCPToolDefinition toolDef) {
        return toolDef != null &&
               toolDef.toolName != null && !toolDef.toolName.trim().isEmpty() &&
               toolDef.toolVersion != null && !toolDef.toolVersion.trim().isEmpty() &&
               toolDef.capabilities != null && !toolDef.capabilities.isEmpty();
    }
    
    private static String buildToolKey(String toolName, String version) {
        return toolName + ":" + version;
    }
    
    private static void registerCapabilityIndex(MCPToolDefinition toolDef) {
        String toolKey = buildToolKey(toolDef.toolName, toolDef.toolVersion);
        for (String capability : toolDef.capabilities) {
            mcpCapabilityIndex.rd(capability).ofSet().addMember(toolKey);
        }
    }
    
    private static void registerVersionInfo(MCPToolDefinition toolDef) {
        String versionKey = toolDef.toolName + ":versions";
        mcpVersionRegistry.rd(versionKey).ofZSet().setScore(toolDef.toolVersion, System.currentTimeMillis() * 1D);
    }
    
    private static List<String> getToolVersions(String toolName) {
        try {
            String versionKey = toolName + ":versions";
            return mcpVersionRegistry.rd(versionKey).ofZSet().getMembers(1, System.currentTimeMillis());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private static void recordUsageStats(MCPToolDefinition tool) {
        try {
            String statsKey = tool.toolName + ":usage";
            String today = String.valueOf(System.currentTimeMillis() / 86400000);
            
            mcpUsageStats.rd(statsKey).ofHash().incrHashValue("total_calls", 1);
            mcpUsageStats.rd(statsKey).ofHash().incrHashValue("calls_" + today, 1);
            mcpUsageStats.rd(statsKey).ofHash().set("last_call_time", String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            System.err.println("记录使用统计失败: " + e.getMessage());
        }
    }
    
    private static void cleanupCapabilityIndex(String toolName, String version) {
        try {
            String toolKey = buildToolKey(toolName, version);
            MCPToolDefinition tool = localToolCache.get(toolKey);
            if (tool != null) {
                for (String capability : tool.capabilities) {
                    mcpCapabilityIndex.rd(capability).ofSet().deleteMember(toolKey);
                }
            }
        } catch (Exception e) {
            System.err.println("清理能力索引失败: " + e.getMessage());
        }
    }
    
    // ========================= 数据模型 =========================
    
    /**
     * MCP工具定义
     */
    @Data
    @Accessors(chain = true)
    public static class MCPToolDefinition {
        /** 工具名称 */
        private String toolName;
        
        /** 工具版本 */
        private String toolVersion;
        
        /** 工具描述 */
        private String description;
        
        /** 工具能力列表 */
        private Set<String> capabilities = new HashSet<>();
        
        /** 工具参数定义 */
        private Map<String, Object> parameterSchema = new HashMap<>();
        
        /** 工具优先级 */
        private Integer priority = 100;
        
        /** 提供者节点 */
        private LQEnhancedNodeInfo providerNode;
        
        /** 工具端点 */
        private String endpoint;
        
        /** 工具配置 */
        private Map<String, Object> configuration = new HashMap<>();
        
        /** 注册时间 */
        private Long registerTime;
        
        /** 最后更新时间 */
        private Long lastUpdateTime;
        
        /** 工具状态 */
        private String status = "ACTIVE";
        
        /** 工具标签 */
        private Set<String> tags = new HashSet<>();
    }
    
    /**
     * MCP工具请求
     */
    @Data
    @Accessors(chain = true)
    public static class MCPToolRequest {
        /** 请求ID */
        private String requestId;
        
        /** 请求参数 */
        private Map<String, Object> parameters = new HashMap<>();
        
        /** 请求上下文 */
        private Map<String, Object> context = new HashMap<>();
        
        /** 超时时间(毫秒) */
        private Long timeout = 30000L;
    }
    
    /**
     * MCP工具响应
     */
    @Data
    @Accessors(chain = true)
    public static class MCPToolResponse {
        /** 是否成功 */
        private boolean success;
        
        /** 响应消息 */
        private String message;
        
        /** 响应数据 */
        private Object data;
        
        /** 错误代码 */
        private String errorCode;
        
        /** 响应时间戳 */
        private Long timestamp;
        
        public static MCPToolResponse success(String message, Object data) {
            return new MCPToolResponse()
                    .setSuccess(true)
                    .setMessage(message)
                    .setData(data)
                    .setTimestamp(System.currentTimeMillis());
        }
        
        public static MCPToolResponse error(String message) {
            return new MCPToolResponse()
                    .setSuccess(false)
                    .setMessage(message)
                    .setTimestamp(System.currentTimeMillis());
        }
    }
}