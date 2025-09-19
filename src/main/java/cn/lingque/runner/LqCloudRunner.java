package cn.lingque.runner;

import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import cn.lingque.bus.LQBus;
import cn.lingque.cloud.node.LQRegisterCenter;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.config.LQProperties;
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
        LQNodeInfo node = startRegisterCenter(lqProperties);

        //启动消息总线
        if (lqProperties.getBus().getEnable()){
            //集群消息总线
            LQBus.startBus(lqProperties.getServer().getServerName());
            //精准消息总线
            LQBus.startBus(lqProperties.getServer().getServerName()+":"+ MD5.create().digestHex16(JSONUtil.toJsonStr(node)));
        }

        //启动MQ

        log.info("<<<<<<<<<灵雀云组件启动成功>>>>>>>>");}

    /**
     * 检查配置
     * @param lqProperties
     */
    private void checkConfig(LQProperties lqProperties){
        if (LQUtil.isEmpty(lqProperties.getServer().getServerName())){
            String applicationName = LqSpringUtil.getApplicationName();
            lqProperties.getServer().setServerName(LQUtil.isEmpty(applicationName) ?  "lq_server" : applicationName);
        }
        if (LQUtil.isEmpty(lqProperties.getServer().getServerHost())){
            lqProperties.getServer().setServerHost(IpUtil.getLocalIp());
        }
        if (lqProperties.getServer().getServerPort() == null){
            String port = LqSpringUtil.getProperty("server.port");
            lqProperties.getServer().setServerPort(Integer.parseInt(LQUtil.defaultString(port,"8080")));
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
    private LQNodeInfo startRegisterCenter(LQProperties lqProperties){
        LQNodeInfo node = new LQNodeInfo();
        node.setNodeIp(lqProperties.getServer().getServerHost());
        node.setNodePort(lqProperties.getServer().getServerPort());
        node.setServerName(lqProperties.getServer().getServerName());
        LQRegisterCenter.registerNode(node);
        //启动注册服务中心
        LQRegisterCenter.start();
        return node;
    }
}
