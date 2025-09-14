package cn.lingque.mq.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * @author aisen
 * @date 2024/10/8
 * @desc 顺序队列
 * @deprecated 建议使用 LQUnifiedQueue 替代，功能更强大且性能更好
 **/
@Deprecated
@AllArgsConstructor
public class LQSequenceQueue<T> implements  IMQConsumer<ILQMessage<T>,T>{

    private final LingQueRedis redis;

    /**
     * 默认发送MQ
     * @param message
     */
    public long pushMessage(Object message){
       return pushMessageInsertAfter(message);
    }

    /**
     * 把消息插到最前面
     * @param message
     */
    public long pushMessageInsertBefore(Object message){
       return redis.ofList().rpush(LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message));
    }


    /**
     * 把消息插到最后面 【默认】
     * @param message
     */
    public long pushMessageInsertAfter(Object message){
        return redis.ofList().lpush(LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message));
    }

    /**
     * 取消消息
     * @param message
     * @return
     */
    public boolean cancelMessage(Object message){
       return cancelMessageAfter(message);
    }

    /**
     * 取消最前面的插入的消息
     * @param message
     * @return
     */
    public boolean cancelMessageBefore(Object message){
       return redis.ofList().afterDeleteFirst(LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message),1) > 0;
    }

    /**
     * 取消末尾的消息
     * @param message
     * @return
     */
    public boolean cancelMessageAfter(Object message){
        return redis.ofList().beforeDeleteFirst(LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message),1) > 0;
    }

    /**
     * 消费mq
     * @param handle
     */
    @Override
    public void consumer(List<ILQMessage<T>> handle) {
        List<String> messages = redis.ofList().rpop(1);
        if (!messages.isEmpty()) {
            messages.forEach(m -> {
                handle.forEach(h->{
                    LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(
                            (LQUtil.isBasClass(h.getEntityClass()) ? LQUtil.baseClassTran(m, h.getEntityClass()) : LQUtil.jsonToBean(m, h.getEntityClass()))
                    )));
                });
            });
        }
    }


}
