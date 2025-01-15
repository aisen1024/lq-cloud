package cn.lingque.config;

import cn.lingque.console.config.LqConsoleConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author aisen
 * @date 2024-09-10
 * @desc redis plus 配置
 **/
@Data
@Accessors(chain = true)
@Configuration
@ConfigurationProperties(prefix = "ling-que")
public class LQProperties {
    //-------------------------------------基础配置-------------------------------------------//
    /**redis的IP地址*/
    private String ip = "localhost";
    /**redis的端口地址*/
    private String port = "6379";
    /**redis的数据库，默认0*/
    private Integer db = 0;
    /**redis的数据连接名*/
    private String username;
    /**redis的数据库连接密码，默认为空*/
    private String password;
    /**redis最大连接数
     * 参考 lettuce 默认值：最大连接数设为8*/
    private int maxTotal = 8;
    /**redis最大空闲连接数
     * 参考 lettuce 默认值：最大空闲连接数设为8*/
    private int maxIdle = 8;
    /**redis最小空闲连接数
     * 参考 lettuce 默认值：最小空闲连接数设为0*/
    private int minIdle = 0;
    /**redis最大等待时间
     * 参考 lettuce 默认值：设置为-1表示无限等待*/
    private long maxWaitMillis = -1L;
    /**redis连接超时时间
     * 参考 spring-boot-redis 默认值：设置为2000毫秒*/
    private int timeout = 2000;
    /**主线程池*/
    private LQThreadPoolProperties masterPool = new LQThreadPoolProperties();
    /**辅助线程池*/
    private LQThreadPoolProperties slavePool = new LQThreadPoolProperties();
    //-------------------------------------分布式配置-------------------------------------------//
    /**全局的微服务名称*/
    private String serverName = null;
    /**全局微服务注册IP*/
    private String serverHost = null;
    /**全局微服务注册端口*/
    private Integer serverPort = null;

    //-------------------------------------模式配置-------------------------------------------//
    /**模式：standalone单机 cluster集群 sentinel哨兵，默认单机*/
    private String mode = "standalone";
    /**sentinel哨兵*/
    private SentinelProperties sentinel = new SentinelProperties();
    /**cluster集群*/
    private ClusterProperties cluster = new ClusterProperties();
    //-------------------------------------消息总线配置-------------------------------------------//
    /**消息总线*/
    private RedisPlusBusProperties bus = new RedisPlusBusProperties();
    //-------------------------------------配置中心-------------------------------------------//
    /**配置中心*/
    private LqConfigCenterProperties configCenter = new LqConfigCenterProperties();

    /**控制台*/
    private LqConsoleConfig console = new LqConsoleConfig();


    /**哨兵*/
    @Data
    public static class SentinelProperties {
        /**哨兵主节点*/
        private String master;
        /**哨兵节点地址*/
        private List<String> sentinelNodes;
    }

    /**集群*/
    @Data
    public static class ClusterProperties {
        /**主节点*/
        private String user;
        /**集群地址*/
        private List<String> clusterNodes;
    }

    /**消息总线*/
    @Data
    public class RedisPlusBusProperties{
        /**服务名称*/
        private String serverName = "ling-server";
        /**是否开启消息总线，默认开启*/
        private Boolean enable = true;
    }

}
