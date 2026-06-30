package cn.lingque.cloud.rpc.example;

import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.cloud.rpc.LQDistributedServiceCaller;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * LQ分布式RPC框架使用示例
 * 展示如何使用比Dubbo更强大的微服务调用框架
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQDistributedRpcExample {
    
    public static void main(String[] args) {
        try {
            // 启动增强版注册中心
            LQEnhancedRegisterCenter.start();
            
            // 注册服务节点
            registerServiceNodes();
            
            // 等待服务注册完成
            Thread.sleep(1000);
            
            // 示例1: 使用服务代理调用
            demonstrateServiceProxy();
            
            // 示例2: 直接调用远程服务
            demonstrateDirectCall();
            
            // 示例3: 展示负载均衡
            demonstrateLoadBalance();
            
            // 示例4: 展示熔断器
            demonstrateCircuitBreaker();
            
            // 示例5: 展示调用统计
            demonstrateCallStats();
            
        } catch (Exception e) {
            log.error("示例执行失败", e);
        }
    }
    
    /**
     * 注册服务节点
     */
    private static void registerServiceNodes() {
        log.info("=== 注册服务节点 ===");
        
        // 注册用户服务节点1
        LQEnhancedNodeInfo userService1 = new LQEnhancedNodeInfo()
                .setServerName("user-service")
                .setNodeIp("192.168.1.100")
                .setNodePort(8080)
                .setProtocol("HTTP")
                .setWeight(100)
                .setVersion("1.0.0")
                .addTag("user")
                .addTag("api")
                .addMetadata("region", "us-west")
                .addMetadata("zone", "zone-a")
                .setHealthCheckUrl("/lq-rpc/health")
                .setHealthCheckInterval(30);
        
        // 注册用户服务节点2（不同权重）
        LQEnhancedNodeInfo userService2 = new LQEnhancedNodeInfo()
                .setServerName("user-service")
                .setNodeIp("192.168.1.101")
                .setNodePort(8080)
                .setProtocol("HTTP")
                .setWeight(200) // 更高权重
                .setVersion("1.0.0")
                .addTag("user")
                .addTag("api")
                .addMetadata("region", "us-west")
                .addMetadata("zone", "zone-b")
                .setHealthCheckUrl("/lq-rpc/health")
                .setHealthCheckInterval(30);
        
        // 注册订单服务节点
        LQEnhancedNodeInfo orderService = new LQEnhancedNodeInfo()
                .setServerName("order-service")
                .setNodeIp("192.168.1.102")
                .setNodePort(8081)
                .setProtocol("HTTP")
                .setWeight(150)
                .setVersion("1.0.0")
                .addTag("order")
                .addTag("api")
                .addMetadata("region", "us-east")
                .addMetadata("zone", "zone-a")
                .setHealthCheckUrl("/lq-rpc/health")
                .setHealthCheckInterval(30);
        
        // 执行注册
        boolean result1 = LQEnhancedRegisterCenter.registerEnhancedNode(userService1);
        boolean result2 = LQEnhancedRegisterCenter.registerEnhancedNode(userService2);
        boolean result3 = LQEnhancedRegisterCenter.registerEnhancedNode(orderService);
        
        log.info("服务注册结果: user-service-1={}, user-service-2={}, order-service={}", 
                result1, result2, result3);
    }
    
    /**
     * 演示服务代理调用
     */
    private static void demonstrateServiceProxy() {
        log.info("=== 演示服务代理调用 ===");
        
        try {
            // 创建服务代理
            UserService userService = LQDistributedServiceCaller.createServiceProxy(UserService.class);
            
            // 调用远程服务方法
            UserInfo user = userService.getUserById(1L);
            log.info("通过代理获取用户信息: {}", user.getUsername());
            
            // 创建新用户
            UserInfo newUser = new UserInfo()
                    .setUsername("testuser")
                    .setPassword("testpass")
                    .setEmail("test@example.com")
                    .setRealName("测试用户");
            
            Long userId = userService.createUser(newUser);
            log.info("通过代理创建用户成功: userId={}", userId);
            
            // 获取用户列表
            List<UserInfo> userList = userService.getUserList(1, 10);
            log.info("通过代理获取用户列表: count={}", userList.size());
            
        } catch (Exception e) {
            log.error("服务代理调用失败", e);
        }
    }
    
    /**
     * 演示直接调用远程服务
     */
    private static void demonstrateDirectCall() {
        log.info("=== 演示直接调用远程服务 ===");
        
        try {
            // 直接调用远程服务
            UserInfo user = LQDistributedServiceCaller.callRemoteService(
                    "user-service", 
                    "getUserById", 
                    new Object[]{2L}, 
                    UserInfo.class
            );
            log.info("直接调用获取用户信息: {}", user.getUsername());
            
            // 带完整参数的调用
            String token = LQDistributedServiceCaller.callRemoteService(
                    "user-service", 
                    "login", 
                    new Object[]{"user1", "password1"}, 
                    String.class,
                    "HTTP",
                    3000,
                    2
            );
            log.info("直接调用用户登录: token={}", token);
            
        } catch (Exception e) {
            log.error("直接调用远程服务失败", e);
        }
    }
    
    /**
     * 演示负载均衡
     */
    private static void demonstrateLoadBalance() {
        log.info("=== 演示负载均衡 ===");
        
        try {
            // 多次调用同一服务，观察负载均衡效果
            for (int i = 0; i < 5; i++) {
                UserInfo user = LQDistributedServiceCaller.callRemoteService(
                        "user-service", 
                        "getUserById", 
                        new Object[]{1L}, 
                        UserInfo.class
                );
                log.info("第{}次调用，获取用户: {}", i + 1, user.getUsername());
                
                Thread.sleep(100); // 短暂延迟
            }
            
        } catch (Exception e) {
            log.error("负载均衡演示失败", e);
        }
    }
    
    /**
     * 演示熔断器
     */
    private static void demonstrateCircuitBreaker() {
        log.info("=== 演示熔断器 ===");
        
        try {
            // 调用不存在的服务，触发熔断器
            for (int i = 0; i < 10; i++) {
                try {
                    LQDistributedServiceCaller.callRemoteService(
                            "nonexistent-service", 
                            "someMethod", 
                            new Object[]{}, 
                            String.class
                    );
                } catch (Exception e) {
                    log.warn("第{}次调用失败: {}", i + 1, e.getMessage());
                }
                
                Thread.sleep(100);
            }
            
        } catch (Exception e) {
            log.error("熔断器演示失败", e);
        }
    }
    
    /**
     * 演示调用统计
     */
    private static void demonstrateCallStats() {
        log.info("=== 演示调用统计 ===");
        
        try {
            // 获取调用统计信息
            var stats = LQDistributedServiceCaller.getCallStats();
            log.info("调用统计信息:");
            stats.forEach((key, value) -> log.info("  {}: {}", key, value));
            
        } catch (Exception e) {
            log.error("获取调用统计失败", e);
        }
    }
}