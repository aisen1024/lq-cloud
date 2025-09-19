package cn.lingque.cloud.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LQ服务注解
 * 标注在服务接口上，用于标识这是一个可以远程调用的服务
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LQService {
    
    /**
     * 服务名称
     * 如果不指定，则使用接口的简单类名
     */
    String value() default "";
    
    /**
     * 服务版本
     */
    String version() default "1.0.0";
    
    /**
     * 服务分组
     */
    String group() default "default";
    
    /**
     * 默认协议
     */
    String protocol() default "HTTP";
    
    /**
     * 默认超时时间（毫秒）
     */
    int timeout() default 5000;
    
    /**
     * 是否启用负载均衡
     */
    boolean loadBalance() default true;
    
    /**
     * 是否启用熔断器
     */
    boolean circuitBreaker() default true;
}