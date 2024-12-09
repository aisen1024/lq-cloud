package com.lingque.bus;

import cn.hutool.json.JSONUtil;
import com.lingque.base.LQKey;
import com.lingque.cloud.node.LQRegisterCenter;
import com.lingque.cloud.node.bean.LQNodeInfo;

import java.util.Set;

public class LQBus {

    private final static LQKey LQ_BUS_MESSAGE_CHANNEL = LQKey.key("LQ:BUS:MESSAGE:CHANNEL",1D,LQKey.TEN_MINUTE);

    /**
     * 发送消息给相同服务的节点
     * @param sv 服务名称,如果业务量少，可以共用一个，如果消息多，想要及时，则各自定义一个
     * @param topic 主题消息
     * @param msg 消息
     * @param isNotSendSelf 是否不发自己节点，true 不发自己，false 同时发自己
     */
    public static void sendBusMsg(String sv,String topic,String msg,boolean isNotSendSelf){
        Set<LQNodeInfo> nodeList = LQRegisterCenter.getSvNodeList(sv);
        if (nodeList != null && nodeList.size() > 0){
            for (LQNodeInfo node : nodeList) {
                if (isNotSendSelf && LQRegisterCenter.isCurrentNode(node)){
                    continue;
                }
                LQ_BUS_MESSAGE_CHANNEL.rd(node.getNodeIp())
            }
        }
    }

    /**
     *  默认发送节点
     * @param topic
     * @param msg 默认是 obj.toString 如果需要特定的格式化，请自行初始化成string再传入
     * @param isNotSendSelf
     */
    public static void defaultSendNodeMsg(String topic,Object msg,boolean isNotSendSelf){
        String msgStr = FriendUtil.isBaseValue(msg) ? msg.toString() :JSONUtil.toJsonStr(msg);
        sendSvNodeMsg(MINI_MAIN_SV,topic,msgStr,isNotSendSelf);
    }

}
