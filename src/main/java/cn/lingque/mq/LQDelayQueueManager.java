package cn.lingque.mq;

import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc 延迟队列消费者管理器 - 统一管理所有延迟队列的处理逻辑
 **/
@Slf4j
public class LQDelayQueueManager {
    
    // 单例实例
    private static volatile LQDelayQueueManager instance;
    
    // 延迟队列处理器注册表 <queueKey, processor>
    private final ConcurrentHashMap<String, DelayProcessor> processors = new ConcurrentHashMap<>();
    
    // 全局启动状态
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    private LQDelayQueueManager() {}
    
    public static LQDelayQueueManager getInstance() {
        if (instance == null) {
            synchronized (LQDelayQueueManager.class) {
                if (instance == null) {
                    instance = new LQDelayQueueManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册延迟队列处理器
     * @param queueKey 队列key
     * @param processor 处理器
     */
    public void registerProcessor(String queueKey, DelayProcessor processor) {
        processors.put(queueKey, processor);
        log.info("注册延迟队列处理器: {}", queueKey);
        
        // 如果管理器还未启动，则启动
        startIfNotRunning();
    }
    
    /**
     * 取消注册延迟队列处理器
     * @param queueKey 队列key
     */
    public void unregisterProcessor(String queueKey) {
        DelayProcessor removed = processors.remove(queueKey);
        if (removed != null) {
            log.info("取消注册延迟队列处理器: {}", queueKey);
        }
    }
    
    /**
     * 启动延迟队列管理器（如果还未启动）
     */
    private void startIfNotRunning() {
        if (isRunning.compareAndSet(false, true)) {
            start();
        }
    }
    
    /**
     * 启动延迟队列管理器
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("启动延迟队列管理器");
            
            LQThreadUtil.execMaster(() -> {
                while (isRunning.get()) {
                    try {
                        if (!processors.isEmpty()) {
                            // 处理所有注册的延迟队列
                            processors.forEach((queueKey, processor) -> {
                                TryCatch.trying(() -> {
                                    LQThreadUtil.execSlave(() -> {
                                        processor.processDelayedMessages();
                                    });
                                }, "处理延迟队列{}时发生异常", queueKey);
                            });
                        }
                        
                        // 延迟队列检查间隔（可以稍长一些，因为延迟消息不需要极高的实时性）
                        Thread.sleep(500);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("延迟队列管理器运行时发生错误", e);
                        try {
                            Thread.sleep(1000); // 错误时等待较长时间
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                log.info("延迟队列管理器已停止");
            });
        }
    }
    
    /**
     * 停止延迟队列管理器
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止延迟队列管理器");
            processors.clear();
        }
    }
    
    /**
     * 获取当前运行状态
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 获取已注册的处理器数量
     */
    public int getProcessorCount() {
        return processors.size();
    }
    
    /**
     * 延迟处理器接口
     */
    @FunctionalInterface
    public interface DelayProcessor {
        /**
         * 处理延迟消息
         */
        void processDelayedMessages();
    }
}
