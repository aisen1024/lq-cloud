package cn.lingque.runner.annon;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LqApiSchema {

    /**
     * 字段名
     * @return
     */
    String name() default "";

    /**
     * 描述
     * @return
     */
    String description();

}
