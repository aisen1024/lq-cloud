package com.lingque.mq.exten;

import com.lingque.mq.exten.itf.ILQMessage;
import com.lingque.mq.exten.itf.IMQConsumer;
import com.lingque.redis.LingQueRedis;
import com.lingque.thread.LQThreadUtil;
import com.lingque.util.LQUtil;
import com.lingque.util.TryCatch;

import java.util.List;

/**
 * @author aisen
 * @date 2024/10/10
 * @desc 唯一队列，去重
 **/
public class LQUniqueQueue<T> implements  IMQConsumer<ILQMessage<T>,T> {

    private LingQueRedis<T> redis;

    /**
     * 发送MQ
     * @param message
     */
    public void pushMessage(Object message){
        redis.ofSet().add(message);
    }
    /**
     * 取消消息
     * @param message
     * @return
     */
    public boolean cancelMessage(Object message){
         return redis.ofSet().deleteMember(message);
    }


    @Override
    public void consumer(List<ILQMessage<T>> handle) {
        Class<?> beanClass = LQUtil.getTypeClass(this.getClass());
        String messages = redis.ofSet().pop();
        if (LQUtil.isNotEmpty(messages)) {
            boolean isBase = LQUtil.isBasClass(beanClass);
            T data = (T) (isBase ? LQUtil.baseClassTran(messages, beanClass) : LQUtil.jsonToBean(messages, beanClass));
            handle.forEach(h->{
                LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(data)));
            });
        }
    }
}
