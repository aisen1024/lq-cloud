package cn.lingque.mq.core;

import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc MQ统一启动器 - 管理所有MQ的生命周期
 * 
 * <p>核心功能：
 * - 统一启动List、ZSet、Stream三种MQ
 * - 管理MQ生命周期
 * - 定期清理过期的取消消息标记
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 1. 注册订阅（应用启动时）
 * LQMQSubscriptionManager.getInstance()
 *     .subscribe("order:created", new ILQMessage<Order>() {
 *         public void handle(Order order) {
 *             // 处理订单...
 *         }
 *         public Class<Order> getEntityClass() {
 *             return Order.class;
 *         }
 *     });
 * 
 * // 2. 启动所有MQ
 * LQMQStarter.getInstance().start();
 * 
 * // 3. 发送消息
 * LQMQStarter.getInstance().getListMQ().sendMessage("order:created", order);
 * 
 * // 4. 关闭（应用停止时）
 * LQMQStarter.getInstance().stop();
 * }</pre>
 **/
@Slf4j
public class LQMQStarter {
    
    /**
     * 单例实例
     */
    private static volatile LQMQStarter instance;
    
    /**
     * List MQ
     */
    private final ListMQ listMQ;
    
    /**
     * ZSet MQ
     */
    private final ZSetMQ zsetMQ;
    
    /**
     * Stream MQ
     */
    private final StreamMQ streamMQ;
    
    /**
     * 订阅管理器
     */
    private final LQMQSubscriptionManager subscriptionManager;
    
    /**
     * 运行状态
     */
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /**
     * 清理间隔（5分钟）
     */
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000L;
    
    private LQMQStarter() {
        this.subscriptionManager = LQMQSubscriptionManager.getInstance();
        
        // 初始化三种MQ
        this.listMQ = new ListMQ();
        this.zsetMQ = new ZSetMQ();
        this.streamMQ = new StreamMQ();
        
        // 设置订阅管理器
        this.listMQ.setSubscriptionManager(subscriptionManager);
        this.zsetMQ.setSubscriptionManager(subscriptionManager);
        this.streamMQ.setSubscriptionManager(subscriptionManager);
    }
    
    /**
     * 获取单例实例
     */
    public static LQMQStarter getInstance() {
        if (instance == null) {
            synchronized (LQMQStarter.class) {
                if (instance == null) {
                    instance = new LQMQStarter();
                }
            }
        }
        return instance;
    }
    
    /**
     * 启动所有MQ
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("========================================");
            log.info("========== 启动 LQ MQ 系统 ==========");
            log.info("========================================");
            
            // 打印订阅信息
            subscriptionManager.printStatus();
            
            // 启动三种MQ
            listMQ.start();
            
            zsetMQ.start();
            
            streamMQ.start();
            
            // 启动取消消息清理器
            startCancellationCleaner();
            
            log.info("========================================");
            log.info("========== LQ MQ 系统启动完成 ==========");
            log.info("========================================");
        }
    }
    
    /**
     * 停止所有MQ
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("========== 停止 LQ MQ 系统 ==========");
            
            // 停止三种MQ
            listMQ.stop();
            zsetMQ.stop();
            streamMQ.stop();
            
            // 关闭订阅管理器
            subscriptionManager.shutdown();
            
            log.info("========== LQ MQ 系统已停止 ==========");
        }
    }
    
    /**
     * 启动取消消息清理器
     */
    private void startCancellationCleaner() {
        log.info("启动取消消息清理器，清理间隔: {}ms", CLEANUP_INTERVAL);
        
        LQThreadUtil.execSlave(() -> {
            while (isRunning.get()) {
                try {
                    TryCatch.trying(() -> {
                        long cleaned = MessageCancellationHandler.getInstance().cleanExpired();
                        if (cleaned > 0) {
                            log.info("清理过期取消标记: {} 条", cleaned);
                        }
                    });
                    
                    Thread.sleep(CLEANUP_INTERVAL);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("清理取消消息时发生错误", e);
                }
            }
            log.info("取消消息清理器已停止");
        });
    }
    
    // ==================== Getter ====================
    
    /**
     * 获取List MQ
     */
    public ListMQ getListMQ() {
        return listMQ;
    }
    
    /**
     * 获取ZSet MQ
     */
    public ZSetMQ getZSetMQ() {
        return zsetMQ;
    }
    
    /**
     * 获取Stream MQ
     */
    public StreamMQ getStreamMQ() {
        return streamMQ;
    }
    
    /**
     * 获取订阅管理器
     */
    public LQMQSubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }
    
    /**
     * 获取运行状态
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 打印系统状态
     */
    public void printStatus() {
        log.info("========== MQ系统状态 ==========");
        log.info("运行状态: {}", isRunning.get() ? "运行中" : "已停止");
        log.info("");
        
        log.info("List MQ: {}", listMQ.isRunning() ? "运行中" : "已停止");
        log.info("ZSet MQ: {}", zsetMQ.isRunning() ? "运行中" : "已停止");
        log.info("Stream MQ: {}", streamMQ.isRunning() ? "运行中" : "已停止");
        log.info("");
        
        subscriptionManager.printStatus();
        
        log.info("取消消息数: {}", MessageCancellationHandler.getInstance().getCancelledCount());
        log.info("================================");
    }
}
