package cn.lingque.bus.enhanced;

/**
 * 增强版消息总线订阅者接口
 * 用于处理接收到的消息
 * 
 * @author aisen
 * @date 2024-12-19
 */
public interface BusSubscriber<T> {
    
    /**
     * 处理接收到的消息
     * @param message 消息内容
     * @param busMessage 完整的总线消息对象
     */
    void onMessage(T message, EnhancedBusMessage busMessage);
    
    /**
     * 获取消息类型
     * @return 消息类型Class
     */
    Class<T> getMessageClass();
    
    /**
     * 获取订阅者名称（可选，用于日志和调试）
     * @return 订阅者名称
     */
    default String getSubscriberName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 是否异步处理消息（默认为true）
     * @return true表示异步处理，false表示同步处理
     */
    default boolean isAsync() {
        return true;
    }
    
    /**
     * 获取处理优先级（数字越小优先级越高，默认为0）
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
}