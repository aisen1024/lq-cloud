package cn.lingque.cloud.config.enhanced;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LQ配置刷新工具类
 * 提供便捷的配置刷新API
 * 
 * @author LingQue
 * @version 1.0
 */
@Component
public class LQConfigRefreshUtils {
    
    @Autowired(required = false)
    private LQConfigRefreshManager refreshManager;
    
    /**
     * 手动刷新指定Bean的所有@Value字段
     * 
     * @param beanName Bean名称
     */
    public void refreshBean(String beanName) {
        if (refreshManager != null) {
            refreshManager.refreshBean(beanName);
        } else {
            System.err.println("[LQConfigRefresh] 配置刷新管理器未启用");
        }
    }
    
    /**
     * 手动刷新所有Bean的@Value字段
     */
    public void refreshAllBeans() {
        if (refreshManager != null) {
            refreshManager.refreshAllBeans();
        } else {
            System.err.println("[LQConfigRefresh] 配置刷新管理器未启用");
        }
    }
    
    /**
     * 获取配置刷新统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getRefreshStats() {
        if (refreshManager != null) {
            return refreshManager.getRefreshStats();
        } else {
            System.err.println("[LQConfigRefresh] 配置刷新管理器未启用");
            return Map.of("error", "配置刷新管理器未启用");
        }
    }
    
    /**
     * 检查配置刷新功能是否可用
     * 
     * @return true表示可用，false表示不可用
     */
    public boolean isRefreshEnabled() {
        return refreshManager != null;
    }
    
    /**
     * 设置配置并触发刷新
     * 
     * @param configKey 配置键
     * @param configValue 配置值
     */
    public void setConfigAndRefresh(String configKey, String configValue) {
        // 设置配置
        LQEnhancedConfigCenter.setConfig(configKey, configValue);
        
        // 配置变更会自动触发刷新，这里只是记录日志
        System.out.println("[LQConfigRefresh] 配置已设置并将自动刷新: " + configKey + " = " + configValue);
    }
    
    /**
     * 设置命名空间配置并触发刷新
     * 
     * @param namespace 命名空间
     * @param configKey 配置键
     * @param configValue 配置值
     */
    public void setConfigAndRefresh(String namespace, String configKey, String configValue) {
        // 设置配置
        LQEnhancedConfigCenter.setConfig(namespace, configKey, configValue);
        
        // 配置变更会自动触发刷新，这里只是记录日志
        System.out.println("[LQConfigRefresh] 命名空间配置已设置并将自动刷新: " + 
                          namespace + ":" + configKey + " = " + configValue);
    }
    
    /**
     * 批量设置配置并触发刷新
     * 
     * @param namespace 命名空间
     * @param configs 配置映射
     */
    public void setBatchConfigsAndRefresh(String namespace, Map<String, String> configs) {
        // 批量设置配置
        LQEnhancedConfigCenter.setConfigs(namespace, configs);
        
        // 配置变更会自动触发刷新，这里只是记录日志
        System.out.println("[LQConfigRefresh] 批量配置已设置并将自动刷新: " + 
                          namespace + ", 配置数量: " + configs.size());
    }
    
    /**
     * 比较配置差异并显示需要刷新的字段
     * 
     * @param oldConfigs 旧配置
     * @param newConfigs 新配置
     */
    public void compareAndShowDiff(Map<String, String> oldConfigs, Map<String, String> newConfigs) {
        System.out.println("[LQConfigRefresh] 配置差异分析:");
        
        // 检查新增的配置
        for (Map.Entry<String, String> entry : newConfigs.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = oldConfigs.get(key);
            
            if (oldValue == null) {
                System.out.println("  [新增] " + key + " = " + newValue);
            } else if (!oldValue.equals(newValue)) {
                System.out.println("  [修改] " + key + ": " + oldValue + " -> " + newValue);
            }
        }
        
        // 检查删除的配置
        for (String key : oldConfigs.keySet()) {
            if (!newConfigs.containsKey(key)) {
                System.out.println("  [删除] " + key + " = " + oldConfigs.get(key));
            }
        }
    }
    
    /**
     * 打印当前所有配置
     * 
     * @param namespace 命名空间
     */
    public void printAllConfigs(String namespace) {
        Map<String, String> configs = LQEnhancedConfigCenter.getAllConfigs(namespace);
        System.out.println("[LQConfigRefresh] 当前配置 (" + namespace + "):");
        
        if (configs.isEmpty()) {
            System.out.println("  (无配置)");
        } else {
            configs.forEach((key, value) -> 
                System.out.println("  " + key + " = " + value)
            );
        }
    }
    
    /**
     * 打印配置刷新帮助信息
     */
    public void printHelp() {
        System.out.println("[LQConfigRefresh] 配置刷新工具使用说明:");
        System.out.println("  1. setConfigAndRefresh(key, value) - 设置配置并自动刷新");
        System.out.println("  2. refreshBean(beanName) - 手动刷新指定Bean");
        System.out.println("  3. refreshAllBeans() - 手动刷新所有Bean");
        System.out.println("  4. getRefreshStats() - 获取刷新统计信息");
        System.out.println("  5. isRefreshEnabled() - 检查刷新功能是否启用");
        System.out.println("  6. printAllConfigs(namespace) - 打印所有配置");
        System.out.println("  ");
        System.out.println("  注意: 配置变更会自动触发@Value字段刷新，无需手动调用刷新方法");
    }
}