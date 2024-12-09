package com.lingque.mq.exten;

import cn.hutool.json.JSONUtil;
import com.lingque.mq.exten.itf.ILQMessage;
import com.lingque.mq.exten.itf.IMQConsumer;
import com.lingque.redis.LingQueRedis;
import com.lingque.thread.LQThreadUtil;
import com.lingque.util.LQUtil;
import com.lingque.util.TryCatch;

import java.util.List;

/**
 * @author aisen
 * @date 2024/10/8
 * @desc 顺序队列
 **/
public class LQSequenceQueue<T> implements  IMQConsumer<ILQMessage<T>,T>{

    private LingQueRedis<T> redis;

    /**
     * 默认发送MQ
     * @param message
     */
    public void pushMessage(Object message){
        pushMessageInsertAfter(message);
    }

    /**
     * 把消息插到最前面
     * @param message
     */
    public void pushMessageInsertBefore(Object message){
        redis.ofList().rightPush(LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message));
    }


    /**
     * 把消息插到最后面 【默认】
     * @param message
     */
    public void pushMessageInsertAfter(Object message){
        redis.ofList().leftPush(LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message));
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
        Class<?> beanClass = LQUtil.getTypeClass(this.getClass());
        List<String> messages = redis.ofList().rpops(1);
        if (!messages.isEmpty()) {
            messages.forEach(m -> {
                boolean isBase = LQUtil.isBasClass(beanClass);
                T data = (T) (isBase ? LQUtil.baseClassTran(m, beanClass) : LQUtil.jsonToBean(m, beanClass));
                handle.forEach(h->{
                    LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(data)));
                });
            });
        }
    }


}
