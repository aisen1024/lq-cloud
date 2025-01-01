package cn.lingque.mq.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * @author aisen
 * @date 2024/10/10
 * @desc 延迟队列
 **/
@AllArgsConstructor
public class LQLazyQueue<T> implements IMQConsumer<ILQMessage<T>,T> {

    private final LingQueRedis<T> redis;

    /**
     * 发送MQ
     * @param message
     */
    public void pushMessage(Object message,Long lazyTime){
        redis.ofZSet().setScore(LQUtil.isBaseValue(message) ? message.toString(): JSONUtil.toJsonStr(message),lazyTime.doubleValue());
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
        TryCatch.trying(() -> {
            Long endTime = System.currentTimeMillis();
            List<RedisRank> msgList = redis.ofZSet().getByScoreRange(1D, endTime.doubleValue(),20);
            if (msgList.isEmpty()){
                return;
            }
            redis.execBase((jedis) -> {
                if (!msgList.isEmpty()) {
                    for (RedisRank msgInfo : msgList) {
                        String msg = msgInfo.getMemberId();
                        TryCatch.trying(() -> {
                                    String ackKey = redis.key + ":consumer:ack:" + LQUtil.getMD5(msg);
                                    if (jedis.setnx(ackKey, "ack") > 0) {
                                        jedis.expire(ackKey, 10);
                                        handle.forEach(h -> {
                                            LQThreadUtil.execSlave(() -> TryCatch.trying(() -> h.handle(
                                                    (LQUtil.isBasClass( h.getEntityClass()) ? LQUtil.baseClassTran(msg, h.getEntityClass()) : LQUtil.jsonToBean(msg,  h.getEntityClass()))
                                            )));
                                        });
                                        TryCatch.trying(() -> redis.ofZSet().delete(msg));
                                    }
                                }
                        );
                    }
                }
                return true;
            });
        });
    }
}
