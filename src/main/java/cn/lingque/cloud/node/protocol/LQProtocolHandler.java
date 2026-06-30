package cn.lingque.cloud.node.protocol;

import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多协议处理器 - 支持HTTP和Socket协议的服务发现
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQProtocolHandler {
    
    private static final Map<String, ProtocolServer> protocolServers = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 启动协议服务器
     */
    public static void startProtocolServers(int httpPort, int socketPort) {
        // 启动HTTP服务器
        startHttpServer(httpPort);
        
        // 启动Socket服务器
        startSocketServer(socketPort);
        
        log.info("协议服务器启动完成 - HTTP端口: {}, Socket端口: {}", httpPort, socketPort);
    }
    
    /**
     * 启动HTTP服务器
     */
    private static void startHttpServer(int port) {
        executorService.submit(() -> {
            try {
                HttpServer httpServer = new HttpServer(port);
                protocolServers.put("HTTP", httpServer);
                httpServer.start();
            } catch (Exception e) {
                log.error("启动HTTP服务器失败", e);
            }
        });
    }
    
    /**
     * 启动Socket服务器
     */
    private static void startSocketServer(int port) {
        executorService.submit(() -> {
            try {
                SocketServer socketServer = new SocketServer(port);
                protocolServers.put("SOCKET", socketServer);
                socketServer.start();
            } catch (Exception e) {
                log.error("启动Socket服务器失败", e);
            }
        });
    }
    
    /**
     * 停止所有协议服务器
     */
    public static void stopAllServers() {
        protocolServers.values().forEach(ProtocolServer::stop);
        executorService.shutdown();
    }
    
    /**
     * 协议服务器接口
     */
    interface ProtocolServer {
        void start();
        void stop();
    }
    
    /**
     * HTTP服务器实现
     */
    static class HttpServer implements ProtocolServer {
        private final int port;
        private ServerSocket serverSocket;
        private volatile boolean running = false;
        
        public HttpServer(int port) {
            this.port = port;
        }
        
        @Override
        public void start() {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                log.info("HTTP服务器启动在端口: {}", port);
                
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleHttpRequest(clientSocket));
                }
            } catch (IOException e) {
                if (running) {
                    log.error("HTTP服务器运行异常", e);
                }
            }
        }
        
        @Override
        public void stop() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log.error("关闭HTTP服务器异常", e);
            }
        }
        
        private void handleHttpRequest(Socket clientSocket) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String requestLine = in.readLine();
                if (requestLine == null) return;
                
                String[] parts = requestLine.split(" ");
                if (parts.length < 2) return;
                
                String method = parts[0];
                String path = parts[1];
                
                // 跳过HTTP头
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    // 跳过头部
                }
                
                String response = processHttpRequest(method, path);
                
                // 发送HTTP响应
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json; charset=utf-8");
                out.println("Content-Length: " + response.getBytes("UTF-8").length);
                out.println("Access-Control-Allow-Origin: *");
                out.println();
                out.println(response);
                
            } catch (Exception e) {
                log.error("处理HTTP请求异常", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    log.error("关闭客户端连接异常", e);
                }
            }
        }
        
        private String processHttpRequest(String method, String path) {
            try {
                if ("GET".equals(method)) {
                    if (path.startsWith("/services/")) {
                        // 获取服务列表: /services/{serviceName}
                        String serviceName = path.substring(10);
                        List<LQEnhancedNodeInfo> nodes = LQEnhancedRegisterCenter.getEnhancedNodeList(serviceName);
                        return JSONUtil.toJsonStr(Map.of("code", 200, "data", nodes, "message", "success"));
                    } else if (path.startsWith("/services/") && path.contains("/protocol/")) {
                        // 按协议获取服务: /services/{serviceName}/protocol/{protocol}
                        String[] pathParts = path.split("/");
                        if (pathParts.length >= 5) {
                            String serviceName = pathParts[2];
                            String protocol = pathParts[4];
                            List<LQEnhancedNodeInfo> nodes = LQEnhancedRegisterCenter.getNodesByProtocol(serviceName, protocol);
                            return JSONUtil.toJsonStr(Map.of("code", 200, "data", nodes, "message", "success"));
                        }
                    } else if ("/health".equals(path)) {
                        // 健康检查
                        return JSONUtil.toJsonStr(Map.of("code", 200, "status", "healthy", "timestamp", System.currentTimeMillis()));
                    } else if ("/all-services".equals(path)) {
                        // 获取所有服务
                        return JSONUtil.toJsonStr(Map.of("code", 200, "data", LQEnhancedRegisterCenter.getAllEnhancedNodeList(), "message", "success"));
                    }
                }
                
                return JSONUtil.toJsonStr(Map.of("code", 404, "message", "Not Found"));
            } catch (Exception e) {
                log.error("处理HTTP请求业务逻辑异常", e);
                return JSONUtil.toJsonStr(Map.of("code", 500, "message", "Internal Server Error"));
            }
        }
    }
    
    /**
     * Socket服务器实现
     */
    static class SocketServer implements ProtocolServer {
        private final int port;
        private ServerSocket serverSocket;
        private volatile boolean running = false;
        
        public SocketServer(int port) {
            this.port = port;
        }
        
        @Override
        public void start() {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                log.info("Socket服务器启动在端口: {}", port);
                
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleSocketRequest(clientSocket));
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Socket服务器运行异常", e);
                }
            }
        }
        
        @Override
        public void stop() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log.error("关闭Socket服务器异常", e);
            }
        }
        
        private void handleSocketRequest(Socket clientSocket) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String request = in.readLine();
                if (request == null) return;
                
                String response = processSocketRequest(request);
                out.println(response);
                
            } catch (Exception e) {
                log.error("处理Socket请求异常", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    log.error("关闭Socket客户端连接异常", e);
                }
            }
        }
        
        private String processSocketRequest(String request) {
            try {
                // Socket协议格式: COMMAND|PARAM1|PARAM2|...
                String[] parts = request.split("\\|");
                if (parts.length == 0) {
                    return "ERROR|Invalid request format";
                }
                
                String command = parts[0];
                
                switch (command) {
                    case "GET_SERVICES":
                        if (parts.length >= 2) {
                            String serviceName = parts[1];
                            List<LQEnhancedNodeInfo> nodes = LQEnhancedRegisterCenter.getEnhancedNodeList(serviceName);
                            return "SUCCESS|" + JSONUtil.toJsonStr(nodes);
                        }
                        break;
                    case "GET_SERVICES_BY_PROTOCOL":
                        if (parts.length >= 3) {
                            String serviceName = parts[1];
                            String protocol = parts[2];
                            List<LQEnhancedNodeInfo> nodes = LQEnhancedRegisterCenter.getNodesByProtocol(serviceName, protocol);
                            return "SUCCESS|" + JSONUtil.toJsonStr(nodes);
                        }
                        break;
                    case "HEALTH_CHECK":
                        return "SUCCESS|HEALTHY|" + System.currentTimeMillis();
                    case "GET_ALL_SERVICES":
                        return "SUCCESS|" + JSONUtil.toJsonStr(LQEnhancedRegisterCenter.getAllEnhancedNodeList());
                    default:
                        return "ERROR|Unknown command: " + command;
                }
                
                return "ERROR|Invalid parameters";
            } catch (Exception e) {
                log.error("处理Socket请求业务逻辑异常", e);
                return "ERROR|Internal server error";
            }
        }
    }
}