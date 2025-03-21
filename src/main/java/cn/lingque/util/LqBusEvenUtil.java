package cn.lingque.util;

import cn.lingque.bus.LQBus;

/**
 * 消息bus事件工具类
 */
public class LqBusEvenUtil {


    /**
     * 发送消息给相同服务的节点
     * @param sv 服务名称,如果业务量少，可以共用一个，如果消息多，想要及时，则各自定义一个
     * @param topic 主题消息
     * @param msg 消息
     * @param isNotSendSelf 是否不发自己节点，true 不发自己，false 同时发自己
     */
    public static void sendBusEven(String sv, String topic, String msg, boolean isNotSendSelf) {
        LQBus.sendBus(sv, topic, msg, isNotSendSelf);
    }


    /**
     * 发送消息给相同服务的节点
     * @param topic 主题消息
     * @param msg 消息
     * @param isNotSendSelf 是否不发自己节点，true 不发自己，false 同时发自己
     */
    public static void sendBusEven(String topic, String msg, boolean isNotSendSelf) {
        LQBus.sendBus(LQBus.serverName.iterator().next(), topic, msg, isNotSendSelf);
    }

    /**
     * 发送消息给所有服务节点
     * @param topic 主题消息
     * @param msg 消息
     * @param isNotSendSelf 是否不发自己节点，true 不发自己，false 同时发自己
     */
    public static void sendBusEvenToAllServer(String topic, String msg, boolean isNotSendSelf) {
        LQBus.sendBusAll(topic, msg, isNotSendSelf);
    }



}
