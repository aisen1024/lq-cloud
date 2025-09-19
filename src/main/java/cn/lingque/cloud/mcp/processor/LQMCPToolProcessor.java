package cn.lingque.cloud.mcp.processor;

import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.mcp.annotation.MCPToolMethod;
import cn.lingque.cloud.mcp.registry.LQMCPToolRegistry;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * LQ MCP工具处理器
 * 负责处理MCP工具的调用、参数转换、结果处理等
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
public class LQMCPToolProcessor {

    @Autowired
    private LQMCPToolRegistry toolRegistry;

    /**
     * 调用结果缓存
     */
    private final Map<String, CachedResult> resultCache = new ConcurrentHashMap<>();

    /**
     * 处理MCP工具调用
     */
    public LQMCPToolManager.MCPToolResponse processToolCall(String toolName, String methodName, 
                                                           LQMCPToolManager.MCPToolRequest request) {
        try {
            // 1. 获取工具信息
            LQMCPToolRegistry.MCPToolInfo toolInfo = toolRegistry.getToolInfo(toolName);
            if (toolInfo == null) {
                return LQMCPToolManager.MCPToolResponse.error("工具未找到: " + toolName);
            }

            if (!toolInfo.isEnabled()) {
                return LQMCPToolManager.MCPToolResponse.error("工具已禁用: " + toolName);
            }

            // 2. 获取方法
            Method method = toolRegistry.getToolMethod(toolName, methodName);
            if (method == null) {
                // 如果没有指定方法名，尝试查找默认方法
                method = findDefaultMethod(toolName);
                if (method == null) {
                    return LQMCPToolManager.MCPToolResponse.error(
                            String.format("方法未找到: %s.%s", toolName, methodName));
                }
            }

            // 3. 检查缓存
            MCPToolMethod mcpMethod = method.getAnnotation(MCPToolMethod.class);
            if (mcpMethod != null && mcpMethod.cacheable()) {
                String cacheKey = buildCacheKey(toolName, methodName, request);
                CachedResult cached = resultCache.get(cacheKey);
                if (cached != null && !cached.isExpired()) {
                    log.debug("[LQ-MCP] 返回缓存结果: {}", cacheKey);
                    return LQMCPToolManager.MCPToolResponse.success("缓存结果", cached.getResult());
                }
            }

            // 4. 执行方法调用
            Object result = invokeMethod(toolInfo.getInstance(), method, request);

            // 5. 缓存结果
            if (mcpMethod != null && mcpMethod.cacheable()) {
                String cacheKey = buildCacheKey(toolName, methodName, request);
                resultCache.put(cacheKey, new CachedResult(result, mcpMethod.cacheTime()));
            }

            // 6. 返回结果
            return LQMCPToolManager.MCPToolResponse.success("调用成功", result);

        } catch (Exception e) {
            log.error("[LQ-MCP] 工具调用失败: {}.{}", toolName, methodName, e);
            return LQMCPToolManager.MCPToolResponse.error("调用失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理MCP工具调用
     */
    public CompletableFuture<LQMCPToolManager.MCPToolResponse> processToolCallAsync(
            String toolName, String methodName, LQMCPToolManager.MCPToolRequest request) {
        
        return CompletableFuture.supplyAsync(() -> processToolCall(toolName, methodName, request))
                .orTimeout(request.getTimeout(), TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    log.error("[LQ-MCP] 异步工具调用失败: {}.{}", toolName, methodName, throwable);
                    return LQMCPToolManager.MCPToolResponse.error("异步调用失败: " + throwable.getMessage());
                });
    }

    /**
     * 查找默认方法
     */
    private Method findDefaultMethod(String toolName) {
        LQMCPToolRegistry.MCPToolInfo toolInfo = toolRegistry.getToolInfo(toolName);
        if (toolInfo == null) {
            return null;
        }

        Class<?> toolClass = toolInfo.getInstance().getClass();
        for (Method method : toolClass.getDeclaredMethods()) {
            MCPToolMethod mcpMethod = method.getAnnotation(MCPToolMethod.class);
            if (mcpMethod != null && mcpMethod.isDefault()) {
                return method;
            }
        }

        return null;
    }

    /**
     * 执行方法调用
     */
    private Object invokeMethod(Object instance, Method method, LQMCPToolManager.MCPToolRequest request) 
            throws Exception {
        
        // 1. 准备方法参数
        Object[] args = prepareMethodArguments(method, request);
        
        // 2. 设置方法可访问
        method.setAccessible(true);
        
        // 3. 执行方法
        long startTime = System.currentTimeMillis();
        Object result = method.invoke(instance, args);
        long endTime = System.currentTimeMillis();
        
        log.debug("[LQ-MCP] 方法执行完成: {}, 耗时: {}ms", method.getName(), endTime - startTime);
        
        return result;
    }

    /**
     * 准备方法参数
     */
    private Object[] prepareMethodArguments(Method method, LQMCPToolManager.MCPToolRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            String paramName = param.getName();
            
            // 特殊处理请求对象
            if (paramType.isAssignableFrom(LQMCPToolManager.MCPToolRequest.class)) {
                args[i] = request;
                continue;
            }
            
            // 从请求参数中获取值
            Object paramValue = request.getParameters().get(paramName);
            if (paramValue == null) {
                // 尝试使用参数索引
                paramValue = request.getParameters().get("arg" + i);
            }
            
            // 类型转换
            args[i] = convertParameterValue(paramValue, paramType);
        }
        
        return args;
    }

    /**
     * 参数值类型转换
     */
    private Object convertParameterValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // 如果类型匹配，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // 基础类型转换
        try {
            if (targetType == String.class) {
                return value.toString();
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(value.toString());
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(value.toString());
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(value.toString());
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.valueOf(value.toString());
            } else if (targetType == Map.class) {
                if (value instanceof String) {
                    return JSONUtil.toBean(value.toString(), Map.class);
                }
                return value;
            } else if (targetType == List.class) {
                if (value instanceof String) {
                    return JSONUtil.toBean(value.toString(), List.class);
                }
                return value;
            } else {
                // 尝试JSON转换
                if (value instanceof String) {
                    return JSONUtil.toBean(value.toString(), targetType);
                }
                return value;
            }
        } catch (Exception e) {
            log.warn("[LQ-MCP] 参数类型转换失败: {} -> {}", value.getClass(), targetType, e);
            return value;
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String toolName, String methodName, LQMCPToolManager.MCPToolRequest request) {
        return String.format("%s:%s:%s", toolName, methodName, 
                JSONUtil.toJsonStr(request.getParameters()).hashCode());
    }

    /**
     * 清理过期缓存
     */
    public void cleanExpiredCache() {
        resultCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("[LQ-MCP] 清理过期缓存完成，当前缓存数量: {}", resultCache.size());
    }

    /**
     * 获取缓存统计
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCached", resultCache.size());
        stats.put("expiredCount", resultCache.values().stream().mapToLong(c -> c.isExpired() ? 1 : 0).sum());
        return stats;
    }

    /**
     * 缓存结果
     */
    @Data
    private static class CachedResult {
        private final Object result;
        private final long expireTime;
        
        public CachedResult(Object result, int cacheTimeSeconds) {
            this.result = result;
            this.expireTime = System.currentTimeMillis() + (cacheTimeSeconds * 1000L);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}