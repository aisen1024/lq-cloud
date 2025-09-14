package cn.lingque.mq.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.mq.exten.itf.ILQMessage;
import cn.lingque.mq.exten.itf.IMQConsumer;
import cn.lingque.mq.LQDelayQueueManager;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc 基于Redis Streams的统一消息队列
 **/
@Slf4j
@AllArgsConstructor
public class LQStreamUnifiedQueue<T> implements IMQConsumer<ILQMessage<T>, T> {

    private final LingQueRedis redis;
    
    // 存储每个队列key对应的消费者处理器
    private static final ConcurrentHashMap<String, List<ILQMessage<Object>>> CONSUMER_HANDLERS = new ConcurrentHashMap<>();
    
    // 存储每个队列key对应的监听器状态
    private static final ConcurrentHashMap<String, AtomicBoolean> LISTENER_STATUS = new ConcurrentHashMap<>();
    
    // Stream键名
    private static final String STREAM_SUFFIX = ":stream";
    private static final String DELAY_SUFFIX = ":delay";
    private static final String CONSUMER_GROUP = "default_group";
    private static final String CONSUMER_NAME = "consumer_";
    
    /**
     * 发送瞬时消息到Stream
     * @param message 消息内容
     * @return 消息ID
     */
    public String pushInstantMessage(Object message) {
        return (String) redis.execBase((commands) -> {
            String streamKey = redis.key + STREAM_SUFFIX;
            
            // 确保消费者组存在
            TryCatch.trying(() -> {
                commands.xgroupCreate(
                    XReadArgs.StreamOffset.from(streamKey, "0"), 
                    CONSUMER_GROUP, 
                    XGroupCreateArgs.Builder.mkstream(true)
                );
            });
            
            // 添加消息到Stream
            Map<String, String> messageMap = Map.of(
                "type", "instant",
                "content", LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message),
                "timestamp", String.valueOf(System.currentTimeMillis())
            );
            
            return commands.xadd(streamKey, messageMap);
        });
    }
    
    /**
     * 发送延迟消息（使用ZSet + Stream组合）
     * @param message 消息内容
     * @param delaySeconds 延迟秒数
     * @return 消息ID
     */
    public String pushDelayMessage(Object message, long delaySeconds) {
        String messageId = generateMessageId();
        long executeTime = System.currentTimeMillis() + (delaySeconds * 1000);
        
        return (String) redis.execBase((commands) -> {
            String delayKey = redis.key + DELAY_SUFFIX;
            
            // 将延迟消息存储到ZSet中
            Map<String, Object> delayMessage = Map.of(
                "id", messageId,
                "type", "delay",
                "content", LQUtil.isBaseValue(message) ? message.toString() : JSONUtil.toJsonStr(message),
                "executeTime", executeTime
            );
            
            commands.zadd(delayKey, executeTime, JSONUtil.toJsonStr(delayMessage));
            return messageId;
        });
    }
    
    /**
     * 取消消息
     * @param messageId 消息ID
     * @return 是否成功
     */
    public boolean cancelMessage(String messageId) {
        return (Boolean) redis.execBase((commands) -> {
            String delayKey = redis.key + DELAY_SUFFIX;
            
            // 从延迟队列中查找并删除
            List<String> messages = commands.zrange(delayKey, 0, -1);
            for (String msg : messages) {
                if (msg.contains("\"id\":\"" + messageId + "\"")) {
                    commands.zrem(delayKey, msg);
                    return true;
                }
            }
            
            // 注意：已经进入Stream的消息无法取消，这是Streams的限制
            log.warn("消息 {} 可能已经进入Stream，无法取消", messageId);
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
            transferDelayedMessages();
        });
        
        // 启动监听器（只启动一次）
        startListener();
    }
    
    /**
     * 启动Stream监听器
     */
    private void startListener() {
        String queueKey = redis.key;
        AtomicBoolean isStarted = LISTENER_STATUS.computeIfAbsent(queueKey, k -> new AtomicBoolean(false));
        
        if (isStarted.compareAndSet(false, true)) {
            log.info("启动Stream监听器: {}", queueKey);
            
            // 只启动Stream消息监听器，延迟消息由统一管理器处理
            startStreamMessageListener(queueKey, isStarted);
        }
    }
    
    
    /**
     * 启动Stream消息监听器
     */
    private void startStreamMessageListener(String queueKey, AtomicBoolean isStarted) {
        LQThreadUtil.execMaster(() -> {
            log.info("启动Stream消息监听器: {}", queueKey);
            while (isStarted.get()) {
                try {
                    List<ILQMessage<Object>> handles = CONSUMER_HANDLERS.get(queueKey);
                    if (handles != null) {
                        // 消费Stream中的消息
                        consumeStreamMessages((List<ILQMessage<T>>) (List<?>) handles);
                    }
                    
                    // Stream消息处理间隔很短，保证实时性
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("处理Stream消息时发生错误: {}", queueKey, e);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            log.info("Stream消息监听器已停止: {}", queueKey);
        });
    }
    
    /**
     * 停止监听器
     */
    public void stopListener() {
        AtomicBoolean isStarted = LISTENER_STATUS.get(redis.key);
        if (isStarted != null) {
            isStarted.set(false);
            CONSUMER_HANDLERS.remove(redis.key);
            // 取消注册延迟队列处理器
            LQDelayQueueManager.getInstance().unregisterProcessor(redis.key);
            log.info("停止Stream监听器: {}", redis.key);
        }
    }
    
    /**
     * 将到期的延迟消息转移到Stream
     */
    private void transferDelayedMessages() {
        TryCatch.trying(() -> {
            long currentTime = System.currentTimeMillis();
            
            redis.execBase((commands) -> {
                String delayKey = redis.key + DELAY_SUFFIX;
                String streamKey = redis.key + STREAM_SUFFIX;
                
                // 获取到期的延迟消息
                List<String> expiredMessages = commands.zrangebyscore(delayKey, 0, currentTime);
                
                for (String msgJson : expiredMessages) {
                    try {
                        Map<String, Object> delayMessage = JSONUtil.toBean(msgJson, Map.class);
                        
                        // 转移到Stream
                        Map<String, String> streamMessage = Map.of(
                            "type", "delay_executed",
                            "content", delayMessage.get("content").toString(),
                            "originalId", delayMessage.get("id").toString(),
                            "timestamp", String.valueOf(System.currentTimeMillis())
                        );
                        
                        commands.xadd(streamKey, streamMessage);
                        
                        // 从延迟队列中移除
                        commands.zrem(delayKey, msgJson);
                        
                    } catch (Exception e) {
                        log.error("转移延迟消息失败: {}", msgJson, e);
                    }
                }
                
                return null;
            });
        });
    }
    
    /**
     * 消费Stream中的消息
     */
    private void consumeStreamMessages(List<ILQMessage<T>> handles) {
        TryCatch.trying(() -> {
            String streamKey = redis.key + STREAM_SUFFIX;
            String consumerName = CONSUMER_NAME + Thread.currentThread().getId();
            
            redis.execBase((commands) -> {
                // 确保消费者组存在
                TryCatch.trying(() -> {
                    commands.xgroupCreate(
                        XReadArgs.StreamOffset.from(streamKey, "0"), 
                        CONSUMER_GROUP, 
                        XGroupCreateArgs.Builder.mkstream(true)
                    );
                });
                
                // 创建Consumer对象
                Consumer<String> consumer = Consumer.from(CONSUMER_GROUP, consumerName);
                
                // 创建XReadArgs参数（设置COUNT和BLOCK）
                XReadArgs readArgs = XReadArgs.Builder
                    .count(1)
                    .block(Duration.ofMillis(1000));
                
                // 创建StreamOffset（使用">"获取新消息）
                XReadArgs.StreamOffset<String> streamOffset = XReadArgs.StreamOffset.from(streamKey, ">");
                
                // 读取消息
                List<StreamMessage<String, String>> messages = commands.xreadgroup(
                    consumer, 
                    readArgs, 
                    streamOffset
                );
                
                if (messages != null && !messages.isEmpty()) {
                    for (StreamMessage<String, String> streamMessage : messages) {
                        String messageId = streamMessage.getId();
                        Map<String, String> fields = streamMessage.getBody();
                        
                        try {
                            // 处理消息
                            processStreamMessage(fields, handles);
                            
                            // 确认消息处理完成
                            commands.xack(streamKey, CONSUMER_GROUP, messageId);
                            
                        } catch (Exception e) {
                            log.error("处理Stream消息失败: {}", messageId, e);
                            // 消息处理失败，不ACK，会被重新分配
                        }
                    }
                }
                
                return null;
            });
        });
    }
    
    /**
     * 处理Stream消息
     */
    private void processStreamMessage(Map<String, String> fields, List<ILQMessage<T>> handles) {
        String content = fields.get("content");
        String type = fields.get("type");
        
        handles.forEach(handle -> {
            LQThreadUtil.execSlave(() -> TryCatch.trying(() -> {
                T typedMessage = LQUtil.isBasClass(handle.getEntityClass()) 
                    ? LQUtil.baseClassTran(content, handle.getEntityClass())
                    : LQUtil.jsonToBean(content, handle.getEntityClass());
                handle.handle(typedMessage);
            }));
        });
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return System.currentTimeMillis() + "_" + Thread.currentThread().getId() + "_" + 
               (int)(Math.random() * 10000);
    }
}