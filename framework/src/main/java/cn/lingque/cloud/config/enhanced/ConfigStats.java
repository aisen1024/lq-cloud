package cn.lingque.cloud.config.enhanced;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置统计信息
 * 提供配置中心的运行状态和统计数据
 * 
 * @author LingQue Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigStats {
    
    /**
     * 命名空间数量
     */
    private int namespaceCount;
    
    /**
     * 总配置数量
     */
    private int totalConfigCount;
    
    /**
     * 过期配置数量
     */
    private int expiredConfigCount;
    
    /**
     * 监听器数量
     */
    private int listenerCount;
    
    /**
     * 统计生成时间
     */
    private long timestamp;
    
    /**
     * 构造函数
     * @param namespaceCount 命名空间数量
     * @param totalConfigCount 总配置数量
     * @param expiredConfigCount 过期配置数量
     * @param listenerCount 监听器数量
     */
    public ConfigStats(int namespaceCount, int totalConfigCount, int expiredConfigCount, int listenerCount) {
        this.namespaceCount = namespaceCount;
        this.totalConfigCount = totalConfigCount;
        this.expiredConfigCount = expiredConfigCount;
        this.listenerCount = listenerCount;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取有效配置数量
     * @return 有效配置数量
     */
    public int getValidConfigCount() {
        return totalConfigCount - expiredConfigCount;
    }
    
    /**
     * 获取过期配置比例
     * @return 过期配置比例（0-1之间）
     */
    public double getExpiredRatio() {
        if (totalConfigCount == 0) {
            return 0.0;
        }
        return (double) expiredConfigCount / totalConfigCount;
    }
    
    /**
     * 获取平均每个命名空间的配置数量
     * @return 平均配置数量
     */
    public double getAvgConfigPerNamespace() {
        if (namespaceCount == 0) {
            return 0.0;
        }
        return (double) totalConfigCount / namespaceCount;
    }
    
    /**
     * 检查是否需要清理过期配置
     * @return 是否需要清理
     */
    public boolean needsCleanup() {
        return expiredConfigCount > 0 && getExpiredRatio() > 0.1; // 超过10%过期配置时建议清理
    }
    
    /**
     * 获取健康状态
     * @return 健康状态
     */
    public HealthStatus getHealthStatus() {
        double expiredRatio = getExpiredRatio();
        
        if (expiredRatio > 0.3) {
            return HealthStatus.UNHEALTHY;
        } else if (expiredRatio > 0.1) {
            return HealthStatus.WARNING;
        } else {
            return HealthStatus.HEALTHY;
        }
    }
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY("健康", "green"),
        WARNING("警告", "yellow"),
        UNHEALTHY("不健康", "red");
        
        private final String description;
        private final String color;
        
        HealthStatus(String description, String color) {
            this.description = description;
            this.color = color;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    /**
     * 生成统计报告
     * @return 统计报告字符串
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== LQ配置中心统计报告 ===").append("\n");
        report.append("生成时间: ").append(new java.util.Date(timestamp)).append("\n");
        report.append("命名空间数量: ").append(namespaceCount).append("\n");
        report.append("总配置数量: ").append(totalConfigCount).append("\n");
        report.append("有效配置数量: ").append(getValidConfigCount()).append("\n");
        report.append("过期配置数量: ").append(expiredConfigCount).append("\n");
        report.append("过期配置比例: ").append(String.format("%.2f%%", getExpiredRatio() * 100)).append("\n");
        report.append("监听器数量: ").append(listenerCount).append("\n");
        report.append("平均每命名空间配置数: ").append(String.format("%.2f", getAvgConfigPerNamespace())).append("\n");
        report.append("健康状态: ").append(getHealthStatus().getDescription()).append("\n");
        
        if (needsCleanup()) {
            report.append("建议: 执行过期配置清理").append("\n");
        }
        
        report.append("=========================");
        return report.toString();
    }
    
    @Override
    public String toString() {
        return String.format("ConfigStats{namespaces=%d, total=%d, valid=%d, expired=%d, listeners=%d, health=%s}",
                namespaceCount, totalConfigCount, getValidConfigCount(), expiredConfigCount, 
                listenerCount, getHealthStatus().getDescription());
    }
}