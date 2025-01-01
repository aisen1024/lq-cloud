package cn.lingque.redis;

import cn.hutool.json.JSONUtil;
import cn.lingque.config.LQProperties;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisInstance {
    /**哨兵模式*/
    private  JedisSentinelPool sentinel;
    /**单机模式*/
    private  JedisPool standalone;
    /**集群模式*/
    private  JedisCluster cluster;
    /**模式选择*/
    private  String mode;

    /**
     * 初始化Redis连接
     * @param redisPlusProperties Redis配置属性
     */
    public RedisInstance(LQProperties redisPlusProperties) {
        // 配置连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 最大连接数
        poolConfig.setMaxTotal(redisPlusProperties.getMaxTotal());
        // 最大空闲连接数
        poolConfig.setMaxIdle(redisPlusProperties.getMaxIdle());
        // 最小空闲连接数
        poolConfig.setMinIdle(redisPlusProperties.getMinIdle());
        // 当池内没有可用连接时，最大等待时间
        Duration maxWait = Duration.ofMillis(redisPlusProperties.getMaxWaitMillis());
        poolConfig.setMaxWait(maxWait);
        // 对拿到的connection进行validateObject校验
        poolConfig.setTestOnBorrow(true);
        // 定时对线程池中空闲的链接进行validateObject校验
        poolConfig.setTestWhileIdle(true);

        try {
            switch (redisPlusProperties.getMode().toLowerCase()) {
                case "standalone":
                    initStandalone(redisPlusProperties, poolConfig);
                    break;
                case "sentinel":
                    initSentinel(redisPlusProperties, poolConfig);
                    break;
                case "cluster":
                    initCluster(redisPlusProperties, poolConfig);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Redis mode: " + redisPlusProperties.getMode());
            }

            mode = redisPlusProperties.getMode();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Redis connection", e);
        }
    }



    /**
     * 初始化单机模式连接池
     */
    private  void initStandalone(LQProperties props, JedisPoolConfig poolConfig) {
        standalone = new JedisPool(
                poolConfig,
                props.getIp(),
                Integer.parseInt(props.getPort()),
                props.getTimeout(),
                "",
                props.getPassword(),
                props.getDb()
        );
    }

    /**
     * 初始化哨兵模式连接池
     */
    private void initSentinel(LQProperties props, JedisPoolConfig poolConfig) {
        Set<String> sentinels = new HashSet<>(props.getSentinel().getSentinelNodes());
        sentinel = new JedisSentinelPool(
                props.getSentinel().getMaster(),
                sentinels,
                poolConfig,
                props.getTimeout(),
                props.getPassword(),
                props.getDb()
        );
    }

    /**
     * 初始化集群模式连接池
     */
    private void initCluster(LQProperties props, JedisPoolConfig poolConfig) {
        Set<HostAndPort> nodes = props.getCluster().getClusterNodes().stream()
                .map(node -> {
                    String[] parts = node.split(":");
                    return new HostAndPort(parts[0], Integer.parseInt(parts[1]));
                })
                .collect(Collectors.toSet());
        GenericObjectPoolConfig<Connection> config = JSONUtil.toBean(JSONUtil.toJsonStr(poolConfig),GenericObjectPoolConfig.class);
        cluster = new JedisCluster(
                nodes,
                props.getTimeout(),
                props.getTimeout(),
                3,  // 最大重试次数
                props.getPassword(),
                config
        );
    }

    public synchronized Jedis getRedisTemplate() {
        if ("sentinel".equalsIgnoreCase(mode)) {
            // Sentinel mode configuration
            return sentinel.getResource();
        } else if ("cluster".equalsIgnoreCase(mode)) {
            // Cluster mode configuration
            return new Jedis(cluster.getClusterNodes().values().iterator().next().getResource());
        }else {
            // Standalone mode configuration
            return standalone.getResource();
        }
    }

}
