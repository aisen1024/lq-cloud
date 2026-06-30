package cn.lingque.cloud.mcp.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * LQ MCP工具客户端
 * 用于连接MCP服务器并调用远程工具
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQMCPToolClient {

    private final String serverHost;
    private final int serverPort;
    private final int connectTimeout;
    private final int readTimeout;

    public LQMCPToolClient(String serverHost, int serverPort) {
        this(serverHost, serverPort, 5000, 30000);
    }

    public LQMCPToolClient(String serverHost, int serverPort, int connectTimeout, int readTimeout) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * 通过HTTP调用工具
     */
    public LQMCPToolManager.MCPToolResponse callToolHttp(String toolName, String methodName, Map<String, Object> parameters) {
        try {
            String url = String.format("http://%s:%d/mcp/tools/%s/%s", serverHost, serverPort, toolName, methodName);
            String requestBody = JSONUtil.toJsonStr(parameters);
            
            log.debug("[LQ-MCP-Client] HTTP调用工具: {} -> {}", url, requestBody);
            
            HttpResponse response = HttpRequest.post(url)
                    .body(requestBody)
                    .contentType("application/json")
                    .timeout(readTimeout)
                    .execute();
            
            if (response.getStatus() == 200) {
                String responseBody = response.body();
                return JSONUtil.toBean(responseBody, LQMCPToolManager.MCPToolResponse.class);
            } else {
                log.error("[LQ-MCP-Client] HTTP调用失败: {} - {}", response.getStatus(), response.body());
                return createErrorResponse("HTTP调用失败: " + response.getStatus() + " - " + response.body());
            }
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Client] HTTP调用工具失败", e);
            return createErrorResponse("HTTP调用异常: " + e.getMessage());
        }
    }

    /**
     * 通过TCP调用工具
     */
    public LQMCPToolManager.MCPToolResponse callToolTcp(String toolName, String methodName, Map<String, Object> parameters) {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // 设置超时
            socket.setSoTimeout(readTimeout);
            
            // 构建请求
            String parametersJson = JSONUtil.toJsonStr(parameters);
            String request = String.format("%s:%s:%s", toolName, methodName, parametersJson);
            
            log.debug("[LQ-MCP-Client] TCP调用工具: {}", request);
            
            // 发送请求
            writer.println(request);
            
            // 读取响应
            String response = reader.readLine();
            if (response == null) {
                return createErrorResponse("服务器无响应");
            }
            
            log.debug("[LQ-MCP-Client] TCP响应: {}", response);
            
            // 解析响应
            if (response.startsWith("SUCCESS:")) {
                String responseJson = response.substring(8);
                return JSONUtil.toBean(responseJson, LQMCPToolManager.MCPToolResponse.class);
            } else if (response.startsWith("ERROR:")) {
                String errorMessage = response.substring(6);
                return createErrorResponse(errorMessage);
            } else {
                return createErrorResponse("未知响应格式: " + response);
            }
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Client] TCP调用工具失败", e);
            return createErrorResponse("TCP调用异常: " + e.getMessage());
        }
    }

    /**
     * 异步HTTP调用工具
     */
    public CompletableFuture<LQMCPToolManager.MCPToolResponse> callToolHttpAsync(String toolName, String methodName, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> callToolHttp(toolName, methodName, parameters));
    }

    /**
     * 异步TCP调用工具
     */
    public CompletableFuture<LQMCPToolManager.MCPToolResponse> callToolTcpAsync(String toolName, String methodName, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> callToolTcp(toolName, methodName, parameters));
    }

    /**
     * 获取服务器健康状态
     */
    public Map<String, Object> getServerHealth() {
        try {
            String url = String.format("http://%s:%d/mcp/health", serverHost, serverPort);
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(connectTimeout)
                    .execute();
            
            if (response.getStatus() == 200) {
                return JSONUtil.toBean(response.body(), Map.class);
            } else {
                return Map.of("status", "DOWN", "error", "HTTP " + response.getStatus());
            }
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Client] 获取服务器健康状态失败", e);
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    /**
     * 获取服务器工具列表
     */
    public Map<String, Object> getServerTools() {
        try {
            String url = String.format("http://%s:%d/mcp/tools", serverHost, serverPort);
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(connectTimeout)
                    .execute();
            
            if (response.getStatus() == 200) {
                return JSONUtil.toBean(response.body(), Map.class);
            } else {
                return Map.of("error", "HTTP " + response.getStatus());
            }
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Client] 获取服务器工具列表失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 测试连接
     */
    public boolean testConnection() {
        try {
            Map<String, Object> health = getServerHealth();
            return "UP".equals(health.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 批量调用工具
     */
    public CompletableFuture<Map<String, LQMCPToolManager.MCPToolResponse>> callToolsBatch(
            Map<String, Map<String, Object>> toolCalls) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, LQMCPToolManager.MCPToolResponse> results = new java.util.HashMap<>();
            
            for (Map.Entry<String, Map<String, Object>> entry : toolCalls.entrySet()) {
                String toolKey = entry.getKey();
                Map<String, Object> callInfo = entry.getValue();
                
                String toolName = (String) callInfo.get("toolName");
                String methodName = (String) callInfo.get("methodName");
                Map<String, Object> parameters = (Map<String, Object>) callInfo.getOrDefault("parameters", Map.of());
                
                try {
                    LQMCPToolManager.MCPToolResponse response = callToolHttp(toolName, methodName, parameters);
                    results.put(toolKey, response);
                } catch (Exception e) {
                    log.error("[LQ-MCP-Client] 批量调用工具失败: {}", toolKey, e);
                    results.put(toolKey, createErrorResponse("批量调用异常: " + e.getMessage()));
                }
            }
            
            return results;
        });
    }

    /**
     * 创建错误响应
     */
    private LQMCPToolManager.MCPToolResponse createErrorResponse(String errorMessage) {
        LQMCPToolManager.MCPToolResponse response = new LQMCPToolManager.MCPToolResponse();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * 获取服务器地址
     */
    public String getServerAddress() {
        return serverHost + ":" + serverPort;
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private String serverHost = "localhost";
        private int serverPort = 8080;
        private int connectTimeout = 5000;
        private int readTimeout = 30000;

        public Builder serverHost(String serverHost) {
            this.serverHost = serverHost;
            return this;
        }

        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public LQMCPToolClient build() {
            return new LQMCPToolClient(serverHost, serverPort, connectTimeout, readTimeout);
        }
    }
}