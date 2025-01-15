package cn.lingque.runner.annon;



import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LqApiOperation {
    /**
     * 描述
     * @return
     */
    String description();

    /**
     * 请求类型
     * @return
     */
    String contentType() default "application/json";
}
