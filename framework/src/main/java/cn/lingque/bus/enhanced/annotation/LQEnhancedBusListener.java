package cn.lingque.bus.enhanced.annotation;

import java.lang.annotation.*;

/**
 * 增强版消息总线监听器注解
 * 用于标记方法为消息总线的订阅者
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LQEnhancedBusListener {
    
    /**
     * 订阅的主题名称
     * @return 主题名称
     */
    String topic();
    
    /**
     * 服务组（可选）
     * 如果指定了服务组，只会接收发送到该服务组的消息
     * @return 服务组名称
     */
    String serviceGroup() default "";
    
    /**
     * 是否异步处理消息
     * @return true表示异步处理，false表示同步处理
     */
    boolean async() default true;
    
    /**
     * 处理优先级（数字越小优先级越高）
     * @return 优先级
     */
    int priority() default 0;
    
    /**
     * 订阅者名称（可选，用于日志和调试）
     * 如果不指定，将使用方法名
     * @return 订阅者名称
     */
    String name() default "";
    
    /**
     * 是否启用（默认启用）
     * @return true表示启用，false表示禁用
     */
    boolean enabled() default true;
    
    /**
     * 消息过滤条件（SpEL表达式，可选）
     * 例如："#message.type == 'USER_CREATED'"
     * @return 过滤条件
     */
    String condition() default "";
    
    /**
     * 错误处理策略
     * @return 错误处理策略
     */
    ErrorHandleStrategy errorStrategy() default ErrorHandleStrategy.LOG;
    
    /**
     * 最大重试次数（当errorStrategy为RETRY时有效）
     * @return 最大重试次数
     */
    int maxRetries() default 3;
    
    /**
     * 重试间隔（毫秒，当errorStrategy为RETRY时有效）
     * @return 重试间隔
     */
    long retryInterval() default 1000;
    
    /**
     * 错误处理策略枚举
     */
    enum ErrorHandleStrategy {
        /** 仅记录日志 */
        LOG,
        /** 重试处理 */
        RETRY,
        /** 忽略错误 */
        IGNORE,
        /** 抛出异常 */
        THROW
    }
}