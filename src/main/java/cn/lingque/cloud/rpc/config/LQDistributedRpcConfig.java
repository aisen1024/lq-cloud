package cn.lingque.cloud.rpc.config;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.rpc.LQDistributedServiceCaller;
import cn.lingque.cloud.rpc.server.LQRpcServerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

/**
 * LQ分布式RPC框架配置类
 * 自动配置分布式服务调用相关组件
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "lq.rpc.enabled", havingValue = "true", matchIfMissing = false)
public class LQDistributedRpcConfig {
    
    @PostConstruct
    public void init() {
        log.info("LQ分布式RPC框架启动中...");
        
        // 启动增强版注册中心
        LQEnhancedRegisterCenter.start();
        
        log.info("LQ分布式RPC框架启动完成");
    }
    
    /**
     * 分布式服务调用器Bean
     */
    @Bean
    @Primary
    public LQDistributedServiceCaller distributedServiceCaller() {
        return new LQDistributedServiceCaller();
    }
    
    /**
     * RPC服务端处理器Bean
     */
    @Bean
    public LQRpcServerHandler rpcServerHandler() {
        return new LQRpcServerHandler();
    }
}