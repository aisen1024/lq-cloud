package cn.lingque.cloud.node;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ling-que.node")
public class ServerConfig {
    /**全局的微服务名称*/
    private String serverName = null;
    /**全局微服务注册IP*/
    private String serverHost = null;
    /**全局微服务注册端口*/
    private Integer serverPort = null;

    public String getNodeInfo(){
        return serverName + ":" + serverHost + ":" + serverPort;
    }
}
