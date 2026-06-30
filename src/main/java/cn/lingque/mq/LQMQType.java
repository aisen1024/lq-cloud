package cn.lingque.mq;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc MQ类型枚举
 **/
public enum LQMQType {
    
    /**
     * List队列 - 顺序消息队列，FIFO，高性能
     * 适用场景：日志收集、简单通知、不需要可靠性保证的消息
     */
    LIST("LIST", "顺序消息队列"),
    
    /**
     * ZSet队列 - 延迟消息队列，支持精确延迟时间
     * 适用场景：订单超时、定时任务、延迟通知
     */
    ZSET("ZSET", "延迟消息队列"),
    
    /**
     * Stream队列 - 流队列，支持消费者组和ACK机制，保证可靠性
     * 适用场景：订单处理、支付回调、需要可靠性保证的业务消息
     */
    STREAM("STREAM", "流队列");
    
    private final String type;
    private final String desc;
    
    LQMQType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
    
    public String getType() {
        return type;
    }
    
    public String getDesc() {
        return desc;
    }
}
