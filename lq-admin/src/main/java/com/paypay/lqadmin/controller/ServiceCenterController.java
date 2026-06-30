package com.paypay.lqadmin.controller;

import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import com.paypay.lqadmin.service.ServiceCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 服务中心管理接口 - 参考 Nacos 设计
 */
@RestController
@RequestMapping("/api/service")
@CrossOrigin(origins = "*")
public class ServiceCenterController {

    @Autowired
    private ServiceCenterService serviceCenterService;

    /**
     * 获取所有服务及其节点（简化版，用于前端展示）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllServices() {
        Set<String> allServices = serviceCenterService.getAllServices();
        List<Map<String, Object>> services = new ArrayList<>();
        
        for (String serviceName : allServices) {
            List<LQEnhancedNodeInfo> nodes = serviceCenterService.getServiceNodes(serviceName);
            
            Map<String, Object> service = new HashMap<>();
            service.put("name", serviceName);
            
            // 构建节点列表
            List<Map<String, Object>> nodeList = new ArrayList<>();
            for (LQEnhancedNodeInfo node : nodes) {
                Map<String, Object> nodeMap = new HashMap<>();
                nodeMap.put("id", node.nodeId());
                nodeMap.put("host", node.getNodeIp());
                nodeMap.put("port", node.getNodePort());
                nodeMap.put("protocol", node.getProtocol());
                nodeMap.put("healthy", serviceCenterService.isNodeHealthy(serviceName, node.nodeId()));
                nodeMap.put("lastHeartbeat", System.currentTimeMillis()); // 简化处理
                nodeMap.put("weight", node.getWeight());
                nodeMap.put("status", node.getStatus());
                nodeList.add(nodeMap);
            }
            
            service.put("nodes", nodeList);
            services.add(service);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", services);
        result.put("code", 200);
        
        return result;
    }

    /**
     * 获取服务列表（分页）
     */
    @GetMapping("/list")
    public Map<String, Object> getServiceList(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String groupName) {
        
        Set<String> allServices = serviceCenterService.getAllServices();
        List<Map<String, Object>> services = new ArrayList<>();
        
        for (String name : allServices) {
            // 过滤
            if (serviceName != null && !name.contains(serviceName)) {
                continue;
            }
            
            List<LQEnhancedNodeInfo> nodes = serviceCenterService.getServiceNodes(name);
            List<LQEnhancedNodeInfo> healthyNodes = serviceCenterService.getHealthyNodes(name);
            
            Map<String, Object> service = new HashMap<>();
            service.put("name", name);
            service.put("groupName", groupName != null ? groupName : "DEFAULT_GROUP");
            service.put("clusterCount", 1);
            service.put("ipCount", nodes.size());
            service.put("healthyInstanceCount", healthyNodes.size());
            service.put("triggerFlag", false);
            services.add(service);
        }
        
        // 分页
        int total = services.size();
        int start = (pageNo - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String, Object>> pageData = start < total ? services.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", total);
        result.put("serviceList", pageData);
        result.put("code", 200);
        
        return result;
    }

    /**
     * 获取服务详情
     */
    @GetMapping("/detail")
    public Map<String, Object> getServiceDetail(
            @RequestParam String serviceName,
            @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName) {
        
        List<LQEnhancedNodeInfo> nodes = serviceCenterService.getServiceNodes(serviceName);
        
        Map<String, Object> service = new HashMap<>();
        service.put("name", serviceName);
        service.put("groupName", groupName);
        service.put("protectThreshold", 0.0);
        service.put("metadata", new HashMap<>());
        service.put("selector", new HashMap<>());
        
        // 集群信息
        Map<String, Object> cluster = new HashMap<>();
        cluster.put("name", "DEFAULT");
        cluster.put("healthChecker", new HashMap<>());
        cluster.put("defaultPort", 8080);
        cluster.put("defaultCheckPort", 8080);
        cluster.put("useIPPort4Check", true);
        cluster.put("metadata", new HashMap<>());
        
        service.put("clusters", Collections.singletonList(cluster));
        service.put("code", 200);
        
        return service;
    }

    /**
     * 获取实例列表
     */
    @GetMapping("/instance/list")
    public Map<String, Object> getInstanceList(
            @RequestParam String serviceName,
            @RequestParam(defaultValue = "DEFAULT_GROUP") String groupName,
            @RequestParam(defaultValue = "DEFAULT") String clusterName,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "true") boolean healthyOnly) {
        
        List<LQEnhancedNodeInfo> nodes = healthyOnly 
            ? serviceCenterService.getHealthyNodes(serviceName)
            : serviceCenterService.getServiceNodes(serviceName);
        
        List<Map<String, Object>> instances = new ArrayList<>();
        for (LQEnhancedNodeInfo node : nodes) {
            instances.add(buildInstanceMap(serviceName, node));
        }
        
        // 分页
        int total = instances.size();
        int start = (pageNo - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String, Object>> pageData = start < total ? instances.subList(start, end) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", total);
        result.put("hosts", pageData);
        result.put("code", 200);
        
        return result;
    }

    /**
     * 更新实例
     */
    @PutMapping("/instance")
    public Map<String, Object> updateInstance(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    /**
     * 下线实例（简化版）
     */
    @DeleteMapping("/node/{serviceName}/{nodeId}")
    public Map<String, Object> deregisterNode(
            @PathVariable String serviceName,
            @PathVariable String nodeId) {
        
        boolean success = serviceCenterService.deregisterNode(serviceName, nodeId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "下线成功" : "下线失败");
        result.put("data", success);
        return result;
    }

    /**
     * 下线实例
     */
    @DeleteMapping("/instance")
    public Map<String, Object> deleteInstance(
            @RequestParam String serviceName,
            @RequestParam String ip,
            @RequestParam int port) {
        
        // 查找并下线节点
        List<LQEnhancedNodeInfo> nodes = serviceCenterService.getServiceNodes(serviceName);
        for (LQEnhancedNodeInfo node : nodes) {
            if (node.getNodeIp().equals(ip) && node.getNodePort().equals(port)) {
                boolean success = serviceCenterService.deregisterNode(serviceName, node.nodeId());
                
                Map<String, Object> result = new HashMap<>();
                result.put("code", success ? 200 : 500);
                result.put("message", success ? "下线成功" : "下线失败");
                return result;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 404);
        result.put("message", "实例不存在");
        return result;
    }

    /**
     * 获取服务统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Set<String> allServices = serviceCenterService.getAllServices();
        
        int totalServices = allServices.size();
        int totalInstances = 0;
        int healthyInstances = 0;
        
        for (String serviceName : allServices) {
            totalInstances += serviceCenterService.getServiceNodes(serviceName).size();
            healthyInstances += serviceCenterService.getHealthyNodes(serviceName).size();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("serviceCount", totalServices);
        result.put("instanceCount", totalInstances);
        result.put("healthyInstanceCount", healthyInstances);
        result.put("unhealthyInstanceCount", totalInstances - healthyInstances);
        result.put("code", 200);
        
        return result;
    }

    /**
     * 构建实例信息
     */
    private Map<String, Object> buildInstanceMap(String serviceName, LQEnhancedNodeInfo node) {
        Map<String, Object> instance = new HashMap<>();
        instance.put("instanceId", node.nodeId());
        instance.put("ip", node.getNodeIp());
        instance.put("port", node.getNodePort());
        instance.put("weight", node.getWeight());
        instance.put("healthy", serviceCenterService.isNodeHealthy(serviceName, node.nodeId()));
        instance.put("enabled", "ONLINE".equals(node.getStatus()));
        instance.put("ephemeral", true);
        instance.put("clusterName", "DEFAULT");
        instance.put("serviceName", serviceName);
        instance.put("metadata", buildMetadata(node));
        instance.put("instanceHeartBeatInterval", node.getHealthCheckInterval() * 1000);
        instance.put("instanceHeartBeatTimeOut", node.getHealthCheckInterval() * 2000);
        instance.put("ipDeleteTimeout", node.getHealthCheckInterval() * 3000);
        
        return instance;
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata(LQEnhancedNodeInfo node) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("protocol", node.getProtocol());
        metadata.put("version", node.getVersion());
        metadata.put("status", node.getStatus());
        
        if (node.getTags() != null && !node.getTags().isEmpty()) {
            metadata.put("tags", String.join(",", node.getTags()));
        }
        
        if (node.getMetadata() != null) {
            metadata.putAll(node.getMetadata());
        }
        
        return metadata;
    }
}

