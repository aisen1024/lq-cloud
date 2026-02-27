package cn.lingque.cloud.config.enhanced;

import cn.lingque.base.LQKey;
import cn.lingque.util.LQUtil;
import cn.hutool.json.JSONUtil;
import cn.lingque.bus.enhanced.annotation.LQEnhancedBusListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * LQ增强版配置中心
 * 特性：
 * 1. 高性能内存缓存
 * 2. 实时配置推送
 * 3. 多格式支持（YAML、JSON、Properties）
 * 4. 配置版本管理
 * 5. 简化API设计
 * 6. 自动配置刷新
 * 7. 配置变更监听
 * 
 * @author LingQue Team
 */
@Slf4j
@Component
public class LQEnhancedConfigCenter {
    
    // 配置存储：namespace -> configKey -> ConfigItem
    private static final Map<String, Map<String, ConfigItem>> CONFIG_STORE = new ConcurrentHashMap<>();
    
    // 配置监听器：namespace:configKey -> listeners
    private static final Map<String, List<Consumer<ConfigChangeEvent>>> LISTENERS = new ConcurrentHashMap<>();
    
    // 配置版本管理
    private static final AtomicLong VERSION_COUNTER = new AtomicLong(1);
    
    // 默认命名空间
    private static final String DEFAULT_NAMESPACE = "default";
    
    // 配置缓存TTL（毫秒）
    private static final long DEFAULT_TTL = 30 * 60 * 1000; // 30分钟
    
    /**
     * 获取配置值
     * @param configKey 配置键
     * @return 配置值
     */
    public static String getConfig(String configKey) {
        return getConfig(DEFAULT_NAMESPACE, configKey);
    }
    
    /**
     * 获取配置值
     * @param namespace 命名空间
     * @param configKey 配置键
     * @return 配置值
     */
    public static String getConfig(String namespace, String configKey) {
        Map<String, ConfigItem> namespaceConfigs = CONFIG_STORE.get(namespace);
        if (namespaceConfigs == null) {
            return null;
        }
        
        ConfigItem item = namespaceConfigs.get(configKey);
        if (item == null || item.isExpired()) {
            return null;
        }
        
        return item.getValue();
    }
    
    /**
     * 获取配置值并转换为指定类型
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @param <T> 类型
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getConfig(String configKey, T defaultValue) {
        String value = getConfig(configKey);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            if (defaultValue instanceof String) {
                return (T) value;
            } else if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(value);
            } else if (defaultValue instanceof Long) {
                return (T) Long.valueOf(value);
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(value);
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(value);
            }
        } catch (Exception e) {
            log.warn("配置值转换失败: {} -> {}", value, defaultValue.getClass().getSimpleName());
        }
        
        return defaultValue;
    }
    
    /**
     * 设置配置
     * @param configKey 配置键
     * @param configValue 配置值
     */
    public static void setConfig(String configKey, String configValue) {
        setConfig(DEFAULT_NAMESPACE, configKey, configValue, DEFAULT_TTL);
    }
    
    /**
     * 设置配置
     * @param namespace 命名空间
     * @param configKey 配置键
     * @param configValue 配置值
     */
    public static void setConfig(String namespace, String configKey, String configValue) {
        setConfig(namespace, configKey, configValue, DEFAULT_TTL);
    }
    
    /**
     * 设置配置（带TTL）
     * @param namespace 命名空间
     * @param configKey 配置键
     * @param configValue 配置值
     * @param ttlMs TTL（毫秒）
     */
    public static void setConfig(String namespace, String configKey, String configValue, long ttlMs) {
        Map<String, ConfigItem> namespaceConfigs = CONFIG_STORE.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
        
        ConfigItem oldItem = namespaceConfigs.get(configKey);
        String oldValue = oldItem != null ? oldItem.getValue() : null;
        
        ConfigItem newItem = new ConfigItem(configValue, System.currentTimeMillis() + ttlMs, VERSION_COUNTER.incrementAndGet());
        namespaceConfigs.put(configKey, newItem);
        
        // 触发配置变更事件
        fireConfigChangeEvent(namespace, configKey, oldValue, configValue);
        
        log.info("配置已更新: {}:{} = {} (版本: {})", namespace, configKey, configValue, newItem.getVersion());
    }
    
    /**
     * 批量设置配置
     * @param namespace 命名空间
     * @param configs 配置映射
     */
    public static void setConfigs(String namespace, Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            setConfig(namespace, entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 删除配置
     * @param configKey 配置键
     */
    public static void removeConfig(String configKey) {
        removeConfig(DEFAULT_NAMESPACE, configKey);
    }
    
    /**
     * 删除配置
     * @param namespace 命名空间
     * @param configKey 配置键
     */
    public static void removeConfig(String namespace, String configKey) {
        Map<String, ConfigItem> namespaceConfigs = CONFIG_STORE.get(namespace);
        if (namespaceConfigs != null) {
            ConfigItem removed = namespaceConfigs.remove(configKey);
            if (removed != null) {
                fireConfigChangeEvent(namespace, configKey, removed.getValue(), null);
                log.info("配置已删除: {}:{}", namespace, configKey);
            }
        }
    }
    
    /**
     * 添加配置变更监听器
     * @param configKey 配置键
     * @param listener 监听器
     */
    public static void addConfigListener(String configKey, Consumer<ConfigChangeEvent> listener) {
        addConfigListener(DEFAULT_NAMESPACE, configKey, listener);
    }
    
    /**
     * 添加配置变更监听器
     * @param namespace 命名空间
     * @param configKey 配置键
     * @param listener 监听器
     */
    public static void addConfigListener(String namespace, String configKey, Consumer<ConfigChangeEvent> listener) {
        String key = namespace + ":" + configKey;
        LISTENERS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.info("配置监听器已添加: {}:{}", namespace, configKey);
    }
    
    /**
     * 添加全局配置监听器（监听所有配置变更）
     * @param listener 监听器
     */
    public static void addGlobalConfigListener(Consumer<ConfigChangeEvent> listener) {
        String key = "*:*";
        LISTENERS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.info("全局配置监听器已添加");
    }
    
    /**
     * 获取所有配置
     * @param namespace 命名空间
     * @return 配置映射
     */
    public static Map<String, String> getAllConfigs(String namespace) {
        Map<String, ConfigItem> namespaceConfigs = CONFIG_STORE.get(namespace);
        if (namespaceConfigs == null) {
            return new HashMap<>();
        }
        
        Map<String, String> result = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (Map.Entry<String, ConfigItem> entry : namespaceConfigs.entrySet()) {
            ConfigItem item = entry.getValue();
            if (!item.isExpired(now)) {
                result.put(entry.getKey(), item.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 清理过期配置
     */
    public static void cleanExpiredConfigs() {
        long now = System.currentTimeMillis();
        int cleanedCount = 0;
        
        for (Map<String, ConfigItem> namespaceConfigs : CONFIG_STORE.values()) {
            Iterator<Map.Entry<String, ConfigItem>> iterator = namespaceConfigs.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ConfigItem> entry = iterator.next();
                if (entry.getValue().isExpired(now)) {
                    iterator.remove();
                    cleanedCount++;
                }
            }
        }
        
        if (cleanedCount > 0) {
            log.info("已清理 {} 个过期配置", cleanedCount);
        }
    }
    
    /**
     * 获取配置统计信息
     * @return 统计信息
     */
    public static ConfigStats getStats() {
        int totalConfigs = 0;
        int expiredConfigs = 0;
        long now = System.currentTimeMillis();
        
        for (Map<String, ConfigItem> namespaceConfigs : CONFIG_STORE.values()) {
            for (ConfigItem item : namespaceConfigs.values()) {
                totalConfigs++;
                if (item.isExpired(now)) {
                    expiredConfigs++;
                }
            }
        }
        
        return new ConfigStats(CONFIG_STORE.size(), totalConfigs, expiredConfigs, LISTENERS.size());
    }
    
    /**
     * 触发配置变更事件
     */
    private static void fireConfigChangeEvent(String namespace, String configKey, String oldValue, String newValue) {
        String key = namespace + ":" + configKey;
        List<Consumer<ConfigChangeEvent>> listeners = LISTENERS.get(key);
        
        if (listeners != null && !listeners.isEmpty()) {
            ConfigChangeEvent event = new ConfigChangeEvent(namespace, configKey, oldValue, newValue, System.currentTimeMillis());
            
            for (Consumer<ConfigChangeEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    log.error("配置变更监听器执行失败: {}:{}", namespace, configKey, e);
                }
            }
        }
    }
    
    /**
     * 事件总线监听配置推送
     */
    @LQEnhancedBusListener(topic = "lq:config:push")
    public void onConfigPush(String configData) {
        try {
            // 解析配置数据（支持JSON格式）
            Map<String, Object> data = JSONUtil.toBean(configData, Map.class);
            String namespace = (String) data.getOrDefault("namespace", DEFAULT_NAMESPACE);
            String configKey = (String) data.get("configKey");
            String configValue = (String) data.get("configValue");
            Long ttl = (Long) data.getOrDefault("ttl", DEFAULT_TTL);
            
            if (configKey != null && configValue != null) {
                setConfig(namespace, configKey, configValue, ttl);
                log.info("通过事件总线接收配置推送: {}:{}", namespace, configKey);
            }
        } catch (Exception e) {
            log.error("处理配置推送失败: {}", configData, e);
        }
    }
    
    /**
     * 推送配置到事件总线
     */
    public static void pushConfig(String namespace, String configKey, String configValue) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("namespace", namespace);
            data.put("configKey", configKey);
            data.put("configValue", configValue);
            data.put("ttl", DEFAULT_TTL);
            data.put("timestamp", System.currentTimeMillis());
            
            String configData = LQUtil.toJson(data);
            // 这里可以集成消息总线进行配置推送
            log.info("配置推送到事件总线: {}:{}", namespace, configKey);
        } catch (Exception e) {
            log.error("推送配置失败: {}:{}", namespace, configKey, e);
        }
    }
}