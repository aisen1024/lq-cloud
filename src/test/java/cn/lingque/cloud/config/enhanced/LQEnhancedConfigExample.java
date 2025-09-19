package cn.lingque.cloud.config.enhanced;

import cn.lingque.util.LQUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * LQ增强版配置中心使用示例
 * 展示配置中心的各种功能和使用方法
 * 
 * @author LingQue Team
 */
@Slf4j
public class LQEnhancedConfigExample {
    
    public static void main(String[] args) {
        log.info("=== LQ增强版配置中心使用示例 ===");
        
        try {
            // 1. 基本配置操作
            basicConfigOperations();
            
            // 2. 配置监听器
            configListeners();
            
            // 3. 命名空间操作
            namespaceOperations();
            
            // 4. 配置管理器功能
            configManagerFeatures();
            
            // 5. 配置导入导出
            configImportExport();
            
            // 6. 配置搜索和比较
            configSearchAndCompare();
            
            // 7. 统计信息
            configStats();
            
            log.info("=== 示例执行完成 ===");
            
        } catch (Exception e) {
            log.error("示例执行失败", e);
        }
    }
    
    /**
     * 基本配置操作示例
     */
    private static void basicConfigOperations() {
        log.info("\n--- 1. 基本配置操作 ---");
        
        // 设置配置
        LQEnhancedConfigCenter.setConfig("app.name", "LQ配置中心示例");
        LQEnhancedConfigCenter.setConfig("app.version", "1.0.0");
        LQEnhancedConfigCenter.setConfig("app.debug", "true");
        LQEnhancedConfigCenter.setConfig("app.port", "8080");
        
        // 获取配置
        String appName = LQEnhancedConfigCenter.getConfig("app.name");
        String appVersion = LQEnhancedConfigCenter.getConfig("app.version");
        boolean debug = LQEnhancedConfigCenter.getConfig("app.debug", false);
        int port = LQEnhancedConfigCenter.getConfig("app.port", 8080);
        
        log.info("应用名称: {}", appName);
        log.info("应用版本: {}", appVersion);
        log.info("调试模式: {}", debug);
        log.info("端口号: {}", port);
        
        // 批量设置配置
        Map<String, String> configs = new HashMap<>();
        configs.put("database.host", "localhost");
        configs.put("database.port", "3306");
        configs.put("database.name", "lq_config");
        configs.put("database.username", "root");
        
        LQEnhancedConfigCenter.setConfigs("default", configs);
        log.info("批量设置配置完成: {} 个", configs.size());
    }
    
    /**
     * 配置监听器示例
     */
    private static void configListeners() {
        log.info("\n--- 2. 配置监听器 ---");
        
        // 添加配置监听器
        LQEnhancedConfigCenter.addConfigListener("app.debug", event -> {
            log.info("配置变更监听: {} -> {}", event.getOldValue(), event.getNewValue());
            log.info("变更类型: {}", event.getChangeType().getDescription());
        });
        
        // 添加数据库配置监听器
        LQEnhancedConfigCenter.addConfigListener("database.host", event -> {
            log.info("数据库主机配置变更: {} -> {}", event.getOldValue(), event.getNewValue());
            // 这里可以重新初始化数据库连接
        });
        
        // 触发配置变更
        LQEnhancedConfigCenter.setConfig("app.debug", "false");
        LQEnhancedConfigCenter.setConfig("database.host", "192.168.1.100");
        
        // 删除配置
        LQEnhancedConfigCenter.removeConfig("app.debug");
    }
    
    /**
     * 命名空间操作示例
     */
    private static void namespaceOperations() {
        log.info("\n--- 3. 命名空间操作 ---");
        
        // 在不同命名空间设置配置
        LQEnhancedConfigCenter.setConfig("dev", "database.host", "dev.example.com");
        LQEnhancedConfigCenter.setConfig("test", "database.host", "test.example.com");
        LQEnhancedConfigCenter.setConfig("prod", "database.host", "prod.example.com");
        
        // 获取不同环境的配置
        String devHost = LQEnhancedConfigCenter.getConfig("dev", "database.host");
        String testHost = LQEnhancedConfigCenter.getConfig("test", "database.host");
        String prodHost = LQEnhancedConfigCenter.getConfig("prod", "database.host");
        
        log.info("开发环境数据库: {}", devHost);
        log.info("测试环境数据库: {}", testHost);
        log.info("生产环境数据库: {}", prodHost);
        
        // 获取命名空间所有配置
        Map<String, String> devConfigs = LQEnhancedConfigCenter.getAllConfigs("dev");
        log.info("开发环境配置数量: {}", devConfigs.size());
    }
    
    /**
     * 配置管理器功能示例
     */
    private static void configManagerFeatures() {
        log.info("\n--- 4. 配置管理器功能 ---");
        
        // 注册配置模板
        LQConfigManager.registerTemplate("database-template", variables -> {
            Map<String, String> configs = new HashMap<>();
            String env = variables.getOrDefault("env", "dev");
            String dbType = variables.getOrDefault("dbType", "mysql");
            
            configs.put("database.type", dbType);
            configs.put("database.host", env + "." + dbType + ".example.com");
            configs.put("database.port", "mysql".equals(dbType) ? "3306" : "5432");
            configs.put("database.name", "lq_" + env);
            
            return configs;
        });
        
        // 从模板创建配置
        Map<String, String> variables = new HashMap<>();
        variables.put("env", "staging");
        variables.put("dbType", "postgresql");
        
        LQConfigManager.createFromTemplate("database-template", "staging", variables);
        log.info("从模板创建配置完成");
        
        // 注册配置验证器
        LQConfigManager.registerValidator("port-validator", (key, value) -> {
            if (key.contains("port")) {
                try {
                    int port = Integer.parseInt(value);
                    return port > 0 && port <= 65535;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        });
        
        // 使用验证器批量设置配置
        Map<String, String> validatedConfigs = new HashMap<>();
        validatedConfigs.put("redis.port", "6379");
        validatedConfigs.put("elasticsearch.port", "9200");
        
        try {
            LQConfigManager.setBatchConfigs("cache", validatedConfigs, "port-validator");
            log.info("验证通过，配置设置成功");
        } catch (Exception e) {
            log.error("配置验证失败: {}", e.getMessage());
        }
    }
    
    /**
     * 配置导入导出示例
     */
    private static void configImportExport() {
        log.info("\n--- 5. 配置导入导出 ---");
        
        // YAML配置导入
        String yamlConfig = """
            server:
              port: 8080
              host: localhost
            spring:
              datasource:
                url: jdbc:mysql://localhost:3306/test
                username: root
                password: 123456
            logging:
              level:
                root: INFO
                com.example: DEBUG
            """;
        
        LQConfigManager.importYamlConfig("yaml-import", yamlConfig);
        log.info("YAML配置导入完成");
        
        // JSON配置导入
        String jsonConfig = """
            {
              "redis": {
                "host": "localhost",
                "port": 6379,
                "database": 0
              },
              "cache": {
                "ttl": 3600,
                "maxSize": 1000
              }
            }
            """;
        
        LQConfigManager.importJsonConfig("json-import", jsonConfig);
        log.info("JSON配置导入完成");
        
        // Properties配置导入
        String propertiesConfig = """
            app.name=LQ配置中心
            app.version=1.0.0
            app.author=LingQue Team
            feature.cache.enabled=true
            feature.monitoring.enabled=true
            """;
        
        LQConfigManager.importPropertiesConfig("properties-import", propertiesConfig);
        log.info("Properties配置导入完成");
        
        // 导出配置
        String exportedYaml = LQConfigManager.exportToYaml("yaml-import");
        log.info("导出YAML配置:\n{}", exportedYaml);
        
        String exportedJson = LQConfigManager.exportToJson("json-import");
        log.info("导出JSON配置:\n{}", exportedJson);
    }
    
    /**
     * 配置搜索和比较示例
     */
    private static void configSearchAndCompare() {
        log.info("\n--- 6. 配置搜索和比较 ---");
        
        // 搜索配置
        Map<String, String> portConfigs = LQConfigManager.searchConfigs("default", "*port*", null);
        log.info("搜索包含'port'的配置: {} 个", portConfigs.size());
        portConfigs.forEach((key, value) -> log.info("  {} = {}", key, value));
        
        // 搜索特定值的配置
        Map<String, String> localhostConfigs = LQConfigManager.searchConfigs("default", null, ".*localhost.*");
        log.info("搜索值包含'localhost'的配置: {} 个", localhostConfigs.size());
        
        // 比较不同命名空间的配置
        LQConfigManager.ConfigDiff diff = LQConfigManager.compareConfigs("dev", "test");
        log.info("配置差异比较: {}", diff);
        log.info("仅在dev中存在: {} 个", diff.getOnlyInFirst().size());
        log.info("仅在test中存在: {} 个", diff.getOnlyInSecond().size());
        log.info("不同的配置: {} 个", diff.getDifferent().size());
        log.info("相同的配置: {} 个", diff.getSame().size());
    }
    
    /**
     * 统计信息示例
     */
    private static void configStats() {
        log.info("\n--- 7. 统计信息 ---");
        
        // 获取配置统计
        ConfigStats stats = LQEnhancedConfigCenter.getStats();
        log.info("配置中心统计: {}", stats);
        log.info("健康状态: {}", stats.getHealthStatus().getDescription());
        
        // 打印详细报告
        log.info("详细统计报告:\n{}", stats.generateReport());
        
        // 清理过期配置
        if (stats.needsCleanup()) {
            log.info("检测到需要清理过期配置，执行清理...");
            LQEnhancedConfigCenter.cleanExpiredConfigs();
            
            ConfigStats afterCleanup = LQEnhancedConfigCenter.getStats();
            log.info("清理后统计: {}", afterCleanup);
        }
    }
}