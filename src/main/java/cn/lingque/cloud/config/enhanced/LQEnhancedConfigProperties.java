package cn.lingque.cloud.config.enhanced;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * LQ增强版配置中心属性配置
 * 
 * @author LingQue Team
 */
@Data
@ConfigurationProperties(prefix = "lq.config.enhanced")
public class LQEnhancedConfigProperties {
    
    /**
     * 是否启用增强版配置中心
     */
    private boolean enabled = true;
    
    /**
     * 是否启用自动清理过期配置
     */
    private boolean autoCleanup = true;
    
    /**
     * 清理任务初始延迟（秒）
     */
    private long cleanupInitialDelay = 60;
    
    /**
     * 清理任务间隔（秒）
     */
    private long cleanupInterval = 300; // 5分钟
    
    /**
     * 是否启用统计信息
     */
    private boolean enableStats = true;
    
    /**
     * 统计信息打印间隔（秒）
     */
    private long statsInterval = 600; // 10分钟
    
    /**
     * 默认配置TTL（毫秒）
     */
    private long defaultTtl = 30 * 60 * 1000; // 30分钟
    
    /**
     * 最大配置数量限制
     */
    private int maxConfigCount = 10000;
    
    /**
     * 最大命名空间数量限制
     */
    private int maxNamespaceCount = 100;
    
    /**
     * 是否启用配置变更日志
     */
    private boolean enableChangeLog = true;
    
    /**
     * 是否启用配置推送
     */
    private boolean enableConfigPush = true;
    
    /**
     * 配置推送事件名称
     */
    private String configPushEvent = "lq:config:push";
    
    /**
     * 初始配置列表
     */
    private List<ConfigItem> initialConfigs = new ArrayList<>();
    
    /**
     * 配置项
     */
    @Data
    public static class ConfigItem {
        
        /**
         * 命名空间
         */
        private String namespace = "default";
        
        /**
         * 配置键
         */
        private String key;
        
        /**
         * 配置值
         */
        private String value;
        
        /**
         * TTL（毫秒）
         */
        private Long ttl;
        
        /**
         * 配置描述
         */
        private String description;
        
        /**
         * 配置类型（yaml、json、properties、text）
         */
        private String type = "text";
        
        /**
         * 是否只读
         */
        private boolean readonly = false;
        
        /**
         * 是否加密
         */
        private boolean encrypted = false;
    }
    
    /**
     * 验证配置
     */
    public void validate() {
        if (cleanupInterval <= 0) {
            throw new IllegalArgumentException("清理间隔必须大于0");
        }
        
        if (defaultTtl <= 0) {
            throw new IllegalArgumentException("默认TTL必须大于0");
        }
        
        if (maxConfigCount <= 0) {
            throw new IllegalArgumentException("最大配置数量必须大于0");
        }
        
        if (maxNamespaceCount <= 0) {
            throw new IllegalArgumentException("最大命名空间数量必须大于0");
        }
        
        // 验证初始配置
        if (initialConfigs != null) {
            for (ConfigItem item : initialConfigs) {
                if (item.getKey() == null || item.getKey().trim().isEmpty()) {
                    throw new IllegalArgumentException("配置键不能为空");
                }
                if (item.getValue() == null) {
                    throw new IllegalArgumentException("配置值不能为null");
                }
            }
        }
    }
    
    /**
     * 获取配置摘要
     */
    public String getSummary() {
        return String.format("LQEnhancedConfigProperties{enabled=%s, autoCleanup=%s, cleanupInterval=%ds, " +
                "defaultTtl=%dms, maxConfigs=%d, maxNamespaces=%d, initialConfigs=%d}",
                enabled, autoCleanup, cleanupInterval, defaultTtl, maxConfigCount, 
                maxNamespaceCount, initialConfigs != null ? initialConfigs.size() : 0);
    }
}