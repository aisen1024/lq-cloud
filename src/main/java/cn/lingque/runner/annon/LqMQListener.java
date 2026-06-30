package cn.lingque.runner.annon;

import java.lang.annotation.*;

/**
 * 消息队列
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LqMQListener {
    /**
     * 队列key
     */
    String topic();
}
