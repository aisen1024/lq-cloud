package cn.lingque.cloud.config.enhanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置变更事件
 * 当配置发生变更时触发此事件
 * 
 * @author LingQue Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigChangeEvent {
    
    /**
     * 命名空间
     */
    private String namespace;
    
    /**
     * 配置键
     */
    private String configKey;
    
    /**
     * 旧值
     */
    private String oldValue;
    
    /**
     * 新值
     */
    private String newValue;
    
    /**
     * 变更时间戳
     */
    private long timestamp;
    
    /**
     * 获取变更类型
     * @return 变更类型
     */
    public ChangeType getChangeType() {
        if (oldValue == null && newValue != null) {
            return ChangeType.ADDED;
        } else if (oldValue != null && newValue == null) {
            return ChangeType.REMOVED;
        } else if (oldValue != null && newValue != null) {
            return ChangeType.UPDATED;
        } else {
            return ChangeType.UNKNOWN;
        }
    }
    
    /**
     * 检查是否为新增配置
     * @return 是否为新增
     */
    public boolean isAdded() {
        return getChangeType() == ChangeType.ADDED;
    }
    
    /**
     * 检查是否为删除配置
     * @return 是否为删除
     */
    public boolean isRemoved() {
        return getChangeType() == ChangeType.REMOVED;
    }
    
    /**
     * 检查是否为更新配置
     * @return 是否为更新
     */
    public boolean isUpdated() {
        return getChangeType() == ChangeType.UPDATED;
    }
    
    /**
     * 获取完整的配置键（包含命名空间）
     * @return 完整配置键
     */
    public String getFullConfigKey() {
        return namespace + ":" + configKey;
    }
    
    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        ADDED("新增"),
        UPDATED("更新"),
        REMOVED("删除"),
        UNKNOWN("未知");
        
        private final String description;
        
        ChangeType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return String.format("ConfigChangeEvent{namespace='%s', configKey='%s', changeType=%s, oldValue='%s', newValue='%s', timestamp=%d}",
                namespace, configKey, getChangeType().getDescription(), oldValue, newValue, timestamp);
    }
}