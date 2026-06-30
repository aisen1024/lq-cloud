package cn.lingque.mq.core;

import cn.lingque.mq.itf.ILQMessage;
import cn.lingque.thread.LQThread;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc MQ订阅管理器 - 管理topic订阅和消息分发
 * 
 * <p>核心功能：
 * - 管理所有topic的订阅关系
 * - 接收MQ推送的消息并分发给对应的处理器
 * - 使用线程池异步处理消息
 * - 在消费前检查消息是否被取消
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 1. 获取单例
 * LQMQSubscriptionManager manager = LQMQSubscriptionManager.getInstance();
 * 
 * // 2. 注册订阅
 * manager.subscribe("order:created", new ILQMessage<Order>() {
 *     public void handle(Order order) {
 *         // 处理订单...
 *     }
 *     public Class<Order> getEntityClass() {
 *         return Order.class;
 *     }
 * });
 * 
 * // 3. 分发消息（由MQ调用）
 * manager.dispatchMessage("order:created", orderJson, messageId);
 * }</pre>
 **/
@Slf4j
public class LQMQSubscriptionManager {
    
    /**
     * 单例实例
     */
    private static volatile LQMQSubscriptionManager instance;
    
    /**
     * Topic订阅注册表 <topic, Subscription>
     */
    private final ConcurrentHashMap<String, TopicSubscription> subscriptions = new ConcurrentHashMap<>();
    
    /**
     * 消息处理线程池
     */
    private final ExecutorService messageExecutor;
    
    private LQMQSubscriptionManager() {
        // 创建消息处理线程池
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.messageExecutor = Executors.newFixedThreadPool(
            poolSize,
            new LQThread.NamedThreadFactory("mq-handler")
        );
        
        log.info("订阅管理器初始化完成，线程池大小: {}", poolSize);
    }
    
    /**
     * 获取单例实例
     */
    public static LQMQSubscriptionManager getInstance() {
        if (instance == null) {
            synchronized (LQMQSubscriptionManager.class) {
                if (instance == null) {
                    instance = new LQMQSubscriptionManager();
                }
            }
        }
        return instance;
    }
    
    // ==================== 订阅注册 ====================
    
    /**
     * 注册订阅（链式调用）
     * 
     * @param topic topic名称
     * @param handler 消息处理器
     * @return TopicSubscription 用于继续链式调用
     */
    public <T> TopicSubscription subscribe(String topic, ILQMessage<T> handler) {
        TopicSubscription subscription = subscriptions.computeIfAbsent(topic, k -> {
            TopicSubscription sub = new TopicSubscription();
            sub.setTopic(topic);
            sub.setHandlers(new ArrayList<>());
            log.info("注册topic订阅: {}", topic);
            return sub;
        });
        
        subscription.addHandler(handler);
        return subscription;
    }
    
    /**
     * 批量注册订阅
     * 
     * @param topic topic名称
     * @param handlers 消息处理器列表
     */
    public <T> void subscribeAll(String topic, List<ILQMessage<T>> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        
        for (ILQMessage<T> handler : handlers) {
            subscribe(topic, handler);
        }
    }
    
    /**
     * 取消订阅
     * 
     * @param topic topic名称
     */
    public void unsubscribe(String topic) {
        TopicSubscription removed = subscriptions.remove(topic);
        if (removed != null) {
            log.info("取消订阅topic: {}", topic);
        }
    }
    
    // ==================== 消息分发 ====================
    
    /**
     * 分发消息（由MQ调用）
     * 
     * @param topic 消息主题
     * @param message 消息内容
     * @param messageId 消息ID
     */
    public void dispatchMessage(String topic, Object message, String messageId) {
        // 先检查消息是否被取消
        if (MessageCancellationHandler.getInstance().isCancelled(messageId)) {
            log.info("消息已取消，跳过分发: topic={}, messageId={}", topic, messageId);
            MessageCancellationHandler.getInstance().removeCancellation(messageId);
            return;
        }
        
        // 获取订阅信息
        TopicSubscription subscription = subscriptions.get(topic);
        if (subscription == null || subscription.getHandlers().isEmpty()) {
            log.warn("没有找到topic的订阅: {}", topic);
            return;
        }
        
        // 异步处理消息
        List<ILQMessage> handlers = subscription.getHandlers();
        for (ILQMessage handler : handlers) {
            messageExecutor.submit(() -> {
                processMessage(topic, message, messageId, handler);
            });
        }
    }
    
    /**
     * 处理单个消息
     */
    private <T> void processMessage(String topic, Object message, String messageId, ILQMessage<T> handler) {
        TryCatch.trying(() -> {
            // 再次检查消息是否被取消（双重检查）
            if (MessageCancellationHandler.getInstance().isCancelled(messageId)) {
                log.info("消息已取消，跳过处理: topic={}, messageId={}", topic, messageId);
                MessageCancellationHandler.getInstance().removeCancellation(messageId);
                return;
            }
            
            // 转换消息类型
            T typedMessage = convertMessage(message, handler.getEntityClass());
            
            // 调用处理器
            long startTime = System.currentTimeMillis();
            handler.handle(typedMessage);
            long costTime = System.currentTimeMillis() - startTime;
            
            log.debug("消息处理完成: topic={}, messageId={}, cost={}ms", 
                      topic, messageId, costTime);
            
        });
    }
    
    /**
     * 转换消息类型
     */
    @SuppressWarnings("unchecked")
    private <T> T convertMessage(Object message, Class<T> targetClass) {
        if (message == null) {
            return null;
        }
        
        // 如果已经是目标类型，直接返回
        if (targetClass.isInstance(message)) {
            return (T) message;
        }
        
        // 如果是基本类型
        if (LQUtil.isBasClass(targetClass)) {
            return LQUtil.baseClassTran(message.toString(), targetClass);
        }
        
        // 否则按JSON转换
        String json = message instanceof String ? (String) message : LQUtil.toJson(message);
        return LQUtil.jsonToBean(json, targetClass);
    }
    
    // ==================== 查询接口 ====================
    
    /**
     * 获取已注册的topic列表
     */
    public List<String> getRegisteredTopics() {
        return new ArrayList<>(subscriptions.keySet());
    }
    
    /**
     * 获取topic的订阅信息
     */
    public TopicSubscription getSubscription(String topic) {
        return subscriptions.get(topic);
    }
    
    /**
     * 检查是否订阅了指定的topic
     * 
     * @param topic topic名称
     * @return true-已订阅，false-未订阅
     */
    public boolean hasSubscription(String topic) {
        TopicSubscription subscription = subscriptions.get(topic);
        return subscription != null && !subscription.getHandlers().isEmpty();
    }
    
    /**
     * 获取已注册的topic数量
     */
    public int getTopicCount() {
        return subscriptions.size();
    }
    
    /**
     * 获取线程池状态
     */
    public Map<String, Object> getThreadPoolStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        
        if (messageExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) messageExecutor;
            status.put("poolSize", executor.getPoolSize());
            status.put("activeCount", executor.getActiveCount());
            status.put("queueSize", executor.getQueue().size());
            status.put("completedTaskCount", executor.getCompletedTaskCount());
        }
        
        return status;
    }
    
    /**
     * 打印订阅状态
     */
    public void printStatus() {
        log.info("=== MQ订阅管理器状态 ===");
        log.info("已注册topic数: {}", getTopicCount());
        
        if (!subscriptions.isEmpty()) {
            log.info("已注册的topic:");
            subscriptions.forEach((topic, sub) -> {
                log.info("  - {}: {} 个handler", topic, sub.getHandlers().size());
            });
        }
        
        Map<String, Object> poolStatus = getThreadPoolStatus();
        if (!poolStatus.isEmpty()) {
            log.info("线程池状态: {}", poolStatus);
        }
        
        log.info("========================");
    }
    
    /**
     * 关闭订阅管理器
     */
    public void shutdown() {
        log.info("关闭订阅管理器...");
        messageExecutor.shutdown();
        subscriptions.clear();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * Topic订阅信息
     */
    @Data
    public static class TopicSubscription {
        private String topic;
        private List<ILQMessage> handlers = new ArrayList<>();
        
        /**
         * 添加消息处理器（链式调用）
         */
        public <T> TopicSubscription addHandler(ILQMessage<T> handler) {
            this.handlers.add(handler);
            return this;
        }
        
        /**
         * 批量添加消息处理器（链式调用）
         */
        public <T> TopicSubscription addHandlers(List<ILQMessage<T>> handlers) {
            this.handlers.addAll(handlers);
            return this;
        }
    }
}
