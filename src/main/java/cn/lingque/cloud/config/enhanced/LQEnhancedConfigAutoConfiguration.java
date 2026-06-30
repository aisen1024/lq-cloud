package cn.lingque.cloud.config.enhanced;

import cn.lingque.util.LQUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

/**
 * LQ增强版配置中心自动配置类
 * 提供Spring Boot集成支持
 * 
 * @author LingQue Team
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(LQEnhancedConfigProperties.class)
@ConditionalOnProperty(prefix = "lq.config.enhanced", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LQEnhancedConfigAutoConfiguration {
    
    private final LQEnhancedConfigProperties properties;
    
    public LQEnhancedConfigAutoConfiguration(LQEnhancedConfigProperties properties) {
        this.properties = properties;
    }
    
    /**
     * 配置中心Bean
     */
    @Bean
    public LQEnhancedConfigCenter configCenter() {
        return new LQEnhancedConfigCenter();
    }
    
    /**
     * 配置管理器Bean
     */
    @Bean
    public LQConfigManager configManager() {
        return new LQConfigManager();
    }
    
    /**
     * 初始化配置中心
     */
    @PostConstruct
    public void initConfigCenter() {
        log.info("正在初始化LQ增强版配置中心...");
        
        // 加载初始配置
        loadInitialConfigs();
        
        // 启动配置清理任务
        startCleanupTask();
        
        // 注册关闭钩子
        registerShutdownHook();
        
        log.info("LQ增强版配置中心初始化完成");
        log.info("配置中心特性: 高性能内存缓存、实时推送、多格式支持、版本管理");
        
        // 打印配置统计
        ConfigStats stats = LQEnhancedConfigCenter.getStats();
        log.info("配置中心统计: {}", stats);
    }
    
    /**
     * 加载初始配置
     */
    private void loadInitialConfigs() {
        if (properties.getInitialConfigs() != null && !properties.getInitialConfigs().isEmpty()) {
            log.info("加载初始配置: {} 个", properties.getInitialConfigs().size());
            
            for (LQEnhancedConfigProperties.ConfigItem item : properties.getInitialConfigs()) {
                LQEnhancedConfigCenter.setConfig(
                    item.getNamespace() != null ? item.getNamespace() : "default",
                    item.getKey(),
                    item.getValue(),
                    item.getTtl() != null ? item.getTtl() : 30 * 60 * 1000L // 默认30分钟
                );
                
                log.debug("加载配置: {}:{} = {}", item.getNamespace(), item.getKey(), item.getValue());
            }
        }
    }
    
    /**
     * 启动配置清理任务
     */
    private void startCleanupTask() {
        if (properties.isAutoCleanup()) {
            LQUtil.execLoadJob("config-cleanup", () -> {
                try {
                    LQEnhancedConfigCenter.cleanExpiredConfigs();
                } catch (Exception e) {
                    log.error("配置清理任务执行失败", e);
                }
            }, properties.getCleanupInitialDelay(), properties.getCleanupInterval(), TimeUnit.SECONDS);
            
            log.info("配置清理任务已启动: 初始延迟{}秒, 间隔{}秒", 
                    properties.getCleanupInitialDelay(), properties.getCleanupInterval());
        }
    }
    
    /**
     * 注册关闭钩子
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭LQ增强版配置中心...");
            
            // 打印最终统计
            ConfigStats finalStats = LQEnhancedConfigCenter.getStats();
            log.info("配置中心最终统计: {}", finalStats);
            
            log.info("LQ增强版配置中心已关闭");
        }, "lq-config-shutdown"));
    }
    
    /**
     * 定时清理过期配置
     */
    @Scheduled(fixedRateString = "${lq.config.enhanced.cleanup-interval:300}", timeUnit = TimeUnit.SECONDS)
    public void scheduledCleanup() {
        if (properties.isAutoCleanup()) {
            try {
                ConfigStats statsBefore = LQEnhancedConfigCenter.getStats();
                
                if (statsBefore.needsCleanup()) {
                    LQEnhancedConfigCenter.cleanExpiredConfigs();
                    
                    ConfigStats statsAfter = LQEnhancedConfigCenter.getStats();
                    log.info("定时清理完成: 清理前{}个过期配置, 清理后{}个过期配置", 
                            statsBefore.getExpiredConfigCount(), statsAfter.getExpiredConfigCount());
                }
            } catch (Exception e) {
                log.error("定时清理任务执行失败", e);
            }
        }
    }
    
    /**
     * 定时打印统计信息
     */
    @Scheduled(fixedRateString = "${lq.config.enhanced.stats-interval:600}", timeUnit = TimeUnit.SECONDS)
    public void printStats() {
        if (properties.isEnableStats()) {
            try {
                ConfigStats stats = LQEnhancedConfigCenter.getStats();
                log.info("配置中心统计: {}", stats);
                
                if (log.isDebugEnabled()) {
                    log.debug("配置中心详细报告:\n{}", stats.generateReport());
                }
            } catch (Exception e) {
                log.error("统计信息打印失败", e);
            }
        }
    }
    
    /**
     * 销毁配置中心
     */
    @PreDestroy
    public void destroyConfigCenter() {
        log.info("正在销毁LQ增强版配置中心...");
        
        // 执行最后一次清理
        if (properties.isAutoCleanup()) {
            try {
                LQEnhancedConfigCenter.cleanExpiredConfigs();
                log.info("执行最后一次配置清理完成");
            } catch (Exception e) {
                log.error("最后一次配置清理失败", e);
            }
        }
        
        log.info("LQ增强版配置中心销毁完成");
    }
}