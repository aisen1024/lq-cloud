package cn.lingque.redis;

import cn.lingque.config.LQProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

/**
 * 初始化代理
 */
@Slf4j
public class JedisProxy {
    private volatile static RedisInstance redisInstance;

    public static void init(LQProperties redisPlusProperties){
        redisInstance = new RedisInstance(redisPlusProperties);
        Jedis jedis = redisInstance.getRedisTemplate();
        if (jedis == null){
            throw new RuntimeException("redisInstance init error");
        }
        String flag = jedis.setex("RDS_INI",5,"halo");
        log.info("初始化 LQ Redis flag:{}",flag);
    }

    public synchronized static RedisInstance getRedisInstance() {
        if (redisInstance == null) {
            throw new RuntimeException("redisInstance is null , please init redisInstance first");
        }
        return redisInstance;
    }

}
