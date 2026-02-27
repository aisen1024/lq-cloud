package cn.lingque.mq.itf;

/**
 * @author aisen
 * @date 2024/10/8
 * @desc 消息处理器
 **/
public interface ILQMessage<T> {

    /**
     * 消费消息
     * @param message
     * @return
     */
    void handle(T message);

    Class<T> getEntityClass();
}
