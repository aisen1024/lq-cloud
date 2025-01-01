package cn.lingque.runner.annon;

import cn.lingque.runner.forest.ServiceNameCycle;
import com.dtflys.forest.annotation.MethodLifeCycle;
import com.dtflys.forest.annotation.RequestAttributes;

import java.lang.annotation.*;

/**
 * 服务代理
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@MethodLifeCycle(ServiceNameCycle.class)
@RequestAttributes
@Documented
public @interface LqService {
    /**
     * 服务名称
     */
    String serviceName() default "";

}
