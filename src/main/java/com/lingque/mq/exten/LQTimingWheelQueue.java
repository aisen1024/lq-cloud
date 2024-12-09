package com.lingque.mq.exten;

import com.lingque.mq.exten.itf.IMQConsumer;
import com.lingque.mq.exten.itf.ITimingWheelHandle;
import com.lingque.redis.LingQueRedis;
import com.lingque.thread.LQThreadUtil;
import com.lingque.util.LQUtil;
import com.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class LQTimingWheelQueue<T> implements IMQConsumer<ITimingWheelHandle<T>,T> {
    private LingQueRedis<T> redis;

    private static final String DATE_KEY = "$HLH$";


    public LingQueRedis rebuildRedis(String date){
        String[] keys = redis.key.split(":");
        int index = keys.length - 2;
        String finalKey = "";
        for (int i = 0; i < index; i++) {
            finalKey += keys[i] + ":";
            if (index == i) {
                finalKey += date + ":";
            }
        }
        finalKey = finalKey.substring(0, finalKey.length() - 1);
        return LingQueRedis.ofKey(finalKey,redis.ttl);
    }


    @Override
    public void consumer(List<ITimingWheelHandle<T>> handle) {
        Class<?> beanClass = LQUtil.getTypeClass(this.getClass());
        String msg = null;
        String timeKey = handle.get(0).timeKey();
        if (LQUtil.isEmpty(timeKey)){
            return;
        }
        LingQueRedis rds = rebuildRedis(timeKey);
        try {
            msg = rds.ofSet().pop();
            if (LQUtil.isNotEmpty(msg)) {
                try {
                    final String finalMsg = msg;
                    boolean isBase = LQUtil.isBasClass(beanClass);
                    T data = (T) (isBase ? LQUtil.baseClassTran(finalMsg, beanClass) : LQUtil.jsonToBean(finalMsg, beanClass));
                    handle.forEach(h->{
                        LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(data)));
                    });
                } catch (Exception e) {
                    log.error("TimingWheelQueue MQ {} 消费 {} 异常 ", rds.key, msg);
                }
            }
        } catch (Exception e) {
            log.error("订阅SET异常 | {} ",rds.key);
        }
    }
}
