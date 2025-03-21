package cn.lingque.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 微服务配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ling-que.config")
public class LQConfigCenterProperties {

    /**
     * 文件ids
     */
    private List<DataId> dataIds = new ArrayList<>();

}
