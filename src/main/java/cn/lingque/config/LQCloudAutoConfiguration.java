package cn.lingque.config;

import cn.lingque.runner.LqCloudRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * 灵雀云核心自动配置类
 * 作为Spring Boot Starter组件的核心配置
 * 在构造时立即初始化LqCloudRunner，确保线程池等基础设施最先就绪
 * 
 * @author aisen
 * @date 2024-12-16
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(LqCloudRunner.class)
@EnableConfigurationProperties(LQProperties.class)
@ConditionalOnProperty(name = "ling-que.enabled", havingValue = "true", matchIfMissing = true)
public class LQCloudAutoConfiguration implements InitializingBean {

    private final LQProperties lqProperties;
    private LqCloudRunner lqCloudRunner;

    /**
     * 构造函数 - 配置类实例化时立即初始化LqCloudRunner
     * 此时yml配置已加载，可以立即启动核心组件
     * 
     * @param lqProperties yml配置属性
     */
    public LQCloudAutoConfiguration(LQProperties lqProperties) {
        this.lqProperties = lqProperties;
        log.info("=================================================");
        log.info("灵雀云配置已加载，开始初始化核心组件...");
        
        // 立即初始化LqCloudRunner，不等待Bean创建
        this.lqCloudRunner = new LqCloudRunner(lqProperties);
        
        log.info("✅ 灵雀云核心组件初始化完成（线程池、Redis、注册中心已就绪）");
        log.info("=================================================");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("LQCloudAutoConfiguration afterPropertiesSet完成");
    }

    /**
     * 将已初始化的LqCloudRunner暴露为Bean
     * 供其他组件通过@DependsOn("lqCloudRunner")依赖
     * 
     * @return 已初始化的LqCloudRunner实例
     */
    @Bean(name = "lqCloudRunner")
    public LqCloudRunner lqCloudRunner() {
        return this.lqCloudRunner;
    }
}

