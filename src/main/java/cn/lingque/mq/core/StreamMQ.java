package cn.lingque.mq.core;

import cn.hutool.json.JSONUtil;
import cn.lingque.mq.LQMQConstants;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.TryCatch;
import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc Stream队列实现 - 基于Redis Stream的可靠消息队列
 * 
 * <p>特点：
 * - 支持消费者组和ACK机制
 * - 消息持久化，保证可靠性
 * - 支持消息回溯和pending消息处理
 * 
 * <p>Key结构：
 * - 全局监听Key: lq:mq:stream
 * - 消费者组: lq_mq_group
 * - 消费者名称: consumer_{timestamp}
 **/
@Slf4j
public class StreamMQ extends AbstractMQ {
    
    /**
     * 消费者组名称（每个项目使用唯一的消费者组）
     * 格式：lq_mq_group_{项目标识}_{时间戳}
     */
    private final String consumerGroup;
    
    /**
     * 消费者名称
     */
    private final String consumerName;
    
    public StreamMQ() {
        super(LQMQConstants.STREAM_GLOBAL_KEY);
        // 使用项目标识 + 时间戳确保每个项目实例有唯一的消费者组
        String projectId = System.getProperty("project.id", "default");
        long timestamp = System.currentTimeMillis();
        this.consumerGroup = "lq_mq_group_" + projectId + "_" + timestamp;
        this.consumerName = "consumer_" + timestamp;
    }
    
    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("========== 启动 Stream MQ ==========");
            log.info("全局Key: {}", globalKey);
            log.info("消费者组: {}", consumerGroup);
            log.info("消费者名称: {}", consumerName);
            
            // 创建消费者组
            createConsumerGroup();
            
            // 启动监听线程
            LQThreadUtil.execMaster(() -> {
                while (isRunning.get()) {
                    try {
                        readAndDispatch();
                        Thread.sleep(LQMQConstants.STREAM_POLL_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Stream MQ 处理消息异常", e);
                        TryCatch.trying(() -> Thread.sleep(LQMQConstants.ERROR_RETRY_INTERVAL));
                    }
                }
                log.info("Stream MQ 监听线程已停止");
            });
            
            log.info("========== Stream MQ 启动完成 ==========");
        }
    }
    
    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止 Stream MQ");
        }
    }
    
    @Override
    public String sendMessage(String topic, Object message) {
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setTopic(topic);
        wrapper.setMessageId(generateMessageId());
        wrapper.setContent(message);
        wrapper.setCreateTime(System.currentTimeMillis());
        
        String messageJson = JSONUtil.toJsonStr(wrapper);
        
        // 使用XADD添加消息到Stream
        String streamId = (String) redis.execBase((commands) -> {
            Map<String, String> fields = new HashMap<>();
            fields.put("data", messageJson);
            // 使用 Lettuce 的 xadd 方法
            return commands.xadd(globalKey, fields);
        });
        
        log.debug("Stream MQ 发送消息: topic={}, messageId={}, streamId={}", 
                  topic, wrapper.getMessageId(), streamId);
        return wrapper.getMessageId();
    }
    
    @Override
    public String sendDelayMessage(String topic, Object message, long delaySeconds) {
        // Stream MQ 暂不支持延迟消息
        log.warn("Stream MQ 不支持延迟消息，将作为即时消息发送");
        return sendMessage(topic, message);
    }
    
    @Override
    public String getMQType() {
        return "STREAM";
    }
    
    /**
     * 创建消费者组
     */
    private void createConsumerGroup() {
        TryCatch.tryingIgnoreError(() -> {
            redis.execBase((commands) -> {
                TryCatch.tryingIgnoreError(() -> {
                    // 使用 Lettuce 的 API 创建消费者组
                    commands.xgroupCreate(
                        XReadArgs.StreamOffset.from(globalKey, "0"),
                        consumerGroup,
                        XGroupCreateArgs.Builder.mkstream(true)
                    );
                    log.info("创建消费者组成功: {}", consumerGroup);
                });
                return null;
            });
        });
    }
    
    /**
     * 读取并分发消息
     */
    private void readAndDispatch() {
        TryCatch.trying(() -> {
            redis.execBase((commands) -> {
                // 创建 Consumer 对象
                Consumer<String> consumer = Consumer.from(consumerGroup, consumerName);
                
                // 创建 XReadArgs 参数
                XReadArgs readArgs = XReadArgs.Builder
                    .count(10)  // 每次最多读取10条
                    .block(Duration.ofMillis(LQMQConstants.STREAM_BLOCK_TIMEOUT));
                
                // 创建 StreamOffset（使用">"获取新消息）
                XReadArgs.StreamOffset<String> streamOffset = XReadArgs.StreamOffset.from(globalKey, ">");
                
                // 读取消息
                List<StreamMessage<String, String>> messages = commands.xreadgroup(
                    consumer,
                    readArgs,
                    streamOffset
                );
                
                if (messages != null && !messages.isEmpty()) {
                    for (StreamMessage<String, String> streamMessage : messages) {
                        dispatchMessage(streamMessage);
                    }
                }
                
                return null;
            });
        });
    }
    
    /**
     * 分发消息到订阅管理器
     */
    private void dispatchMessage(StreamMessage<String, String> streamMessage) {
        try {
            String messageId = streamMessage.getId();
            Map<String, String> fields = streamMessage.getBody();
            String messageJson = fields.get("data");
            
            MessageWrapper wrapper = JSONUtil.toBean(messageJson, MessageWrapper.class);
            
            // 检查消息是否被取消
            if (MessageCancellationHandler.getInstance().isCancelled(wrapper.getMessageId())) {
                log.info("消息已取消，跳过处理: {}", wrapper.getMessageId());
                MessageCancellationHandler.getInstance().removeCancellation(wrapper.getMessageId());
                
                // ACK消息
                ackMessage(messageId);
                return;
            }
            
            // 检查当前项目是否订阅了这个topic
            if (subscriptionManager == null || !subscriptionManager.hasSubscription(wrapper.getTopic())) {
                log.debug("当前项目未订阅topic: {}，ACK后重新发送", wrapper.getTopic());
                
                // 先ACK确认（从当前消费者组中移除）
                ackMessage(messageId);
                
                // 重新发送到Stream（让其他消费者组可以读取）
                final String finalMessageJson = messageJson;
                redis.execBase((commands) -> {
                    Map<String, String> newFields = new HashMap<>();
                    newFields.put("data", finalMessageJson);
                    commands.xadd(globalKey, newFields);
                    return null;
                });
                
                return;
            }
            
            // 推送给订阅管理器
            subscriptionManager.dispatchMessage(wrapper.getTopic(), wrapper.getContent(), wrapper.getMessageId());
            
            // ACK消息
            ackMessage(messageId);
            
        } catch (Exception e) {
            log.error("分发消息失败: {}", streamMessage, e);
            // 发生异常时，不ACK消息，让消息可以被重新消费
        }
    }
    
    /**
     * ACK消息
     */
    private void ackMessage(String messageId) {
        TryCatch.trying(() -> {
            redis.execBase((commands) -> {
                commands.xack(globalKey, consumerGroup, messageId);
                return null;
            });
        });
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "stream_" + System.currentTimeMillis() + "_" + 
               Thread.currentThread().getId() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * 消息包装类
     */
    @Data
    public static class MessageWrapper {
        private String topic;
        private String messageId;
        private Object content;
        private long createTime;
    }
}
