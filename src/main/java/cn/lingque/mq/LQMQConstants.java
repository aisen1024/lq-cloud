package cn.lingque.mq;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc MQ常量定义 - 统一管理所有消息队列的Key和配置
 **/
public class LQMQConstants {
    
    // ==================== 全局Key常量 ====================
    
    /**
     * List MQ 全局Key（顺序消息队列）
     */
    public static final String LIST_GLOBAL_KEY = "lq:mq:list";
    
    /**
     * ZSet MQ 全局Key（延迟消息队列）
     */
    public static final String ZSET_GLOBAL_KEY = "lq:mq:zset";
    
    /**
     * Stream MQ 全局Key（流队列）
     */
    public static final String STREAM_GLOBAL_KEY = "lq:mq:stream";
    
    /**
     * 全局取消消息Key（ZSet存储所有需要取消的消息ID）
     */
    public static final String GLOBAL_CANCELLED_MESSAGES_KEY = "lq:mq:cancelled:messages";
    
    // ==================== 轮询配置 ====================
    
    /**
     * List队列轮询间隔（毫秒）- 极短间隔保证实时性
     */
    public static final long LIST_POLL_INTERVAL = 10L;
    
    /**
     * ZSet延迟消息扫描间隔（毫秒）
     */
    public static final long ZSET_SCAN_INTERVAL = 500L;
    
    /**
     * Stream队列轮询间隔（毫秒）- Stream有阻塞读取，间隔可以更短
     */
    public static final long STREAM_POLL_INTERVAL = 10L;
    
    /**
     * Stream阻塞读取超时时间（毫秒）
     */
    public static final long STREAM_BLOCK_TIMEOUT = 1000L;
    
    /**
     * 错误重试间隔（毫秒）
     */
    public static final long ERROR_RETRY_INTERVAL = 100L;
    
    // ==================== 取消消息配置 ====================
    
    /**
     * 取消消息标记过期时间（毫秒，1天）
     */
    public static final long CANCELLED_MESSAGE_TTL = 24 * 60 * 60 * 1000L;
    
    /**
     * 取消消息清理间隔（毫秒，12小时）
     */
    public static final long CANCELLED_CLEANUP_INTERVAL = 12 * 60 * 60 * 1000L;
    
    // ==================== Stream相关常量 ====================
    
    /**
     * Stream默认消费者组名
     */
    public static final String STREAM_CONSUMER_GROUP = "lq_mq_group";
    
    /**
     * Stream消费者名称前缀
     */
    public static final String STREAM_CONSUMER_PREFIX = "consumer_";
}
