package cn.lingque.cloud.node.example;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 增强版注册中心使用示例
 * 展示如何使用比Nacos更强大的注册中心功能
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQEnhancedRegisterExample {
    
    public static void main(String[] args) {
        // 启动注册中心
        LQEnhancedRegisterCenter.start();
        
        // 示例1: 注册HTTP服务节点
        registerHttpService();
        
        // 示例2: 注册多协议服务节点
        registerMultiProtocolService();
        
        // 示例3: 注册MCP工具服务
        registerMCPToolService();
        
        // 示例4: 服务发现
        discoverServices();
        
        // 示例5: MCP工具发现和调用
        discoverAndCallMCPTools();
    }
    
    /**
     * 注册HTTP服务节点
     */
    private static void registerHttpService() {
        log.info("=== 注册HTTP服务节点 ===");
        
        LQEnhancedNodeInfo httpNode = new LQEnhancedNodeInfo()
                .setServerName("user-service")
                .setNodeIp("192.168.1.100")
                .setNodePort(8080)
                .setProtocol("HTTP")
                .setWeight(100)
                .setVersion("1.0.0")
                .addTag("web")
                .addTag("api")
                .addMetadata("region", "us-west")
                .addMetadata("zone", "zone-a")
                .setHealthCheckUrl("/health")
                .setHealthCheckInterval(30);
        
        boolean success = LQEnhancedRegisterCenter.registerEnhancedNode(httpNode);
        log.info("HTTP服务注册结果: {}", success ? "成功" : "失败");
    }
    
    /**
     * 注册多协议服务节点
     */
    private static void registerMultiProtocolService() {
        log.info("=== 注册多协议服务节点 ===");
        
        LQEnhancedNodeInfo multiNode = new LQEnhancedNodeInfo()
                .setServerName("gateway-service")
                .setNodeIp("192.168.1.101")
                .setNodePort(8080)  // HTTP端口
                .addProtocolPort("HTTP", 8080)
                .addProtocolPort("SOCKET", 9090)
                .addProtocolPort("GRPC", 9091)
                .setWeight(150)
                .setVersion("2.0.0")
                .addTag("gateway")
                .addTag("proxy")
                .addMetadata("supports_ssl", true)
                .addMetadata("max_connections", 1000);
        
        // 设置节点容量信息
        LQEnhancedNodeInfo.NodeCapacity capacity = new LQEnhancedNodeInfo.NodeCapacity()
                .setCpuCores(8)
                .setMemoryMB(16384L)
                .setDiskGB(500L)
                .setMaxConnections(1000);
        multiNode.setCapacity(capacity);
        
        boolean success = LQEnhancedRegisterCenter.registerEnhancedNode(multiNode);
        log.info("多协议服务注册结果: {}", success ? "成功" : "失败");
    }
    
    /**
     * 注册MCP工具服务
     */
    private static void registerMCPToolService() {
        log.info("=== 注册MCP工具服务 ===");
        
        // 创建MCP工具信息
        LQEnhancedNodeInfo.MCPToolInfo mcpInfo = new LQEnhancedNodeInfo.MCPToolInfo()
                .setToolName("file-processor")
                .setToolVersion("1.0.0")
                .setMcpEndpoint("/mcp/tools/file-processor");
        mcpInfo.getSupportedOperations().add("read_file");
        mcpInfo.getSupportedOperations().add("write_file");
        mcpInfo.getSupportedOperations().add("list_files");
        mcpInfo.getMcpConfig().put("max_file_size", "10MB");
        mcpInfo.getMcpConfig().put("supported_formats", List.of("txt", "json", "xml"));
        
        LQEnhancedNodeInfo mcpNode = new LQEnhancedNodeInfo()
                .setServerName("mcp-tools-service")
                .setNodeIp("192.168.1.102")
                .setNodePort(8080)
                .setProtocol("MCP")
                .addProtocolPort("HTTP", 8080)
                .addProtocolPort("MCP", 8081)
                .setWeight(100)
                .setVersion("1.0.0")
                .addTag("mcp")
                .addTag("tools")
                .addTag("file-processing")
                .setMcpToolInfo(mcpInfo);
        
        boolean success = LQEnhancedRegisterCenter.registerMCPTool(mcpNode);
        log.info("MCP工具服务注册结果: {}", success ? "成功" : "失败");
        
        // 注册MCP工具定义
        LQMCPToolManager.MCPToolDefinition toolDef = new LQMCPToolManager.MCPToolDefinition()
                .setToolName("file-processor")
                .setToolVersion("1.0.0")
                .setDescription("文件处理工具，支持读取、写入和列出文件")
                .setProviderNode(mcpNode);
        toolDef.getCapabilities().add("file_operations");
        toolDef.getCapabilities().add("text_processing");
        
        boolean toolSuccess = LQMCPToolManager.registerMCPTool(toolDef, mcpNode);
        log.info("MCP工具定义注册结果: {}", toolSuccess ? "成功" : "失败");
    }
    
    /**
     * 服务发现示例
     */
    private static void discoverServices() {
        log.info("=== 服务发现示例 ===");
        
        // 1. 获取所有用户服务节点
        List<LQEnhancedNodeInfo> userServices = LQEnhancedRegisterCenter.getEnhancedNodeList("user-service");
        log.info("发现用户服务节点数量: {}", userServices.size());
        
        // 2. 按协议查找服务
        List<LQEnhancedNodeInfo> httpServices = LQEnhancedRegisterCenter.getNodesByProtocol("gateway-service", "HTTP");
        log.info("发现HTTP协议的网关服务节点数量: {}", httpServices.size());
        
        // 3. 按标签查找服务
        Set<String> tags = Set.of("web", "api");
        List<LQEnhancedNodeInfo> webServices = LQEnhancedRegisterCenter.getNodesByTags("user-service", tags);
        log.info("发现带有web和api标签的服务节点数量: {}", webServices.size());
        
        // 4. 使用负载均衡策略获取服务
        List<LQEnhancedNodeInfo> balancedServices = LQEnhancedRegisterCenter.getEnhancedNodeList(
                "gateway-service", 
                "HTTP", 
                Set.of("gateway"), 
                LQEnhancedRegisterCenter.LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN
        );
        log.info("使用加权轮询策略发现的服务节点数量: {}", balancedServices.size());
        
        // 5. 获取所有注册的服务
        Set<LQEnhancedNodeInfo> allServices = LQEnhancedRegisterCenter.getAllEnhancedNodeList();
        log.info("所有注册的增强服务节点数量: {}", allServices.size());
    }
    
    /**
     * MCP工具发现和调用示例
     */
    private static void discoverAndCallMCPTools() {
        log.info("=== MCP工具发现和调用示例 ===");
        
        // 1. 发现MCP工具节点
        List<LQEnhancedNodeInfo> mcpNodes = LQEnhancedRegisterCenter.getMCPToolNodes("file-processor", "1.0.0");
        log.info("发现MCP工具节点数量: {}", mcpNodes.size());
        
        // 2. 按能力发现工具
        List<LQMCPToolManager.MCPToolDefinition> fileTools = LQMCPToolManager.discoverToolsByCapability("file_operations");
        log.info("发现文件操作工具数量: {}", fileTools.size());
        
        // 3. 模拟工具调用
        if (!fileTools.isEmpty()) {
            LQMCPToolManager.MCPToolDefinition tool = fileTools.get(0);
            
            LQMCPToolManager.MCPToolRequest request = new LQMCPToolManager.MCPToolRequest()
                    .setRequestId("req-" + System.currentTimeMillis());
            request.getParameters().put("file_path", "/tmp/test.txt");
            request.getParameters().put("encoding", "UTF-8");
            
            try {
                LQMCPToolManager.MCPToolResponse response = LQMCPToolManager.invokeTool(
                        tool.getToolName(), tool.getToolVersion(), request);
                log.info("MCP工具调用结果: 成功={}, 响应={}", response.isSuccess(), response.getData());
            } catch (Exception e) {
                log.error("MCP工具调用失败", e);
            }
        }
        
        // 4. 获取工具使用统计
        Map<String, Object> stats = LQMCPToolManager.getToolUsageStats("file-processor");
        log.info("工具使用统计: {}", stats);
    }
}