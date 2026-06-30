package cn.lingque.util;

import cn.lingque.bus.enhanced.LQEnhancedBus;

/**
 * 消息总线工具类
 * 基于 LQEnhancedBus 封装，提供常用的消息发布方法
 */
public class LqBusTemplate {

    /**
     * 发布消息到指定主题（不包含自身节点）
     *
     * @param topic 主题
     * @param msg   消息内容
     */
    public static void publish(String topic, Object msg) {
        LQEnhancedBus.publish(topic, msg);
    }

    /**
     * 发布消息到指定主题
     *
     * @param topic       主题
     * @param msg         消息内容
     * @param includeSelf 是否包含自身节点
     */
    public static void publish(String topic, Object msg, boolean includeSelf) {
        LQEnhancedBus.publish(topic, null, msg, includeSelf);
    }

    /**
     * 发布消息到指定服务组的主题（不包含自身节点）
     *
     * @param serviceGroup 目标服务组
     * @param topic        主题
     * @param msg          消息内容
     */
    public static void publishToGroup(String serviceGroup, String topic, Object msg) {
        LQEnhancedBus.publishToGroup(topic, serviceGroup, msg);
    }

    /**
     * 广播消息到所有节点
     *
     * @param topic       主题
     * @param msg         消息内容
     * @param includeSelf 是否包含自身节点
     */
    public static void broadcast(String topic, Object msg, boolean includeSelf) {
        LQEnhancedBus.broadcast(topic, msg, includeSelf);
    }
}
