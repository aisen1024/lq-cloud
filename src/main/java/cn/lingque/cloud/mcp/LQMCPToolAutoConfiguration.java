package cn.lingque.cloud.mcp;

import cn.lingque.cloud.console.service.McpToolManagementService;
import cn.lingque.cloud.mcp.annotation.MCPTool;
import cn.lingque.cloud.mcp.handler.LQMCPToolHandler;
import cn.lingque.cloud.mcp.processor.LQMCPToolProcessor;
import cn.lingque.cloud.mcp.registry.LQMCPToolRegistry;
import cn.lingque.cloud.mcp.server.LQMCPToolServer;
import cn.lingque.cloud.mcp.client.LQMCPToolClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;

import java.util.Map;

/**
 * LQ MCP工具自动配置类
 * 提供开箱即用的MCP工具支持
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LQMCPToolProperties.class)
@ConditionalOnProperty(prefix = "lq.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({LQMCPToolRegistry.class, LQMCPToolProcessor.class, LQMCPToolHandler.class})
public class LQMCPToolAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private LQMCPToolProperties properties;
    
    @Autowired
    private LQMCPToolRegistry toolRegistry;
    
    @Autowired
    private LQMCPToolProcessor toolProcessor;

    /**
     * MCP工具服务器
     */
    @Bean
    @ConditionalOnProperty(prefix = "lq.mcp.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "mcpToolServer")
    public LQMCPToolServer mcpToolServer() {
        return new LQMCPToolServer(properties, toolRegistry, toolProcessor);
    }

    /**
     * MCP工具客户端Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "lq.mcp.client", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "mcpToolClient")
    public LQMCPToolClient mcpToolClient() {
        return new LQMCPToolClient.Builder()
                .serverHost(properties.getClient().getServerHost())
                .serverPort(properties.getClient().getServerPort())
                .connectTimeout((int) properties.getClient().getConnectTimeout())
                .readTimeout((int) properties.getClient().getReadTimeout())
                .build();
    }

    /**
     * 初始化MCP工具
     */
    // 移除 @PostConstruct 注解
    @EventListener(ApplicationReadyEvent.class)
    public void initMCPTools() {
        try {
            log.info("[LQ-MCP] 开始初始化MCP工具模块...");
            
            // 1. 扫描并注册所有@MCPTool注解的Bean
            scanAndRegisterMCPTools();
            
            // 2. 启动MCP工具服务器
            if (properties.getServer().isEnabled()) {
                // 此时所有Bean都已创建完成，可以安全获取
                LQMCPToolServer server = applicationContext.getBean(LQMCPToolServer.class);
                server.start();
                log.info("[LQ-MCP] MCP工具服务器已启动，端口: {}", properties.getServer().getPort());
            }
            
            // 3. 输出初始化摘要
            printInitializationSummary();
            
        } catch (Exception e) {
            log.error("[LQ-MCP] MCP工具模块初始化失败", e);
        }
    }

    /**
     * 扫描并注册MCP工具
     */
    private void scanAndRegisterMCPTools() {
        Map<String, Object> mcpToolBeans = applicationContext.getBeansWithAnnotation(MCPTool.class);
        
        log.info("[LQ-MCP] 发现 {} 个MCP工具Bean", mcpToolBeans.size());
        
        for (Map.Entry<String, Object> entry : mcpToolBeans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            
            try {
                toolRegistry.registerTool(beanName, bean);
                log.info("[LQ-MCP] 成功注册MCP工具: {}", beanName);
            } catch (Exception e) {
                log.error("[LQ-MCP] 注册MCP工具失败: {}", beanName, e);
            }
        }
    }

    /**
     * 输出初始化摘要
     */
    private void printInitializationSummary() {
        log.info("\n" +
                "========================================\n" +
                "  LQ MCP工具模块初始化完成\n" +
                "========================================\n" +
                "📊 统计信息:\n" +
                "   - 已注册工具数量: {}\n" +
                "   - 服务器状态: {}\n" +
                "   - 服务器端口: {}\n" +
                "   - 自动发现: {}\n" +
                "   - 健康检查: {}\n" +
                "========================================\n" +
                "🚀 MCP工具已就绪，可以开始使用！\n" +
                "📚 使用文档: README_MCP_TOOLS.md\n" +
                "========================================",
                toolRegistry.getRegisteredToolCount(),
                properties.getServer().isEnabled() ? "已启动" : "已禁用",
                properties.getServer().getPort(),
                properties.isAutoDiscovery() ? "已启用" : "已禁用",
                properties.isHealthCheck() ? "已启用" : "已禁用"
        );
    }
}