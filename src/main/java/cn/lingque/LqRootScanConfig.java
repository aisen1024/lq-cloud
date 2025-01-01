package cn.lingque;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackages = "cn.lingque")
@PropertySource("classpath:ling-que.properties")
public class LqRootScanConfig {

}
