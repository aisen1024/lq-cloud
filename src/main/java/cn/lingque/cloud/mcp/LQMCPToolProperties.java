package cn.lingque.cloud.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LQ MCP工具配置属性
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Component
@ConfigurationProperties(prefix = "lq.mcp")
public class LQMCPToolProperties {

    /**
     * 是否启用MCP工具模块
     */
    private boolean enabled = true;

    /**
     * 是否启用自动发现
     */
    private boolean autoDiscovery = true;

    /**
     * 是否启用健康检查
     */
    private boolean healthCheck = true;

    /**
     * 工具扫描包路径
     */
    private String[] scanPackages = {"cn.lingque"};

    /**
     * 服务器配置
     */
    private ServerConfig server = new ServerConfig();

    /**
     * 客户端配置
     */
    private ClientConfig client = new ClientConfig();

    /**
     * 注册中心配置
     */
    private RegistryConfig registry = new RegistryConfig();

    /**
     * 工具配置
     */
    private Map<String, Object> tools = new HashMap<>();

    /**
     * 服务器配置
     */
    @Data
    public static class ServerConfig {
        /**
         * 是否启用MCP服务器
         */
        private boolean enabled = true;

        /**
         * 服务器端口
         */
        private int port = 8090;

        /**
         * 服务器主机
         */
        private String host = "0.0.0.0";

        /**
         * 最大连接数
         */
        private int maxConnections = 100;

        /**
         * 连接超时时间(毫秒)
         */
        private long connectionTimeout = 30000;

        /**
         * 请求超时时间(毫秒)
         */
        private long requestTimeout = 60000;
    }

    /**
     * 客户端配置
     */
    @Data
    public static class ClientConfig {
        /**
         * 是否启用客户端
         */
        private boolean enabled = false;

        /**
         * 服务器主机
         */
        private String serverHost = "localhost";

        /**
         * 服务器端口
         */
        private int serverPort = 8080;

        /**
         * 连接超时时间(毫秒)
         */
        private long connectTimeout = 5000;

        /**
         * 读取超时时间(毫秒)
         */
        private long readTimeout = 30000;
    }

    /**
     * 注册中心配置
     */
    @Data
    public static class RegistryConfig {
        /**
         * 是否启用注册中心
         */
        private boolean enabled = true;

        /**
         * 注册中心地址
         */
        private String address = "localhost:6379";

        /**
         * 服务名称
         */
        private String serviceName = "mcp-tools";

        /**
         * 服务版本
         */
        private String serviceVersion = "1.0.0";

        /**
         * 心跳间隔(毫秒)
         */
        private long heartbeatInterval = 30000;

        /**
         * 服务权重
         */
        private int weight = 100;
    }

    /**
     * 获取配置摘要
     */
    public String getConfigSummary() {
        return String.format(
                "MCP配置摘要: enabled=%s, autoDiscovery=%s, healthCheck=%s, serverPort=%d, registryEnabled=%s",
                enabled, autoDiscovery, healthCheck, server.port, registry.enabled
        );
    }

    /**
     * 验证配置
     */
    public boolean validateConfig() {
        if (server.port <= 0 || server.port > 65535) {
            return false;
        }
        if (server.connectionTimeout <= 0 || server.requestTimeout <= 0) {
            return false;
        }
        if (registry.heartbeatInterval <= 0) {
            return false;
        }
        return true;
    }
}