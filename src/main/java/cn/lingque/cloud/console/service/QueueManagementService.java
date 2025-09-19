package cn.lingque.cloud.console.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 队列管理服务
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Service
public class QueueManagementService {
    
    // 模拟队列数据存储
    private final Map<String, QueueInfo> enhancedQueues = new ConcurrentHashMap<>();
    private final Map<String, QueueInfo> standardQueues = new ConcurrentHashMap<>();
    private final AtomicLong queueIdGenerator = new AtomicLong(1);
    
    public QueueManagementService() {
        // 初始化一些示例队列
        initializeSampleQueues();
    }
    
    /**
     * 队列信息
     */
    public static class QueueInfo {
        private String id;
        private String name;
        private String type; // "enhanced" 或 "standard"
        private String status; // "active", "paused", "stopped"
        private long messageCount;
        private long consumerCount;
        private long producerCount;
        private Date createTime;
        private Date lastUpdateTime;
        private Map<String, Object> properties;
        private QueueStats stats;
        
        public QueueInfo() {
            this.createTime = new Date();
            this.lastUpdateTime = new Date();
            this.properties = new HashMap<>();
            this.stats = new QueueStats();
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getMessageCount() { return messageCount; }
        public void setMessageCount(long messageCount) { this.messageCount = messageCount; }
        
        public long getConsumerCount() { return consumerCount; }
        public void setConsumerCount(long consumerCount) { this.consumerCount = consumerCount; }
        
        public long getProducerCount() { return producerCount; }
        public void setProducerCount(long producerCount) { this.producerCount = producerCount; }
        
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        
        public Date getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(Date lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
        
        public QueueStats getStats() { return stats; }
        public void setStats(QueueStats stats) { this.stats = stats; }
    }
    
    /**
     * 队列统计信息
     */
    public static class QueueStats {
        private long totalMessages;
        private long processedMessages;
        private long failedMessages;
        private double avgProcessingTime;
        private long peakMessageCount;
        private Date lastMessageTime;
        
        // Getters and Setters
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        
        public long getProcessedMessages() { return processedMessages; }
        public void setProcessedMessages(long processedMessages) { this.processedMessages = processedMessages; }
        
        public long getFailedMessages() { return failedMessages; }
        public void setFailedMessages(long failedMessages) { this.failedMessages = failedMessages; }
        
        public double getAvgProcessingTime() { return avgProcessingTime; }
        public void setAvgProcessingTime(double avgProcessingTime) { this.avgProcessingTime = avgProcessingTime; }
        
        public long getPeakMessageCount() { return peakMessageCount; }
        public void setPeakMessageCount(long peakMessageCount) { this.peakMessageCount = peakMessageCount; }
        
        public Date getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    }
    
    /**
     * 获取所有队列
     */
    public Map<String, List<QueueInfo>> getAllQueues() {
        Map<String, List<QueueInfo>> result = new HashMap<>();
        result.put("enhanced", new ArrayList<>(enhancedQueues.values()));
        result.put("standard", new ArrayList<>(standardQueues.values()));
        return result;
    }
    
    /**
     * 获取增强队列列表
     */
    public List<QueueInfo> getEnhancedQueues() {
        return new ArrayList<>(enhancedQueues.values());
    }
    
    /**
     * 获取标准队列列表
     */
    public List<QueueInfo> getStandardQueues() {
        return new ArrayList<>(standardQueues.values());
    }
    
    /**
     * 根据ID获取队列信息
     */
    public QueueInfo getQueueById(String queueId) {
        QueueInfo queue = enhancedQueues.get(queueId);
        if (queue == null) {
            queue = standardQueues.get(queueId);
        }
        return queue;
    }
    
    /**
     * 创建队列
     */
    public QueueInfo createQueue(String name, String type, Map<String, Object> properties) {
        QueueInfo queue = new QueueInfo();
        queue.setId("queue_" + queueIdGenerator.getAndIncrement());
        queue.setName(name);
        queue.setType(type);
        queue.setStatus("active");
        queue.setMessageCount(0);
        queue.setConsumerCount(0);
        queue.setProducerCount(0);
        
        if (properties != null) {
            queue.getProperties().putAll(properties);
        }
        
        if ("enhanced".equals(type)) {
            enhancedQueues.put(queue.getId(), queue);
        } else {
            standardQueues.put(queue.getId(), queue);
        }
        
        return queue;
    }
    
    /**
     * 更新队列
     */
    public QueueInfo updateQueue(String queueId, Map<String, Object> updates) {
        QueueInfo queue = getQueueById(queueId);
        if (queue == null) {
            throw new RuntimeException("队列不存在: " + queueId);
        }
        
        if (updates.containsKey("name")) {
            queue.setName((String) updates.get("name"));
        }
        if (updates.containsKey("status")) {
            queue.setStatus((String) updates.get("status"));
        }
        if (updates.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) updates.get("properties");
            queue.getProperties().putAll(props);
        }
        
        queue.setLastUpdateTime(new Date());
        return queue;
    }
    
    /**
     * 删除队列
     */
    public boolean deleteQueue(String queueId) {
        boolean removed = enhancedQueues.remove(queueId) != null;
        if (!removed) {
            removed = standardQueues.remove(queueId) != null;
        }
        return removed;
    }
    
    /**
     * 暂停队列
     */
    public boolean pauseQueue(String queueId) {
        QueueInfo queue = getQueueById(queueId);
        if (queue != null) {
            queue.setStatus("paused");
            queue.setLastUpdateTime(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * 恢复队列
     */
    public boolean resumeQueue(String queueId) {
        QueueInfo queue = getQueueById(queueId);
        if (queue != null) {
            queue.setStatus("active");
            queue.setLastUpdateTime(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * 清空队列消息
     */
    public boolean purgeQueue(String queueId) {
        QueueInfo queue = getQueueById(queueId);
        if (queue != null) {
            queue.setMessageCount(0);
            queue.setLastUpdateTime(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * 获取队列统计信息
     */
    public Map<String, Object> getQueueStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalEnhancedQueues = enhancedQueues.size();
        long totalStandardQueues = standardQueues.size();
        long totalQueues = totalEnhancedQueues + totalStandardQueues;
        
        long activeQueues = getAllQueues().values().stream()
                .flatMap(List::stream)
                .mapToLong(q -> "active".equals(q.getStatus()) ? 1 : 0)
                .sum();
        
        long totalMessages = getAllQueues().values().stream()
                .flatMap(List::stream)
                .mapToLong(QueueInfo::getMessageCount)
                .sum();
        
        stats.put("totalQueues", totalQueues);
        stats.put("enhancedQueues", totalEnhancedQueues);
        stats.put("standardQueues", totalStandardQueues);
        stats.put("activeQueues", activeQueues);
        stats.put("totalMessages", totalMessages);
        stats.put("lastUpdateTime", new Date());
        
        return stats;
    }
    
    /**
     * 初始化示例队列
     */
    private void initializeSampleQueues() {
        // 创建增强队列示例
        Map<String, Object> enhancedProps = new HashMap<>();
        enhancedProps.put("maxRetries", 3);
        enhancedProps.put("deadLetterQueue", true);
        enhancedProps.put("priority", "high");
        
        QueueInfo enhancedQueue1 = createQueue("user-events", "enhanced", enhancedProps);
        enhancedQueue1.setMessageCount(150);
        enhancedQueue1.setConsumerCount(3);
        enhancedQueue1.setProducerCount(5);
        
        QueueInfo enhancedQueue2 = createQueue("order-processing", "enhanced", enhancedProps);
        enhancedQueue2.setMessageCount(89);
        enhancedQueue2.setConsumerCount(2);
        enhancedQueue2.setProducerCount(3);
        
        // 创建标准队列示例
        Map<String, Object> standardProps = new HashMap<>();
        standardProps.put("timeout", 30000);
        standardProps.put("persistent", true);
        
        QueueInfo standardQueue1 = createQueue("notifications", "standard", standardProps);
        standardQueue1.setMessageCount(45);
        standardQueue1.setConsumerCount(1);
        standardQueue1.setProducerCount(2);
        
        QueueInfo standardQueue2 = createQueue("logs", "standard", standardProps);
        standardQueue2.setMessageCount(320);
        standardQueue2.setConsumerCount(4);
        standardQueue2.setProducerCount(8);
    }
}