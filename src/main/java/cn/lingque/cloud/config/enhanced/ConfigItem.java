package cn.lingque.cloud.config.enhanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置项
 * 包含配置值、过期时间和版本信息
 * 
 * @author LingQue Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItem {
    
    /**
     * 配置值
     */
    private String value;
    
    /**
     * 过期时间戳（毫秒）
     */
    private long expireTime;
    
    /**
     * 配置版本号
     */
    private long version;
    
    /**
     * 创建时间戳
     */
    private long createTime;
    
    /**
     * 构造函数
     * @param value 配置值
     * @param expireTime 过期时间
     * @param version 版本号
     */
    public ConfigItem(String value, long expireTime, long version) {
        this.value = value;
        this.expireTime = expireTime;
        this.version = version;
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 检查配置是否过期
     * @return 是否过期
     */
    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }
    
    /**
     * 检查配置是否过期
     * @param currentTime 当前时间戳
     * @return 是否过期
     */
    public boolean isExpired(long currentTime) {
        return currentTime > expireTime;
    }
    
    /**
     * 获取剩余TTL（毫秒）
     * @return 剩余TTL，如果已过期返回0
     */
    public long getRemainingTtl() {
        long remaining = expireTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * 刷新过期时间
     * @param ttlMs TTL（毫秒）
     */
    public void refreshExpireTime(long ttlMs) {
        this.expireTime = System.currentTimeMillis() + ttlMs;
    }
    
    @Override
    public String toString() {
        return String.format("ConfigItem{value='%s', version=%d, expireTime=%d, remainingTtl=%d}", 
                value, version, expireTime, getRemainingTtl());
    }
}