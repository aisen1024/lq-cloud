package cn.lingque.cloud.http.annotation;

import java.lang.annotation.*;

/**
 * POST请求注解
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Post {
    
    /**
     * 请求路径
     */
    String value() default "";
    
    /**
     * 请求头
     */
    String[] headers() default {};
    
    /**
     * Content-Type
     */
    String contentType() default "application/json";
    
    /**
     * 连接超时时间（毫秒）
     */
    int connectTimeout() default -1;
    
    /**
     * 读取超时时间（毫秒）
     */
    int readTimeout() default -1;
    
    /**
     * 重试次数
     */
    int retryCount() default -1;
}