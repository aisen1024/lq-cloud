package cn.lingque.redis;

import cn.lingque.config.LQProperties;

import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JedisProxy {
    private volatile static RedisInstance redisInstance;
    private volatile static AtomicBoolean initialized = new AtomicBoolean(false);
    private static final ThreadLocal<RedisCommands<String, String>> commandsThreadLocal = new ThreadLocal<>();

    public static void init(LQProperties redisPlusProperties) {
        if (initialized.compareAndSet(false, true)) {
            redisInstance = new RedisInstance(redisPlusProperties);
            // 测试连接
            RedisCommands<String, String> commands = redisInstance.getRedisCommands();
            String result = commands.setex("RDS_INI", 5, "halo");
            log.info("初始化 LQ Redis flag: {}", result);
        }
    }

    public static <T> T execBaseWithRetry(LingQueRedis.BaseSimpleExec exec, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                RedisCommands<String, String> commands = getRedisCommands();
                return (T) exec.exec(commands);
            } catch (Exception e) {
                lastException = e;
                log.warn("Redis操作异常，尝试重试 ({}/{})", attempts + 1, maxRetries, e);
                commandsThreadLocal.remove(); // 清除可能失效的连接
                attempts++;
                
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(Math.min(100 * attempts, 1000)); // 最大等待1秒
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Redis操作被中断", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Redis操作失败，已重试" + maxRetries + "次", lastException);
    }

    private static RedisCommands<String, String> getRedisCommands() {
        if (redisInstance == null) {
            throw new RuntimeException("Redis实例未初始化，请先调用init方法");
        }

        RedisCommands<String, String> commands = commandsThreadLocal.get();
        if (commands == null) {
            commands = redisInstance.getRedisCommands();
            commandsThreadLocal.set(commands);
        }
        return commands;
    }

    public static void shutdown() {
        if (redisInstance != null) {
            redisInstance.close();
            commandsThreadLocal.remove();
            initialized.set(false);
        }
    }
}
