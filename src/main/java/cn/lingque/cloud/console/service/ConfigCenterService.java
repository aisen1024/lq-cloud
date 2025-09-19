package cn.lingque.cloud.console.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置中心服务
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Service
public class ConfigCenterService {
    
    private final Map<String, ConfigItem> configItems = new ConcurrentHashMap<>();
    private final Map<String, List<ConfigItem>> configGroups = new ConcurrentHashMap<>();
    private final Map<String, List<ConfigHistory>> configHistories = new ConcurrentHashMap<>();
    private final AtomicLong configIdGenerator = new AtomicLong(1);
    
    public ConfigCenterService() {
        // 初始化示例配置
        initializeSampleConfigs();
    }
    
    /**
     * 配置项
     */
    public static class ConfigItem {
        private String configId;
        private String key;
        private String value;
        private String group;
        private String namespace;
        private String dataType; // "string", "number", "boolean", "json", "yaml", "properties"
        private String description;
        private String environment; // "dev", "test", "staging", "prod"
        private boolean encrypted;
        private String tags;
        private Date createTime;
        private Date updateTime;
        private String createdBy;
        private String updatedBy;
        private int version;
        private boolean active;
        private ConfigValidation validation;
        
        public ConfigItem() {
            this.createTime = new Date();
            this.updateTime = new Date();
            this.version = 1;
            this.active = true;
            this.encrypted = false;
            this.validation = new ConfigValidation();
        }
        
        // Getters and Setters
        public String getConfigId() { return configId; }
        public void setConfigId(String configId) { this.configId = configId; }
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
        
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        
        public Date getUpdateTime() { return updateTime; }
        public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
        
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public ConfigValidation getValidation() { return validation; }
        public void setValidation(ConfigValidation validation) { this.validation = validation; }
        
        public String getFullKey() {
            return namespace + "." + group + "." + key;
        }
    }
    
    /**
     * 配置验证规则
     */
    public static class ConfigValidation {
        private boolean required;
        private String pattern;
        private String minValue;
        private String maxValue;
        private List<String> allowedValues;
        private String validationMessage;
        
        public ConfigValidation() {
            this.required = false;
            this.allowedValues = new ArrayList<>();
        }
        
        // Getters and Setters
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public String getMinValue() { return minValue; }
        public void setMinValue(String minValue) { this.minValue = minValue; }
        
        public String getMaxValue() { return maxValue; }
        public void setMaxValue(String maxValue) { this.maxValue = maxValue; }
        
        public List<String> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
        
        public String getValidationMessage() { return validationMessage; }
        public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
    }
    
    /**
     * 配置历史记录
     */
    public static class ConfigHistory {
        private String historyId;
        private String configId;
        private String oldValue;
        private String newValue;
        private String operation; // "create", "update", "delete", "rollback"
        private String operatedBy;
        private Date operationTime;
        private String reason;
        private int version;
        
        public ConfigHistory() {
            this.operationTime = new Date();
        }
        
        // Getters and Setters
        public String getHistoryId() { return historyId; }
        public void setHistoryId(String historyId) { this.historyId = historyId; }
        
        public String getConfigId() { return configId; }
        public void setConfigId(String configId) { this.configId = configId; }
        
        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }
        
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public String getOperatedBy() { return operatedBy; }
        public void setOperatedBy(String operatedBy) { this.operatedBy = operatedBy; }
        
        public Date getOperationTime() { return operationTime; }
        public void setOperationTime(Date operationTime) { this.operationTime = operationTime; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
    }
    
    /**
     * 获取所有配置项
     */
    public List<ConfigItem> getAllConfigs() {
        return new ArrayList<>(configItems.values());
    }
    
    /**
     * 根据组获取配置项
     */
    public List<ConfigItem> getConfigsByGroup(String group) {
        return configGroups.getOrDefault(group, new ArrayList<>());
    }
    
    /**
     * 根据命名空间和环境获取配置项
     */
    public List<ConfigItem> getConfigsByNamespaceAndEnv(String namespace, String environment) {
        return configItems.values().stream()
                .filter(config -> namespace.equals(config.getNamespace()) && 
                                environment.equals(config.getEnvironment()) && 
                                config.isActive())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 根据配置ID获取配置项
     */
    public ConfigItem getConfigById(String configId) {
        return configItems.get(configId);
    }
    
    /**
     * 根据完整键获取配置项
     */
    public ConfigItem getConfigByFullKey(String namespace, String group, String key, String environment) {
        return configItems.values().stream()
                .filter(config -> namespace.equals(config.getNamespace()) &&
                                group.equals(config.getGroup()) &&
                                key.equals(config.getKey()) &&
                                environment.equals(config.getEnvironment()) &&
                                config.isActive())
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 创建配置项
     */
    public ConfigItem createConfig(String namespace, String group, String key, String value,
                                 String dataType, String environment, String description,
                                 String createdBy) {
        // 检查是否已存在
        ConfigItem existing = getConfigByFullKey(namespace, group, key, environment);
        if (existing != null) {
            throw new RuntimeException("配置项已存在: " + existing.getFullKey());
        }
        
        ConfigItem config = new ConfigItem();
        config.setConfigId("config_" + configIdGenerator.getAndIncrement());
        config.setNamespace(namespace);
        config.setGroup(group);
        config.setKey(key);
        config.setValue(value);
        config.setDataType(dataType);
        config.setEnvironment(environment);
        config.setDescription(description);
        config.setCreatedBy(createdBy);
        config.setUpdatedBy(createdBy);
        
        // 验证配置值
        validateConfigValue(config);
        
        // 保存配置
        configItems.put(config.getConfigId(), config);
        configGroups.computeIfAbsent(group, k -> new ArrayList<>()).add(config);
        
        // 记录历史
        addConfigHistory(config.getConfigId(), null, value, "create", createdBy, "创建配置项", 1);
        
        return config;
    }
    
    /**
     * 更新配置项
     */
    public ConfigItem updateConfig(String configId, String value, String updatedBy, String reason) {
        ConfigItem config = configItems.get(configId);
        if (config == null) {
            throw new RuntimeException("配置项不存在: " + configId);
        }
        
        String oldValue = config.getValue();
        config.setValue(value);
        config.setUpdateTime(new Date());
        config.setUpdatedBy(updatedBy);
        config.setVersion(config.getVersion() + 1);
        
        // 验证配置值
        validateConfigValue(config);
        
        // 记录历史
        addConfigHistory(configId, oldValue, value, "update", updatedBy, reason, config.getVersion());
        
        return config;
    }
    
    /**
     * 删除配置项
     */
    public boolean deleteConfig(String configId, String deletedBy, String reason) {
        ConfigItem config = configItems.get(configId);
        if (config == null) {
            return false;
        }
        
        // 软删除
        config.setActive(false);
        config.setUpdateTime(new Date());
        config.setUpdatedBy(deletedBy);
        
        // 从组中移除
        List<ConfigItem> groupConfigs = configGroups.get(config.getGroup());
        if (groupConfigs != null) {
            groupConfigs.removeIf(c -> c.getConfigId().equals(configId));
        }
        
        // 记录历史
        addConfigHistory(configId, config.getValue(), null, "delete", deletedBy, reason, config.getVersion());
        
        return true;
    }
    
    /**
     * 回滚配置
     */
    public ConfigItem rollbackConfig(String configId, int targetVersion, String operatedBy, String reason) {
        ConfigItem config = configItems.get(configId);
        if (config == null) {
            throw new RuntimeException("配置项不存在: " + configId);
        }
        
        // 查找目标版本的历史记录
        List<ConfigHistory> histories = configHistories.get(configId);
        if (histories == null) {
            throw new RuntimeException("未找到配置历史记录");
        }
        
        ConfigHistory targetHistory = histories.stream()
                .filter(h -> h.getVersion() == targetVersion)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到目标版本: " + targetVersion));
        
        String oldValue = config.getValue();
        String newValue = targetHistory.getNewValue();
        
        config.setValue(newValue);
        config.setUpdateTime(new Date());
        config.setUpdatedBy(operatedBy);
        config.setVersion(config.getVersion() + 1);
        
        // 记录历史
        addConfigHistory(configId, oldValue, newValue, "rollback", operatedBy, 
                        reason + " (回滚到版本 " + targetVersion + ")", config.getVersion());
        
        return config;
    }
    
    /**
     * 批量更新配置
     */
    public List<ConfigItem> batchUpdateConfigs(Map<String, String> configUpdates, String updatedBy, String reason) {
        List<ConfigItem> updatedConfigs = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : configUpdates.entrySet()) {
            try {
                ConfigItem updated = updateConfig(entry.getKey(), entry.getValue(), updatedBy, reason);
                updatedConfigs.add(updated);
            } catch (Exception e) {
                // 记录错误但继续处理其他配置
                System.err.println("更新配置失败: " + entry.getKey() + ", 错误: " + e.getMessage());
            }
        }
        
        return updatedConfigs;
    }
    
    /**
     * 获取配置历史
     */
    public List<ConfigHistory> getConfigHistory(String configId) {
        return configHistories.getOrDefault(configId, new ArrayList<>());
    }
    
    /**
     * 搜索配置
     */
    public List<ConfigItem> searchConfigs(String keyword, String namespace, String group, String environment) {
        return configItems.values().stream()
                .filter(config -> {
                    if (!config.isActive()) return false;
                    if (namespace != null && !namespace.equals(config.getNamespace())) return false;
                    if (group != null && !group.equals(config.getGroup())) return false;
                    if (environment != null && !environment.equals(config.getEnvironment())) return false;
                    if (keyword != null && !keyword.isEmpty()) {
                        return config.getKey().toLowerCase().contains(keyword.toLowerCase()) ||
                               config.getValue().toLowerCase().contains(keyword.toLowerCase()) ||
                               (config.getDescription() != null && config.getDescription().toLowerCase().contains(keyword.toLowerCase()));
                    }
                    return true;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 获取配置统计信息
     */
    public Map<String, Object> getConfigStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalConfigs = configItems.values().stream().mapToLong(c -> c.isActive() ? 1 : 0).sum();
        long totalGroups = configGroups.size();
        
        Map<String, Long> envStats = new HashMap<>();
        Map<String, Long> typeStats = new HashMap<>();
        Map<String, Long> namespaceStats = new HashMap<>();
        
        for (ConfigItem config : configItems.values()) {
            if (!config.isActive()) continue;
            
            envStats.merge(config.getEnvironment(), 1L, Long::sum);
            typeStats.merge(config.getDataType(), 1L, Long::sum);
            namespaceStats.merge(config.getNamespace(), 1L, Long::sum);
        }
        
        stats.put("totalConfigs", totalConfigs);
        stats.put("totalGroups", totalGroups);
        stats.put("environmentStats", envStats);
        stats.put("dataTypeStats", typeStats);
        stats.put("namespaceStats", namespaceStats);
        stats.put("lastUpdateTime", new Date());
        
        return stats;
    }
    
    /**
     * 验证配置值
     */
    private void validateConfigValue(ConfigItem config) {
        ConfigValidation validation = config.getValidation();
        if (validation == null) return;
        
        String value = config.getValue();
        
        // 必填验证
        if (validation.isRequired() && (value == null || value.trim().isEmpty())) {
            throw new RuntimeException("配置值不能为空: " + config.getKey());
        }
        
        if (value == null) return;
        
        // 正则验证
        if (validation.getPattern() != null && !value.matches(validation.getPattern())) {
            throw new RuntimeException("配置值格式不正确: " + config.getKey());
        }
        
        // 允许值验证
        if (!validation.getAllowedValues().isEmpty() && !validation.getAllowedValues().contains(value)) {
            throw new RuntimeException("配置值不在允许范围内: " + config.getKey());
        }
        
        // 数值范围验证
        if ("number".equals(config.getDataType())) {
            try {
                double numValue = Double.parseDouble(value);
                if (validation.getMinValue() != null) {
                    double minValue = Double.parseDouble(validation.getMinValue());
                    if (numValue < minValue) {
                        throw new RuntimeException("配置值小于最小值: " + config.getKey());
                    }
                }
                if (validation.getMaxValue() != null) {
                    double maxValue = Double.parseDouble(validation.getMaxValue());
                    if (numValue > maxValue) {
                        throw new RuntimeException("配置值大于最大值: " + config.getKey());
                    }
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("配置值不是有效数字: " + config.getKey());
            }
        }
    }
    
    /**
     * 添加配置历史记录
     */
    private void addConfigHistory(String configId, String oldValue, String newValue, 
                                String operation, String operatedBy, String reason, int version) {
        ConfigHistory history = new ConfigHistory();
        history.setHistoryId("history_" + System.currentTimeMillis());
        history.setConfigId(configId);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setOperation(operation);
        history.setOperatedBy(operatedBy);
        history.setReason(reason);
        history.setVersion(version);
        
        configHistories.computeIfAbsent(configId, k -> new ArrayList<>()).add(history);
    }
    
    /**
     * 初始化示例配置
     */
    private void initializeSampleConfigs() {
        // 数据库配置
        createConfig("application", "database", "url", 
                    "jdbc:mysql://localhost:3306/lqcloud", "string", "dev", 
                    "数据库连接URL", "system");
        createConfig("application", "database", "username", 
                    "root", "string", "dev", 
                    "数据库用户名", "system");
        createConfig("application", "database", "pool.max-size", 
                    "20", "number", "dev", 
                    "数据库连接池最大大小", "system");
        
        // Redis配置
        createConfig("application", "redis", "host", 
                    "localhost", "string", "dev", 
                    "Redis服务器地址", "system");
        createConfig("application", "redis", "port", 
                    "6379", "number", "dev", 
                    "Redis服务器端口", "system");
        createConfig("application", "redis", "timeout", 
                    "3000", "number", "dev", 
                    "Redis连接超时时间(毫秒)", "system");
        
        // 应用配置
        createConfig("application", "app", "name", 
                    "LingQue Cloud", "string", "dev", 
                    "应用名称", "system");
        createConfig("application", "app", "version", 
                    "1.0.0", "string", "dev", 
                    "应用版本", "system");
        createConfig("application", "app", "debug", 
                    "true", "boolean", "dev", 
                    "是否开启调试模式", "system");
        
        // 日志配置
        createConfig("application", "logging", "level.root", 
                    "INFO", "string", "dev", 
                    "根日志级别", "system");
        createConfig("application", "logging", "level.cn.lingque", 
                    "DEBUG", "string", "dev", 
                    "应用日志级别", "system");
        
        // MCP工具配置
        createConfig("mcp", "tools", "enabled", 
                    "true", "boolean", "dev", 
                    "是否启用MCP工具", "system");
        createConfig("mcp", "tools", "timeout", 
                    "30000", "number", "dev", 
                    "MCP工具超时时间(毫秒)", "system");
        
        // 生产环境配置
        createConfig("application", "database", "url", 
                    "jdbc:mysql://prod-db:3306/lqcloud", "string", "prod", 
                    "生产环境数据库连接URL", "system");
        createConfig("application", "app", "debug", 
                    "false", "boolean", "prod", 
                    "生产环境调试模式", "system");
        createConfig("application", "logging", "level.root", 
                    "WARN", "string", "prod", 
                    "生产环境根日志级别", "system");
    }
}