package cn.lingque.cloud.node.bean;

import cn.hutool.json.JSONUtil;
import cn.lingque.util.LQUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 增强版节点信息，支持多协议和MCP工具注册
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Accessors(chain = true)
public class LQEnhancedNodeInfo {
    
    /** 服务名称 */
    private String serverName;
    
    /** 节点IP */
    private String nodeIp;
    
    /** 主端口 */
    private Integer nodePort;
    
    /** 协议类型：HTTP, SOCKET, GRPC, MCP */
    private String protocol = "HTTP";
    
    /** 支持的协议端口映射 */
    private Map<String, Integer> protocolPorts = new HashMap<>();
    
    /** 节点权重，用于负载均衡 */
    private Integer weight = 100;
    
    /** 节点状态：ONLINE, OFFLINE, MAINTENANCE */
    private String status = "ONLINE";
    
    /** 节点版本 */
    private String version = "1.0.0";
    
    /** 节点标签，用于服务发现过滤 */
    private Set<String> tags = new HashSet<>();
    
    /** 扩展元数据 */
    private Map<String, Object> metadata = new HashMap<>();
    
    /** 健康检查URL */
    private String healthCheckUrl;
    
    /** 健康检查间隔（秒） */
    private Integer healthCheckInterval = 30;
    
    /** MCP工具信息 */
    private MCPToolInfo mcpToolInfo;
    
    /** 节点容量信息 */
    private NodeCapacity capacity;
    
    /** 最后心跳时间 */
    private Long lastHeartbeat;
    
    /** 注册时间 */
    private Long registerTime;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LQEnhancedNodeInfo that = (LQEnhancedNodeInfo) o;
        return nodeId().equals(that.nodeId());
    }
    
    @Override
    public int hashCode() {
        return nodeId().hashCode();
    }
    
    public String nodeId() {
        return LQUtil.getMD5(serverName + ":" + nodeIp + ":" + nodePort + ":" + protocol);
    }
    
    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }
    
    /**
     * 获取指定协议的URI
     */
    public URI getUri(String targetProtocol) {
        Integer port = protocolPorts.getOrDefault(targetProtocol.toUpperCase(), nodePort);
        return URI.create(targetProtocol.toLowerCase() + "://" + nodeIp + ":" + port);
    }
    
    /**
     * 添加协议端口
     */
    public LQEnhancedNodeInfo addProtocolPort(String protocol, Integer port) {
        this.protocolPorts.put(protocol.toUpperCase(), port);
        return this;
    }
    
    /**
     * 添加标签
     */
    public LQEnhancedNodeInfo addTag(String tag) {
        this.tags.add(tag);
        return this;
    }
    
    /**
     * 添加元数据
     */
    public LQEnhancedNodeInfo addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * 检查是否支持指定协议
     */
    public boolean supportsProtocol(String protocol) {
        return this.protocol.equalsIgnoreCase(protocol) || 
               protocolPorts.containsKey(protocol.toUpperCase());
    }
    
    /**
     * 检查是否包含指定标签
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    
    /**
     * 检查节点是否健康
     */
    public boolean isHealthy() {
        return "ONLINE".equals(status) && 
               lastHeartbeat != null && 
               (System.currentTimeMillis() - lastHeartbeat) < (healthCheckInterval * 2000L);
    }
    
    /**
     * MCP工具信息
     */
    @Data
    @Accessors(chain = true)
    public static class MCPToolInfo {
        /** MCP工具名称 */
        private String toolName;
        
        /** MCP工具版本 */
        private String toolVersion;
        
        /** 支持的MCP操作 */
        private Set<String> supportedOperations = new HashSet<>();
        
        /** MCP配置 */
        private Map<String, Object> mcpConfig = new HashMap<>();
        
        /** MCP端点 */
        private String mcpEndpoint;
    }
    
    /**
     * 节点容量信息
     */
    @Data
    @Accessors(chain = true)
    public static class NodeCapacity {
        /** CPU核心数 */
        private Integer cpuCores;
        
        /** 内存大小(MB) */
        private Long memoryMB;
        
        /** 磁盘大小(GB) */
        private Long diskGB;
        
        /** 最大并发连接数 */
        private Integer maxConnections;
        
        /** 当前连接数 */
        private Integer currentConnections = 0;
        
        /** CPU使用率(%) */
        private Double cpuUsage = 0.0;
        
        /** 内存使用率(%) */
        private Double memoryUsage = 0.0;
        
        /** 磁盘使用率(%) */
        private Double diskUsage = 0.0;
    }
}