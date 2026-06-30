package cn.lingque.cloud.http.annotation;

import java.lang.annotation.*;

/**
 * 请求体注解
 * 用于标记请求体参数
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
    
    /**
     * 是否必需
     */
    boolean required() default true;
}