package cn.lingque.mq.core;

import cn.hutool.json.JSONUtil;
import cn.lingque.mq.LQMQConstants;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.TryCatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc List队列实现 - 基于Redis List的轻量级消息队列
 * 
 * <p>特点：
 * - 使用LPUSH/RPOP实现FIFO队列
 * - 高性能、低延迟
 * - 不支持消费者组和ACK机制
 * 
 * <p>Key结构：
 * - 全局监听Key: lq:mq:list
 * - 消息格式: {topic}:{messageId}:{messageJson}
 **/
@Slf4j
public class ListMQ extends AbstractMQ {
    
    public ListMQ() {
        super(LQMQConstants.LIST_GLOBAL_KEY);
    }
    
    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("========== 启动 List MQ ==========");
            log.info("全局Key: {}", globalKey);
            
            // 启动监听线程
            LQThreadUtil.execMaster(() -> {
                while (isRunning.get()) {
                    try {
                        pollAndDispatch();
                        Thread.sleep(LQMQConstants.LIST_POLL_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("List MQ 处理消息异常", e);
                        TryCatch.trying(() -> Thread.sleep(LQMQConstants.ERROR_RETRY_INTERVAL));
                    }
                }
                log.info("List MQ 监听线程已停止");
            });
            
            log.info("========== List MQ 启动完成 ==========");
        }
    }
    
    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止 List MQ");
        }
    }
    
    @Override
    public String sendMessage(String topic, Object message) {
        String messageId = generateMessageId();
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setTopic(topic);
        wrapper.setMessageId(messageId);
        wrapper.setContent(message);
        wrapper.setCreateTime(System.currentTimeMillis());
        
        String messageJson = JSONUtil.toJsonStr(wrapper);
        
        redis.execBase((commands) -> {
            commands.lpush(globalKey, messageJson);
            return null;
        });
        
        log.debug("List MQ 发送消息: topic={}, messageId={}", topic, messageId);
        return messageId;
    }
    
    @Override
    public String sendDelayMessage(String topic, Object message, long delaySeconds) {
        // List MQ 暂不支持延迟消息
        log.warn("List MQ 不支持延迟消息，将作为即时消息发送");
        return sendMessage(topic, message);
    }
    
    @Override
    public String getMQType() {
        return "LIST";
    }
    
    /**
     * 轮询并分发消息
     */
    private void pollAndDispatch() {
        TryCatch.trying(() -> {
            String messageJson = (String) redis.execBase((commands) -> {
                return commands.rpop(globalKey);
            });
            
            if (messageJson != null) {
                dispatchMessage(messageJson);
            }
        });
    }
    
    /**
     * 分发消息到订阅管理器
     */
    private void dispatchMessage(String messageJson) {
        try {
            MessageWrapper wrapper = JSONUtil.toBean(messageJson, MessageWrapper.class);
            
            // 检查消息是否被取消
            if (MessageCancellationHandler.getInstance().isCancelled(wrapper.getMessageId())) {
                log.info("消息已取消，跳过处理: {}", wrapper.getMessageId());
                MessageCancellationHandler.getInstance().removeCancellation(wrapper.getMessageId());
                return;
            }
            
            // 检查当前项目是否订阅了这个topic
            if (subscriptionManager == null || !subscriptionManager.hasSubscription(wrapper.getTopic())) {
                log.debug("当前项目未订阅topic: {}，归还消息到队列", wrapper.getTopic());
                // 归还消息到队列头部（使用RPUSH，这样下次RPOP时会先取到其他消息）
                redis.execBase((commands) -> {
                    commands.rpush(globalKey, messageJson);
                    return null;
                });
                return;
            }
            
            // 推送给订阅管理器
            subscriptionManager.dispatchMessage(wrapper.getTopic(), wrapper.getContent(), wrapper.getMessageId());
            
        } catch (Exception e) {
            log.error("分发消息失败: {}", messageJson, e);
            // 发生异常时，也归还消息
            TryCatch.trying(() -> {
                redis.execBase((commands) -> {
                    commands.rpush(globalKey, messageJson);
                    return null;
                });
            });
        }
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "list_" + System.currentTimeMillis() + "_" + 
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
