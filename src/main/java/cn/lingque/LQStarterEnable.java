package cn.lingque;

import cn.lingque.config.LQProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 灵雀启动类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(value = {LqRootScanConfig.class})
public @interface LQStarterEnable {

}
