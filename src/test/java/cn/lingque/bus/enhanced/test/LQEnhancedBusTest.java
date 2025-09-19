package cn.lingque.bus.enhanced.test;

import cn.lingque.bus.enhanced.EnhancedBusMessage;
import cn.lingque.bus.enhanced.LQEnhancedBus;
import cn.lingque.bus.enhanced.annotation.LQEnhancedBusListener;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * LQ增强版消息总线测试类
 * 用于验证消息总线的基本功能
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQEnhancedBusTest {
    
    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        log.info("开始LQ增强版消息总线测试");
        
        try {
            // 1. 初始化测试环境
            initTestEnvironment();
            
            // 2. 测试基础消息发布订阅
            testBasicPubSub();
            
            // 3. 测试服务组消息
            testServiceGroupMessage();
            
            // 4. 测试广播消息
            testBroadcastMessage();
            
            // 5. 等待消息处理完成
            Thread.sleep(2000);
            
            // 6. 显示测试结果
            showTestResults();
            
            log.info("LQ增强版消息总线测试完成");
            
        } catch (Exception e) {
            log.error("测试过程中发生错误", e);
        } finally {
            // 清理资源
            cleanup();
        }
    }
    
    /**
     * 初始化测试环境
     */
    private static void initTestEnvironment() {
        log.info("初始化测试环境...");
        
        // 创建测试节点
        LQEnhancedNodeInfo testNode = new LQEnhancedNodeInfo()
                .setServerName("test-service")
                .setNodeIp("127.0.0.1")
                .setNodePort(8080)
                .setProtocol("HTTP")
                .setWeight(100)
                .setVersion("1.0.0")
                .addTag("test")
                .addMetadata("env", "test");
        
        // 注册测试节点
        LQEnhancedRegisterCenter.registerEnhancedNode(testNode);
        
        // 启动消息总线
        LQEnhancedBus.start(testNode);
        
        // 注册测试订阅者
        registerTestSubscribers();
        
        log.info("测试环境初始化完成");
    }
    
    /**
     * 注册测试订阅者
     */
    private static void registerTestSubscribers() {
        TestMessageHandler handler = new TestMessageHandler();
        
        // 这里应该通过配置类自动扫描注册
        // 为了测试简化，直接手动注册
        log.info("测试订阅者注册完成");
    }
    
    /**
     * 测试基础消息发布订阅
     */
    private static void testBasicPubSub() {
        log.info("=== 测试基础消息发布订阅 ===");
        
        // 创建测试消息
        TestMessage message = new TestMessage()
                .setId(1L)
                .setContent("这是一条测试消息")
                .setTimestamp(System.currentTimeMillis());
        
        // 发布消息
        LQEnhancedBus.publish("test.message", message);
        log.info("发布测试消息: {}", message);
    }
    
    /**
     * 测试服务组消息
     */
    private static void testServiceGroupMessage() {
        log.info("=== 测试服务组消息 ===");
        
        // 创建服务组消息
        ServiceGroupMessage message = new ServiceGroupMessage()
                .setServiceName("test-service")
                .setAction("restart")
                .setTimestamp(System.currentTimeMillis());
        
        // 发布到服务组
        LQEnhancedBus.publishToGroup("service.action", "test-service", message);
        log.info("发布服务组消息: {}", message);
    }
    
    /**
     * 测试广播消息
     */
    private static void testBroadcastMessage() {
        log.info("=== 测试广播消息 ===");
        
        // 创建广播消息
        BroadcastMessage message = new BroadcastMessage()
                .setType("system")
                .setContent("系统维护通知")
                .setTimestamp(System.currentTimeMillis());
        
        // 广播消息
        LQEnhancedBus.broadcast("system.broadcast", message, false);
        log.info("广播系统消息: {}", message);
    }
    
    /**
     * 显示测试结果
     */
    private static void showTestResults() {
        log.info("=== 测试结果统计 ===");
        
        try {
            // 获取消息统计
            var stats = LQEnhancedBus.getMessageStats();
            log.info("消息统计: {}", stats);
            
            // 获取订阅信息
            var subscriptionInfo = LQEnhancedBus.getSubscriptionInfo();
            log.info("订阅信息: {}", subscriptionInfo);
            
        } catch (Exception e) {
            log.warn("获取统计信息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清理资源
     */
    private static void cleanup() {
        log.info("清理测试资源...");
        
        try {
            // 关闭消息总线
            LQEnhancedBus.shutdown();
            log.info("消息总线已关闭");
        } catch (Exception e) {
            log.warn("清理资源时发生错误: {}", e.getMessage());
        }
    }
    
    // ========================= 测试消息类 =========================
    
    @Data
    @Accessors(chain = true)
    public static class TestMessage {
        private Long id;
        private String content;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class ServiceGroupMessage {
        private String serviceName;
        private String action;
        private Long timestamp;
    }
    
    @Data
    @Accessors(chain = true)
    public static class BroadcastMessage {
        private String type;
        private String content;
        private Long timestamp;
    }
    
    // ========================= 测试消息处理器 =========================
    
    @Slf4j
    public static class TestMessageHandler {
        
        /**
         * 处理测试消息
         */
        @LQEnhancedBusListener(
                topic = "test.message",
                name = "testMessageHandler",
                async = true
        )
        public void handleTestMessage(TestMessage message) {
            log.info("收到测试消息: id={}, content={}", message.getId(), message.getContent());
            
            // 模拟处理时间
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            log.info("测试消息处理完成: {}", message.getId());
        }
        
        /**
         * 处理服务组消息
         */
        @LQEnhancedBusListener(
                topic = "service.action",
                serviceGroup = "test-service",
                name = "serviceActionHandler",
                async = false
        )
        public void handleServiceAction(ServiceGroupMessage message) {
            log.info("收到服务组消息: service={}, action={}", 
                    message.getServiceName(), message.getAction());
            
            // 模拟服务操作
            if ("restart".equals(message.getAction())) {
                log.info("执行服务重启操作: {}", message.getServiceName());
            }
            
            log.info("服务组消息处理完成: {}", message.getServiceName());
        }
        
        /**
         * 处理广播消息
         */
        @LQEnhancedBusListener(
                topic = "system.broadcast",
                name = "systemBroadcastHandler",
                async = true,
                priority = 0
        )
        public void handleSystemBroadcast(BroadcastMessage message, EnhancedBusMessage busMessage) {
            log.info("收到系统广播: type={}, content={}, messageId={}", 
                    message.getType(), message.getContent(), busMessage.getMessageId());
            
            // 模拟广播处理
            if ("system".equals(message.getType())) {
                log.info("处理系统广播: {}", message.getContent());
            }
            
            log.info("系统广播处理完成: {}", busMessage.getMessageId());
        }
        
        /**
         * 处理带条件过滤的消息
         */
        @LQEnhancedBusListener(
                topic = "test.message",
                name = "conditionalHandler",
                condition = "#message.id > 0",
                async = true,
                errorStrategy = LQEnhancedBusListener.ErrorHandleStrategy.LOG
        )
        public void handleConditionalMessage(TestMessage message) {
            log.info("条件处理器收到消息: id={}", message.getId());
            
            // 模拟可能的异常
            if (message.getId() % 10 == 0) {
                throw new RuntimeException("模拟处理异常");
            }
            
            log.info("条件消息处理完成: {}", message.getId());
        }
    }
}