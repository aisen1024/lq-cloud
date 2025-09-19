package cn.lingque.cloud.rpc.circuit;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LQ熔断器
 * 实现熔断保护机制，防止服务雪崩
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQCircuitBreaker {
    
    /** 熔断器状态 */
    public enum State {
        CLOSED,    // 关闭状态（正常）
        OPEN,      // 开启状态（熔断）
        HALF_OPEN  // 半开状态（尝试恢复）
    }
    
    /** 当前状态 */
    private volatile State state = State.CLOSED;
    
    /** 失败计数器 */
    private final AtomicInteger failureCount = new AtomicInteger(0);
    
    /** 成功计数器 */
    private final AtomicInteger successCount = new AtomicInteger(0);
    
    /** 最后失败时间 */
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    /** 下次尝试时间 */
    private final AtomicLong nextAttemptTime = new AtomicLong(0);
    
    /** 失败阈值 */
    private final int failureThreshold;
    
    /** 恢复超时时间（毫秒） */
    private final long recoveryTimeout;
    
    /** 半开状态下的成功阈值 */
    private final int halfOpenSuccessThreshold;
    
    /** 时间窗口（毫秒） */
    private final long timeWindow;
    
    /**
     * 默认构造函数
     */
    public LQCircuitBreaker() {
        this(5, 60000, 3, 10000);
    }
    
    /**
     * 自定义构造函数
     * @param failureThreshold 失败阈值
     * @param recoveryTimeout 恢复超时时间
     * @param halfOpenSuccessThreshold 半开状态成功阈值
     * @param timeWindow 时间窗口
     */
    public LQCircuitBreaker(int failureThreshold, long recoveryTimeout, 
                           int halfOpenSuccessThreshold, long timeWindow) {
        this.failureThreshold = failureThreshold;
        this.recoveryTimeout = recoveryTimeout;
        this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
        this.timeWindow = timeWindow;
    }
    
    /**
     * 是否允许请求通过
     */
    public boolean allowRequest() {
        long currentTime = System.currentTimeMillis();
        
        switch (state) {
            case CLOSED:
                return true;
                
            case OPEN:
                // 检查是否到了尝试恢复的时间
                if (currentTime >= nextAttemptTime.get()) {
                    setState(State.HALF_OPEN);
                    log.info("熔断器进入半开状态，开始尝试恢复");
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * 记录成功调用
     */
    public void recordSuccess() {
        long currentTime = System.currentTimeMillis();
        
        switch (state) {
            case CLOSED:
                resetCounters();
                break;
                
            case HALF_OPEN:
                int currentSuccessCount = successCount.incrementAndGet();
                if (currentSuccessCount >= halfOpenSuccessThreshold) {
                    setState(State.CLOSED);
                    resetCounters();
                    log.info("熔断器恢复到关闭状态");
                }
                break;
                
            case OPEN:
                // 在开启状态下不应该有成功调用
                break;
        }
    }
    
    /**
     * 记录失败调用
     */
    public void recordFailure() {
        long currentTime = System.currentTimeMillis();
        lastFailureTime.set(currentTime);
        
        switch (state) {
            case CLOSED:
                int currentFailureCount = failureCount.incrementAndGet();
                if (currentFailureCount >= failureThreshold) {
                    setState(State.OPEN);
                    nextAttemptTime.set(currentTime + recoveryTimeout);
                    log.warn("熔断器开启，失败次数: {}", currentFailureCount);
                }
                break;
                
            case HALF_OPEN:
                setState(State.OPEN);
                nextAttemptTime.set(currentTime + recoveryTimeout);
                resetCounters();
                log.warn("熔断器重新开启，半开状态下出现失败");
                break;
                
            case OPEN:
                // 已经是开启状态，更新下次尝试时间
                nextAttemptTime.set(currentTime + recoveryTimeout);
                break;
        }
    }
    
    /**
     * 获取当前状态
     */
    public State getState() {
        return state;
    }
    
    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * 获取成功次数
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * 获取最后失败时间
     */
    public long getLastFailureTime() {
        return lastFailureTime.get();
    }
    
    /**
     * 手动重置熔断器
     */
    public void reset() {
        setState(State.CLOSED);
        resetCounters();
        log.info("熔断器已手动重置");
    }
    
    /**
     * 设置状态
     */
    private void setState(State newState) {
        this.state = newState;
    }
    
    /**
     * 重置计数器
     */
    private void resetCounters() {
        failureCount.set(0);
        successCount.set(0);
    }
    
    /**
     * 获取熔断器统计信息
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
                state,
                failureCount.get(),
                successCount.get(),
                lastFailureTime.get(),
                nextAttemptTime.get()
        );
    }
    
    /**
     * 熔断器统计信息
     */
    public static class CircuitBreakerStats {
        private final State state;
        private final int failureCount;
        private final int successCount;
        private final long lastFailureTime;
        private final long nextAttemptTime;
        
        public CircuitBreakerStats(State state, int failureCount, int successCount, 
                                 long lastFailureTime, long nextAttemptTime) {
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.lastFailureTime = lastFailureTime;
            this.nextAttemptTime = nextAttemptTime;
        }
        
        // Getters
        public State getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public long getLastFailureTime() { return lastFailureTime; }
        public long getNextAttemptTime() { return nextAttemptTime; }
    }
}