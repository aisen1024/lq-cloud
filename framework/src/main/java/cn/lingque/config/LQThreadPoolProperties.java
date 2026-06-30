package cn.lingque.config;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author aisen
 * @date 2024/9/24
 * @desc 线程池配置
 **/
@Data
@Accessors(chain = true)
public class LQThreadPoolProperties {
    /**核心线程数*/
    private Integer corePoolSize = 10;
    /**最大线程数量*/
    private Integer maximumPoolSize = 50;
    /**闲置超过n毫秒后，回收线程*/
    private Integer keepAliveTime = 18000;
}
