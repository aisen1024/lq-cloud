package cn.lingque.mq;

import cn.lingque.mq.core.*;
import cn.lingque.mq.itf.ILQMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc MQ统一操作模板 - 简化MQ操作的门面类
 * 
 * <p>核心功能：
 * - 提供统一的消息发送接口
 * - 支持顺序消息（List队列）
 * - 支持延迟消息（ZSet队列）
 * - 支持流队列消息（Stream队列）
 * - 提供消息取消功能
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 1. 注册订阅（应用启动时）
 * LQMQTemplate.subscribe("order:created", new ILQMessage<Order>() {
 *     public void handle(Order order) {
 *         // 处理订单...
 *     }
 *     public Class<Order> getEntityClass() {
 *         return Order.class;
 *     }
 * });
 * 
 * // 2. 启动MQ系统
 * LQMQTemplate.start();
 * 
 * // 3. 发送顺序消息（List队列）
 * String msgId = LQMQTemplate.sendOrderMessage("order:created", order);
 * 
 * // 4. 发送延迟消息（ZSet队列）
 * String delayMsgId = LQMQTemplate.sendDelayMessage("order:timeout", order, 3600);
 * 
 * // 5. 发送流队列消息（Stream队列）
 * String streamMsgId = LQMQTemplate.sendStreamMessage("order:payment", order);
 * 
 * // 6. 取消消息
 * LQMQTemplate.cancelMessage(msgId);
 * }</pre>
 **/
@Slf4j
public class LQMQTemplate {
    
    /**
     * 获取MQ启动器
     */
    private static LQMQStarter getStarter() {
        return LQMQStarter.getInstance();
    }
    
    /**
     * 获取订阅管理器
     */
    private static LQMQSubscriptionManager getSubscriptionManager() {
        return LQMQSubscriptionManager.getInstance();
    }
    
    // ==================== 生命周期管理 ====================
    
    /**
     * 启动MQ系统
     * 会启动List、ZSet、Stream三种MQ
     */
    public static void start() {
        getStarter().start();
    }
    
    /**
     * 停止MQ系统
     */
    public static void stop() {
        getStarter().stop();
    }
    
    /**
     * 获取运行状态
     * 
     * @return true-运行中，false-已停止
     */
    public static boolean isRunning() {
        return getStarter().isRunning();
    }
    
    /**
     * 打印系统状态
     */
    public static void printStatus() {
        getStarter().printStatus();
    }
    
    // ==================== 订阅管理 ====================
    
    /**
     * 注册订阅
     * 
     * @param topic 主题
     * @param handler 消息处理器
     * @return 订阅管理器（支持链式调用）
     */
    public static <T> LQMQSubscriptionManager.TopicSubscription subscribe(String topic, ILQMessage<T> handler) {
        return getSubscriptionManager().subscribe(topic, handler);
    }
    
    /**
     * 取消订阅
     * 
     * @param topic 主题
     */
    public static void unsubscribe(String topic) {
        getSubscriptionManager().unsubscribe(topic);
    }
    
    // ==================== 顺序消息发送（List队列） ====================
    
    /**
     * 发送顺序消息（使用List队列）
     * 
     * <p>特点：
     * - FIFO顺序保证
     * - 高性能、低延迟
     * - 不保证可靠性
     * 
     * <p>适用场景：
     * - 日志收集
     * - 简单通知
     * - 不需要可靠性保证的消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @return 消息ID
     */
    public static String sendOrderMessage(String topic, Object message) {
        return getStarter().getListMQ().sendMessage(topic, message);
    }
    
    // ==================== 延迟消息发送（ZSet队列） ====================
    
    /**
     * 发送延迟消息（使用ZSet队列）
     * 
     * <p>特点：
     * - 支持精确延迟时间控制
     * - 基于score排序，自动到期处理
     * - 支持消息取消
     * 
     * <p>适用场景：
     * - 订单超时自动取消
     * - 定时任务
     * - 延迟通知
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param delaySeconds 延迟秒数
     * @return 消息ID
     */
    public static String sendDelayMessage(String topic, Object message, long delaySeconds) {
        return getStarter().getZSetMQ().sendDelayMessage(topic, message, delaySeconds);
    }
    
    /**
     * 发送即时消息到ZSet队列（延迟0秒）
     * 
     * @param topic 主题
     * @param message 消息内容
     * @return 消息ID
     */
    public static String sendZSetMessage(String topic, Object message) {
        return getStarter().getZSetMQ().sendMessage(topic, message);
    }
    
    // ==================== 流队列消息发送（Stream队列） ====================
    
    /**
     * 发送流队列消息（使用Stream队列）
     * 
     * <p>特点：
     * - 支持消费者组和ACK机制
     * - 消息持久化，保证可靠性
     * - 支持消息回溯和pending消息处理
     * 
     * <p>适用场景：
     * - 订单处理
     * - 支付回调
     * - 需要可靠性保证的业务消息
     * - 需要消息追溯的场景
     * 
     * @param topic 主题
     * @param message 消息内容
     * @return 消息ID
     */
    public static String sendStreamMessage(String topic, Object message) {
        return getStarter().getStreamMQ().sendMessage(topic, message);
    }
    
    // ==================== 通用消息发送（根据类型选择） ====================
    
    /**
     * 发送消息（根据MQ类型选择）
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param mqType MQ类型（LIST/ZSET/STREAM）
     * @return 消息ID
     */
    public static String sendMessage(String topic, Object message, LQMQType mqType) {
        AbstractMQ mq = getMQByType(mqType);
        return mq.sendMessage(topic, message);
    }
    
    /**
     * 发送延迟消息（根据MQ类型选择）
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param delaySeconds 延迟秒数
     * @param mqType MQ类型（LIST/ZSET/STREAM）
     * @return 消息ID
     */
    public static String sendDelayMessage(String topic, Object message, long delaySeconds, LQMQType mqType) {
        AbstractMQ mq = getMQByType(mqType);
        return mq.sendDelayMessage(topic, message, delaySeconds);
    }
    
    // ==================== 消息取消 ====================
    
    /**
     * 取消消息
     * 
     * <p>说明：
     * - 将消息ID标记为已取消
     * - 消费时自动过滤已取消的消息
     * - 取消标记会在1天后自动过期
     * 
     * @param messageId 消息ID
     * @return 是否成功
     */
    public static boolean cancelMessage(String messageId) {
        return MessageCancellationHandler.getInstance().cancelMessage(messageId);
    }
    
    /**
     * 判断消息是否被取消
     * 
     * @param messageId 消息ID
     * @return true-已取消，false-未取消
     */
    public static boolean isCancelled(String messageId) {
        return MessageCancellationHandler.getInstance().isCancelled(messageId);
    }
    
    /**
     * 移除取消标记
     * 
     * @param messageId 消息ID
     * @return 是否成功
     */
    public static boolean removeCancellation(String messageId) {
        return MessageCancellationHandler.getInstance().removeCancellation(messageId);
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 根据类型获取MQ实例
     */
    private static AbstractMQ getMQByType(LQMQType mqType) {
        switch (mqType) {
            case LIST:
                return getStarter().getListMQ();
            case ZSET:
                return getStarter().getZSetMQ();
            case STREAM:
                return getStarter().getStreamMQ();
            default:
                throw new IllegalArgumentException("不支持的MQ类型: " + mqType);
        }
    }
    
    // ==================== 高级接口（直接获取MQ实例） ====================
    
    /**
     * 获取List MQ实例（顺序消息队列）
     * 
     * @return ListMQ实例
     */
    public static ListMQ getListMQ() {
        return getStarter().getListMQ();
    }
    
    /**
     * 获取ZSet MQ实例（延迟消息队列）
     * 
     * @return ZSetMQ实例
     */
    public static ZSetMQ getZSetMQ() {
        return getStarter().getZSetMQ();
    }
    
    /**
     * 获取Stream MQ实例（流队列）
     * 
     * @return StreamMQ实例
     */
    public static StreamMQ getStreamMQ() {
        return getStarter().getStreamMQ();
    }
    
    /**
     * 获取消息取消处理器
     * 
     * @return 消息取消处理器实例
     */
    public static MessageCancellationHandler getCancellationHandler() {
        return MessageCancellationHandler.getInstance();
    }
}
