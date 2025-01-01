package cn.lingque.runner;

import cn.lingque.base.LQKey;
import cn.lingque.bus.LQBus;
import cn.lingque.cloud.node.LQRegisterCenter;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.config.LQProperties;
import cn.lingque.mq.LQMQConsumer;
import cn.lingque.redis.JedisProxy;
import cn.lingque.runner.config.LqSpringUtil;
import cn.lingque.thread.LQThread;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.IpUtil;
import cn.lingque.util.LQUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动灵雀云组件
 */
@Slf4j
public class LqCloudRunner {

    public LqCloudRunner(LQProperties lqProperties){

        //检查配置，初始化基础
        checkConfig(lqProperties);

        //初始化基础配置
        doInit(lqProperties);

        //启动注册中心
        startRegisterCenter(lqProperties);

        //启动消息总线
        if (lqProperties.getBus().getEnable()){
            LQBus.startBus(lqProperties.getServerName());
        }

        //启动MQ
        LQMQConsumer.start();

        log.info("<<<<<<<<<灵雀云组件启动成功>>>>>>>>");

    }

    /**
     * 检查配置
     * @param lqProperties
     */
    private void checkConfig(LQProperties lqProperties){
        if (LQUtil.isEmpty(lqProperties.getServerName())){
            String applicationName = LqSpringUtil.getApplicationName();
            lqProperties.setServerName(LQUtil.isEmpty(applicationName) ?  "lq_server" : applicationName);
        }
        if (LQUtil.isEmpty(lqProperties.getServerHost())){
            lqProperties.setServerHost(IpUtil.getLocalIp());
        }
        if (lqProperties.getServerPort() == null){
            String port = LqSpringUtil.getProperty("server.port");
            lqProperties.setServerPort(Integer.parseInt(LQUtil.defaultString(port,"8080")));
        }
    }

    /**
     * 初始化redis
     * @param lqProperties
     */
    private void doInit(LQProperties lqProperties){
        //构建redis实例
        JedisProxy.init(lqProperties);
        //构建线程池
        LQThread thread = LQThread.init(lqProperties);
        LQThreadUtil.init(thread);
    }

    /**
     * 启动注册中心
     * @param lqProperties
     */
    private void startRegisterCenter(LQProperties lqProperties){
        LQNodeInfo node = new LQNodeInfo();
        node.setNodeIp(lqProperties.getServerHost());
        node.setNodePort(lqProperties.getServerPort());
        node.setServerName(lqProperties.getServerName());
        LQRegisterCenter.registerNode(node);
        //启动注册服务中心
        LQRegisterCenter.start();
    }

//    public static void main(String[] args) {
//        LQProperties lqProperties = new LQProperties();
//        lqProperties.setServerHost("127.0.0.1");
//        lqProperties.setServerPort(9999);
//        lqProperties.setServerName("lq_ss");
//        lqProperties.setDb(1);
//        LqCloudRunner lqCloudRunner = new LqCloudRunner(lqProperties);
//        LQKey ks = LQKey.key("lll",1D,10L);
//        ks.rd().ofZSet().delete("123");
//    }
}
