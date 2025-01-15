package cn.lingque.mq.exten;

import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.mq.exten.itf.ITimingWheelHandle;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
@AllArgsConstructor
public class LQTimingWheelQueue<T> implements IMQConsumer<ITimingWheelHandle<T>,T> {
    private final LingQueRedis redis;

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
                    handle.forEach(h->{
                        LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(
                                (LQUtil.isBasClass(h.getEntityClass()) ? LQUtil.baseClassTran(finalMsg, h.getEntityClass()) : LQUtil.jsonToBean(finalMsg, h.getEntityClass()))
                        )));
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
