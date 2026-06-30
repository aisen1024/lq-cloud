package cn.lingque.cloud.node.config;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.mcp.LQMCPToolManager;
import cn.lingque.cloud.node.protocol.LQProtocolHandler;
import cn.lingque.redis.LingQueRedis;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 增强版注册中心配置类
 * @author aisen
 * @date 2024-12-19
 */
@Configuration
@ConditionalOnProperty(name = "lq.register.enhanced.enabled", havingValue = "true", matchIfMissing = false)
public class LQEnhancedRegisterConfig {
    
    /**
     * 增强版注册中心Bean
     */
    @Bean
    @Primary
    public LQEnhancedRegisterCenter enhancedRegisterCenter() {
        return new LQEnhancedRegisterCenter();
    }
    
    /**
     * MCP工具管理器Bean
     */
    @Bean
    public LQMCPToolManager mcpToolManager() {
        return new LQMCPToolManager();
    }
    
    /**
     * 协议处理器Bean
     */
    @Bean
    public LQProtocolHandler protocolHandler() {
        return new LQProtocolHandler();
    }
}