package cn.lingque.cloud.mcp.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * MCP工具注解
 * 用于标记一个类为MCP工具，支持自动注册和发现
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MCPTool {

    /**
     * 工具名称
     */
    String name() default "";

    /**
     * 工具版本
     */
    String version() default "1.0.0";

    /**
     * 工具描述
     */
    String description() default "";

    /**
     * 工具能力列表
     */
    String[] capabilities() default {};

    /**
     * 工具标签
     */
    String[] tags() default {};

    /**
     * 工具优先级
     */
    int priority() default 100;

    /**
     * 是否启用
     */
    boolean enabled() default true;

    /**
     * 工具端点路径
     */
    String endpoint() default "";

    /**
     * 工具配置
     */
    String[] config() default {};
}