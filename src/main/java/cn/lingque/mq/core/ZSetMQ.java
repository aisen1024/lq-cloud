package cn.lingque.mq.core;

import cn.hutool.json.JSONUtil;
import cn.lingque.base.LQKey;
import cn.lingque.mq.LQMQConstants;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.TryCatch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc ZSet延迟队列实现 - 基于Redis ZSet的延迟消息队列
 * 
 * <p>特点：
 * - 使用ZSet存储延迟消息，score为执行时间戳
 * - 支持精确的延迟时间控制
 * - 定时扫描到期消息并分发
 * 
 * <p>Key结构：
 * - 全局监听Key: lq:mq:zset
 * - 消息格式: {topic}:{messageId}:{messageJson}
 **/
@Slf4j
public class ZSetMQ extends AbstractMQ {
    
    public ZSetMQ() {
        super(LQMQConstants.ZSET_GLOBAL_KEY);
    }
    
    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("========== 启动 ZSet MQ ==========");
            log.info("全局Key: {}", globalKey);
            
            // 启动延迟消息扫描线程
            LQThreadUtil.execMaster(() -> {
                while (isRunning.get()) {
                    try {
                        scanAndDispatch();
                        Thread.sleep(LQMQConstants.ZSET_SCAN_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("ZSet MQ 处理消息异常", e);
                        TryCatch.trying(() -> Thread.sleep(LQMQConstants.ERROR_RETRY_INTERVAL));
                    }
                }
                log.info("ZSet MQ 监听线程已停止");
            });
            
            log.info("========== ZSet MQ 启动完成 ==========");
        }
    }
    
    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止 ZSet MQ");
        }
    }
    
    @Override
    public String sendMessage(String topic, Object message) {
        // ZSet MQ 主要用于延迟消息，即时消息延迟0秒
        return sendDelayMessage(topic, message, 0);
    }
    
    @Override
    public String sendDelayMessage(String topic, Object message, long delaySeconds) {
        String messageId = generateMessageId();
        long executeTime = System.currentTimeMillis() + (delaySeconds * 1000);
        
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setTopic(topic);
        wrapper.setMessageId(messageId);
        wrapper.setContent(message);
        wrapper.setCreateTime(System.currentTimeMillis());
        wrapper.setExecuteTime(executeTime);
        
        String messageJson = JSONUtil.toJsonStr(wrapper);
        
        redis.execBase((commands) -> {
            commands.zadd(globalKey, executeTime, messageJson);
            return null;
        });
        
        log.debug("ZSet MQ 发送延迟消息: topic={}, messageId={}, delaySeconds={}", 
                  topic, messageId, delaySeconds);
        return messageId;
    }
    
    @Override
    public String getMQType() {
        return "ZSET";
    }
    
    /**
     * 扫描并分发到期消息
     */
    private void scanAndDispatch() {
        TryCatch.trying(() -> {
            long currentTime = System.currentTimeMillis();
            
            // 获取所有到期的消息
            List<String> expiredMessages = (List<String>) redis.execBase((commands) -> {
                return commands.zrangebyscore(globalKey, 0, currentTime);
            });
            
            if (expiredMessages != null && !expiredMessages.isEmpty()) {
                for (String messageJson : expiredMessages) {
                    // 使用分布式锁确保消息只被处理一次
                    MessageWrapper wrapper = JSONUtil.toBean(messageJson, MessageWrapper.class);
                    String lockKey = globalKey + ":lock:" + wrapper.getMessageId();
                    LQKey lock = LQKey.key(lockKey, 1D, 30L);
                    if (lock.ofLock().lock(false)) {
                        try {
                            // 先检查是否有订阅，如果有才移除
                            boolean hasSubscription = subscriptionManager != null 
                                && subscriptionManager.hasSubscription(wrapper.getTopic());
                            
                            if (!hasSubscription && !MessageCancellationHandler.getInstance().isCancelled(wrapper.getMessageId())) {
                                // 没有订阅且未被取消，不处理，让消息留在ZSet中
                                log.debug("当前项目未订阅topic: {}，消息保留在ZSet中", wrapper.getTopic());
                                continue;
                            }
                            
                            // 从ZSet中移除
                            redis.execBase((commands) -> {
                                commands.zrem(globalKey, messageJson);
                                return null;
                            });
                            
                            // 分发消息
                            dispatchMessage(messageJson);
                            
                        } finally {
                            // 释放锁
                            redis.execBase((commands) -> {
                                commands.del(lockKey);
                                return null;
                            });
                        }
                    }
                }
            }
        });
    }
    
    /**
     * 分发消息到订阅管理器
     */
    private void dispatchMessage(String messageJson) {
        MessageWrapper wrapper = null;
        try {
            wrapper = JSONUtil.toBean(messageJson, MessageWrapper.class);
            
            // 检查消息是否被取消
            if (MessageCancellationHandler.getInstance().isCancelled(wrapper.getMessageId())) {
                log.info("消息已取消，跳过处理: {}", wrapper.getMessageId());
                MessageCancellationHandler.getInstance().removeCancellation(wrapper.getMessageId());
                return;
            }
            
            // 检查当前项目是否订阅了这个topic
            if (subscriptionManager == null || !subscriptionManager.hasSubscription(wrapper.getTopic())) {
                log.debug("当前项目未订阅topic: {}，归还消息到ZSet", wrapper.getTopic());
                // 归还消息到ZSet（保持原来的执行时间）
                final MessageWrapper finalWrapper = wrapper;
                redis.execBase((commands) -> {
                    commands.zadd(globalKey, finalWrapper.getExecuteTime(), messageJson);
                    return null;
                });
                return;
            }
            
            // 推送给订阅管理器
            subscriptionManager.dispatchMessage(wrapper.getTopic(), wrapper.getContent(), wrapper.getMessageId());
            
        } catch (Exception e) {
            log.error("分发消息失败: {}", messageJson, e);
            // 发生异常时，也归还消息到ZSet
            if (wrapper != null) {
                final MessageWrapper finalWrapper = wrapper;
                TryCatch.trying(() -> {
                    redis.execBase((commands) -> {
                        commands.zadd(globalKey, finalWrapper.getExecuteTime(), messageJson);
                        return null;
                    });
                });
            }
        }
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "zset_" + System.currentTimeMillis() + "_" + 
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
        private long executeTime;
    }
}
