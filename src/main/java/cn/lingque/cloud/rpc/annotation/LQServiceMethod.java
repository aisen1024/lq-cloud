package cn.lingque.cloud.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LQ服务方法注解
 * 标注在服务方法上，用于配置方法级别的调用参数
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LQServiceMethod {
    
    /**
     * 方法名称
     * 如果不指定，则使用方法的实际名称
     */
    String value() default "";
    
    /**
     * 协议类型
     */
    String protocol() default "HTTP";
    
    /**
     * 超时时间（毫秒）
     */
    int timeout() default 5000;
    
    /**
     * 重试次数
     */
    int retryCount() default 3;
    
    /**
     * 是否异步调用
     */
    boolean async() default false;
    
    /**
     * 是否启用缓存
     */
    boolean cache() default false;
    
    /**
     * 缓存过期时间（秒）
     */
    int cacheExpire() default 300;
}