package cn.lingque.cloud.rpc.loadbalance;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LQ负载均衡器
 * 支持多种负载均衡策略
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQLoadBalancer {
    
    /** 轮询计数器 */
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    
    /** 加权轮询计数器 */
    private final Map<String, WeightedRoundRobinState> weightedRoundRobinStates = new ConcurrentHashMap<>();
    
    /** 连接数统计 */
    private final Map<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    
    /** 一致性哈希环 */
    private final Map<String, TreeMap<Long, LQEnhancedNodeInfo>> consistentHashRings = new ConcurrentHashMap<>();
    
    /** 虚拟节点数量 */
    private static final int VIRTUAL_NODES = 160;
    
    /**
     * 根据策略选择节点
     */
    public LQEnhancedNodeInfo select(List<LQEnhancedNodeInfo> nodes, 
                                    LQEnhancedRegisterCenter.LoadBalanceStrategy strategy) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        
        switch (strategy) {
            case ROUND_ROBIN:
                return selectByRoundRobin(nodes);
            case WEIGHTED_ROUND_ROBIN:
                return selectByWeightedRoundRobin(nodes);
            case RANDOM:
                return selectByRandom(nodes);
            case LEAST_CONNECTIONS:
                return selectByLeastConnections(nodes);
            case CONSISTENT_HASH:
                return selectByConsistentHash(nodes, null);
            default:
                return selectByRoundRobin(nodes);
        }
    }
    
    /**
     * 一致性哈希选择（带哈希键）
     */
    public LQEnhancedNodeInfo selectByConsistentHash(List<LQEnhancedNodeInfo> nodes, String hashKey) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        
        String serviceKey = getServiceKey(nodes);
        TreeMap<Long, LQEnhancedNodeInfo> ring = consistentHashRings.computeIfAbsent(serviceKey, 
                k -> buildConsistentHashRing(nodes));
        
        // 如果节点列表发生变化，重建哈希环
        if (ring.size() != nodes.size() * VIRTUAL_NODES) {
            ring = buildConsistentHashRing(nodes);
            consistentHashRings.put(serviceKey, ring);
        }
        
        if (hashKey == null) {
            hashKey = String.valueOf(System.currentTimeMillis());
        }
        
        long hash = hash(hashKey);
        Map.Entry<Long, LQEnhancedNodeInfo> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    /**
     * 轮询选择
     */
    private LQEnhancedNodeInfo selectByRoundRobin(List<LQEnhancedNodeInfo> nodes) {
        String serviceKey = getServiceKey(nodes);
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(serviceKey, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement()) % nodes.size();
        return nodes.get(index);
    }
    
    /**
     * 加权轮询选择
     */
    private LQEnhancedNodeInfo selectByWeightedRoundRobin(List<LQEnhancedNodeInfo> nodes) {
        String serviceKey = getServiceKey(nodes);
        WeightedRoundRobinState state = weightedRoundRobinStates.computeIfAbsent(serviceKey, 
                k -> new WeightedRoundRobinState(nodes));
        
        // 检查节点是否发生变化
        if (state.needsUpdate(nodes)) {
            state = new WeightedRoundRobinState(nodes);
            weightedRoundRobinStates.put(serviceKey, state);
        }
        
        return state.select();
    }
    
    /**
     * 随机选择
     */
    private LQEnhancedNodeInfo selectByRandom(List<LQEnhancedNodeInfo> nodes) {
        int index = ThreadLocalRandom.current().nextInt(nodes.size());
        return nodes.get(index);
    }
    
    /**
     * 最少连接选择
     */
    private LQEnhancedNodeInfo selectByLeastConnections(List<LQEnhancedNodeInfo> nodes) {
        LQEnhancedNodeInfo selected = null;
        int minConnections = Integer.MAX_VALUE;
        
        for (LQEnhancedNodeInfo node : nodes) {
            String nodeKey = getNodeKey(node);
            int connections = connectionCounts.computeIfAbsent(nodeKey, k -> new AtomicInteger(0)).get();
            if (connections < minConnections) {
                minConnections = connections;
                selected = node;
            }
        }
        
        return selected;
    }
    
    /**
     * 增加连接数
     */
    public void incrementConnection(LQEnhancedNodeInfo node) {
        String nodeKey = getNodeKey(node);
        connectionCounts.computeIfAbsent(nodeKey, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * 减少连接数
     */
    public void decrementConnection(LQEnhancedNodeInfo node) {
        String nodeKey = getNodeKey(node);
        AtomicInteger counter = connectionCounts.get(nodeKey);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
    
    /**
     * 构建一致性哈希环
     */
    private TreeMap<Long, LQEnhancedNodeInfo> buildConsistentHashRing(List<LQEnhancedNodeInfo> nodes) {
        TreeMap<Long, LQEnhancedNodeInfo> ring = new TreeMap<>();
        
        for (LQEnhancedNodeInfo node : nodes) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String virtualNodeKey = getNodeKey(node) + "#" + i;
                long hash = hash(virtualNodeKey);
                ring.put(hash, node);
            }
        }
        
        return ring;
    }
    
    /**
     * 哈希函数
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long hash = 0;
            for (int i = 0; i < 4; i++) {
                hash <<= 8;
                hash |= ((int) digest[i]) & 0xFF;
            }
            return hash;
        } catch (Exception e) {
            return key.hashCode();
        }
    }
    
    /**
     * 获取服务键
     */
    private String getServiceKey(List<LQEnhancedNodeInfo> nodes) {
        if (nodes.isEmpty()) {
            return "unknown";
        }
        return nodes.get(0).getServerName();
    }
    
    /**
     * 获取节点键
     */
    private String getNodeKey(LQEnhancedNodeInfo node) {
        return node.getServerName() + ":" + node.getNodeIp() + ":" + node.getNodePort();
    }
    
    /**
     * 加权轮询状态
     */
    private static class WeightedRoundRobinState {
        private final List<WeightedNode> weightedNodes;
        private final int totalWeight;
        
        public WeightedRoundRobinState(List<LQEnhancedNodeInfo> nodes) {
            this.weightedNodes = new ArrayList<>();
            int total = 0;
            
            for (LQEnhancedNodeInfo node : nodes) {
                int weight = node.getWeight() != null ? node.getWeight() : 100;
                this.weightedNodes.add(new WeightedNode(node, weight));
                total += weight;
            }
            
            this.totalWeight = total;
        }
        
        public LQEnhancedNodeInfo select() {
            if (weightedNodes.isEmpty()) {
                return null;
            }
            
            // 增加当前权重
            for (WeightedNode weightedNode : weightedNodes) {
                weightedNode.currentWeight += weightedNode.weight;
            }
            
            // 选择当前权重最大的节点
            WeightedNode selected = weightedNodes.stream()
                    .max(Comparator.comparingInt(wn -> wn.currentWeight))
                    .orElse(weightedNodes.get(0));
            
            // 减少选中节点的当前权重
            selected.currentWeight -= totalWeight;
            
            return selected.node;
        }
        
        public boolean needsUpdate(List<LQEnhancedNodeInfo> nodes) {
            return weightedNodes.size() != nodes.size();
        }
        
        private static class WeightedNode {
            final LQEnhancedNodeInfo node;
            final int weight;
            int currentWeight;
            
            WeightedNode(LQEnhancedNodeInfo node, int weight) {
                this.node = node;
                this.weight = weight;
                this.currentWeight = 0;
            }
        }
    }
    
    /**
     * 获取负载均衡统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("roundRobinCounters", new HashMap<>(roundRobinCounters));
        stats.put("connectionCounts", new HashMap<>(connectionCounts));
        stats.put("consistentHashRings", consistentHashRings.keySet());
        return stats;
    }
}