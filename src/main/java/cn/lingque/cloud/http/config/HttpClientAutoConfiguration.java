package cn.lingque.cloud.http.config;

import cn.lingque.cloud.http.interceptor.InterceptorManager;
import cn.lingque.cloud.http.processor.ResponseProcessorManager;
import cn.lingque.cloud.http.scanner.HttpClientScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP客户端自动配置
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "lq.http.client.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public HttpClientScanner httpClientScanner() {
        log.info("启用LQ HTTP客户端框架");
        return new HttpClientScanner();
    }

    @Bean
    @ConditionalOnMissingBean
    public InterceptorManager interceptorManager() {
        return new InterceptorManager(java.util.Collections.emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseProcessorManager responseProcessorManager() {
        return new ResponseProcessorManager(java.util.Collections.emptyList());
    }
}