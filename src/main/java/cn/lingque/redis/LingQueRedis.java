package cn.lingque.redis;
import cn.lingque.base.LQKey;
import cn.lingque.mq.exten.LQLazyQueue;
import cn.lingque.mq.exten.LQSequenceQueue;
import cn.lingque.mq.exten.LQUniqueQueue;
import cn.lingque.redis.exten.*;
import cn.lingque.scene.LQScene;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.function.Supplier;

/**
 * <p>
 * redis 增强,出于安全考虑，不使用lua脚本
 * </p>
 *
 * @author zlm
 * @date 2022/8/8
 * @since 1.0
 */
@Slf4j
public class LingQueRedis extends BaseOpt{


    public ValueOpt ofValue(){
        return new ValueOpt(this);
    }

    public SetOpt ofSet(){
        return new SetOpt(this);
    }
    public SortedSetOpt ofZSet(){
        return new SortedSetOpt(this);
    }
    public HashOpt ofHash(){
        return new HashOpt(this);
    }
    public GeoOpt ofGeo(){
        return new GeoOpt(this);
    }

    public ListOpt ofList(){
        return new ListOpt(this);
    }

    public LQScene ofScene(){
        return new LQScene(this);
    }

    public LQLazyQueue ofLazyQueue(){
        return new LQLazyQueue(this);
    }
    public LQSequenceQueue ofSequenceQueue(){
        return new LQSequenceQueue(this);
    }
    public LQUniqueQueue ofUniqueQueue(){
        return new LQUniqueQueue(this);
    }
    public LockOpt ofLock(){
        return new LockOpt(this);
    }

    /**>:--------------------------------------------构建-----------------------------------------------**/

    public static  LingQueRedis ofKey(LQKey key, Object... params) {
        if (null == key) {
            throw new RuntimeException("key 不允许为空");
        }
        LingQueRedis plus = new LingQueRedis();
        plus.key = key.buildKey(params);
        plus.ttl = key.getTtl();
        return plus;
    }


    /**
     * 关联一个key，基于普通String构建
     * @param key
     * @return
     */
    public static LingQueRedis ofKey(String key, Long ttl) {
        LingQueRedis plus = new LingQueRedis();
        plus.key = key;
        plus.ttl = ttl;
        return plus;
    }



    /**执行后重制缓存*/
    public <T>T run(Supplier<T> runner){
        try {
            return runner.get();
        }finally {
            resetTTL();
        }
    }

    /**
     * 获取原始执行器
     * @param exec
     * @return
     * @param <T>
     */
    public <T>T exec(BaseExec<T> exec){
        try (Jedis jedis = getRedisTemplate()){
            return exec.exec(jedis,key,ttl,this);
        }
    }

    /**
     * 获取原始执行器-需要自动重置时间的时候可以使用
     * @param exec
     * @return
     * @param <T>
     */
    public <T>T execWithRunner(BaseExec<T> exec){
        try (Jedis jedis = getRedisTemplate()) {
            return run(() -> exec.exec(jedis, key, ttl, this));
        }
    }


    public static interface BaseExec<T>{
        <T> T exec(Jedis redis,String key,long ttl,LingQueRedis lingQueRedis);
    }

    public static interface BaseSimpleExec{
        Object exec(Jedis redis);
    }

    /**
     * 获取原始执行器
     * @param exec
     * @return
     */
    public Object execBase(BaseSimpleExec exec){
        Jedis jedis = getRedisTemplate();
        try{
            return exec.exec(jedis);
        }catch (JedisConnectionException j){
            JedisProxy.returnJedis(jedis,0);
            throw new RuntimeException(j);
        } finally {
            if (jedis != null) {
                JedisProxy.returnJedis(jedis,1);
            }
        }
    }

}
