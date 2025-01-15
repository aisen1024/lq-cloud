package cn.lingque.runner.annon;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LqApi {
   /**
    * 描述
    * @return
    */
   String description();
}
