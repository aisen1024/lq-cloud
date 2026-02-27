package cn.lingque.mq.example;

import cn.lingque.mq.LQMQTemplate;
import cn.lingque.mq.LQMQType;
import cn.lingque.mq.itf.ILQMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author aisen
 * @date 2024/12/19
 * @desc LQ MQ 使用示例
 * 
 * <p>演示新版MQ模块的完整使用流程：
 * 1. 注册订阅
 * 2. 启动MQ系统
 * 3. 发送顺序消息（List队列）
 * 4. 发送延迟消息（ZSet队列）
 * 5. 发送流队列消息（Stream队列）
 * 6. 取消消息
 **/
@Slf4j
public class LQMQUsageExample {
    
    public static void main(String[] args) throws InterruptedException {
        log.info("========== LQ MQ 使用示例 ==========");
        
        // ==================== 步骤1：注册订阅 ====================
        log.info("\n步骤1：注册订阅...");
        
        // 订阅订单创建事件
        LQMQTemplate.subscribe("order:created", new ILQMessage<Order>() {
            @Override
            public void handle(Order order) {
                log.info("处理订单创建: orderId={}, userId={}, amount={}", 
                         order.getOrderId(), order.getUserId(), order.getAmount());
            }
            
            @Override
            public Class<Order> getEntityClass() {
                return Order.class;
            }
        });
        
        // 订阅订单超时事件
        LQMQTemplate.subscribe("order:timeout", new ILQMessage<Order>() {
            @Override
            public void handle(Order order) {
                log.info("处理订单超时: orderId={}, 自动取消订单", order.getOrderId());
            }
            
            @Override
            public Class<Order> getEntityClass() {
                return Order.class;
            }
        });
        
        // 订阅订单支付事件
        LQMQTemplate.subscribe("order:payment", new ILQMessage<Order>() {
            @Override
            public void handle(Order order) {
                log.info("处理订单支付: orderId={}, 支付金额={}", 
                         order.getOrderId(), order.getAmount());
            }
            
            @Override
            public Class<Order> getEntityClass() {
                return Order.class;
            }
        });
        
        // 订阅用户通知事件
        LQMQTemplate.subscribe("user:notification", new ILQMessage<String>() {
            @Override
            public void handle(String message) {
                log.info("发送用户通知: {}", message);
            }
            
            @Override
            public Class<String> getEntityClass() {
                return String.class;
            }
        });
        
        log.info("订阅注册完成！");
        
        // ==================== 步骤2：启动MQ系统 ====================
        log.info("\n步骤2：启动MQ系统...");
        LQMQTemplate.start();
        
        // 等待系统启动完成
        Thread.sleep(2000);
        
        // ==================== 步骤3：发送顺序消息（List队列） ====================
        log.info("\n步骤3：发送顺序消息（List队列 - 高性能、FIFO顺序）...");
        
        Order order1 = new Order("ORDER001", "USER001", 99.99);
        String msgId1 = LQMQTemplate.sendOrderMessage("order:created", order1);
        log.info("发送顺序消息成功，消息ID: {}", msgId1);
        
        String msgId2 = LQMQTemplate.sendOrderMessage("user:notification", "您的订单已创建成功");
        log.info("发送顺序消息成功，消息ID: {}", msgId2);
        
        // 等待消息处理
        Thread.sleep(1000);
        
        // ==================== 步骤4：发送延迟消息（ZSet队列） ====================
        log.info("\n步骤4：发送延迟消息（ZSet队列 - 支持精确延迟）...");
        
        Order order2 = new Order("ORDER002", "USER002", 199.99);
        String delayMsgId = LQMQTemplate.sendDelayMessage("order:timeout", order2, 5);
        log.info("发送延迟消息成功，消息ID: {}，将在5秒后执行", delayMsgId);
        
        // ==================== 步骤5：发送流队列消息（Stream队列） ====================
        log.info("\n步骤5：发送流队列消息（Stream队列 - 保证可靠性）...");
        
        Order order3 = new Order("ORDER003", "USER003", 299.99);
        String streamMsgId = LQMQTemplate.sendStreamMessage("order:payment", order3);
        log.info("发送Stream消息成功，消息ID: {}", streamMsgId);
        
        // 等待消息处理
        Thread.sleep(1000);
        
        // ==================== 步骤6：演示取消消息 ====================
        log.info("\n步骤6：演示取消消息...");
        
        Order order4 = new Order("ORDER004", "USER004", 399.99);
        String cancelMsgId = LQMQTemplate.sendDelayMessage("order:timeout", order4, 3);
        log.info("发送延迟消息: {}", cancelMsgId);
        
        // 等待1秒后取消消息
        Thread.sleep(1000);
        log.info("取消消息: {}", cancelMsgId);
        boolean cancelled = LQMQTemplate.cancelMessage(cancelMsgId);
        log.info("取消结果: {}", cancelled ? "成功" : "失败");
        
        // 等待延迟消息到期（应该不会被处理）
        Thread.sleep(3000);
        
        // ==================== 步骤7：演示不同MQ类型发送 ====================
        log.info("\n步骤7：演示使用MQ类型枚举发送...");
        
        Order order5 = new Order("ORDER005", "USER005", 499.99);
        
        // 使用List队列
        String listMsg = LQMQTemplate.sendMessage("order:created", order5, LQMQType.LIST);
        log.info("使用LIST类型发送: {}", listMsg);
        
        // 使用ZSet队列（延迟10秒）
        String zsetMsg = LQMQTemplate.sendDelayMessage("order:timeout", order5, 10, LQMQType.ZSET);
        log.info("使用ZSET类型发送延迟消息: {}", zsetMsg);
        
        // 使用Stream队列
        String streamMsg = LQMQTemplate.sendMessage("order:payment", order5, LQMQType.STREAM);
        log.info("使用STREAM类型发送: {}", streamMsg);
        
        Thread.sleep(1000);
        
        // ==================== 步骤8：查看系统状态 ====================
        log.info("\n步骤8：查看系统状态...");
        LQMQTemplate.printStatus();
        
        // ==================== 步骤9：等待延迟消息处理 ====================
        log.info("\n步骤9：等待延迟消息处理...");
        Thread.sleep(6000);
        
        // ==================== 步骤10：关闭系统 ====================
        log.info("\n步骤10：关闭MQ系统...");
        LQMQTemplate.stop();
        
        log.info("\n========== 示例完成 ==========");
    }
    
    /**
     * 订单实体
     */
    @Data
    public static class Order {
        private String orderId;
        private String userId;
        private Double amount;
        
        public Order() {}
        
        public Order(String orderId, String userId, Double amount) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
        }
    }
}
