package cn.lingque.cloud.mcp.server;

import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.mcp.LQMCPToolProperties;
import cn.lingque.cloud.mcp.processor.LQMCPToolProcessor;
import cn.lingque.cloud.mcp.registry.LQMCPToolRegistry;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LQ MCP工具服务器
 * 提供HTTP/TCP接口供外部调用MCP工具
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQMCPToolServer {

    private final LQMCPToolProperties properties;
    private final LQMCPToolRegistry toolRegistry;
    private final LQMCPToolProcessor toolProcessor;
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public LQMCPToolServer(LQMCPToolProperties properties, 
                          LQMCPToolRegistry toolRegistry,
                          LQMCPToolProcessor toolProcessor) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.toolProcessor = toolProcessor;
    }

    /**
     * 启动服务器
     */
    public void start() {
        if (running.get()) {
            log.warn("[LQ-MCP-Server] 服务器已经在运行中");
            return;
        }

        try {
            // 创建服务器套接字
            serverSocket = new ServerSocket(properties.getServer().getPort());
            
            // 创建线程池
            executorService = Executors.newFixedThreadPool(properties.getServer().getMaxConnections());
            
            running.set(true);
            
            log.info("[LQ-MCP-Server] 服务器启动成功，监听端口: {}", properties.getServer().getPort());
            
            // 启动接受连接的线程
            Thread acceptThread = new Thread(this::acceptConnections);
            acceptThread.setDaemon(true);
            acceptThread.setName("MCP-Server-Accept");
            acceptThread.start();
            
        } catch (IOException e) {
            log.error("[LQ-MCP-Server] 服务器启动失败", e);
            throw new RuntimeException("MCP服务器启动失败", e);
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            
            log.info("[LQ-MCP-Server] 服务器已停止");
            
        } catch (IOException e) {
            log.error("[LQ-MCP-Server] 服务器停止时发生错误", e);
        }
    }

    /**
     * 接受客户端连接
     */
    private void acceptConnections() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running.get()) {
                    log.error("[LQ-MCP-Server] 接受客户端连接失败", e);
                }
            }
        }
    }

    /**
     * 处理客户端请求
     */
    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            // 读取请求
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            
            log.debug("[LQ-MCP-Server] 收到请求: {}", requestLine);
            
            // 解析HTTP请求
            if (requestLine.startsWith("GET") || requestLine.startsWith("POST")) {
                handleHttpRequest(requestLine, reader, writer);
            } else {
                // 处理原始TCP请求
                handleTcpRequest(requestLine, reader, writer);
            }
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Server] 处理客户端请求失败", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("[LQ-MCP-Server] 关闭客户端连接失败", e);
            }
        }
    }

    /**
     * 处理HTTP请求
     */
    private void handleHttpRequest(String requestLine, BufferedReader reader, PrintWriter writer) 
            throws IOException {
        
        // 解析请求行
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendHttpResponse(writer, 400, "Bad Request", "Invalid request line");
            return;
        }
        
        String method = parts[0];
        String path = parts[1];
        
        // 读取请求头
        String line;
        int contentLength = 0;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }
        
        // 读取请求体
        String requestBody = "";
        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            reader.read(buffer, 0, contentLength);
            requestBody = new String(buffer);
        }
        
        // 路由处理
        if (path.startsWith("/mcp/tools/")) {
            handleToolRequest(method, path, requestBody, writer);
        } else if (path.equals("/mcp/health")) {
            handleHealthCheck(writer);
        } else if (path.equals("/mcp/tools")) {
            handleToolList(writer);
        } else {
            sendHttpResponse(writer, 404, "Not Found", "Path not found: " + path);
        }
    }

    /**
     * 处理工具调用请求
     */
    private void handleToolRequest(String method, String path, String requestBody, PrintWriter writer) {
        try {
            // 解析路径: /mcp/tools/{toolName}/{methodName}
            String[] pathParts = path.split("/");
            if (pathParts.length < 4) {
                sendHttpResponse(writer, 400, "Bad Request", "Invalid tool path");
                return;
            }
            
            String toolName = pathParts[3];
            String methodName = pathParts.length > 4 ? pathParts[4] : null;
            
            // 解析请求参数
            Map<String, Object> parameters;
            if (!requestBody.isEmpty()) {
                parameters = JSONUtil.toBean(requestBody, Map.class);
            } else {
                parameters = Map.of();
            }
            
            // 创建请求对象
            LQMCPToolManager.MCPToolRequest request = new LQMCPToolManager.MCPToolRequest()
                    .setRequestId("http-" + System.currentTimeMillis())
                    .setTimeout(properties.getServer().getRequestTimeout());
            request.getParameters().putAll(parameters);
            
            // 调用工具
            LQMCPToolManager.MCPToolResponse response = toolProcessor.processToolCall(toolName, methodName, request);
            
            // 返回响应
            String responseJson = JSONUtil.toJsonStr(response);
            sendHttpResponse(writer, 200, "OK", responseJson);
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Server] 处理工具请求失败", e);
            sendHttpResponse(writer, 500, "Internal Server Error", "Tool call failed: " + e.getMessage());
        }
    }

    /**
     * 处理健康检查
     */
    private void handleHealthCheck(PrintWriter writer) {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "server", "LQ-MCP-Server",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis(),
                "tools", toolRegistry.getRegisteredToolCount()
        );
        
        String healthJson = JSONUtil.toJsonStr(health);
        sendHttpResponse(writer, 200, "OK", healthJson);
    }

    /**
     * 处理工具列表请求
     */
    private void handleToolList(PrintWriter writer) {
        Map<String, Object> toolList = Map.of(
                "tools", toolRegistry.getAllTools().keySet(),
                "count", toolRegistry.getRegisteredToolCount(),
                "stats", toolRegistry.getToolStats()
        );
        
        String toolListJson = JSONUtil.toJsonStr(toolList);
        sendHttpResponse(writer, 200, "OK", toolListJson);
    }

    /**
     * 处理TCP请求
     */
    private void handleTcpRequest(String requestLine, BufferedReader reader, PrintWriter writer) {
        try {
            // 解析TCP请求格式: TOOL_NAME:METHOD_NAME:PARAMETERS_JSON
            String[] parts = requestLine.split(":", 3);
            if (parts.length < 2) {
                writer.println("ERROR:Invalid request format");
                return;
            }
            
            String toolName = parts[0];
            String methodName = parts[1];
            String parametersJson = parts.length > 2 ? parts[2] : "{}";
            
            // 解析参数
            Map<String, Object> parameters = JSONUtil.toBean(parametersJson, Map.class);
            
            // 创建请求对象
            LQMCPToolManager.MCPToolRequest request = new LQMCPToolManager.MCPToolRequest()
                    .setRequestId("tcp-" + System.currentTimeMillis())
                    .setTimeout(properties.getServer().getRequestTimeout());
            request.getParameters().putAll(parameters);
            
            // 调用工具
            LQMCPToolManager.MCPToolResponse response = toolProcessor.processToolCall(toolName, methodName, request);
            
            // 返回响应
            String responseJson = JSONUtil.toJsonStr(response);
            writer.println("SUCCESS:" + responseJson);
            
        } catch (Exception e) {
            log.error("[LQ-MCP-Server] 处理TCP请求失败", e);
            writer.println("ERROR:" + e.getMessage());
        }
    }

    /**
     * 发送HTTP响应
     */
    private void sendHttpResponse(PrintWriter writer, int statusCode, String statusText, String body) {
        writer.println("HTTP/1.1 " + statusCode + " " + statusText);
        writer.println("Content-Type: application/json; charset=utf-8");
        writer.println("Content-Length: " + body.getBytes().length);
        writer.println("Connection: close");
        writer.println();
        writer.println(body);
    }

    /**
     * 检查服务器是否运行
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }
}