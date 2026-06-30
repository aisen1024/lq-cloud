package cn.lingque.redis.exten;

import cn.lingque.redis.LingQueRedis;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 分布式锁实现
 */
@Slf4j
public class LockOpt extends BaseOpt{

    private LingQueRedis lingQueRedis;

    public LockOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

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
                lingQueRedis.resetTTL(lazyUnlockTime < 1 ? 1 : lazyUnlockTime);
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
                lingQueRedis.resetTTL(lazyUnlockTime < 1 ? 1 : lazyUnlockTime);
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
                lingQueRedis.resetTTL(lazyUnlockTime < 1 ? 1 : lazyUnlockTime);
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
        if (lingQueRedis.ofValue().setNx(Long.toString(threadId)) || (isItReentrant && threadId.equals(lockThreadId.get()))){
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
            lingQueRedis.delete();
            return true;
        }
        //自旋锁状态下需要陆续退出，直到为0才自动释放
        if (threadId.equals(ownerThreadId)) {
            int current = lockReentrant.decrementAndGet();
            if (current <= 0 ){
                lingQueRedis.delete();
                return true;
            }
        }
        log.error("当前线程（{}）释放锁[{}]失败，该锁不属于当前线程，锁拥有者是ThreadId:{}", threadId,  lingQueRedis.key, ownerThreadId);
        return false;
    }
}
