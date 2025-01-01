package cn.lingque;

import cn.lingque.config.LQProperties;
import cn.lingque.runner.LqCloudRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LqMainRunnerConfig {
    @Bean
    public LqCloudRunner lqCloudRunner(LQProperties lqProperties) {
        return new LqCloudRunner(lqProperties);
    }
}
