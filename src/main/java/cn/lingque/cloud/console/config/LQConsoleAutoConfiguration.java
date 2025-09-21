package cn.lingque.cloud.console.config;

import cn.lingque.cloud.console.security.JwtTokenProvider;
import cn.lingque.cloud.console.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 控制台自动配置
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(LQConsoleProperties.class)
@ConditionalOnProperty(prefix = "lq.console", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "cn.lingque.cloud.console")
public class LQConsoleAutoConfiguration implements WebMvcConfigurer {
    
    private final LQConsoleProperties properties;
    
    public LQConsoleAutoConfiguration(LQConsoleProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(properties.getSecurity().getJwtSecret(), 
                                  properties.getSecurity().getTokenExpireHours());
    }
    
    @Bean
    public ConsoleAuthService consoleAuthService(JwtTokenProvider jwtTokenProvider) {
        return new ConsoleAuthService(properties, jwtTokenProvider);
    }
    
    @Bean
    public QueueManagementService queueManagementService() {
        return new QueueManagementService();
    }
    
    @Bean
    public ServiceDiscoveryService serviceDiscoveryService() {
        return new ServiceDiscoveryService();
    }
    
    @Bean
    public ConfigCenterService configCenterService() {
        return new ConfigCenterService();
    }
    
    @Bean
    public McpToolManagementService mcpToolManagementService() {
        return new McpToolManagementService();
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                    "http://localhost:*",
                    "https://localhost:*", 
                    "http://127.0.0.1:*",
                    "https://127.0.0.1:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射
        registry.addResourceHandler("/console/**")
                .addResourceLocations("classpath:/static/console/");
        
        // 控制台首页
        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/console/index.html");
    }
}