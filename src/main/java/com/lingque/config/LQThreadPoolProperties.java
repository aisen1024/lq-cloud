package com.lingque.config;

import lombok.Data;

/**
 * @author aisen
 * @date 2024/9/24
 * @desc 线程池配置
 **/
@Data
public class LQThreadPoolProperties {
    /**核心线程数*/
    private Integer corePoolSize = 50;
    /**最大线程数量*/
    private Integer maximumPoolSize = 200;
    /**闲置超过n毫秒后，回收线程*/
    private Integer keepAliveTime = 5000;
}
