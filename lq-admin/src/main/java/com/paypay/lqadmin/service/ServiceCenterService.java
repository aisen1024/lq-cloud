package com.paypay.lqadmin.service;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.base.LQKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 服务中心服务
 */
@Slf4j
@Service
public class ServiceCenterService {

    // Redis Keys - 与 LQEnhancedRegisterCenter 保持一致
    private final static LQKey svGroupService = LQKey.key("LQ:CLOUD:ENHANCED:NODE:REG:CENTER:GROUP", 1D, LQKey.FOREVER);
    private final static LQKey enhancedNodeService = LQKey.key("LQ:CLOUD:ENHANCED:NODE:REG:CENTER", 1D, 5L);

    /**
     * 获取所有服务名称
     */
    public Set<String> getAllServices() {
        try {
            List<String> serverGroups = svGroupService.rd().ofZSet().getMembers(1, System.currentTimeMillis());
            return serverGroups != null ? new HashSet<>(serverGroups) : new HashSet<>();
        } catch (Exception e) {
            log.error("获取所有服务失败", e);
            return new HashSet<>();
        }
    }

    /**
     * 获取服务的所有节点
     */
    public List<LQEnhancedNodeInfo> getServiceNodes(String serviceName) {
        try {
            return LQEnhancedRegisterCenter.getEnhancedNodeList(serviceName);
        } catch (Exception e) {
            log.error("获取服务节点失败: {}", serviceName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取服务的健康节点
     */
    public List<LQEnhancedNodeInfo> getHealthyNodes(String serviceName) {
        try {
            List<LQEnhancedNodeInfo> allNodes = getServiceNodes(serviceName);
            return allNodes.stream()
                    .filter(LQEnhancedNodeInfo::isHealthy)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取健康节点失败: {}", serviceName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 下线节点
     */
    public boolean deregisterNode(String serviceName, String nodeId) {
        try {
            // 从 Redis ZSet 中删除节点ID
            enhancedNodeService.rd(serviceName).ofZSet().zrem(nodeId);
            
            // 从 Redis Hash 中删除节点详细信息
            enhancedNodeService.rd(serviceName + ":details").ofHash().deleteField(nodeId);
            
            log.info("成功下线节点: serviceName={}, nodeId={}", serviceName, nodeId);
            return true;
        } catch (Exception e) {
            log.error("下线节点失败: serviceName={}, nodeId={}", serviceName, nodeId, e);
            return false;
        }
    }

    /**
     * 检查节点是否健康
     */
    public boolean isNodeHealthy(String serviceName, String nodeId) {
        try {
            List<LQEnhancedNodeInfo> healthyNodes = getHealthyNodes(serviceName);
            return healthyNodes.stream()
                    .anyMatch(node -> node.nodeId().equals(nodeId));
        } catch (Exception e) {
            log.error("检查节点健康状态失败: serviceName={}, nodeId={}", serviceName, nodeId, e);
            return false;
        }
    }
}
