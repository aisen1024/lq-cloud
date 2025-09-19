package cn.lingque.cloud.http.annotation;

import java.lang.annotation.*;

/**
 * 路径变量注解
 * 用于标记URL路径中的变量参数
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    
    /**
     * 变量名称
     */
    String value() default "";
}