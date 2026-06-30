package cn.lingque.cloud.mcp.annotation;

import java.lang.annotation.*;

/**
 * MCP工具方法注解
 * 用于标记MCP工具中的可调用方法
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MCPToolMethod {

    /**
     * 方法名称
     */
    String name() default "";

    /**
     * 方法描述
     */
    String description() default "";

    /**
     * 是否为默认方法
     */
    boolean isDefault() default false;

    /**
     * 方法超时时间(毫秒)
     */
    long timeout() default 30000;

    /**
     * 是否异步执行
     */
    boolean async() default false;

    /**
     * 是否缓存结果
     */
    boolean cacheable() default false;

    /**
     * 缓存时间(秒)
     */
    int cacheTime() default 300;
}