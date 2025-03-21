package cn.lingque.redis;

import cn.lingque.config.LQProperties;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

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
        Thread run = new Thread(()->{
           while (true){
               TryCatch.trying(()-> cleanSource());
               TryCatch.trying(()->autoLoadSource());
               TryCatch.trying(()->{
                   Thread.sleep(500L);
               });
           }
        });
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
    private static void loadSource(int minInstance){
        if (sourceMap.size() < minInstance){
            for (int i = 0; i< minInstance - sourceMap.size(); i++){
                Jedis jedis = redisInstance.getRedisTemplate();
                sourceMap.put(jedis,System.currentTimeMillis()+timeout);
                useMap.put(jedis,new AtomicInteger(0));
                LockMap.put(jedis,new AtomicBoolean(false));
            }
        }
    }

    /**
     * 获取Jedis
     */
    public static Jedis getRedisInstance() {
        if (redisInstance == null) {
            throw new RuntimeException("redisInstance is null , please init redisInstance first");
        }
        if (sourceMap.isEmpty()) {
            throw new RuntimeException("sourceMap is null , please run JedisProxy.init or JedisProxy.runCheck");
        }

        Map<Jedis,Long> tmp = sourceMap;
        int i = index.incrementAndGet() - 1;
        if (i >= tmp.size()){
            i = 0;
            index.set(0);
        }
        Jedis instance = tmp.keySet().stream().collect(Collectors.toList()).get(i);
        if (LockMap.get(instance).compareAndSet(false,true)){
            useTime(instance);
            LockMap.get(instance).set(false);
            return instance;
        }
        //递归
        return getRedisInstance();
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
                useMap.get(jedis).decrementAndGet();
            }
    }


}
