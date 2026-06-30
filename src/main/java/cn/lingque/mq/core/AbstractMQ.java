package cn.lingque.mq.core;

import cn.lingque.base.LQKey;
import cn.lingque.redis.LingQueRedis;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc MQ抽象基类 - 定义统一的MQ接口
 **/
@Slf4j
public abstract class AbstractMQ {
    
    /**
     * 全局唯一Key
     */
    protected final String globalKey;
    
    /**
     * Redis实例
     */
    protected final LingQueRedis redis;
    
    /**
     * 运行状态
     */
    protected final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /**
     * 订阅管理器
     */
    protected LQMQSubscriptionManager subscriptionManager;
    
    public AbstractMQ(String globalKey) {
        this.globalKey = globalKey;
        this.redis = LingQueRedis.ofKey(globalKey,LQKey.ONE_DAY);
    }
    
    /**
     * 设置订阅管理器
     */
    public void setSubscriptionManager(LQMQSubscriptionManager manager) {
        this.subscriptionManager = manager;
    }
    
    /**
     * 启动MQ监听
     */
    public abstract void start();
    
    /**
     * 停止MQ监听
     */
    public abstract void stop();
    
    /**
     * 发送消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @return 消息ID
     */
    public abstract String sendMessage(String topic, Object message);
    
    /**
     * 发送延迟消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param delaySeconds 延迟秒数
     * @return 消息ID
     */
    public abstract String sendDelayMessage(String topic, Object message, long delaySeconds);
    
    /**
     * 获取MQ类型
     */
    public abstract String getMQType();
    
    /**
     * 获取运行状态
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 获取全局Key
     */
    public String getGlobalKey() {
        return globalKey;
    }
}
