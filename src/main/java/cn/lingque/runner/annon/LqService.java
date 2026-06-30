package cn.lingque.runner.annon;

import cn.lingque.cloud.http.annotation.HttpClient;

import java.lang.annotation.*;

/**
 * 服务代理
 * 现在基于自定义HTTP客户端框架实现
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@HttpClient
@Documented
public @interface LqService {
    /**
     * 服务名称
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
}
