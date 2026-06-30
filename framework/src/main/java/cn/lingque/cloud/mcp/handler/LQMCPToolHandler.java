package cn.lingque.cloud.mcp.handler;

import cn.lingque.cloud.mcp.processor.LQMCPToolProcessor;
import cn.lingque.cloud.mcp.registry.LQMCPToolRegistry;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * LQ MCP工具处理器
 * 提供统一的MCP工具调用入口和管理功能
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
public class LQMCPToolHandler {

    @Autowired
    private LQMCPToolRegistry toolRegistry;

    @Autowired
    private LQMCPToolProcessor toolProcessor;

    /**
     * 调用MCP工具
     */
    public LQMCPToolManager.MCPToolResponse callTool(String toolName, String methodName, 
                                                     Map<String, Object> parameters) {
        
        LQMCPToolManager.MCPToolRequest request = new LQMCPToolManager.MCPToolRequest()
                .setRequestId("req-" + System.currentTimeMillis());
        request.getParameters().putAll(parameters);
        
        return toolProcessor.processToolCall(toolName, methodName, request);
    }

    /**
     * 调用MCP工具（使用默认方法）
     */
    public LQMCPToolManager.MCPToolResponse callTool(String toolName, Map<String, Object> parameters) {
        return callTool(toolName, null, parameters);
    }

    /**
     * 异步调用MCP工具
     */
    public CompletableFuture<LQMCPToolManager.MCPToolResponse> callToolAsync(
            String toolName, String methodName, Map<String, Object> parameters) {
        
        LQMCPToolManager.MCPToolRequest request = new LQMCPToolManager.MCPToolRequest()
                .setRequestId("async-req-" + System.currentTimeMillis());
        request.getParameters().putAll(parameters);
        
        return toolProcessor.processToolCallAsync(toolName, methodName, request);
    }

    /**
     * 异步调用MCP工具（使用默认方法）
     */
    public CompletableFuture<LQMCPToolManager.MCPToolResponse> callToolAsync(
            String toolName, Map<String, Object> parameters) {
        return callToolAsync(toolName, null, parameters);
    }

    /**
     * 批量调用MCP工具
     */
    public Map<String, LQMCPToolManager.MCPToolResponse> callToolsBatch(
            Map<String, Map<String, Object>> toolCalls) {
        
        Map<String, LQMCPToolManager.MCPToolResponse> results = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : toolCalls.entrySet()) {
            String toolName = entry.getKey();
            Map<String, Object> parameters = entry.getValue();
            
            try {
                LQMCPToolManager.MCPToolResponse response = callTool(toolName, parameters);
                results.put(toolName, response);
            } catch (Exception e) {
                log.error("[LQ-MCP] 批量调用工具失败: {}", toolName, e);
                results.put(toolName, LQMCPToolManager.MCPToolResponse.error(
                        "批量调用失败: " + e.getMessage()));
            }
        }
        
        return results;
    }

    /**
     * 异步批量调用MCP工具
     */
    public CompletableFuture<Map<String, LQMCPToolManager.MCPToolResponse>> callToolsBatchAsync(
            Map<String, Map<String, Object>> toolCalls) {
        
        List<CompletableFuture<AbstractMap.SimpleEntry<String, LQMCPToolManager.MCPToolResponse>>> futures = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : toolCalls.entrySet()) {
            String toolName = entry.getKey();
            Map<String, Object> parameters = entry.getValue();
            
            CompletableFuture<AbstractMap.SimpleEntry<String, LQMCPToolManager.MCPToolResponse>> future = 
                    callToolAsync(toolName, parameters)
                            .thenApply(response -> new AbstractMap.SimpleEntry<>(toolName, response))
                            .exceptionally(throwable -> {
                                log.error("[LQ-MCP] 异步批量调用工具失败: {}", toolName, throwable);
                                return new AbstractMap.SimpleEntry<>(toolName, 
                                        LQMCPToolManager.MCPToolResponse.error(
                                                "异步批量调用失败: " + throwable.getMessage()));
                            });
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, LQMCPToolManager.MCPToolResponse> results = new HashMap<>();
                    for (CompletableFuture<AbstractMap.SimpleEntry<String, LQMCPToolManager.MCPToolResponse>> future : futures) {
                        try {
                            AbstractMap.SimpleEntry<String, LQMCPToolManager.MCPToolResponse> entry = future.get();
                            results.put(entry.getKey(), entry.getValue());
                        } catch (Exception e) {
                            log.error("[LQ-MCP] 获取异步批量调用结果失败", e);
                        }
                    }
                    return results;
                });
    }

    /**
     * 按能力查找并调用工具
     */
    public List<LQMCPToolManager.MCPToolResponse> callToolsByCapability(
            String capability, Map<String, Object> parameters) {
        
        Set<String> toolNames = toolRegistry.getToolsByCapability(capability);
        List<LQMCPToolManager.MCPToolResponse> results = new ArrayList<>();
        
        for (String toolName : toolNames) {
            try {
                LQMCPToolManager.MCPToolResponse response = callTool(toolName, parameters);
                results.add(response);
            } catch (Exception e) {
                log.error("[LQ-MCP] 按能力调用工具失败: {} ({})", toolName, capability, e);
                results.add(LQMCPToolManager.MCPToolResponse.error(
                        "按能力调用失败: " + e.getMessage()));
            }
        }
        
        return results;
    }

    /**
     * 获取工具列表
     */
    public List<ToolInfo> getAvailableTools() {
        List<ToolInfo> tools = new ArrayList<>();
        
        for (LQMCPToolRegistry.MCPToolInfo toolInfo : toolRegistry.getAllTools().values()) {
            if (toolInfo.isEnabled()) {
                tools.add(new ToolInfo(
                        toolInfo.getToolName(),
                        toolInfo.getToolVersion(),
                        toolInfo.getDescription(),
                        new ArrayList<>(toolInfo.getCapabilities()),
                        new ArrayList<>(toolInfo.getTags())
                ));
            }
        }
        
        return tools;
    }

    /**
     * 获取工具详细信息
     */
    public ToolDetailInfo getToolDetail(String toolName) {
        LQMCPToolRegistry.MCPToolInfo toolInfo = toolRegistry.getToolInfo(toolName);
        if (toolInfo == null) {
            return null;
        }
        
        return new ToolDetailInfo(
                toolInfo.getToolName(),
                toolInfo.getToolVersion(),
                toolInfo.getDescription(),
                new ArrayList<>(toolInfo.getCapabilities()),
                new ArrayList<>(toolInfo.getTags()),
                toolInfo.getPriority(),
                toolInfo.getEndpoint(),
                toolInfo.isEnabled(),
                toolInfo.getRegisterTime()
        );
    }

    /**
     * 检查工具健康状态
     */
    public Map<String, Object> checkToolHealth(String toolName) {
        Map<String, Object> health = new HashMap<>();
        
        LQMCPToolRegistry.MCPToolInfo toolInfo = toolRegistry.getToolInfo(toolName);
        if (toolInfo == null) {
            health.put("status", "NOT_FOUND");
            health.put("message", "工具未找到");
            return health;
        }
        
        health.put("status", toolInfo.isEnabled() ? "UP" : "DOWN");
        health.put("toolName", toolInfo.getToolName());
        health.put("version", toolInfo.getToolVersion());
        health.put("registerTime", toolInfo.getRegisterTime());
        health.put("uptime", System.currentTimeMillis() - toolInfo.getRegisterTime());
        
        return health;
    }

    /**
     * 获取所有工具健康状态
     */
    public Map<String, Map<String, Object>> checkAllToolsHealth() {
        Map<String, Map<String, Object>> healthMap = new HashMap<>();
        
        for (String toolName : toolRegistry.getAllTools().keySet()) {
            healthMap.put(toolName, checkToolHealth(toolName));
        }
        
        return healthMap;
    }

    /**
     * 工具信息
     */
    public static class ToolInfo {
        public final String name;
        public final String version;
        public final String description;
        public final List<String> capabilities;
        public final List<String> tags;
        
        public ToolInfo(String name, String version, String description, 
                       List<String> capabilities, List<String> tags) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.capabilities = capabilities;
            this.tags = tags;
        }
    }

    /**
     * 工具详细信息
     */
    public static class ToolDetailInfo extends ToolInfo {
        public final int priority;
        public final String endpoint;
        public final boolean enabled;
        public final long registerTime;
        
        public ToolDetailInfo(String name, String version, String description,
                             List<String> capabilities, List<String> tags,
                             int priority, String endpoint, boolean enabled, long registerTime) {
            super(name, version, description, capabilities, tags);
            this.priority = priority;
            this.endpoint = endpoint;
            this.enabled = enabled;
            this.registerTime = registerTime;
        }
    }
}