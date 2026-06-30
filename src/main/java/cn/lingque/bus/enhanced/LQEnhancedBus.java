package cn.lingque.bus.enhanced;

import cn.lingque.base.LQKey;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.thread.LQThreadUtil;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * LQ增强版消息总线
 * 基于注册中心实现的高性能分布式消息总线，支持服务组和主题订阅
 * 比原版LQBus更强大、更易用、性能更突出
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQEnhancedBus {
    
    /** 消息通道前缀 */
    private static final String BUS_CHANNEL_PREFIX = "LQ:ENHANCED:BUS:";
    
    /** 主题订阅映射 */
    private static final String TOPIC_SUBSCRIPTION_KEY = "LQ:ENHANCED:BUS:SUBSCRIPTIONS";
    
    /** 服务组映射 */
    private static final String SERVICE_GROUP_KEY = "LQ:ENHANCED:BUS:SERVICE_GROUPS";
    
    /** 消息统计 */
    private static final String MESSAGE_STATS_KEY = "LQ:ENHANCED:BUS:STATS";
    
    /** 初始化标识 */
    private static final AtomicInteger isInit = new AtomicInteger(0);
    
    /** 本地订阅者映射: topic -> List<BusSubscriber> */
    private static final Map<String, List<BusSubscriber>> localSubscribers = new ConcurrentHashMap<>();
    
    /** 服务组订阅者映射: serviceGroup -> Set<topic> */
    private static final Map<String, Set<String>> serviceGroupTopics = new ConcurrentHashMap<>();
    
    /** 消息统计 */
    private static final AtomicLong sentMessages = new AtomicLong(0);
    private static final AtomicLong receivedMessages = new AtomicLong(0);
    private static final AtomicLong failedMessages = new AtomicLong(0);
    
    /** 当前节点信息 */
    private static LQEnhancedNodeInfo currentNode;
    
    /**
     * 启动增强版消息总线
     * @param nodeInfo 当前节点信息
     */
    public static void start(LQEnhancedNodeInfo nodeInfo) {
        if (isInit.compareAndSet(0, 1)) {
            currentNode = nodeInfo;
            
            log.info("启动LQ增强版消息总线: {}", nodeInfo.getServerName());
            
            // 启动消息消费线程
            startMessageConsumer();
            
            // 启动健康检查线程
            startHealthChecker();
            
            // 注册关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(LQEnhancedBus::shutdown));
            
            log.info("LQ增强版消息总线启动完成");
        }
    }
    
    /**
     * 订阅主题
     * @param topic 主题名称
     * @param serviceGroup 服务组（可选，为null表示全局订阅）
     * @param subscriber 订阅者
     */
    public static void subscribe(String topic, String serviceGroup, BusSubscriber subscriber) {
        // 本地订阅
        localSubscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(subscriber);
        
        // 服务组订阅
        if (serviceGroup != null) {
            serviceGroupTopics.computeIfAbsent(serviceGroup, k -> new HashSet<>()).add(topic);
        }
        
        // 注册到Redis
        registerSubscription(topic, serviceGroup);
        
        log.info("订阅主题成功: topic={}, serviceGroup={}, subscriber={}", 
                topic, serviceGroup, subscriber.getClass().getSimpleName());
    }
    
    /**
     * 取消订阅
     * @param topic 主题名称
     * @param serviceGroup 服务组
     * @param subscriber 订阅者
     */
    public static void unsubscribe(String topic, String serviceGroup, BusSubscriber subscriber) {
        List<BusSubscriber> subscribers = localSubscribers.get(topic);
        if (subscribers != null) {
            subscribers.remove(subscriber);
            if (subscribers.isEmpty()) {
                localSubscribers.remove(topic);
            }
        }
        
        if (serviceGroup != null) {
            Set<String> topics = serviceGroupTopics.get(serviceGroup);
            if (topics != null) {
                topics.remove(topic);
                if (topics.isEmpty()) {
                    serviceGroupTopics.remove(serviceGroup);
                }
            }
        }
        
        // 从Redis注销
        unregisterSubscription(topic, serviceGroup);
        
        log.info("取消订阅成功: topic={}, serviceGroup={}", topic, serviceGroup);
    }
    
    /**
     * 发布消息到指定主题
     * @param topic 主题名称
     * @param message 消息内容
     */
    public static void publish(String topic, Object message) {
        publish(topic, null, message, false);
    }
    
    /**
     * 发布消息到指定服务组的主题
     * @param topic 主题名称
     * @param serviceGroup 目标服务组
     * @param message 消息内容
     */
    public static void publishToGroup(String topic, String serviceGroup, Object message) {
        publish(topic, serviceGroup, message, false);
    }
    
    /**
     * 发布消息（包含自己）
     * @param topic 主题名称
     * @param serviceGroup 服务组（可选）
     * @param message 消息内容
     * @param includeSelf 是否包含自己
     */
    public static void publish(String topic, String serviceGroup, Object message, boolean includeSelf) {
        try {
            EnhancedBusMessage busMessage = new EnhancedBusMessage()
                    .setTopic(topic)
                    .setServiceGroup(serviceGroup)
                    .setMessage(LQUtil.toJson(message))
                    .setMessageId(generateMessageId())
                    .setTimestamp(System.currentTimeMillis())
                    .setSenderNode(currentNode.getServerName())
                    .setMessageType(message.getClass().getName());
            
            // 获取目标节点
            Set<LQEnhancedNodeInfo> targetNodes = getTargetNodes(topic, serviceGroup, includeSelf);
            
            if (targetNodes.isEmpty()) {
                log.warn("没有找到订阅主题的节点: topic={}, serviceGroup={}", topic, serviceGroup);
                return;
            }
            
            // 分发消息到目标节点
            for (LQEnhancedNodeInfo node : targetNodes) {
                String channelKey = BUS_CHANNEL_PREFIX + node.getServerName();
                LQKey.key(channelKey, 1D, LQKey.TEN_MINUTE).ofS().addMember(busMessage);
            }
            
            sentMessages.incrementAndGet();
            updateMessageStats("sent", 1);
            
            log.debug("消息发布成功: topic={}, serviceGroup={}, targets={}, messageId={}", 
                    topic, serviceGroup, targetNodes.size(), busMessage.getMessageId());
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            updateMessageStats("failed", 1);
            log.error("消息发布失败: topic={}, serviceGroup={}", topic, serviceGroup, e);
        }
    }
    
    /**
     * 广播消息到所有节点
     * @param topic 主题名称
     * @param message 消息内容
     * @param includeSelf 是否包含自己
     */
    public static void broadcast(String topic, Object message, boolean includeSelf) {
        Set<LQEnhancedNodeInfo> allNodes = new HashSet<>(LQEnhancedRegisterCenter.getAllEnhancedNodeList());
        
        if (!includeSelf && currentNode != null) {
            allNodes = allNodes.stream()
                    .filter(node -> !node.getServerName().equals(currentNode.getServerName()))
                    .collect(Collectors.toSet());
        }
        
        try {
            EnhancedBusMessage busMessage = new EnhancedBusMessage()
                    .setTopic(topic)
                    .setMessage(LQUtil.toJson(message))
                    .setMessageId(generateMessageId())
                    .setTimestamp(System.currentTimeMillis())
                    .setSenderNode(currentNode.getServerName())
                    .setMessageType(message.getClass().getName())
                    .setBroadcast(true);
            
            for (LQEnhancedNodeInfo node : allNodes) {
                String channelKey = BUS_CHANNEL_PREFIX + node.getServerName();
                LQKey.key(channelKey, 1D, LQKey.TEN_MINUTE).ofS().addMember(busMessage);
            }
            
            sentMessages.addAndGet(allNodes.size());
            updateMessageStats("broadcast", allNodes.size());
            
            log.info("广播消息成功: topic={}, targets={}, messageId={}", 
                    topic, allNodes.size(), busMessage.getMessageId());
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            updateMessageStats("failed", 1);
            log.error("广播消息失败: topic={}", topic, e);
        }
    }
    
    /**
     * 获取消息统计信息
     */
    public static Map<String, Object> getMessageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sentMessages", sentMessages.get());
        stats.put("receivedMessages", receivedMessages.get());
        stats.put("failedMessages", failedMessages.get());
        stats.put("localSubscribers", localSubscribers.size());
        stats.put("serviceGroups", serviceGroupTopics.size());
        stats.put("currentNode", currentNode != null ? currentNode.getServerName() : "unknown");
        return stats;
    }
    
    /**
     * 获取订阅信息
     */
    public static Map<String, Object> getSubscriptionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("localSubscribers", localSubscribers.keySet());
        info.put("serviceGroupTopics", serviceGroupTopics);
        return info;
    }
    
    /**
     * 启动消息消费线程
     */
    private static void startMessageConsumer() {
        LQThreadUtil.execSlave(() -> {
            String channelKey = BUS_CHANNEL_PREFIX + currentNode.getServerName();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Set<EnhancedBusMessage> messages = LQKey.key(channelKey, 1D, LQKey.TEN_MINUTE)
                            .ofS().popMembers(10, EnhancedBusMessage.class);
                    
                    if (!messages.isEmpty()) {
                        for (EnhancedBusMessage message : messages) {
                            processMessage(message);
                        }
                    } else {
                        Thread.sleep(100); // 没有消息时短暂休眠
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("消息消费异常", e);
                    TryCatch.trying(() -> Thread.sleep(1000)); // 异常时休眠1秒
                }
            }
        });
    }
    
    /**
     * 处理接收到的消息
     */
    private static void processMessage(EnhancedBusMessage message) {
        try {
            String topic = message.getTopic();
            List<BusSubscriber> subscribers = localSubscribers.get(topic);
            
            if (subscribers == null || subscribers.isEmpty()) {
                log.debug("没有找到主题的订阅者: {}", topic);
                return;
            }
            
            receivedMessages.incrementAndGet();
            updateMessageStats("received", 1);
            
            // 异步处理每个订阅者
            for (BusSubscriber subscriber : subscribers) {
                LQThreadUtil.execSlave(() -> {
                    try {
                        Object messageObj = LQUtil.isBasClass(subscriber.getMessageClass()) 
                                ? LQUtil.baseClassTran(message.getMessage(), subscriber.getMessageClass())
                                : LQUtil.jsonToBean(message.getMessage(), subscriber.getMessageClass());
                        
                        subscriber.onMessage(messageObj, message);
                        
                    } catch (Exception e) {
                        log.error("处理消息失败: topic={}, messageId={}, subscriber={}", 
                                topic, message.getMessageId(), subscriber.getClass().getSimpleName(), e);
                        failedMessages.incrementAndGet();
                        updateMessageStats("failed", 1);
                    }
                });
            }
            
            log.debug("消息处理完成: topic={}, messageId={}, subscribers={}", 
                    topic, message.getMessageId(), subscribers.size());
            
        } catch (Exception e) {
            log.error("消息处理异常: messageId={}", message.getMessageId(), e);
            failedMessages.incrementAndGet();
            updateMessageStats("failed", 1);
        }
    }
    
    /**
     * 启动健康检查线程
     */
    private static void startHealthChecker() {
        LQThreadUtil.execSlave(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 清理过期的订阅信息
                    cleanupExpiredSubscriptions();
                    
                    // 更新节点健康状态
                    updateNodeHealth();
                    
                    Thread.sleep(30000); // 30秒检查一次
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("健康检查异常", e);
                }
            }
        });
    }
    
    /**
     * 获取目标节点
     */
    private static Set<LQEnhancedNodeInfo> getTargetNodes(String topic, String serviceGroup, boolean includeSelf) {
        Set<LQEnhancedNodeInfo> targetNodes = new HashSet<>();
        
        if (serviceGroup != null) {
            // 根据服务组获取节点 - 暂时使用服务名获取
            targetNodes = new HashSet<>(LQEnhancedRegisterCenter.getEnhancedNodeList(serviceGroup));
        } else {
            // 获取所有订阅了该主题的节点
            targetNodes = getSubscribedNodes(topic);
        }
        
        if (!includeSelf && currentNode != null) {
            targetNodes = targetNodes.stream()
                    .filter(node -> !node.getServerName().equals(currentNode.getServerName()))
                    .collect(Collectors.toSet());
        }
        
        return targetNodes;
    }
    
    /**
     * 获取订阅了指定主题的节点
     */
    private static Set<LQEnhancedNodeInfo> getSubscribedNodes(String topic) {
        // 这里可以从Redis中获取订阅信息，暂时返回所有在线节点
        return new HashSet<>(LQEnhancedRegisterCenter.getAllEnhancedNodeList());
    }
    
    /**
     * 注册订阅信息到Redis
     */
    private static void registerSubscription(String topic, String serviceGroup) {
        try {
            String subscriptionKey = TOPIC_SUBSCRIPTION_KEY + ":" + topic;
            String nodeInfo = currentNode.getServerName() + (serviceGroup != null ? ":" + serviceGroup : "");
            LQKey.key(subscriptionKey, 1D, LQKey.ONE_HOUR).ofS().addMember(nodeInfo);
        } catch (Exception e) {
            log.error("注册订阅信息失败: topic={}, serviceGroup={}", topic, serviceGroup, e);
        }
    }
    
    /**
     * 注销订阅信息
     */
    private static void unregisterSubscription(String topic, String serviceGroup) {
        try {
            String subscriptionKey = TOPIC_SUBSCRIPTION_KEY + ":" + topic;
            String nodeInfo = currentNode.getServerName() + (serviceGroup != null ? ":" + serviceGroup : "");
            LQKey.key(subscriptionKey, 1D, LQKey.ONE_HOUR).ofS().deleteMember(nodeInfo);
        } catch (Exception e) {
            log.error("注销订阅信息失败: topic={}, serviceGroup={}", topic, serviceGroup, e);
        }
    }
    
    /**
     * 更新消息统计
     */
    private static void updateMessageStats(String type, long count) {
        try {
            String statsKey = MESSAGE_STATS_KEY + ":" + currentNode.getServerName();
            LQKey.key(statsKey, 1D, LQKey.ONE_DAY).ofH().set(statsKey, type);
        } catch (Exception e) {
            log.debug("更新统计信息失败: type={}, count={}", type, count, e);
        }
    }
    
    /**
     * 清理过期的订阅信息
     */
    private static void cleanupExpiredSubscriptions() {
        // 实现清理逻辑
    }
    
    /**
     * 更新节点健康状态
     */
    private static void updateNodeHealth() {
        if (currentNode != null) {
            currentNode.setLastHeartbeat(System.currentTimeMillis());
        }
    }
    
    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return System.currentTimeMillis() + "_" + Thread.currentThread().getId() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * 关闭消息总线
     */
    public static void shutdown() {
        log.info("关闭LQ增强版消息总线");
        // 清理资源
        localSubscribers.clear();
        serviceGroupTopics.clear();
        isInit.set(0);
    }
}