package com.lingque.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * @author aisen
 * @date 2024-09-10
 * @desc redis plus 配置
 **/
@Data
@Accessors(chain = true)
public class LQProperties {
    //-------------------------------------基础配置-------------------------------------------//
    /**redis的IP地址*/
    private String ip = "localhost";
    /**redis的端口地址*/
    private String port = "6379";
    /**redis的数据库，默认0*/
    private Integer db = 0;
    /**redis的数据库连接密码，默认为空*/
    private String password;
    /**主线程池*/
    private LQThreadPoolProperties masterPool;
    /**辅助线程池*/
    private LQThreadPoolProperties slavePool;
    //-------------------------------------分布式配置-------------------------------------------//
    /**全局的微服务名称*/
    private String serverName = "ling-server";
    /**全局微服务分组*/
    private String group = "ling-que";
    //-------------------------------------模式配置-------------------------------------------//
    /**模式：standalone单机 cluster集群 sentinel哨兵，默认单机*/
    private String mode = "standalone";
    /**sentinel哨兵*/
    private SentinelNodes sentinel;
    /**cluster集群*/
    private ClusterNodes cluster;
    //-------------------------------------消息总线配置-------------------------------------------//
    /**消息总线*/
    private RedisPlusBusProperties bus;
    //-------------------------------------配置中心-------------------------------------------//
    /**配置中心*/
    private RedisPlusConfigCenterProperties configCenter;


    /**哨兵*/
    @Data
    public class SentinelNodes{
        /**哨兵节点地址*/
        private Set<String> sentinelNodes;
        /**哨兵主节点*/
        private String master;
    }

    /**集群*/
    @Data
    public class ClusterNodes{
        /**集群地址*/
        private Set<String> clusterNodes;
        /**主节点*/
        private String user;
    }

    /**消息总线*/
    @Data
    public class RedisPlusBusProperties{
        /**服务名称*/
        private String serverName = "ling-server";
        /**是否开启消息总线，默认开启*/
        private Boolean enable = true;
    }

    /**配置中心*/
    @Data
    public class RedisPlusConfigCenterProperties{
        /**配置ID，默认：serverName*/
        private String configId;
        /**配置分组*/
        private String group = "ling-que";;
    }

}
