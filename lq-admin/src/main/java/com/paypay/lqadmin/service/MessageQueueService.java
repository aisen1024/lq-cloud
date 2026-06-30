package com.paypay.lqadmin.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 消息队列服务
 */
@Service
public class MessageQueueService {

    // 用于存储队列信息（实际应该从 Redis 或其他存储中获取）
    private final Map<String, QueueInfo> queues = new HashMap<>();
    private long totalProcessed = 0;
    private long totalFailed = 0;

    public List<Map<String, Object>> getAllQueues() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        queues.forEach((name, info) -> {
            Map<String, Object> queueMap = new HashMap<>();
            queueMap.put("name", name);
            queueMap.put("type", info.type);
            queueMap.put("pending", info.pending);
            queueMap.put("processed", info.processed);
            result.add(queueMap);
        });
        
        return result;
    }

    public Map<String, Object> getQueueInfo(String queue) {
        QueueInfo info = queues.getOrDefault(queue, new QueueInfo());
        Map<String, Object> result = new HashMap<>();
        result.put("name", queue);
        result.put("type", info.type);
        result.put("pending", info.pending);
        result.put("processed", info.processed);
        result.put("failed", info.failed);
        return result;
    }

    public Map<String, Object> getMQStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed);
        stats.put("totalFailed", totalFailed);
        stats.put("totalQueues", queues.size());
        stats.put("totalPending", queues.values().stream()
                .mapToLong(q -> q.pending).sum());
        return stats;
    }
    
    public void clearQueue(String queue) {
        QueueInfo info = queues.get(queue);
        if (info != null) {
            info.pending = 0;
        }
    }
    
    public int getQueueCount() {
        return queues.size();
    }
    
    private static class QueueInfo {
        String type = "LIST";
        long pending = 0;
        long processed = 0;
        long failed = 0;
    }
}
