package cn.lingque.runner.annon;

import cn.lingque.runner.enums.LqMqType;
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
    String key();

    /**
     * 版本
     */
    double version() default 1.0;
}
