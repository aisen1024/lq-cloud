package cn.lingque.mq.exten;

import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * @author aisen
 * @date 2024/10/10
 * @desc 唯一队列，去重
 * @deprecated 建议使用 LQUnifiedQueue 替代，功能更强大且性能更好
 **/
@Deprecated
@AllArgsConstructor
public class LQUniqueQueue<T> implements  IMQConsumer<ILQMessage<T>,T> {

    private final LingQueRedis redis;

    /**
     * 发送MQ
     * @param message
     */
    public void pushMessage(Object message){
        redis.ofSet().addMember(message);
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
        Set<String> messagesSet = redis.ofSet().popMembers(1);
        if (LQUtil.isNotEmpty(messagesSet)) {
            String messages = messagesSet.iterator().next();
            handle.forEach(h->{
                LQThreadUtil.execSlave(()-> TryCatch.trying(() -> h.handle(
                        (LQUtil.isBasClass(h.getEntityClass()) ? LQUtil.baseClassTran(messages, h.getEntityClass()) : LQUtil.jsonToBean(messages, h.getEntityClass()))
                )));
            });
        }
    }
}
