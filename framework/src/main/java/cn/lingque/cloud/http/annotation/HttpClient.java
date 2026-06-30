package cn.lingque.cloud.http.annotation;

import java.lang.annotation.*;

/**
 * HTTP客户端注解
 * 用于标记一个接口为HTTP客户端
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpClient {
    
    /**
     * 基础URL
     */
    String baseUrl() default "";
    
    /**
     * 服务名称（用于服务发现）
     */
    String serviceName() default "";
    
    /**
     * 连接超时时间（毫秒）
     */
    int connectTimeout() default 5000;
    
    /**
     * 读取超时时间（毫秒）
     */
    int readTimeout() default 30000;
    
    /**
     * 重试次数
     */
    int retryCount() default 0;
    
    /**
     * 是否启用负载均衡
     */
    boolean loadBalance() default true;
}