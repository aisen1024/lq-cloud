package cn.lingque.cloud.mcp.registry;

import cn.lingque.cloud.mcp.annotation.MCPTool;
import cn.lingque.cloud.mcp.annotation.MCPToolMethod;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LQ MCP工具注册表
 * 负责管理所有MCP工具的注册、发现和元数据
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
public class LQMCPToolRegistry {

    /**
     * 工具实例缓存
     */
    private final Map<String, MCPToolInfo> toolInstances = new ConcurrentHashMap<>();

    /**
     * 工具方法缓存
     */
    private final Map<String, Map<String, Method>> toolMethods = new ConcurrentHashMap<>();

    /**
     * 工具能力索引
     */
    private final Map<String, Set<String>> capabilityIndex = new ConcurrentHashMap<>();

    /**
     * 注册MCP工具
     */
    public void registerTool(String beanName, Object toolInstance) {
        Class<?> toolClass = toolInstance.getClass();
        MCPTool mcpTool = toolClass.getAnnotation(MCPTool.class);
        
        if (mcpTool == null) {
            log.warn("[LQ-MCP] 类 {} 没有@MCPTool注解，跳过注册", toolClass.getName());
            return;
        }

        if (!mcpTool.enabled()) {
            log.info("[LQ-MCP] 工具 {} 已禁用，跳过注册", beanName);
            return;
        }

        try {
            // 1. 创建工具信息
            MCPToolInfo toolInfo = createToolInfo(beanName, toolInstance, mcpTool);
            
            // 2. 扫描工具方法
            Map<String, Method> methods = scanToolMethods(toolClass);
            
            // 3. 注册到本地缓存
            toolInstances.put(beanName, toolInfo);
            toolMethods.put(beanName, methods);
            
            // 4. 注册能力索引
            registerCapabilities(beanName, mcpTool.capabilities());
            
            // 5. 注册到全局MCP管理器
            registerToGlobalManager(toolInfo, mcpTool);
            
            log.info("[LQ-MCP] 成功注册工具: {} ({}), 方法数: {}, 能力: {}", 
                    beanName, mcpTool.name(), methods.size(), Arrays.toString(mcpTool.capabilities()));
            
        } catch (Exception e) {
            log.error("[LQ-MCP] 注册工具失败: {}", beanName, e);
            throw new RuntimeException("注册MCP工具失败: " + beanName, e);
        }
    }

    /**
     * 创建工具信息
     */
    private MCPToolInfo createToolInfo(String beanName, Object instance, MCPTool mcpTool) {
        String toolName = mcpTool.name().isEmpty() ? beanName : mcpTool.name();
        
        return new MCPToolInfo()
                .setBeanName(beanName)
                .setToolName(toolName)
                .setToolVersion(mcpTool.version())
                .setDescription(mcpTool.description())
                .setInstance(instance)
                .setCapabilities(new HashSet<>(Arrays.asList(mcpTool.capabilities())))
                .setTags(new HashSet<>(Arrays.asList(mcpTool.tags())))
                .setPriority(mcpTool.priority())
                .setEndpoint(mcpTool.endpoint())
                .setEnabled(mcpTool.enabled())
                .setRegisterTime(System.currentTimeMillis());
    }

    /**
     * 扫描工具方法
     */
    private Map<String, Method> scanToolMethods(Class<?> toolClass) {
        Map<String, Method> methods = new HashMap<>();
        
        for (Method method : toolClass.getDeclaredMethods()) {
            MCPToolMethod mcpMethod = method.getAnnotation(MCPToolMethod.class);
            if (mcpMethod != null) {
                String methodName = mcpMethod.name().isEmpty() ? method.getName() : mcpMethod.name();
                methods.put(methodName, method);
                log.debug("[LQ-MCP] 发现工具方法: {} -> {}", methodName, method.getName());
            }
        }
        
        return methods;
    }

    /**
     * 注册能力索引
     */
    private void registerCapabilities(String toolName, String[] capabilities) {
        for (String capability : capabilities) {
            capabilityIndex.computeIfAbsent(capability, k -> new HashSet<>()).add(toolName);
        }
    }

    /**
     * 注册到全局MCP管理器
     */
    private void registerToGlobalManager(MCPToolInfo toolInfo, MCPTool mcpTool) {
        // 创建节点信息
        LQEnhancedNodeInfo nodeInfo = new LQEnhancedNodeInfo()
                .setServerName(toolInfo.getToolName())
                .setNodeIp("127.0.0.1")
                .setNodePort(8090)
                .setProtocol("MCP")
                .setVersion(toolInfo.getToolVersion());

        // 创建工具定义
        LQMCPToolManager.MCPToolDefinition toolDef = new LQMCPToolManager.MCPToolDefinition()
                .setToolName(toolInfo.getToolName())
                .setToolVersion(toolInfo.getToolVersion())
                .setDescription(toolInfo.getDescription())
                .setPriority(toolInfo.getPriority())
                .setProviderNode(nodeInfo);
        
        toolDef.getCapabilities().addAll(toolInfo.getCapabilities());
        toolDef.getTags().addAll(toolInfo.getTags());
        
        // 注册到全局管理器
        LQMCPToolManager.registerMCPTool(toolDef, nodeInfo);
    }

    /**
     * 获取工具实例
     */
    public MCPToolInfo getToolInfo(String toolName) {
        return toolInstances.get(toolName);
    }

    /**
     * 获取工具方法
     */
    public Method getToolMethod(String toolName, String methodName) {
        Map<String, Method> methods = toolMethods.get(toolName);
        return methods != null ? methods.get(methodName) : null;
    }

    /**
     * 按能力查找工具
     */
    public Set<String> getToolsByCapability(String capability) {
        return capabilityIndex.getOrDefault(capability, Collections.emptySet());
    }

    /**
     * 获取所有已注册工具
     */
    public Map<String, MCPToolInfo> getAllTools() {
        return new HashMap<>(toolInstances);
    }

    /**
     * 获取已注册工具数量
     */
    public int getRegisteredToolCount() {
        return toolInstances.size();
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasToolRegistered(String toolName) {
        return toolInstances.containsKey(toolName);
    }

    /**
     * 获取工具统计信息
     */
    public Map<String, Object> getToolStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTools", toolInstances.size());
        stats.put("totalMethods", toolMethods.values().stream().mapToInt(Map::size).sum());
        stats.put("totalCapabilities", capabilityIndex.size());
        stats.put("enabledTools", toolInstances.values().stream().mapToLong(t -> t.isEnabled() ? 1 : 0).sum());
        return stats;
    }

    /**
     * MCP工具信息
     */
    @Data
    public static class MCPToolInfo {
        private String beanName;
        private String toolName;
        private String toolVersion;
        private String description;
        private Object instance;
        private Set<String> capabilities = new HashSet<>();
        private Set<String> tags = new HashSet<>();
        private int priority;
        private String endpoint;
        private boolean enabled;
        private long registerTime;
        
        public MCPToolInfo setBeanName(String beanName) {
            this.beanName = beanName;
            return this;
        }
        
        public MCPToolInfo setToolName(String toolName) {
            this.toolName = toolName;
            return this;
        }
        
        public MCPToolInfo setToolVersion(String toolVersion) {
            this.toolVersion = toolVersion;
            return this;
        }
        
        public MCPToolInfo setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public MCPToolInfo setInstance(Object instance) {
            this.instance = instance;
            return this;
        }
        
        public MCPToolInfo setCapabilities(Set<String> capabilities) {
            this.capabilities = capabilities;
            return this;
        }
        
        public MCPToolInfo setTags(Set<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public MCPToolInfo setPriority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public MCPToolInfo setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public MCPToolInfo setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public MCPToolInfo setRegisterTime(long registerTime) {
            this.registerTime = registerTime;
            return this;
        }
    }
}