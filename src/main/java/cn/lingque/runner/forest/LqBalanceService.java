package cn.lingque.runner.forest;

import cn.lingque.base.LQKey;
import cn.lingque.cloud.node.LQRegisterCenter;
import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.runner.LqCloudRunner;
import cn.lingque.util.LQUtil;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 *  负载均衡
 * </p>
 *
 * @author zlm
 * @version v1.0
 * @date 2023/11/3
 */
@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnBean(value = {LqCloudRunner.class})
public class LqBalanceService {

    //服务组管理
    private static Map<String, List<LQNodeInfo>> nodeManager = new HashMap<>();
    //节点负载
    private static Map<String,AtomicInteger> incrPoint = new HashMap<>();
    private static final LQKey meltdownService = LQKey.key("lq:meltdown:server:nodes",1D,LQKey.TEN_MINUTE);
    //默认熔断是20秒，20秒内不可用
    private static int meltdownTime = 20;

    @PostConstruct
    public void flushNode() {
        LQUtil.execLoadJob("刷新节点信息", () -> {
                Set<LQNodeInfo> nodes = LQRegisterCenter.getAllNodeList();
                Map<String, List<LQNodeInfo>> result = new HashMap<>();
                List<RedisRank> meltdownList = meltdownService.rd().ofZSet().pageRankLimit(0, 9999);
                Set<String> meltdownSet = meltdownList.stream().filter(i -> i.getScore() > System.currentTimeMillis()).map(i -> i.getMemberId()).collect(Collectors.toSet());
                if (nodes.size() > 0) {
                    nodes.forEach(node -> {
                        if (!meltdownSet.contains(node.toString())) {
                            LQUtil.getMapList(result, node.getServerName()).add(node);
                        }
                    });
                    nodeManager = result;
                    //移除熔断过期节点
                    List<String> timeoutmMltdownList = meltdownList.stream().filter(i -> i.getScore() < System.currentTimeMillis()).map(i -> i.getMemberId()).collect(Collectors.toList());
                    if(timeoutmMltdownList.size() > 0)
                        meltdownService.rd().ofZSet().zremAll(timeoutmMltdownList);
                }

        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 轮询负载
     * @param serverName
     * @return
     */
    public LQNodeInfo pollingBalance(String serverName) {
        List<LQNodeInfo> list = nodeManager.get(serverName);
        if (LQUtil.isEmpty(list)) {
            return null;
        }
        AtomicInteger atomicInteger = incrPoint.get(serverName);
        if (atomicInteger == null){
            atomicInteger = new AtomicInteger(0);
            incrPoint.put(serverName,atomicInteger);
        }
        int point = atomicInteger.incrementAndGet();
        if (point >= list.size()){
            incrPoint.get(serverName).set(0);
            return list.get(0);
        }
        return list.get(point);
    }

    /**
     * 随机负载
     * @param serverName
     * @return
     */
    public LQNodeInfo randomBalance(String serverName) {
        try {
            List<LQNodeInfo> list = nodeManager.get(serverName);
            if (LQUtil.isEmpty(list)) {
                return null;
            }
            return list.get(LQUtil.randomInt(list.size()));
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 熔断服务
     * @param node
     */
    public void disableNode(LQNodeInfo node){
        if (null == node){
            return;
        }
        meltdownService.rd().ofZSet().setScore(node.toString(),System.currentTimeMillis() + (meltdownTime * 1000D));
    }


}
