package cn.lingque.redis;

import cn.lingque.config.LQProperties;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 初始化代理
 */
@Slf4j
public class JedisProxy {

    private volatile static RedisInstance redisInstance;
    //jedis连接池
    private volatile static Map<Jedis,Long> sourceMap = new ConcurrentHashMap<>();
    //使用连接池
    private volatile static Map<Jedis, AtomicInteger> useMap = new ConcurrentHashMap<>();
    //占有连接池
    private volatile static Map<Jedis, AtomicBoolean> LockMap = new ConcurrentHashMap<>();
    //最大实例
    private static volatile Integer maxInstance = 16;
    //最小实例
    private static volatile Integer minInstance = 8;
    //默认18秒过期
    private static volatile Integer timeout = 18000;

    //运行检查
    private static volatile AtomicBoolean runCheck = new AtomicBoolean(false);

    //轮询
    private static volatile AtomicInteger index = new AtomicInteger(0);
    /**
     * 初始化
     * @param redisPlusProperties
     */
    public static void init(LQProperties redisPlusProperties){
        redisInstance = new RedisInstance(redisPlusProperties);
        maxInstance = LQUtil.lt(redisPlusProperties.getMaxTotal(),0,maxInstance);
        minInstance = LQUtil.lt(redisPlusProperties.getMaxIdle(),0,minInstance);
        timeout = LQUtil.lt( redisPlusProperties.getMinEvictableIdleTimeMillis(),1000,timeout);
        if (runCheck.compareAndSet(false,true)){
            startCheck();
        }
        Jedis jedis = getRedisInstance();
        if (jedis == null){
            throw new RuntimeException("redisInstance init error");
        }
        String flag = jedis.setex("RDS_INI",5,"halo");
        log.info("初始化 LQ Redis flag:{}",flag);

    }

    /**
     * 健康检查
     */
    private static void startCheck(){
        //第一次先加载
        loadSource(minInstance);
        Thread run = new Thread(() -> {
            while (true) {
                TryCatch.trying(() -> cleanSource());
                TryCatch.trying(() -> healthCheck());
                TryCatch.trying(() -> autoLoadSource());
                TryCatch.trying(() -> {
                    Thread.sleep(500L);
                });
            }
        });
        run.setDaemon(true);
        run.setName("Redis-Health-Checker");
        run.start();
    }

    /**
     * 根据使用量进行判断
     */
    private static void autoLoadSource(){
        //当前使用
        int sum =  useMap.values().stream().mapToInt(AtomicInteger::get).sum();
        //实例个数
        int count = useMap.size();
        //某一时刻当它飙升到2.5倍时后，按照倍数增长
        int size = sum > count * 2.5 ? minInstance * (BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count),0,BigDecimal.ROUND_UP).intValue()) : minInstance;
        if (size > maxInstance){
            size = maxInstance;
        }
        loadSource(size);
    }
    /**
     * 清理过期资源
     */
    private static void cleanSource(){
        //超时归还
        for (Jedis jedis : sourceMap.keySet()){
            if (sourceMap.get(jedis) < System.currentTimeMillis()){
                if (LockMap.get(jedis).compareAndSet(false,true)){
                    if (useMap.get(jedis).get() == 0) {
                        sourceMap.remove(jedis);
                        useMap.remove(jedis);
                        LockMap.remove(jedis);
                        TryCatch.trying(() -> redisInstance.returnResource(jedis));
                    }else {
                        LockMap.get(jedis).set(false);
                    }
                }
            }
        }
    }

    /**
     * 加载资源
     */
    private static void loadSource(int minInstance) {
        int attempts = 0;
        int maxAttempts = 3; // 最大尝试次数
        int currentSize = sourceMap.size();
        int needed = minInstance - currentSize;
        
        if (needed <= 0) {
            return; // 已经有足够的连接
        }
        
        while (attempts < maxAttempts && sourceMap.size() < minInstance) {
            int successCount = 0;
            for (int i = 0; i < needed; i++) {
                try {
                    Jedis jedis = redisInstance.getRedisTemplate();
                    // 测试连接是否有效
                    String pingResult = jedis.ping();
                    if ("PONG".equals(pingResult)) {
                        sourceMap.put(jedis, System.currentTimeMillis() + timeout);
                        useMap.put(jedis, new AtomicInteger(0));
                        LockMap.put(jedis, new AtomicBoolean(false));
                        successCount++;
                    } else {
                        log.warn("创建的Redis连接无效，返回非PONG响应");
                        redisInstance.returnBrokenResource(jedis);
                    }
                } catch (Exception e) {
                    log.error("创建新Redis连接失败", e);
                }
            }
            
            if (successCount == 0) {
                // 如果一个连接都没创建成功，可能Redis服务有问题
                log.error("无法创建任何新的Redis连接，Redis服务可能不可用");
                attempts++;
                try {
                    // 等待一段时间再重试
                    Thread.sleep(500 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // 有连接创建成功，跳出循环
                break;
            }
        }
        
        // 记录当前连接池状态
        log.debug("Redis连接池当前状态: 总连接数={}, 成功加载={}", sourceMap.size(), sourceMap.size() - currentSize);
    }

    /**
     * 获取Jedis
     */
    private static Jedis getRedisInstance() {
        if (redisInstance == null) {
            throw new RuntimeException("redisInstance is null, please init redisInstance first");
        }
        if (sourceMap.isEmpty()) {
            // 尝试重新初始化连接池而不是直接抛出异常
            log.warn("sourceMap为空，尝试重新加载连接");
            loadSource(minInstance);
            if (sourceMap.isEmpty()) {
                throw new RuntimeException("无法初始化Redis连接池");
            }
        }

        // 使用计数器限制重试次数，避免死循环
        int attempts = 0;
        int maxAttempts = sourceMap.size() * 2; // 设置最大尝试次数
        
        while (attempts < maxAttempts) {
            Map<Jedis, Long> tmp = sourceMap;
            if (tmp.isEmpty()) {
                loadSource(minInstance);
                tmp = sourceMap;
                if (tmp.isEmpty()) {
                    throw new RuntimeException("无法获取Redis连接");
                }
            }
            
            int i = index.incrementAndGet() - 1;
            if (i >= tmp.size()) {
                i = 0;
                index.set(0);
            }
            
            try {
                Jedis instance = tmp.keySet().stream().collect(Collectors.toList()).get(i);
                if (LockMap.get(instance).compareAndSet(false, true)) {
                    useTime(instance);
                    LockMap.get(instance).set(false);
                    return instance;
                }
            } catch (Exception e) {
                log.warn("获取Redis连接失败，尝试重新选择", e);
            }
            
            attempts++;
            // 短暂延迟避免CPU占用过高
            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        
        throw new RuntimeException("无法获取可用的Redis连接，已尝试" + maxAttempts + "次");
    }

    /**
     * 使用后自增1
     * @param jedis
     */
    private synchronized static void useTime(Jedis jedis){
        if (useMap.containsKey(jedis)){
            useMap.get(jedis).incrementAndGet();
        }else {
            AtomicInteger atomicInteger = new AtomicInteger();
            useMap.put(jedis,atomicInteger);
        }
    }


    /**
     * 归还jedis
     */
    public static void returnJedis(Jedis jedis, int state) {
            //立即回收
            if (state == 0){
                if (LockMap.get(jedis).compareAndSet(false,true)) {
                    sourceMap.remove(jedis);
                    useMap.remove(jedis);
                    LockMap.remove(jedis);
                    TryCatch.trying(() -> redisInstance.returnBrokenResource(jedis));
                    //立马补上
                    autoLoadSource();
                }
            }else {
               TryCatch.tryingIgnoreError(()->{
                   AtomicInteger atomicInteger = useMap.get(jedis);
                   if (atomicInteger!=null)
                       atomicInteger.decrementAndGet();
               });
            }
    }

    private static void healthCheck() {
        int unhealthyCount = 0;
        int totalConnections = sourceMap.size();
        
        for (Jedis jedis : sourceMap.keySet()) {
            if (LockMap.get(jedis) == null || useMap.get(jedis) == null) {
                continue; // 跳过已被其他线程移除的连接
            }
            
            if (LockMap.get(jedis).compareAndSet(false, true)) {
                try {
                    // 检查连接是否被使用中
                    if (useMap.get(jedis).get() > 0) {
                        LockMap.get(jedis).set(false);
                        continue; // 跳过正在使用的连接
                    }
                    
                    // 简单的ping测试确认连接可用
                    String result = jedis.ping();
                    if (!"PONG".equals(result)) {
                        unhealthyCount++;
                        // 连接不健康，标记为损坏并移除
                        log.warn("Redis连接不健康，移除并重建: {}", jedis);
                        sourceMap.remove(jedis);
                        useMap.remove(jedis);
                        LockMap.remove(jedis);
                        TryCatch.trying(() -> redisInstance.returnBrokenResource(jedis));
                    }
                } catch (Exception e) {
                    unhealthyCount++;
                    // 发生异常，连接可能已断开
                    log.error("Redis连接健康检查异常，移除并重建: {}", jedis, e);
                    sourceMap.remove(jedis);
                    useMap.remove(jedis);
                    LockMap.remove(jedis);
                    TryCatch.trying(() -> redisInstance.returnBrokenResource(jedis));
                } finally {
                    if (LockMap.containsKey(jedis)) {
                        LockMap.get(jedis).set(false);
                    }
                }
            }
        }
        
        // 如果大部分连接都不健康，记录警告
        if (totalConnections > 0 && (double)unhealthyCount/totalConnections > 0.5) {
            log.warn("Redis连接池健康状况不佳: {}% 的连接不健康", (unhealthyCount * 100) / totalConnections);
        }
        
        // 确保连接池保持最小连接数
        loadSource(minInstance);
    }

    public static <E>E execBaseWithRetry(LingQueRedis.BaseSimpleExec exec, int maxRetries) {
        int retryCount = 0;
        Jedis jedis = null;
        Exception lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                jedis = getRedisInstance();
                // 添加连接测试
                if (retryCount > 0) {
                    try {
                        String pingResult = jedis.ping();
                        if (!"PONG".equals(pingResult)) {
                            throw new JedisConnectionException("连接测试失败: " + pingResult);
                        }
                    } catch (Exception e) {
                        log.warn("连接测试失败，尝试获取新连接", e);
                        returnJedis(jedis, 0);
                        jedis = getRedisInstance();
                    }
                }
                
                return (E)exec.exec(jedis);
            } catch (JedisConnectionException j) {
                lastException = j;
                log.warn("Redis连接异常，尝试重试 ({}/{})", retryCount + 1, maxRetries, j);
                if (jedis != null) {
                    returnJedis(jedis, 0); // 标记为损坏连接
                    jedis = null;
                }
                retryCount++;
                
                // 避免立即重试，增加短暂延迟
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                // 处理其他异常
                log.error("执行Redis操作时发生非连接异常", e);
                if (jedis != null) {
                    returnJedis(jedis, 1);
                }
                throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
            } finally {
                if (jedis != null) {
                    returnJedis(jedis, 1);
                }
            }
        }
        
        throw new RuntimeException("Redis操作失败，已重试" + maxRetries + "次", lastException);
    }

}
