package cn.lingque.bus.enhanced.example;

import cn.lingque.bus.enhanced.EnhancedBusMessage;
import cn.lingque.bus.enhanced.LQEnhancedBus;
import cn.lingque.bus.enhanced.annotation.LQEnhancedBusListener;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 增强版消息总线使用示例
 * 展示如何使用LQEnhancedBus进行消息发布和订阅
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Service
public class LQEnhancedBusExample {
    
    /**
     * 运行增强版消息总线示例
     */
    public static void runExample() {
        log.info("=== LQ增强版消息总线示例 ===");
        
        try {
            // 1. 启动增强版注册中心
            startEnhancedRegisterCenter();
            
            // 2. 启动增强版消息总线
            startEnhancedBus();
            
            // 3. 演示消息发布和订阅
            demonstrateMessaging();
            
            // 4. 演示服务组消息
            demonstrateServiceGroupMessaging();
            
            // 5. 演示广播消息
            demonstrateBroadcastMessaging();
            
            // 6. 显示统计信息
            showStatistics();
            
        } catch (Exception e) {
            log.error("增强版消息总线示例运行失败", e);
        }
    }
    
    /**
     * 启动增强版注册中心
     */
    private static void startEnhancedRegisterCenter() {
        log.info("=== 启动增强版注册中心 ===");
        
        // 注册多个服务节点
        LQEnhancedNodeInfo userService = new LQEnhancedNodeInfo()
                .setServerName("user-service")
                .setNodeIp("192.168.1.100")
                .setNodePort(8080)
                .setProtocol("HTTP")
                .setWeight(100)
                .setVersion("1.0.0")
                .addTag("user")
                .addTag("api")
                .addMetadata("region", "us-west")
                .addMetadata("zone", "zone-a");
        
        LQEnhancedNodeInfo orderService = new LQEnhancedNodeInfo()
                .setServerName("order-service")
                .setNodeIp("192.168.1.101")
                .setNodePort(8081)
                .setProtocol("HTTP")
                .setWeight(150)
                .setVersion("1.0.0")
                .addTag("order")
                .addTag("api")
                .addMetadata("region", "us-west")
                .addMetadata("zone", "zone-b");
        
        LQEnhancedNodeInfo notificationService = new LQEnhancedNodeInfo()
                .setServerName("notification-service")
                .setNodeIp("192.168.1.102")
                .setNodePort(8082)
                .setProtocol("HTTP")
                .setWeight(80)
                .setVersion("1.0.0")
                .addTag("notification")
                .addTag("message")
                .addMetadata("region", "us-east")
                .addMetadata("zone", "zone-a");
        
        // 注册服务节点
        LQEnhancedRegisterCenter.registerEnhancedNode(userService);
        LQEnhancedRegisterCenter.registerEnhancedNode(orderService);
        LQEnhancedRegisterCenter.registerEnhancedNode(notificationService);
        
        log.info("增强版注册中心启动完成，已注册 {} 个服务节点", 
                LQEnhancedRegisterCenter.currentEnhancedNodes.size());
    }
    
    /**
     * 启动增强版消息总线
     */
    private static void startEnhancedBus() {
        log.info("=== 启动增强版消息总线 ===");
        
        // 获取当前节点信息
        LQEnhancedNodeInfo currentNode = LQEnhancedRegisterCenter.currentEnhancedNodes.iterator().next();
        
        // 启动消息总线
        LQEnhancedBus.start(currentNode);
        
        log.info("增强版消息总线启动完成: {}", currentNode.getServerName());
    }
    
    /**
     * 演示消息发布和订阅
     */
    private static void demonstrateMessaging() {
        log.info("=== 演示基础消息发布和订阅 ===");
        
        // 发布用户创建事件
        UserCreatedEvent userEvent = new UserCreatedEvent()
                .setUserId(12345L)
                .setUsername("john_doe")
                .setEmail("john@example.com")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.publish("user.created", userEvent);
        log.info("发布用户创建事件: {}", userEvent);
        
        // 发布订单创建事件
        OrderCreatedEvent orderEvent = new OrderCreatedEvent()
                .setOrderId("ORDER-001")
                .setUserId(12345L)
                .setAmount(99.99)
                .setStatus("PENDING")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.publish("order.created", orderEvent);
        log.info("发布订单创建事件: {}", orderEvent);
        
        // 发布系统通知
        SystemNotification notification = new SystemNotification()
                .setType("INFO")
                .setTitle("系统维护通知")
                .setContent("系统将于今晚22:00-24:00进行维护")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.publish("system.notification", notification);
        log.info("发布系统通知: {}", notification);
    }
    
    /**
     * 演示服务组消息
     */
    private static void demonstrateServiceGroupMessaging() {
        log.info("=== 演示服务组消息 ===");
        
        // 发送到用户服务组
        UserUpdateEvent updateEvent = new UserUpdateEvent()
                .setUserId(12345L)
                .setField("email")
                .setOldValue("john@example.com")
                .setNewValue("john.doe@example.com")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.publishToGroup("user.updated", "user-service", updateEvent);
        log.info("发送用户更新事件到用户服务组: {}", updateEvent);
        
        // 发送到订单服务组
        OrderStatusChangedEvent statusEvent = new OrderStatusChangedEvent()
                .setOrderId("ORDER-001")
                .setOldStatus("PENDING")
                .setNewStatus("CONFIRMED")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.publishToGroup("order.status.changed", "order-service", statusEvent);
        log.info("发送订单状态变更事件到订单服务组: {}", statusEvent);
    }
    
    /**
     * 演示广播消息
     */
    private static void demonstrateBroadcastMessaging() {
        log.info("=== 演示广播消息 ===");
        
        // 广播系统配置更新
        ConfigUpdateEvent configEvent = new ConfigUpdateEvent()
                .setConfigKey("system.maintenance.enabled")
                .setOldValue("false")
                .setNewValue("true")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.broadcast("config.updated", configEvent, false);
        log.info("广播配置更新事件: {}", configEvent);
        
        // 广播紧急通知
        EmergencyAlert alert = new EmergencyAlert()
                .setLevel("HIGH")
                .setMessage("检测到异常流量，请立即检查系统状态")
                .setSource("monitoring-system")
                .setTimestamp(System.currentTimeMillis());
        
        LQEnhancedBus.broadcast("emergency.alert", alert, true);
        log.info("广播紧急警报: {}", alert);
    }
    
    /**
     * 显示统计信息
     */
    private static void showStatistics() {
        log.info("=== 消息总线统计信息 ===");
        
        Map<String, Object> stats = LQEnhancedBus.getMessageStats();
        log.info("发送消息数: {}", stats.get("sentMessages"));
        log.info("接收消息数: {}", stats.get("receivedMessages"));
        log.info("失败消息数: {}", stats.get("failedMessages"));
        log.info("本地订阅者数: {}", stats.get("localSubscribers"));
        log.info("服务组数: {}", stats.get("serviceGroups"));
        log.info("当前节点: {}", stats.get("currentNode"));
        
        Map<String, Object> subscriptionInfo = LQEnhancedBus.getSubscriptionInfo();
        log.info("订阅信息: {}", subscriptionInfo);
    }
    
    // ========================= 注解式订阅者示例 =========================
    
    /**
     * 用户创建事件监听器
     */
    @LQEnhancedBusListener(
            topic = "user.created",
            name = "userCreatedHandler",
            async = true,
            priority = 1
    )
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("处理用户创建事件: userId={}, username={}", event.getUserId(), event.getUsername());
        
        // 模拟业务处理
        try {
            Thread.sleep(100);
            log.info("用户创建事件处理完成: {}", event.getUserId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 订单创建事件监听器（带完整消息信息）
     */
    @LQEnhancedBusListener(
            topic = "order.created",
            serviceGroup = "order-service",
            name = "orderCreatedHandler",
            async = true,
            priority = 2,
            condition = "#message.amount > 50"
    )
    public void handleOrderCreated(OrderCreatedEvent event, EnhancedBusMessage busMessage) {
        log.info("处理订单创建事件: orderId={}, amount={}, messageId={}", 
                event.getOrderId(), event.getAmount(), busMessage.getMessageId());
        
        // 模拟业务处理
        log.info("订单创建事件处理完成: {}", event.getOrderId());
    }
    
    /**
     * 系统通知监听器（带重试机制）
     */
    @LQEnhancedBusListener(
            topic = "system.notification",
            name = "systemNotificationHandler",
            async = false,
            errorStrategy = LQEnhancedBusListener.ErrorHandleStrategy.RETRY,
            maxRetries = 3,
            retryInterval = 2000
    )
    public void handleSystemNotification(SystemNotification notification) {
        log.info("处理系统通知: type={}, title={}", notification.getType(), notification.getTitle());
        
        // 模拟可能失败的处理
        if (Math.random() < 0.3) {
            throw new RuntimeException("模拟处理失败");
        }
        
        log.info("系统通知处理完成: {}", notification.getTitle());
    }
    
    /**
     * 配置更新事件监听器
     */
    @LQEnhancedBusListener(
            topic = "config.updated",
            name = "configUpdateHandler",
            async = true,
            condition = "#message.configKey.startsWith('system.')"
    )
    public void handleConfigUpdate(ConfigUpdateEvent event) {
        log.info("处理配置更新事件: key={}, oldValue={}, newValue={}", 
                event.getConfigKey(), event.getOldValue(), event.getNewValue());
        
        // 模拟配置重载
        log.info("配置更新处理完成: {}", event.getConfigKey());
    }
    
    /**
     * 紧急警报监听器
     */
    @LQEnhancedBusListener(
            topic = "emergency.alert",
            name = "emergencyAlertHandler",
            async = false,
            priority = 0,
            errorStrategy = LQEnhancedBusListener.ErrorHandleStrategy.LOG
    )
    public void handleEmergencyAlert(EmergencyAlert alert) {
        log.warn("收到紧急警报: level={}, message={}, source={}", 
                alert.getLevel(), alert.getMessage(), alert.getSource());
        
        // 模拟紧急处理
        log.warn("紧急警报处理完成: {}", alert.getMessage());
    }
    
    // ========================= 事件类定义 =========================
    
    @Data
    @Accessors(chain = true)
    public static class UserCreatedEvent {
        private Long userId;
        private String username;
        private String email;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class OrderCreatedEvent {
        private String orderId;
        private Long userId;
        private Double amount;
        private String status;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class SystemNotification {
        private String type;
        private String title;
        private String content;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class UserUpdateEvent {
        private Long userId;
        private String field;
        private String oldValue;
        private String newValue;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class OrderStatusChangedEvent {
        private String orderId;
        private String oldStatus;
        private String newStatus;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class ConfigUpdateEvent {
        private String configKey;
        private String oldValue;
        private String newValue;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class EmergencyAlert {
        private String level;
        private String message;
        private String source;
        private Long timestamp;
    }
}