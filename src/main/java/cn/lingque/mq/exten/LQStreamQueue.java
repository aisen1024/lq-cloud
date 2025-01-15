package cn.lingque.mq.exten;

import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.resps.StreamEntry;

import java.util.*;

/**
 * Redis Stream 队列实现
 * 支持消息持久化、消费组模式和消息确认机制
 */
@Slf4j
@AllArgsConstructor
public class LQStreamQueue<T> {
    private final LingQueRedis redis;
    private static final String STREAM_ADD_SCRIPT = 
        "local result = redis.call('XADD', KEYS[1], ARGV[1], ARGV[2], ARGV[3]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[4]); " +
        "return result;";

    /**
     * 发布消息到Stream
     * @param message 消息内容
     * @return 消息ID
     */
    public String publish(String message) {
        return (String)redis.execBase((jedis) -> {
            List<String> params = new ArrayList<>();
            params.add("*");  // 自动生成消息ID
            params.add("message");  // 字段名
            params.add(message);  // 消息内容
            params.add(String.valueOf(redis.getTTL()));
            
            Object result = jedis.eval(
                STREAM_ADD_SCRIPT,
                Collections.singletonList(redis.key),
                params
            );
            return result.toString();
        });
    }

    /**
     * 创建消费组
     * @param groupName 消费组名称
     * @param fromStart 是否从头开始消费
     */
    public void createConsumerGroup(String groupName, boolean fromStart) {
        redis.execBase((jedis) -> {
            try {
                jedis.xgroupCreate(redis.key, groupName, 
                    fromStart ? new StreamEntryID("0-0") : StreamEntryID.LAST_ENTRY,
                    true);
            } catch (Exception e) {
                log.warn("Consumer group may already exist: {}", e.getMessage());
            }
            return null;
        });
    }

    /**
     * 消费消息
     * @param groupName 消费组名称
     * @param consumerName 消费者名称
     * @param count 批量获取数量
     * @param block 阻塞时间(毫秒)
     * @return 消息列表
     */
    public List<StreamEntry> consume(String groupName, String consumerName, int count, int block) {
        return (List<StreamEntry>)redis.execBase((jedis) -> {
            Map<String, StreamEntryID> streams = new HashMap<>();
            streams.put(redis.key, StreamEntryID.UNRECEIVED_ENTRY);
            
            XReadGroupParams params = new XReadGroupParams()
                .count(count)
                .block(block);

            List<Map.Entry<String, List<StreamEntry>>> entries = 
                jedis.xreadGroup(groupName, consumerName, params, streams);
            
            if (LQUtil.isEmpty(entries)) {
                return Collections.emptyList();
            }
            
            return entries.get(0).getValue();
        });
    }

    /**
     * 确认消息已处理
     * @param groupName 消费组名称
     * @param messageIds 消息ID列表
     */
    public void ack(String groupName, String... messageIds) {
        StreamEntryID[] ids = new StreamEntryID[messageIds.length];
        for (int i = 0; i < messageIds.length; i++) {
            ids[i] = new StreamEntryID(messageIds[i]);
        }
        redis.execBase((jedis) -> {
            jedis.xack(redis.key, groupName, ids);
            return null;
        });
    }

    /**
     * 获取待处理的消息列表
     * @param groupName 消费组名称
     * @return 待处理的消息列表
     */
    public List<StreamEntry> getPendingMessages(String groupName) {
        return (List<StreamEntry>)redis.execBase((jedis) -> {
            // 获取所有待处理消息的ID
            List<StreamEntry> pendingEntries = new ArrayList<>();
            Map<String, StreamEntryID> streams = new HashMap<>();
            streams.put(redis.key, StreamEntryID.UNRECEIVED_ENTRY);
            
            // 使用 XREADGROUP 读取待处理消息
            XReadGroupParams params = new XReadGroupParams()
                .count(100)  // 每次最多获取100条
                .noAck();    // 不自动确认
                
            List<Map.Entry<String, List<StreamEntry>>> entries = 
                jedis.xreadGroup(groupName, "pending-reader", params, streams);
                
            if (!LQUtil.isEmpty(entries)) {
                pendingEntries.addAll(entries.get(0).getValue());
            }
            
            return pendingEntries;
        });
    }

    /**
     * 删除消息
     * @param messageIds 消息ID列表
     */
    public void deleteMessages(String... messageIds) {
        redis.execBase((jedis) -> {
            StreamEntryID[] ids = new StreamEntryID[messageIds.length];
            for (int i = 0; i < messageIds.length; i++) {
                ids[i] = new StreamEntryID(messageIds[i]);
            }
            jedis.xdel(redis.key, ids);
            return null;
        });
    }

    /**
     * 获取Stream长度
     */
    public long size() {
        return (long)redis.execBase((jedis) -> {
            return jedis.xlen(redis.key);
        });
    }

}
