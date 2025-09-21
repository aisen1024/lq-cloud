package cn.lingque.mq.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.mq.LQDelayQueueManager;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc 统一消息队列 - 基于Redis pub/sub的高性能队列，支持瞬时和延迟消息
 **/
@Slf4j
@AllArgsConstructor
public class LQUnifiedQueue<T> implements IMQConsumer<ILQMessage<T>, T> {

    private final LingQueRedis redis;
    
    // 存储每个队列key对应的消费者处理器
    private static final ConcurrentHashMap<String, List<ILQMessage<Object>>> CONSUMER_HANDLERS = new ConcurrentHashMap<>();
    
    // 存储每个队列key对应的监听器状态
    private static final ConcurrentHashMap<String, AtomicBoolean> LISTENER_STATUS = new ConcurrentHashMap<>();
    
    // Redis键名常量
    private static final String INSTANT_SUFFIX = ":instant";
    private static final String DELAY_SUFFIX = ":delay";
    private static final String DELAY_INDEX_SUFFIX = ":delay_idx";
    private static final String ACK_SUFFIX = ":ack:";
    
    // 延迟消息分片大小（秒）
    private static final long DELAY_SHARD_SIZE = 60; // 1分钟一个分片
    
    /**
     * 发送瞬时消息
     * @param message 消息内容
     * @return 消息ID
     */
    public String pushInstantMessage(Object message) {
        String messageId = generateMessageId();
        String messageJson = buildMessageJson(messageId, message, 0L);
        
        return (String) redis.execBase((commands) -> {
            // 使用List的lpush实现FIFO队列
            commands.lpush(redis.key + INSTANT_SUFFIX, messageJson);
            return messageId;
        });
    }
    
    /**
     * 发送延迟消息
     * @param message 消息内容
     * @param delaySeconds 延迟秒数
     * @return 消息ID
     */
    public String pushDelayMessage(Object message, long delaySeconds) {
        String messageId = generateMessageId();
        long executeTime = System.currentTimeMillis() + (delaySeconds * 1000);
        String messageJson = buildMessageJson(messageId, message, executeTime);
        
        return (String) redis.execBase((commands) -> {
            // 计算分片键
            long shardKey = executeTime / (DELAY_SHARD_SIZE * 1000);
            String delayShardKey = redis.key + DELAY_SUFFIX + ":" + shardKey;
            
            // 将消息存储到对应的延迟分片中
            commands.zadd(delayShardKey, executeTime, messageJson);
            
            // 在延迟索引中记录这个分片
            commands.zadd(redis.key + DELAY_INDEX_SUFFIX, shardKey, String.valueOf(shardKey));
            
            // 设置分片过期时间（分片时间 + 1小时缓冲）
            commands.expire(delayShardKey, DELAY_SHARD_SIZE + 3600);
            
            return messageId;
        });
    }
    
    /**
     * 取消消息
     * @param messageId 消息ID
     * @return 是否成功取消
     */
    public boolean cancelMessage(String messageId) {
        return (Boolean) redis.execBase((commands) -> {
            // 从瞬时队列中移除
            long instantRemoved = commands.lrem(redis.key + INSTANT_SUFFIX, 0, "*\"id\":\"" + messageId + "\"*");
            
            if (instantRemoved > 0) {
                return true;
            }
            
            // 从延迟队列分片中移除
            List<String> shardKeys = commands.zrange(redis.key + DELAY_INDEX_SUFFIX, 0, -1);
            for (String shardKey : shardKeys) {
                String delayShardKey = redis.key + DELAY_SUFFIX + ":" + shardKey;
                List<String> messages = commands.zrange(delayShardKey, 0, -1);
                
                for (String message : messages) {
                    if (message.contains("\"id\":\"" + messageId + "\"")) {
                        commands.zrem(delayShardKey, message);
                        return true;
                    }
                }
            }
            
            return false;
        });
    }
    
    @Override
    public void consumer(List<ILQMessage<T>> handles) {
        if (handles == null || handles.isEmpty()) {
            return;
        }
        
        // 注册消费者处理器
        CONSUMER_HANDLERS.put(redis.key, (List<ILQMessage<Object>>) (List<?>) handles);
        
        // 注册延迟队列处理器到统一管理器
        LQDelayQueueManager.getInstance().registerProcessor(redis.key, () -> {
            List<ILQMessage<Object>> registeredHandles = CONSUMER_HANDLERS.get(redis.key);
            if (registeredHandles != null) {
                processDelayMessages((List<ILQMessage<T>>) (List<?>) registeredHandles);
            }
        });
        
        // 启动监听器（只启动一次）
        startListener();
    }
    
    /**
     * 启动队列监听器
     */
    private void startListener() {
        String queueKey = redis.key;
        AtomicBoolean isStarted = LISTENER_STATUS.computeIfAbsent(queueKey, k -> new AtomicBoolean(false));
        
        if (isStarted.compareAndSet(false, true)) {
            log.info("启动队列监听器: {}", queueKey);
            
            // 使用定时任务进行消息处理，比轮询更高效
            LQThreadUtil.execMaster(() -> {
                while (isStarted.get()) {
                    try {
                        List<ILQMessage<Object>> handles = CONSUMER_HANDLERS.get(queueKey);
                        if (handles != null) {
                            // 只处理瞬时消息，延迟消息由统一管理器处理
                            processInstantMessages((List<ILQMessage<T>>) (List<?>) handles);
                        }
                        
                        // 短暂休眠，避免过度消耗CPU（比原来的50ms轮询更短）
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("处理队列消息时发生错误: {}", queueKey, e);
                        try {
                            Thread.sleep(100); // 发生错误时稍长等待
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                log.info("队列监听器已停止: {}", queueKey);
            });
        }
    }
    
    /**
     * 停止队列监听器
     */
    public void stopListener() {
        AtomicBoolean isStarted = LISTENER_STATUS.get(redis.key);
        if (isStarted != null) {
            isStarted.set(false);
            CONSUMER_HANDLERS.remove(redis.key);
            // 取消注册延迟队列处理器
            LQDelayQueueManager.getInstance().unregisterProcessor(redis.key);
            log.info("停止队列监听器: {}", redis.key);
        }
    }
    
    /**
     * 处理瞬时消息
     */
    private void processInstantMessages(List<ILQMessage<T>> handles) {
        TryCatch.trying(() -> {
            String message = (String) redis.execBase((commands) -> {
                return commands.rpop(redis.key + INSTANT_SUFFIX);
            });
            
            if (message != null) {
                processMessage(message, handles);
            }
        });
    }
    
    /**
     * 处理延迟消息
     */
    private void processDelayMessages(List<ILQMessage<T>> handles) {
        TryCatch.trying(() -> {
            long currentTime = System.currentTimeMillis();
            long currentShard = currentTime / (DELAY_SHARD_SIZE * 1000);
            
            // 获取需要处理的分片（当前分片和之前的分片）
            Set<String> expiredShards = (Set<String>) redis.execBase((commands) -> {
                return commands.zrangebyscore(redis.key + DELAY_INDEX_SUFFIX, 0, currentShard);
            });
            
            for (String shardKey : expiredShards) {
                String delayShardKey = redis.key + DELAY_SUFFIX + ":" + shardKey;
                
                // 获取到期的消息
                List<String> expiredMessages = (List<String>) redis.execBase((commands) -> {
                    return commands.zrangebyscore(delayShardKey, 0, currentTime);
                });
                
                for (String message : expiredMessages) {
                    // 使用分布式锁确保消息只被处理一次
                    String messageId = extractMessageId(message);
                    String ackKey = redis.key + ACK_SUFFIX + messageId;
                    if (LingQueRedis.ofKey(ackKey,30L).ofLock().lock(false)) {
                        try {
                            // 从延迟队列中移除消息
                            redis.execBase((commands) -> {
                                commands.zrem(delayShardKey, message);
                                return null;
                            });
                            
                            // 处理消息
                            processMessage(message, handles);
                            
                        } finally {
                            // 清理ACK锁
                            redis.execBase((commands) -> {
                                commands.del(ackKey);
                                return null;
                            });
                        }
                    }
                }
                
                // 清理空的分片
                redis.execBase((commands) -> {
                    Long count = commands.zcard(delayShardKey);
                    if (count == 0) {
                        commands.del(delayShardKey);
                        commands.zrem(redis.key + DELAY_INDEX_SUFFIX, shardKey);
                    }
                    return null;
                });
            }
        });
    }
    
    /**
     * 处理单个消息
     */
    private void processMessage(String messageJson, List<ILQMessage<T>> handles) {
        try {
            MessageWrapper wrapper = JSONUtil.toBean(messageJson, MessageWrapper.class);
            Object messageContent = wrapper.getContent();
            
            handles.forEach(handle -> {
                LQThreadUtil.execSlave(() -> TryCatch.trying(() -> {
                    T typedMessage = LQUtil.isBasClass(handle.getEntityClass()) 
                        ? LQUtil.baseClassTran(messageContent.toString(), handle.getEntityClass())
                        : LQUtil.jsonToBean(messageContent.toString(), handle.getEntityClass());
                    handle.handle(typedMessage);
                }));
            });
            
        } catch (Exception e) {
            log.error("处理消息失败: {}", messageJson, e);
        }
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return System.currentTimeMillis() + "_" + Thread.currentThread().getId() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * 构建消息JSON
     */
    private String buildMessageJson(String messageId, Object message, long executeTime) {
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setId(messageId);
        wrapper.setContent(message);
        wrapper.setExecuteTime(executeTime);
        wrapper.setCreateTime(System.currentTimeMillis());
        return JSONUtil.toJsonStr(wrapper);
    }
    
    /**
     * 从消息JSON中提取消息ID
     */
    private String extractMessageId(String messageJson) {
        try {
            MessageWrapper wrapper = JSONUtil.toBean(messageJson, MessageWrapper.class);
            return wrapper.getId();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 消息包装类
     */
    public static class MessageWrapper {
        private String id;
        private Object content;
        private long executeTime;
        private long createTime;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Object getContent() { return content; }
        public void setContent(Object content) { this.content = content; }
        public long getExecuteTime() { return executeTime; }
        public void setExecuteTime(long executeTime) { this.executeTime = executeTime; }
        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }
    }
    
    /**
     * 获取瞬时消息数量
     * @return 瞬时消息数量
     */
    public long getInstantMessageCount() {
        return (Long) redis.execBase((commands) -> {
            try {
                return commands.llen(redis.key + INSTANT_SUFFIX);
            } catch (Exception e) {
                return 0L;
            }
        });
    }
    
    /**
     * 获取延迟消息数量
     * @return 延迟消息数量
     */
    public long getDelayMessageCount() {
        return (Long) redis.execBase((commands) -> {
            try {
                long totalCount = 0;
                
                // 获取所有延迟分片
                List<String> shardKeys = commands.zrange(redis.key + DELAY_INDEX_SUFFIX, 0, -1);
                
                for (String shardKey : shardKeys) {
                    String delayShardKey = redis.key + DELAY_SUFFIX + ":" + shardKey;
                    totalCount += commands.zcard(delayShardKey);
                }
                
                return totalCount;
            } catch (Exception e) {
                return 0L;
            }
        });
    }
    
    /**
     * 获取总消息数量
     * @return 总消息数量
     */
    public long getTotalMessageCount() {
        return getInstantMessageCount() + getDelayMessageCount();
    }
}