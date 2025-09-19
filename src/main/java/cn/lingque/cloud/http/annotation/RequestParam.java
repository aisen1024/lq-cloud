package cn.lingque.cloud.http.annotation;

import java.lang.annotation.*;

/**
 * 查询参数注解
 * 用于标记URL查询参数
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    
    /**
     * 参数名称
     */
    String value() default "";
    
    /**
     * 是否必需
     */
    boolean required() default true;
    
    /**
     * 默认值
     */
    String defaultValue() default "";
}