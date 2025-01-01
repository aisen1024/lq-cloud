package cn.lingque.runner.annon;

import java.lang.annotation.*;

/**
 * 消息总线
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LqEvenBus {
    /**
     * 事件
     */
    String even();

}
