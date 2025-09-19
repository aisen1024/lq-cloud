package cn.lingque.bus.enhanced;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 增强版消息总线消息对象
 * 包含完整的消息元数据和内容
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Accessors(chain = true)
public class EnhancedBusMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 消息ID */
    private String messageId;
    
    /** 主题名称 */
    private String topic;
    
    /** 服务组（可选） */
    private String serviceGroup;
    
    /** 消息内容（JSON格式） */
    private String message;
    
    /** 消息类型（完整类名） */
    private String messageType;
    
    /** 发送节点 */
    private String senderNode;
    
    /** 消息时间戳 */
    private Long timestamp;
    
    /** 是否为广播消息 */
    private Boolean broadcast = false;
    
    /** 消息优先级（数字越小优先级越高） */
    private Integer priority = 0;
    
    /** 消息过期时间（毫秒） */
    private Long expireTime;
    
    /** 重试次数 */
    private Integer retryCount = 0;
    
    /** 最大重试次数 */
    private Integer maxRetries = 3;
    
    /** 消息标签（用于过滤和路由） */
    private String tags;
    
    /** 消息属性（扩展字段） */
    private String properties;
    
    /** 消息版本 */
    private String version = "1.0";
    
    /** 消息压缩标识 */
    private Boolean compressed = false;
    
    /** 消息大小（字节） */
    private Integer messageSize;
    
    /**
     * 检查消息是否过期
     * @return true表示已过期
     */
    public boolean isExpired() {
        if (expireTime == null) {
            return false;
        }
        return System.currentTimeMillis() > expireTime;
    }
    
    /**
     * 检查是否可以重试
     * @return true表示可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
    }
    
    /**
     * 设置消息过期时间（相对当前时间的毫秒数）
     * @param ttlMs 生存时间（毫秒）
     * @return 当前对象
     */
    public EnhancedBusMessage setTtl(long ttlMs) {
        this.expireTime = System.currentTimeMillis() + ttlMs;
        return this;
    }
    
    /**
     * 获取消息年龄（毫秒）
     * @return 消息年龄
     */
    public long getAge() {
        if (timestamp == null) {
            return 0;
        }
        return System.currentTimeMillis() - timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("EnhancedBusMessage{messageId='%s', topic='%s', serviceGroup='%s', senderNode='%s', timestamp=%d}",
                messageId, topic, serviceGroup, senderNode, timestamp);
    }
}