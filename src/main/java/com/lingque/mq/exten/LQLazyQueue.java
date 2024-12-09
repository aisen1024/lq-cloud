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
 * @date 2024/10/10
 * @desc 延迟队列
 **/
public class LQLazyQueue<T> implements IMQConsumer<ILQMessage<T>,T> {

    private LingQueRedis<T> redis;

    /**
     * 发送MQ
     * @param message
     */
    public void pushMessage(Object message,Long lazyTime){
        redis.ofZSet().add(LQUtil.isBaseValue(message) ? message.toString(): JSONUtil.toJsonStr(message),lazyTime.doubleValue());
    }
    /**
     * 取消消息
     * @param message
     * @return
     */
    public boolean cancelMessage(Object message){
         return redis.ofZSet().delete(LQUtil.isBaseValue(message) ? message.toString(): JSONUtil.toJsonStr(message));
    }


    @Override
    public void consumer(List<ILQMessage<T>> handle) {
        Class<?> beanClass = LQUtil.getTypeClass(this.getClass());
        TryCatch.trying(() -> {
            Long endTime = System.currentTimeMillis();
            List<String> msgList = redis.ofZSet().getByScore(1D, endTime.doubleValue());
            //控制量,每次只处理10个,避免过长
            if (msgList.size() > 10) {
                msgList = msgList.subList(0, 10);
            }
            if (!msgList.isEmpty()) {
                for (String msg : msgList) {
                    TryCatch.trying(() -> {
                                String ackKey = redis.key + ":consumer:ack:" + LQUtil.getMD5(msg);
                                if (redis.getRedisTemplate().setnx(ackKey, "ack") > 0) {
                                    redis.getRedisTemplate().expire(ackKey, 10);
                                    boolean isBase = LQUtil.isBasClass(beanClass);
                                    T data = (T) (isBase ? LQUtil.baseClassTran(msg, beanClass) : LQUtil.jsonToBean(msg, beanClass));
                                    handle.forEach(h->{
                                        LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(data)));
                                    });
                                    TryCatch.trying(() -> redis.ofZSet().delete(msg));
                                }
                            }
                    );
                }

            }
        });
    }
}
