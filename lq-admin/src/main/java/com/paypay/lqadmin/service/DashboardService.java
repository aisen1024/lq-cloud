package com.paypay.lqadmin.service;

import cn.lingque.cloud.config.enhanced.LQEnhancedConfigCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Dashboard 服务
 */
@Service
public class DashboardService {

    @Autowired
    private MessageBusService messageBusService;
    
    @Autowired
    private MessageQueueService messageQueueService;
    
    @Autowired
    private ServiceCenterService serviceCenterService;

    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        
        // 统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("configCount", getConfigCount());
        stats.put("serviceCount", serviceCenterService.getAllServices().size());
        stats.put("busTopicCount", messageBusService.getTopicCount());
        stats.put("mqQueueCount", messageQueueService.getQueueCount());
        result.put("stats", stats);
        
        // 服务列表
        List<Map<String, Object>> services = new ArrayList<>();
        for (String serviceName : serviceCenterService.getAllServices()) {
            Map<String, Object> service = new HashMap<>();
            service.put("name", serviceName);
            service.put("totalNodes", serviceCenterService.getServiceNodes(serviceName).size());
            service.put("healthyNodes", serviceCenterService.getHealthyNodes(serviceName).size());
            services.add(service);
        }
        result.put("services", services);
        
        // 活动记录
        result.put("activities", getRecentActivities());
        
        return result;
    }
    
    private int getConfigCount() {
        try {
            var stats = LQEnhancedConfigCenter.getStats();
            return stats != null ? stats.getTotalConfigCount() : 0;  // 使用 getTotalConfigCount()
        } catch (Exception e) {
            return 0;
        }
    }
    
    private List<Map<String, Object>> getRecentActivities() {
        // 这里可以实现活动记录的逻辑
        // 暂时返回空列表
        return new ArrayList<>();
    }
}
