package cn.lingque.mq.core;

import cn.lingque.base.LQKey;
import cn.lingque.redis.LingQueRedis;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc 消息取消处理器 - 基于ZSet实现的消息取消机制
 * 
 * <p>功能说明：
 * - 使用Redis ZSet存储被取消的消息ID
 * - Score为过期时间戳（当前时间 + 1天）
 * - 支持判断、添加、移除取消标记
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 取消消息
 * MessageCancellationHandler.getInstance().cancelMessage("msg_123");
 * 
 * // 判断是否被取消
 * boolean isCancelled = MessageCancellationHandler.getInstance().isCancelled("msg_123");
 * 
 * // 移除取消标记
 * MessageCancellationHandler.getInstance().removeCancellation("msg_123");
 * }</pre>
 **/
@Slf4j
public class MessageCancellationHandler {
    
    /**
     * 全局取消消息ZSet Key
     */
    private static final String CANCELLED_MESSAGES_KEY = "lq:mq:cancelled:messages";
    
    /**
     * 取消消息过期时间（1天）
     */
    private static final long CANCELLATION_TTL_MILLIS = TimeUnit.DAYS.toMillis(1);
    
    /**
     * Redis实例
     */
    private final LingQueRedis redis;
    
    /**
     * 单例实例
     */
    private static volatile MessageCancellationHandler instance;
    
    private MessageCancellationHandler() {
        this.redis = LingQueRedis.ofKey(CANCELLED_MESSAGES_KEY, LQKey.ONE_HOUR);
    }
    
    /**
     * 获取单例实例
     */
    public static MessageCancellationHandler getInstance() {
        if (instance == null) {
            synchronized (MessageCancellationHandler.class) {
                if (instance == null) {
                    instance = new MessageCancellationHandler();
                }
            }
        }
        return instance;
    }
    
    /**
     * 方法1：判断消息ID是否被取消
     * 
     * @param messageId 消息ID
     * @return true-已取消，false-未取消
     */
    public boolean isCancelled(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return false;
        }
        
        return (Boolean) redis.execBase((commands) -> {
            Double score = commands.zscore(CANCELLED_MESSAGES_KEY, messageId);
            if (score == null) {
                return false;
            }
            
            // 检查是否已过期
            long currentTime = System.currentTimeMillis();
            if (score < currentTime) {
                // 已过期，自动移除
                commands.zrem(CANCELLED_MESSAGES_KEY, messageId);
                return false;
            }
            
            return true;
        });
    }
    
    /**
     * 方法2：取消消息
     * 将消息ID存入ZSet，score为当前时间+1天
     * 
     * @param messageId 消息ID
     * @return 是否成功
     */
    public boolean cancelMessage(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            log.warn("消息ID不能为空");
            return false;
        }
        
        try {
            long expireTime = System.currentTimeMillis() + CANCELLATION_TTL_MILLIS;
            
            redis.execBase((commands) -> {
                commands.zadd(CANCELLED_MESSAGES_KEY, expireTime, messageId);
                return null;
            });
            
            log.info("消息已标记为取消: {}, 过期时间: {}", messageId, expireTime);
            return true;
            
        } catch (Exception e) {
            log.error("取消消息失败: {}", messageId, e);
            return false;
        }
    }
    
    /**
     * 方法3：移除消息ID
     * 从ZSet中移除取消标记
     * 
     * @param messageId 消息ID
     * @return 是否成功
     */
    public boolean removeCancellation(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            log.warn("消息ID不能为空");
            return false;
        }
        
        try {
            Long removed = (Long) redis.execBase((commands) -> {
                return commands.zrem(CANCELLED_MESSAGES_KEY, messageId);
            });
            
            if (removed != null && removed > 0) {
                log.debug("移除取消标记: {}", messageId);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("移除取消标记失败: {}", messageId, e);
            return false;
        }
    }
    
    /**
     * 获取已取消消息数量
     * 
     * @return 消息数量
     */
    public long getCancelledCount() {
        return (Long) redis.execBase((commands) -> {
            Long count = commands.zcard(CANCELLED_MESSAGES_KEY);
            return count != null ? count : 0L;
        });
    }
    
    /**
     * 清理过期的取消标记
     * 
     * @return 清理数量
     */
    public long cleanExpired() {
        long currentTime = System.currentTimeMillis();
        
        Long removed = (Long) redis.execBase((commands) -> {
            return commands.zremrangebyscore(CANCELLED_MESSAGES_KEY, 0, currentTime);
        });
        
        if (removed != null && removed > 0) {
            log.info("清理过期取消标记: {} 条", removed);
        }
        
        return removed != null ? removed : 0L;
    }
    
    /**
     * 获取全局取消消息Key
     */
    public String getCancelledMessagesKey() {
        return CANCELLED_MESSAGES_KEY;
    }
}
