package com.lingque.cloud.node;

import cn.hutool.json.JSONUtil;
import com.lingque.base.LQKey;
import com.lingque.cloud.node.bean.LQNodeInfo;
import com.lingque.redis.LingQueRedis;
import com.lingque.util.LQUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class LQRegisterCenter {

    //节点服务列表
    private final static LQKey nodeService = LQKey.key("LQ:CLOUD:NODE:REG:CENTER", 1D, 5L);
    //服务列表汇总
    private final static LQKey svService = LQKey.key("LQ:CLOUD:NODE:REG:CENTER:SV", 1D, LQKey.FOREVER);

    /***
     * 节点线程
     */
    private static Thread clearThread;

    /***
     * 心跳线程
     */
    private static Thread heartThread;

    /**
     * 本项目注册的节点
     */
    public static Set<LQNodeInfo> currentNodes = new HashSet<>();

    /**
     * 采用CAS锁来做创建NodeThread
     */
    private static AtomicInteger isInitRegister = new AtomicInteger(0);


    /**
     * 注册服务注册
     *
     * @param node
     * @return
     */
    private static boolean registerNode(LQNodeInfo node) {
        currentNodes.add(node);
        nodeService.rd(node.getServerName()).ofZSet().add(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
        svService.rd().ofSet().add(node.getServerName());
        log.info("成功注册服务 {} | ip {} | port {} ", node.getServerName(), node.getNodeIp(), node.getNodePort());
        return true;
    }

    /**
     * 获取服务节点
     *
     * @param serverName
     * @return
     */
    public static Set<LQNodeInfo> getSvNodeList(String serverName) {
        return (Set<LQNodeInfo>) nodeService.rd(serverName).ofZSet().getByScore(System.currentTimeMillis() - 5000D, System.currentTimeMillis() * 1D).stream().map(i -> LQUtil.jsonToBean((String) i, LQNodeInfo.class)).collect(Collectors.toSet());
    }


    /**
     * 初始化组册中心，上报节点，维持心跳
     *
     * @return
     */
    private static void start() {
        if (isInitRegister.compareAndSet(0, 1)) {
            //更新节点到对应的服务，维持心跳3秒一次
            heartThread = new Thread(() -> {
                while (true) {
                    for (LQNodeInfo node : currentNodes) {
                        try {
                            nodeService.rd(node.getServerName()).ofZSet().add(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
                        } catch (Exception e) {
                            log.error("注册节点并发布心跳异常 | {} ", JSONUtil.toJsonStr(node));
                        }
                    }
                    try {
                        Thread.sleep(3000L);
                    } catch (Exception e) {
                        log.error("tryInitRegisterCenter 睡眠失败！", e);
                    }
                }
            });
            heartThread.start();

            //清空5秒没有上报的节点
            clearThread = new Thread(() -> {
                while (true) {
                    List<String> svList = svService.<String>rd().ofSet().getMembers(String.class);
                    for (String sv : svList) {
                        try {
                            LingQueRedis svHandle = nodeService.rd(sv);
                            List<String> timeoutSvList = svHandle.ofZSet().getByScore(1D, System.currentTimeMillis() - 5000D);
                            if (timeoutSvList != null && timeoutSvList.size() > 0) {
                                String[] keys = new String[timeoutSvList.size()];
                                keys = timeoutSvList.toArray(keys);
                                svHandle.ofZSet().delete(keys);
                            }
                        } catch (Exception e) {
                            log.error("注册节点并发布心跳异常 | {} ", sv);
                        }
                    }
                    try {
                        Thread.sleep(2000L);
                    } catch (Exception e) {
                        log.error("tryInitRegisterCenter 睡眠失败！", e);
                    }
                }
            });
            clearThread.start();
        }
    }

    public static boolean isCurrentNode(LQNodeInfo node) {
        return currentNodes.stream().filter(n->n.equals(node)).count() > 0;
    }

}
