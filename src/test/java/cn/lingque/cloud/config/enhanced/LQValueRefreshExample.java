package cn.lingque.cloud.config.enhanced;

import cn.lingque.bus.enhanced.LQEnhancedBus;
import cn.lingque.bus.enhanced.annotation.LQEnhancedBusListener;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LQ增强版配置中心@Value注解动态刷新示例
 * 演示配置变更后自动刷新@Value注入的字段值
 * 
 * @author LingQue
 * @version 1.0
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "cn.lingque")
public class LQValueRefreshExample {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  LQ增强版配置中心@Value刷新示例");
        System.out.println("  Enhanced Config @Value Refresh Demo");
        System.out.println("========================================");
        
        try {
            // 启动Spring Boot应用
            ConfigurableApplicationContext context = SpringApplication.run(LQValueRefreshExample.class, args);
            
            // 等待Spring容器完全启动
            Thread.sleep(2000);
            
            // 运行示例
            runExample(context);
            
            // 保持应用运行一段时间以观察效果
            Thread.sleep(10000);
            
            context.close();
            
        } catch (Exception e) {
            log.error("示例运行失败", e);
        }
    }
    
    private static void runExample(ConfigurableApplicationContext context) throws InterruptedException {
        log.info("\n=== 开始@Value动态刷新示例 ===");
        
        // 获取测试服务
        TestConfigService testService = context.getBean(TestConfigService.class);
        TestController testController = context.getBean(TestController.class);
        
        // 1. 显示初始配置值
        log.info("\n--- 1. 初始配置值 ---");
        testService.showCurrentConfig();
        testController.showCurrentConfig();
        
        // 2. 设置一些初始配置
        log.info("\n--- 2. 设置初始配置 ---");
        LQEnhancedConfigCenter.setConfig("app.name", "MyApplication");
        LQEnhancedConfigCenter.setConfig("server.port", "8080");
        LQEnhancedConfigCenter.setConfig("app.debug", "false");
        LQEnhancedConfigCenter.setConfig("database.timeout", "30");
        LQEnhancedConfigCenter.setConfig("cache.ttl", "3600");
        
        Thread.sleep(1000);
        log.info("配置设置完成，等待刷新...");
        Thread.sleep(2000);
        
        // 3. 显示刷新后的配置值
        log.info("\n--- 3. 刷新后的配置值 ---");
        testService.showCurrentConfig();
        testController.showCurrentConfig();
        
        // 4. 动态修改配置
        log.info("\n--- 4. 动态修改配置 ---");
        LQEnhancedConfigCenter.setConfig("app.name", "UpdatedApplication");
        LQEnhancedConfigCenter.setConfig("server.port", "9090");
        LQEnhancedConfigCenter.setConfig("app.debug", "true");
        LQEnhancedConfigCenter.setConfig("database.timeout", "60");
        
        Thread.sleep(1000);
        log.info("配置修改完成，等待刷新...");
        Thread.sleep(2000);
        
        // 5. 显示最终配置值
        log.info("\n--- 5. 最终配置值 ---");
        testService.showCurrentConfig();
        testController.showCurrentConfig();
        
        // 6. 测试命名空间配置
        log.info("\n--- 6. 测试命名空间配置 ---");
        LQEnhancedConfigCenter.setConfig("prod", "database.host", "prod.db.com");
        LQEnhancedConfigCenter.setConfig("test", "database.host", "test.db.com");
        
        Thread.sleep(2000);
        
        // 7. 显示刷新统计
        log.info("\n--- 7. 刷新统计信息 ---");
        LQConfigRefreshManager refreshManager = context.getBean(LQConfigRefreshManager.class);
        Map<String, Object> stats = refreshManager.getRefreshStats();
        log.info("刷新统计: {}", stats);
        
        log.info("\n=== @Value动态刷新示例完成 ===");
    }
    
    /**
     * 测试配置服务
     */
    @Component
    @Slf4j
    public static class TestConfigService {
        
        @Value("${app.name:DefaultApp}")
        private String appName;
        
        @Value("${server.port:8080}")
        private Integer serverPort;
        
        @Value("${app.debug:false}")
        private Boolean debugMode;
        
        @Value("${database.timeout:30}")
        private Long databaseTimeout;
        
        @Value("${cache.ttl:1800}")
        private Integer cacheTtl;
        
        @PostConstruct
        public void init() {
            log.info("TestConfigService 初始化完成");
        }
        
        public void showCurrentConfig() {
            log.info("[TestConfigService] 当前配置:");
            log.info("  app.name = {}", appName);
            log.info("  server.port = {}", serverPort);
            log.info("  app.debug = {}", debugMode);
            log.info("  database.timeout = {}", databaseTimeout);
            log.info("  cache.ttl = {}", cacheTtl);
        }
        
        @LQEnhancedBusListener(topic = "config.field.refreshed")
        public void onFieldRefreshed(Map<String, Object> event) {
            log.info("[TestConfigService] 收到字段刷新事件: {}", event);
        }
    }
    
    /**
     * 测试控制器
     */
    @RestController
    @RequestMapping("/api/config")
    @Slf4j
    public static class TestController {
        
        @Value("${app.name:DefaultApp}")
        private String applicationName;
        
        @Value("${server.port:8080}")
        private String port;
        
        @Value("${app.version:1.0.0}")
        private String version;
        
        @PostConstruct
        public void init() {
            log.info("TestController 初始化完成");
        }
        
        @GetMapping("/current")
        public Map<String, Object> getCurrentConfig() {
            Map<String, Object> config = new HashMap<>();
            config.put("applicationName", applicationName);
            config.put("port", port);
            config.put("version", version);
            return config;
        }
        
        public void showCurrentConfig() {
            log.info("[TestController] 当前配置:");
            log.info("  applicationName = {}", applicationName);
            log.info("  port = {}", port);
            log.info("  version = {}", version);
        }
        
        @LQEnhancedBusListener(topic = "config.refresh.completed")
        public void onRefreshCompleted(Map<String, Object> event) {
            log.info("[TestController] 收到配置刷新完成事件: {}", event);
        }
    }
    
    /**
     * 配置变更监听器
     */
    @Component
    @Slf4j
    public static class ConfigChangeListener {
        
        @LQEnhancedBusListener(topic = "config.refresh.completed")
        public void onConfigRefreshCompleted(Map<String, Object> event) {
            log.info("=== 配置刷新完成 ===");
            log.info("配置键: {}", event.get("configKey"));
            log.info("刷新字段数: {}", event.get("refreshedFields"));
            log.info("时间戳: {}", event.get("timestamp"));
        }
        
        @LQEnhancedBusListener(topic = "config.field.refreshed")
        public void onFieldRefreshed(Map<String, Object> event) {
            log.info("=== 字段值已刷新 ===");
            log.info("Bean名称: {}", event.get("beanName"));
            log.info("字段名称: {}", event.get("fieldName"));
            log.info("旧值: {}", event.get("oldValue"));
            log.info("新值: {}", event.get("newValue"));
            log.info("配置键: {}", event.get("configKey"));
        }
    }
}