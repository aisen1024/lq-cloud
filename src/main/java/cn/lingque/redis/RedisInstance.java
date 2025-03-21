package cn.lingque.redis;

import cn.hutool.json.JSONUtil;
import cn.lingque.config.LQProperties;
import cn.lingque.util.TryCatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
       // 增加最大连接数 - 参考SpringBoot默认值
       poolConfig.setMaxTotal(redisPlusProperties.getMaxTotal() != 0 ? redisPlusProperties.getMaxTotal() : 8);
       // 增加最大空闲连接数 - 参考SpringBoot默认值
       poolConfig.setMaxIdle(redisPlusProperties.getMaxIdle() != 0 ? redisPlusProperties.getMaxIdle() : 8);
       // 设置最小空闲连接数 - 参考SpringBoot默认值
       poolConfig.setMinIdle(redisPlusProperties.getMinIdle() != 0 ? redisPlusProperties.getMinIdle() : 0);
       // 当池内没有可用连接时，最大等待时间
       Duration maxWait = Duration.ofMillis(redisPlusProperties.getMaxWaitMillis() != 0 ? 
           redisPlusProperties.getMaxWaitMillis() : -1);
       poolConfig.setMaxWait(maxWait);
       
       // 开启jmx监控 - 默认关闭以减少开销
       poolConfig.setJmxEnabled(false);
       // 连接对象后进先出
       poolConfig.setLifo(true);
       // 在获取连接时检查有效性 - 默认false以提高性能
       poolConfig.setTestOnBorrow(true);
       // 在归还连接时检查有效性 - 默认false以提高性能
       poolConfig.setTestOnReturn(false);
       // 定时检查空闲连接
       poolConfig.setTestWhileIdle(true);
       //连接池耗尽时，获取连接是否阻塞等待
       poolConfig.setBlockWhenExhausted(true);
       // 空闲连接检查间隔时间 - 参考SpringBoot默认值
       poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(redisPlusProperties.getTimeBetweenEvictionRuns() != 0 ?
           redisPlusProperties.getTimeBetweenEvictionRuns() : 100));
       // 每次检查空闲连接的数量
       poolConfig.setNumTestsPerEvictionRun(-1);  // 检查所有空闲连接
       // 连接最小空闲时间
       poolConfig.setMinEvictableIdleTime(Duration.ofMillis(redisPlusProperties.getMinEvictableIdleTimeMillis() != 0 ? 
           redisPlusProperties.getMinEvictableIdleTimeMillis() : 1800000));  // 默认30分钟
       // 设置空闲对象驱逐后最小空闲数量
       poolConfig.setSoftMinEvictableIdleDuration(Duration.ofMillis(1800000));

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
                props.getUsername(),
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
                props.getTimeout() ,
                props.getTimeout(),
                3,  // 最大重试次数
                props.getPassword(),
                config
        );
    }


    public Jedis getRedisTemplate() {
        Jedis jedis = null;
        if ("sentinel".equalsIgnoreCase(mode)) {
            // Sentinel mode configuration
            jedis= sentinel.getResource();
        } else if ("cluster".equalsIgnoreCase(mode)) {
            // Cluster mode configuration
            ConnectionPool pool = cluster.getClusterNodes().values().stream().findAny().get();
            jedis = new JedisObj(pool,pool.getResource());
        }else {
            // Standalone mode configuration
            jedis= standalone.getResource();
        }
        return jedis;
    }

    public void returnBrokenResource(Jedis jedis) {
        log.debug("回收损坏的资源---》jedis ->{}",jedis.hashCode());
        TryCatch.trying(()->{
           switch (mode){
               case "standalone":
                   standalone.returnBrokenResource(jedis);
                   break;
               case "sentinel":
                   sentinel.returnBrokenResource(jedis);
                   break;
               case "cluster":
                   ((JedisObj)jedis).returnBrokenResource();
                   break;
           }
        },"closeJedis回收资源");
    }

    public void returnResource(Jedis jedis) {
        log.debug("回收资源---》jedis ->{}",jedis.hashCode());
        TryCatch.trying(()->{
            switch (mode){
                case "standalone":
                    standalone.returnResource(jedis);
                    break;
                case "sentinel":
                    sentinel.returnResource(jedis);
                    break;
                case "cluster":
                    ((JedisObj)jedis).returnResource();
                    break;
            }
        },"closeJedis回收资源");
    }



}
