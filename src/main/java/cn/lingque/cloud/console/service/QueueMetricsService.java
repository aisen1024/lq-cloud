package cn.lingque.cloud.console.service;

import cn.lingque.mq.exten.LQStreamUnifiedQueue;
import cn.lingque.mq.exten.LQUnifiedQueue;
import cn.lingque.redis.LingQueRedis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队列指标统计服务
 * @author aisen
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueMetricsService {
    

    // 缓存队列实例
    private final Map<String, LQUnifiedQueue<?>> unifiedQueues = new ConcurrentHashMap<>();
    private final Map<String, LQStreamUnifiedQueue<?>> streamQueues = new ConcurrentHashMap<>();
    
    /**
     * 获取所有队列的指标统计
     * @return 队列指标数据
     */
    public Map<String, Object> getAllQueueMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // UNIFIED队列指标
        Map<String, Object> unifiedMetrics = getUnifiedQueueMetrics();
        metrics.put("UNIFIED", unifiedMetrics);
        
        // STREAM_UNIFIED队列指标
        Map<String, Object> streamMetrics = getStreamUnifiedQueueMetrics();
        metrics.put("STREAM_UNIFIED", streamMetrics);
        
        // 总体统计
        long totalUnifiedMessages = (Long) unifiedMetrics.get("totalMessages");
        long totalStreamMessages = (Long) streamMetrics.get("totalMessages");
        
        metrics.put("summary", Map.of(
            "totalUnifiedMessages", totalUnifiedMessages,
            "totalStreamMessages", totalStreamMessages,
            "grandTotal", totalUnifiedMessages + totalStreamMessages,
            "lastUpdateTime", System.currentTimeMillis()
        ));
        
        return metrics;
    }
    
    /**
     * 获取UNIFIED队列指标
     * @return UNIFIED队列指标数据
     */
    public Map<String, Object> getUnifiedQueueMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // 获取所有UNIFIED队列的键
            Set<String> queueKeys = getUnifiedQueueKeys();
            
            long totalInstantMessages = 0;
            long totalDelayMessages = 0;
            int activeQueues = 0;
            
            Map<String, Map<String, Long>> queueDetails = new HashMap<>();
            
            for (String queueKey : queueKeys) {
                LQUnifiedQueue<?> queue = getOrCreateUnifiedQueue(queueKey);
                
                long instantCount = queue.getInstantMessageCount();
                long delayCount = queue.getDelayMessageCount();
                long totalCount = instantCount + delayCount;
                
                if (totalCount > 0) {
                    activeQueues++;
                }
                
                totalInstantMessages += instantCount;
                totalDelayMessages += delayCount;
                
                queueDetails.put(queueKey, Map.of(
                    "instantMessages", instantCount,
                    "delayMessages", delayCount,
                    "totalMessages", totalCount
                ));
            }
            
            metrics.put("type", "UNIFIED");
            metrics.put("description", "瞬时消息队列");
            metrics.put("totalQueues", queueKeys.size());
            metrics.put("activeQueues", activeQueues);
            metrics.put("instantMessages", totalInstantMessages);
            metrics.put("delayMessages", totalDelayMessages);
            metrics.put("totalMessages", totalInstantMessages + totalDelayMessages);
            metrics.put("queueDetails", queueDetails);
            
        } catch (Exception e) {
            log.error("获取UNIFIED队列指标失败", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * 获取STREAM_UNIFIED队列指标
     * @return STREAM_UNIFIED队列指标数据
     */
    public Map<String, Object> getStreamUnifiedQueueMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // 获取所有STREAM_UNIFIED队列的键
            Set<String> queueKeys = getStreamUnifiedQueueKeys();
            
            long totalStreamMessages = 0;
            long totalDelayMessages = 0;
            long totalPendingMessages = 0;
            int activeQueues = 0;
            
            Map<String, Map<String, Long>> queueDetails = new HashMap<>();
            
            for (String queueKey : queueKeys) {
                LQStreamUnifiedQueue<?> queue = getOrCreateStreamQueue(queueKey);
                
                long streamCount = queue.getStreamMessageCount();
                long delayCount = queue.getDelayMessageCount();
                long pendingCount = queue.getPendingMessageCount();
                long totalCount = streamCount + delayCount;
                
                if (totalCount > 0) {
                    activeQueues++;
                }
                
                totalStreamMessages += streamCount;
                totalDelayMessages += delayCount;
                totalPendingMessages += pendingCount;
                
                queueDetails.put(queueKey, Map.of(
                    "streamMessages", streamCount,
                    "delayMessages", delayCount,
                    "pendingMessages", pendingCount,
                    "totalMessages", totalCount
                ));
            }
            
            metrics.put("type", "STREAM_UNIFIED");
            metrics.put("description", "流消息队列");
            metrics.put("totalQueues", queueKeys.size());
            metrics.put("activeQueues", activeQueues);
            metrics.put("streamMessages", totalStreamMessages);
            metrics.put("delayMessages", totalDelayMessages);
            metrics.put("pendingMessages", totalPendingMessages);
            metrics.put("totalMessages", totalStreamMessages + totalDelayMessages);
            metrics.put("queueDetails", queueDetails);
            
        } catch (Exception e) {
            log.error("获取STREAM_UNIFIED队列指标失败", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * 获取所有UNIFIED队列的键
     */
    private Set<String> getUnifiedQueueKeys() {
        return (Set<String>) LingQueRedis.ofKey("lq",1L).execBase((commands) -> {
            // 查找所有以":instant"结尾的键，提取队列名
            List<String> keys = commands.keys("*:instant");
            return keys.stream()
                .map(key -> key.substring(0, key.lastIndexOf(":instant")))
                .collect(java.util.stream.Collectors.toSet());
        });
    }
    
    /**
     * 获取所有STREAM_UNIFIED队列的键
     */
    private Set<String> getStreamUnifiedQueueKeys() {
        return (Set<String>) LingQueRedis.ofKey("lq",1L).execBase((commands) -> {
            // 查找所有以":stream"结尾的键，提取队列名
            List<String> keys = commands.keys("*:stream");
            return keys.stream()
                .map(key -> key.substring(0, key.lastIndexOf(":stream")))
                .collect(java.util.stream.Collectors.toSet());
        });
    }
    
    /**
     * 获取或创建UNIFIED队列实例
     */
    private LQUnifiedQueue<?> getOrCreateUnifiedQueue(String queueKey) {
        return unifiedQueues.computeIfAbsent(queueKey, key -> {
            LingQueRedis queueRedis = LingQueRedis.ofKey(key, 1L);
            return new LQUnifiedQueue<>(queueRedis);
        });
    }
    
    /**
     * 获取或创建STREAM_UNIFIED队列实例
     */
    private LQStreamUnifiedQueue<?> getOrCreateStreamQueue(String queueKey) {
        return streamQueues.computeIfAbsent(queueKey, key -> {
            LingQueRedis queueRedis = LingQueRedis.ofKey(key, 1L);
            return new LQStreamUnifiedQueue<>(queueRedis);
        });
    }
}