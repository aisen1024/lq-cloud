package cn.lingque.bus;

import cn.lingque.base.LQKey;
import cn.lingque.cloud.node.LQRegisterCenter;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LQBus {

    private final static LQKey LQ_BUS_MESSAGE_CHANNEL = LQKey.key("LQ:BUS:MESSAGE:CHANNEL",1D,LQKey.TEN_MINUTE);

    public volatile static String serverName;
    /**
     * 采用CAS锁来做创建Bus节点
     */
    private static AtomicInteger isInit = new AtomicInteger(0);

    private volatile static Map<String,List<BusHandleBeanInfo>> busMap = new ConcurrentHashMap<>();


    /**
     * 发送消息给相同服务的节点
     * @param sv 服务名称,如果业务量少，可以共用一个，如果消息多，想要及时，则各自定义一个
     * @param topic 主题消息
     * @param msg 消息
     * @param isNotSendSelf 是否不发自己节点，true 不发自己，false 同时发自己
     */
    public static void sendBus(String sv,String topic,String msg,boolean isNotSendSelf){
        Set<LQNodeInfo> nodeList = LQRegisterCenter.getSvNodeList(sv);
        if (nodeList != null && nodeList.size() > 0){
            for (LQNodeInfo node : nodeList) {
                if (isNotSendSelf && LQRegisterCenter.isCurrentNode(node)){
                    continue;
                }
                LQBusMessageBean messageBean = new LQBusMessageBean(topic,msg);
                //根据渠道分发到各个节点
                LQ_BUS_MESSAGE_CHANNEL.rd(node.getServerName()).ofSet().add(messageBean);
            }
        }
    }

    /**
     * 启动bus
     * @param serverName
     */
   public static void startBus(String serverName){
       LQBus.serverName = serverName;
       if (isInit.compareAndSet(0,1)){
            new Thread(()->{
                while (true){
                    AtomicBoolean hasMessage = new AtomicBoolean(false);
                    if (busMap.isEmpty()){
                        continue;
                    }
                    TryCatch.trying(()->{
                      List<LQBusMessageBean> list = LQ_BUS_MESSAGE_CHANNEL.rd(LQBus.serverName).ofSet().pops(10, LQBusMessageBean.class);
                      if (!list.isEmpty()){
                          hasMessage.set(true);
                          list.forEach(msg->{
                              System.out.println(msg.getMsg());
                              List<BusHandleBeanInfo> beanInfos = busMap.get(msg.getTopic());
                              if (!beanInfos.isEmpty()){
                                  beanInfos.forEach((b)->TryCatch.trying(()->LQThreadUtil.execSlave(()->{
                                            b.getBusHandle().handle(LQUtil.isBasClass(b.getEntityClass())?LQUtil.baseClassTran(msg.getMsg(),b.getEntityClass()) : LQUtil.jsonToBean(msg.getMsg(),b.getEntityClass()));
                                  })));
                              }
                          });
                      }
                    });
                    if (!hasMessage.get()){
                        TryCatch.trying(()->{
                            Thread.sleep(1000L);
                        });
                    }
                }
            }).start();
       }
   }

    /**
     * 注册一个bus
     * @param even
     * @param busHandleBeanInfo
     */
   public static synchronized void registerBus(String even,BusHandleBeanInfo busHandleBeanInfo){
       List<BusHandleBeanInfo> list = busMap.getOrDefault(even,new ArrayList<>());
       list.add(busHandleBeanInfo);
       busMap.put(even,list);
   }


    /**
     * 向所有在线的服务组分发消息
     * @param topic
     * @param msg
     * @param isNotSendSelf
     */
    public static void sendBusAll(String topic, String msg, boolean isNotSendSelf) {
        Set<LQNodeInfo> nodeList = LQRegisterCenter.getAllNodeList();
        if (nodeList != null && nodeList.size() > 0){
            for (LQNodeInfo node : nodeList) {
                if (isNotSendSelf && LQRegisterCenter.isCurrentNode(node)){
                    continue;
                }
                LQBusMessageBean messageBean = new LQBusMessageBean(topic,msg);
                //根据渠道分发到各个节点
                LQ_BUS_MESSAGE_CHANNEL.rd(node.getServerName()).ofSet().add(messageBean);
            }
        }
    }
}
