package cn.lingque.cloud.node;

import cn.hutool.json.JSONUtil;
import cn.lingque.base.LQKey;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.util.LQUtil;
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
    private final static LQKey svGroupService = LQKey.key("LQ:CLOUD:NODE:REG:CENTER:GROUP", 1D, LQKey.FOREVER);

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
    public volatile static Set<LQNodeInfo> currentNodes = new HashSet<>();

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
    public static boolean registerNode(LQNodeInfo node) {
        currentNodes.add(node);
        nodeService.rd(node.getServerName()).ofZSet().setScore(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
        svGroupService.rd().ofZSet().setScore(node.getServerName(),System.currentTimeMillis() * 1D);
        log.info("成功注册服务 {} | ip {} | port {} ", node.getServerName(), node.getNodeIp(), node.getNodePort());
        return true;
    }

    /**
     * 获取集群服务节点
     *
     * @param serverName
     * @return
     */
    public static Set<LQNodeInfo> getSvNodeList(String serverName) {
        List<RedisRank> svList = nodeService.<List<RedisRank>>rd(serverName).ofZSet().getByScoreRange(System.currentTimeMillis() - 5000D, System.currentTimeMillis() * 1D,99999);
        return svList.stream().map(i -> LQUtil.jsonToBean(i.getMemberId(), LQNodeInfo.class)).collect(Collectors.toSet());
    }


    /**
     * 获取服务组所有节点
     * @return
     */
    public static Set<LQNodeInfo> getAllNodeList() {
        List<String> serverGroups = svGroupService.rd().ofZSet().getMembers(1,System.currentTimeMillis());
        Set<LQNodeInfo> set = new HashSet<>();
        if (serverGroups != null && !serverGroups.isEmpty()) {
            for (String serverGroup : serverGroups) {
                List<RedisRank> svList = nodeService.<List<RedisRank>>rd(serverGroup).ofZSet().getByScoreRange(System.currentTimeMillis() - 5000D, System.currentTimeMillis() * 1D,99999);
                set.addAll(svList.stream().map(i -> LQUtil.jsonToBean(i.getMemberId(), LQNodeInfo.class)).collect(Collectors.toSet()));
            }
        }
        return set;
    }



    /**
     * 初始化组册中心，上报节点，维持心跳
     *
     * @return
     */
    public static void start() {
        if (isInitRegister.compareAndSet(0, 1)) {
            //更新节点到对应的服务，维持心跳3秒一次
            heartThread = new Thread(() -> {
                while (true) {
                    for (LQNodeInfo node : currentNodes) {
                        try {
                            nodeService.rd(node.getServerName()).ofZSet().setScore(JSONUtil.toJsonStr(node), System.currentTimeMillis() * 1D);
                            svGroupService.rd().ofZSet().setScore(node.getServerName(),System.currentTimeMillis() * 1D);
                        } catch (Exception e) {
                            log.error("注册节点并发布心跳异常 | {} ", JSONUtil.toJsonStr(node),e);
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
                    List<String> svList = svGroupService.rd().ofZSet().getMembers(1,System.currentTimeMillis());
                    for (String sv : svList) {
                        try {
                            LingQueRedis svHandle = nodeService.rd(sv);
                            List<String> timeoutSvList = svHandle.ofZSet().getMembers(1, System.currentTimeMillis() - 5000);
                            if (timeoutSvList != null && timeoutSvList.size() > 0) {
                                String[] keys = new String[timeoutSvList.size()];
                                keys = timeoutSvList.toArray(keys);
                                svHandle.ofZSet().delete(keys);
                            }
                            //如果当前服务组节点都没有了，直接下架服务
                            if (svHandle.ofZSet().size() <= 0){
                                svGroupService.rd().ofZSet().delete(sv);
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

    /**
     * 判断是否当前节点
     * @param node
     * @return
     */
    public static boolean isCurrentNode(LQNodeInfo node) {
        return currentNodes.stream().filter(n->n.equals(node)).count() > 0;
    }

}
