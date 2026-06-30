package com.paypay.lqadmin.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 消息总线服务
 */
@Service
public class MessageBusService {

    // 用于存储主题和订阅者信息（实际应该从 Redis 或其他存储中获取）
    private final Map<String, List<String>> topicSubscribers = new HashMap<>();
    private long totalMessages = 0;

    public List<Map<String, Object>> getAllTopics() {
        List<Map<String, Object>> topics = new ArrayList<>();
        
        topicSubscribers.forEach((topic, subscribers) -> {
            Map<String, Object> topicInfo = new HashMap<>();
            topicInfo.put("name", topic);
            topicInfo.put("subscriberCount", subscribers.size());
            topicInfo.put("messageCount", 0); // 可以从统计中获取
            topics.add(topicInfo);
        });
        
        return topics;
    }

    public List<Map<String, Object>> getTopicSubscribers(String topic) {
        List<Map<String, Object>> subscribers = new ArrayList<>();
        
        List<String> subs = topicSubscribers.getOrDefault(topic, new ArrayList<>());
        for (String subscriber : subs) {
            Map<String, Object> subInfo = new HashMap<>();
            subInfo.put("id", subscriber);
            subInfo.put("topic", topic);
            subInfo.put("status", "active");
            subscribers.add(subInfo);
        }
        
        return subscribers;
    }

    public Map<String, Object> getBusStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", totalMessages);
        stats.put("totalTopics", topicSubscribers.size());
        stats.put("totalSubscribers", topicSubscribers.values().stream()
                .mapToInt(List::size).sum());
        return stats;
    }
    
    public int getTopicCount() {
        return topicSubscribers.size();
    }
    
    public void incrementMessageCount() {
        totalMessages++;
    }
}
