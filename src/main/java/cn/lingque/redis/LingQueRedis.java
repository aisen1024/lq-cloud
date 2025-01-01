package cn.lingque.redis;

import cn.hutool.json.JSONUtil;
import cn.lingque.base.LQKey;
import cn.lingque.config.LQProperties;
import cn.lingque.mq.exten.LQLazyQueue;
import cn.lingque.mq.exten.LQSequenceQueue;
import cn.lingque.mq.exten.LQTimingWheelQueue;
import cn.lingque.mq.exten.LQUniqueQueue;
import cn.lingque.redis.exten.*;
import cn.lingque.scene.LQScene;
import cn.lingque.thread.LQThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
public class LingQueRedis<T>{
    /**本对象的操作key*/
    public String key;
    /**缓存时间，单位秒*/
    public Long ttl;
    //-----------------------------key操作配置----------------------------------//

    /**空缓存值*/
    public String NULL_VALUE = "$$NULL$$";
    /**默认内置的成功标记*/
    public String OK = "ok";
    /**默认内置的失败标记*/
    public String FAIL = "fail";


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
    public GeoOps ofGeo(){
        return new GeoOps(this);
    }

    public ListOpt ofList(){
        return new ListOpt(this);
    }

    public LQScene ofScene(){
        return new LQScene(this);
    }

    public LQLazyQueue<T> ofLazyQueue(){
        return new LQLazyQueue(this);
    }

    public LQSequenceQueue<T> ofSequenceQueue(){
        return new LQSequenceQueue(this);
    }

    public LQTimingWheelQueue<T> ofTimingWheelQueue(){
        return new LQTimingWheelQueue(this);
    }

    public LQUniqueQueue<T> ofUniqueQueue(){
        return new LQUniqueQueue(this);
    }

    /**>:--------------------------------------------构建-----------------------------------------------**/

    public static  LingQueRedis ofKey(LQKey key, Object... params) {
        if (null == key) {
            throw new RuntimeException("key 不允许为空");
        }
        LingQueRedis plus = new LingQueRedis<>();
        plus.key = key.buildKey(params);
        plus.ttl = key.getTtl();
        return plus;
    }


    /**
     * 关联一个key，基于普通String构建
     * @param key
     * @return
     */
    public static <T> LingQueRedis<T> ofKey(String key, Long ttl) {
        LingQueRedis<T> plus = new LingQueRedis<>();
        plus.key = key;
        plus.ttl = ttl;
        return plus;
    }
    /**>:----------------------公共操作-------------------------**/
    /**
     * 是否存在
     *
     * @return
     */
    public Boolean isExist() {
        return getRedisTemplate().exists(key);
    }

    /**
     * 是否永久key
     *
     * @return
     */
    public Boolean isForeverKey() {
        return getRedisTemplate().ttl(key)==-1;
    }

    /**
     * 设置过期时间
     */
    public void resetTTL() {
        resetTTL(ttl);
    }


    /**
     * 获取过期时间
     */
    public Long getTTL() {
        return getRedisTemplate().ttl(key);
    }

    /**
     * 删除key
     *
     * @return
     */
    public boolean delete() {
        return getRedisTemplate().del(key) > 0;
    }

    /**
     * 是否为空缓存
     *
     * @param value
     * @return
     */
    public boolean isNullCache(Object value) {
        return null != value && Objects.equals(NULL_VALUE,value.toString());
    }


    /**
     * 重置ttl的时间
     * @param t 时间 单位秒
     */
    public void resetTTL(Long t) {
        if (t > 0){
            getRedisTemplate().expire(key, t);
        }
    }

    /**
     * ----------------------------------------------分布式锁实现-------------------------------------------------------------------
     */

    /**
     * 锁
     *
     * @param waitTimes       等待锁释放时间
     * @param successFunction 成功获取锁后执行的函数
     * @param failFunction    获取锁失败之后执行的函数
     * @param <T>
     * @return
     */
    public <T> T lockFuture(Long waitTimes, Supplier<T> successFunction, Supplier<T> failFunction) {
        if (tryLock(waitTimes)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                unlock();
            }
        } else {
            return null == failFunction ? null : failFunction.get();
        }
    }

    /**
     * 锁,执行完后延迟释放
     *
     * @param waitTimes       等待锁释放时间
     * @param successFunction 成功获取锁后执行的函数
     * @param failFunction    获取锁失败之后执行的函数
     * @param lazyUnlockTime  延迟释放锁的时间
     * @param <T>
     * @return
     */
    public <T> T lockFutureAndLazy(Long waitTimes, Supplier<T> successFunction, Supplier<T> failFunction, Long lazyUnlockTime) {
        if (tryLock(waitTimes)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                resetTTL(lazyUnlockTime < 1 ? 1 : lazyUnlockTime);
            }
        } else {
            return null == failFunction ? null : failFunction.get();
        }
    }

    /**
     * 锁,执行完后延迟释放
     *
     * @param successFunction 成功获取锁后执行的函数
     * @param failFunction    获取锁失败之后执行的函数
     * @param lazyUnlockTime  延迟释放锁的时间
     * @param <T>
     * @return
     */
    public <T> T lockFutureAndLazy(Supplier<T> successFunction, Supplier<T> failFunction, Long lazyUnlockTime) {
        if (lock(false)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                resetTTL(lazyUnlockTime < 1 ? 1 : lazyUnlockTime);
            }
        } else {
            return null == failFunction ? null : failFunction.get();
        }
    }

    /**
     * 锁,执行完后延迟释放
     *
     * @param successFunction 成功获取锁后执行的函数
     * @param lazyUnlockTime  延迟释放锁的时间
     * @param <T>
     * @return
     */
    public <T> T lockFutureAndLazy(Supplier<T> successFunction, Long lazyUnlockTime) {
        if (lock(false)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                resetTTL(lazyUnlockTime < 1 ? 1 : lazyUnlockTime);
            }
        }
        return null;
    }

    /**
     * 获取锁，不等待
     * @param successFunction 成功获取锁后执行的函数
     * @param failFunction    获取锁失败之后执行的函数
     * @param <T>
     * @return
     */
    public <T> T lockFuture(Supplier<T> successFunction, Supplier<T> failFunction) {
        if (lock(false)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                unlock();
            }
        } else {
            return null == failFunction ? null : failFunction.get();
        }
    }


    /**
     * 获取锁
     *
     * @param waitTimes       等待锁的时间
     * @param successFunction 成功获取锁后执行的函数
     * @param <T>
     * @return
     */
    public <T> T lockFuture(Long waitTimes, Supplier<T> successFunction) {
        if (tryLock(waitTimes)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                unlock();
            }
        }
        throw new RuntimeException("获取锁失败");
    }

    /**
     * 获取锁，失败直接返回
     *
     * @param successFunction
     * @param <T>
     * @return
     */
    public <T> T lockFuture(Supplier<T> successFunction) {
        if (lock(false)) {
            try {
                return null == successFunction ? null : successFunction.get();
            } finally {
                unlock();
            }
        }
        throw new RuntimeException("获取锁失败");
    }

    /**
     * 尝试去获取一把锁，等待waitSeconds秒，如果没有获取到就放弃，自旋获取锁 set nx
     *
     * @param waitSeconds 等待获取锁多久，单位秒
     * @return true 获取到一把锁 false 获取锁失败
     */
    public boolean tryLock(Long waitSeconds) {
        long endTime = System.currentTimeMillis() + (waitSeconds * 1000);
        while (endTime - System.currentTimeMillis() > 0) {
            boolean r = lock(false);
            if (r) {
                return r;
            }
            try {
                Thread.sleep(500L);
            } catch (Exception e) {
                log.error("tryLock thread is exception", e);
            }
        }
        return false;
    }

    private AtomicInteger lockReentrant = new AtomicInteger(0);
    private AtomicLong lockThreadId = new AtomicLong(0);

    /**
     * 简单的获取锁，可重入（如果当前线程是锁的拥有者，允许再次拥有锁）
     *
     * @return true获取到一把锁  false 获取锁失败
     */
    public boolean lock(boolean isItReentrant) {
        Long threadId = Thread.currentThread().getId();
        //如果顺利获取锁或者还是当前线程锁拥有者，则默认获得锁
        if (ofValue().setNx(Long.toString(threadId)) || (isItReentrant && threadId.equals(lockThreadId.get()))){
           if (isItReentrant){
               lockThreadId.set(threadId);
               lockReentrant.incrementAndGet();
           }
           return true;
        }
        return false;
    }


    /**
     * 释放锁，优先判断锁的拥有者是同一个才允许删除锁，否则删除失败，这样就可以避免并发情况下误删的情况
     * tips：这里锁的释放判断过期时间大于1秒才去删，否则自动过期，所以这里更加适合做定时器的锁，如果要用在接口上，请结合lua脚本一起用，这样才能保证原子性，不需要判断时间
     *
     * @return true 释放 false 失败
     */
    public boolean unlock() {
        Long threadId = Thread.currentThread().getId();
        Long ownerThreadId = lockThreadId.get();
        //非自旋锁状态下直接释放
        if (ownerThreadId == 0L){
            delete();
            return true;
        }
        //自旋锁状态下需要陆续退出，直到为0才自动释放
        if (threadId.equals(ownerThreadId)) {
            int current = lockReentrant.decrementAndGet();
            if (current <= 0 ){
                delete();
                return true;
            }
        }
        log.error("当前线程（{}）释放锁[{}]失败，该锁不属于当前线程，锁拥有者是ThreadId:{}", threadId, key, ownerThreadId);
        return false;
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
        try {
            return exec.exec(jedis);
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
    }
    private Jedis getRedisTemplate(){
        return JedisProxy.getRedisInstance().getRedisTemplate();
    }
}
