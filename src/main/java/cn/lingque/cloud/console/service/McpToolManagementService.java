package cn.lingque.cloud.console.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP工具管理服务
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Service
public class McpToolManagementService {
    
    private final Map<String, McpTool> mcpTools = new ConcurrentHashMap<>();
    private final Map<String, List<McpTool>> toolCategories = new ConcurrentHashMap<>();
    private final Map<String, List<ToolExecutionLog>> executionLogs = new ConcurrentHashMap<>();
    private final AtomicLong toolIdGenerator = new AtomicLong(1);
    
    public McpToolManagementService() {
        // 初始化示例工具
        initializeSampleTools();
    }
    
    /**
     * MCP工具
     */
    public static class McpTool {
        private String toolId;
        private String name;
        private String displayName;
        private String description;
        private String category;
        private String version;
        private String author;
        private boolean enabled;
        private String status; // "active", "inactive", "error", "loading"
        private Date createTime;
        private Date updateTime;
        private ToolConfiguration configuration;
        private ToolMetrics metrics;
        private List<ToolParameter> parameters;
        private List<String> tags;
        private String iconUrl;
        private String documentationUrl;
        private Map<String, Object> metadata;
        
        public McpTool() {
            this.createTime = new Date();
            this.updateTime = new Date();
            this.enabled = true;
            this.status = "active";
            this.configuration = new ToolConfiguration();
            this.metrics = new ToolMetrics();
            this.parameters = new ArrayList<>();
            this.tags = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        // Getters and Setters
        public String getToolId() { return toolId; }
        public void setToolId(String toolId) { this.toolId = toolId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        
        public Date getUpdateTime() { return updateTime; }
        public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
        
        public ToolConfiguration getConfiguration() { return configuration; }
        public void setConfiguration(ToolConfiguration configuration) { this.configuration = configuration; }
        
        public ToolMetrics getMetrics() { return metrics; }
        public void setMetrics(ToolMetrics metrics) { this.metrics = metrics; }
        
        public List<ToolParameter> getParameters() { return parameters; }
        public void setParameters(List<ToolParameter> parameters) { this.parameters = parameters; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public String getIconUrl() { return iconUrl; }
        public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
        
        public String getDocumentationUrl() { return documentationUrl; }
        public void setDocumentationUrl(String documentationUrl) { this.documentationUrl = documentationUrl; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 工具配置
     */
    public static class ToolConfiguration {
        private int timeout;
        private int maxRetries;
        private boolean asyncExecution;
        private Map<String, String> environment;
        private List<String> dependencies;
        private Map<String, Object> settings;
        
        public ToolConfiguration() {
            this.timeout = 30000;
            this.maxRetries = 3;
            this.asyncExecution = false;
            this.environment = new HashMap<>();
            this.dependencies = new ArrayList<>();
            this.settings = new HashMap<>();
        }
        
        // Getters and Setters
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public boolean isAsyncExecution() { return asyncExecution; }
        public void setAsyncExecution(boolean asyncExecution) { this.asyncExecution = asyncExecution; }
        
        public Map<String, String> getEnvironment() { return environment; }
        public void setEnvironment(Map<String, String> environment) { this.environment = environment; }
        
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        
        public Map<String, Object> getSettings() { return settings; }
        public void setSettings(Map<String, Object> settings) { this.settings = settings; }
    }
    
    /**
     * 工具指标
     */
    public static class ToolMetrics {
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private double avgExecutionTime;
        private double successRate;
        private Date lastExecutionTime;
        private long totalExecutionTime;
        private Map<String, Long> errorCounts;
        
        public ToolMetrics() {
            this.errorCounts = new HashMap<>();
        }
        
        // Getters and Setters
        public long getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(long totalExecutions) { this.totalExecutions = totalExecutions; }
        
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public void setSuccessfulExecutions(long successfulExecutions) { this.successfulExecutions = successfulExecutions; }
        
        public long getFailedExecutions() { return failedExecutions; }
        public void setFailedExecutions(long failedExecutions) { this.failedExecutions = failedExecutions; }
        
        public double getAvgExecutionTime() { return avgExecutionTime; }
        public void setAvgExecutionTime(double avgExecutionTime) { this.avgExecutionTime = avgExecutionTime; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public Date getLastExecutionTime() { return lastExecutionTime; }
        public void setLastExecutionTime(Date lastExecutionTime) { this.lastExecutionTime = lastExecutionTime; }
        
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public void setTotalExecutionTime(long totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }
        
        public Map<String, Long> getErrorCounts() { return errorCounts; }
        public void setErrorCounts(Map<String, Long> errorCounts) { this.errorCounts = errorCounts; }
        
        public void updateMetrics(boolean success, long executionTime) {
            this.totalExecutions++;
            this.totalExecutionTime += executionTime;
            this.lastExecutionTime = new Date();
            
            if (success) {
                this.successfulExecutions++;
            } else {
                this.failedExecutions++;
            }
            
            this.successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0;
            this.avgExecutionTime = totalExecutions > 0 ? (double) totalExecutionTime / totalExecutions : 0;
        }
    }
    
    /**
     * 工具参数
     */
    public static class ToolParameter {
        private String name;
        private String type; // "string", "number", "boolean", "object", "array"
        private String description;
        private boolean required;
        private Object defaultValue;
        private List<Object> allowedValues;
        private String pattern;
        private Object minValue;
        private Object maxValue;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        
        public List<Object> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<Object> allowedValues) { this.allowedValues = allowedValues; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public Object getMinValue() { return minValue; }
        public void setMinValue(Object minValue) { this.minValue = minValue; }
        
        public Object getMaxValue() { return maxValue; }
        public void setMaxValue(Object maxValue) { this.maxValue = maxValue; }
    }
    
    /**
     * 工具执行日志
     */
    public static class ToolExecutionLog {
        private String logId;
        private String toolId;
        private String executionId;
        private Date startTime;
        private Date endTime;
        private long duration;
        private boolean success;
        private String status; // "running", "success", "failed", "timeout"
        private Map<String, Object> inputParameters;
        private Object result;
        private String errorMessage;
        private String stackTrace;
        private String executedBy;
        
        public ToolExecutionLog() {
            this.startTime = new Date();
            this.inputParameters = new HashMap<>();
        }
        
        // Getters and Setters
        public String getLogId() { return logId; }
        public void setLogId(String logId) { this.logId = logId; }
        
        public String getToolId() { return toolId; }
        public void setToolId(String toolId) { this.toolId = toolId; }
        
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Map<String, Object> getInputParameters() { return inputParameters; }
        public void setInputParameters(Map<String, Object> inputParameters) { this.inputParameters = inputParameters; }
        
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
        
        public String getExecutedBy() { return executedBy; }
        public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    }
    
    /**
     * 获取所有MCP工具
     */
    public List<McpTool> getAllTools() {
        return new ArrayList<>(mcpTools.values());
    }
    
    /**
     * 根据分类获取工具
     */
    public List<McpTool> getToolsByCategory(String category) {
        return toolCategories.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * 根据ID获取工具
     */
    public McpTool getToolById(String toolId) {
        return mcpTools.get(toolId);
    }
    
    /**
     * 根据名称获取工具
     */
    public McpTool getToolByName(String name) {
        return mcpTools.values().stream()
                .filter(tool -> name.equals(tool.getName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 注册新工具
     */
    public McpTool registerTool(String name, String displayName, String description, 
                              String category, String version, String author) {
        // 检查工具名是否已存在
        if (getToolByName(name) != null) {
            throw new RuntimeException("工具名已存在: " + name);
        }
        
        McpTool tool = new McpTool();
        tool.setToolId("tool_" + toolIdGenerator.getAndIncrement());
        tool.setName(name);
        tool.setDisplayName(displayName);
        tool.setDescription(description);
        tool.setCategory(category);
        tool.setVersion(version);
        tool.setAuthor(author);
        
        // 保存工具
        mcpTools.put(tool.getToolId(), tool);
        toolCategories.computeIfAbsent(category, k -> new ArrayList<>()).add(tool);
        
        return tool;
    }
    
    /**
     * 更新工具信息
     */
    public McpTool updateTool(String toolId, String displayName, String description, 
                            String version, boolean enabled) {
        McpTool tool = mcpTools.get(toolId);
        if (tool == null) {
            throw new RuntimeException("工具不存在: " + toolId);
        }
        
        if (displayName != null) tool.setDisplayName(displayName);
        if (description != null) tool.setDescription(description);
        if (version != null) tool.setVersion(version);
        tool.setEnabled(enabled);
        tool.setUpdateTime(new Date());
        
        return tool;
    }
    
    /**
     * 删除工具
     */
    public boolean deleteTool(String toolId) {
        McpTool tool = mcpTools.remove(toolId);
        if (tool != null) {
            // 从分类中移除
            List<McpTool> categoryTools = toolCategories.get(tool.getCategory());
            if (categoryTools != null) {
                categoryTools.removeIf(t -> t.getToolId().equals(toolId));
                if (categoryTools.isEmpty()) {
                    toolCategories.remove(tool.getCategory());
                }
            }
            
            // 清理执行日志
            executionLogs.remove(toolId);
            
            return true;
        }
        return false;
    }
    
    /**
     * 启用/禁用工具
     */
    public boolean toggleToolStatus(String toolId, boolean enabled) {
        McpTool tool = mcpTools.get(toolId);
        if (tool != null) {
            tool.setEnabled(enabled);
            tool.setStatus(enabled ? "active" : "inactive");
            tool.setUpdateTime(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * 更新工具配置
     */
    public boolean updateToolConfiguration(String toolId, ToolConfiguration configuration) {
        McpTool tool = mcpTools.get(toolId);
        if (tool != null) {
            tool.setConfiguration(configuration);
            tool.setUpdateTime(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * 记录工具执行
     */
    public ToolExecutionLog logToolExecution(String toolId, String executionId, 
                                           Map<String, Object> inputParameters, 
                                           String executedBy) {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setLogId("log_" + System.currentTimeMillis());
        log.setToolId(toolId);
        log.setExecutionId(executionId);
        log.setInputParameters(inputParameters);
        log.setExecutedBy(executedBy);
        log.setStatus("running");
        
        executionLogs.computeIfAbsent(toolId, k -> new ArrayList<>()).add(log);
        
        return log;
    }
    
    /**
     * 完成工具执行记录
     */
    public boolean completeToolExecution(String toolId, String logId, boolean success, 
                                       Object result, String errorMessage, String stackTrace) {
        List<ToolExecutionLog> logs = executionLogs.get(toolId);
        if (logs != null) {
            ToolExecutionLog log = logs.stream()
                    .filter(l -> l.getLogId().equals(logId))
                    .findFirst()
                    .orElse(null);
            
            if (log != null) {
                log.setEndTime(new Date());
                log.setDuration(log.getEndTime().getTime() - log.getStartTime().getTime());
                log.setSuccess(success);
                log.setStatus(success ? "success" : "failed");
                log.setResult(result);
                log.setErrorMessage(errorMessage);
                log.setStackTrace(stackTrace);
                
                // 更新工具指标
                McpTool tool = mcpTools.get(toolId);
                if (tool != null) {
                    tool.getMetrics().updateMetrics(success, log.getDuration());
                    if (!success && errorMessage != null) {
                        tool.getMetrics().getErrorCounts().merge(errorMessage, 1L, Long::sum);
                    }
                }
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取工具执行日志
     */
    public List<ToolExecutionLog> getToolExecutionLogs(String toolId, int limit) {
        List<ToolExecutionLog> logs = executionLogs.getOrDefault(toolId, new ArrayList<>());
        
        // 按时间倒序排序
        logs.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));
        
        return logs.size() > limit ? logs.subList(0, limit) : logs;
    }
    
    /**
     * 搜索工具
     */
    public List<McpTool> searchTools(String keyword, String category, boolean enabledOnly) {
        return mcpTools.values().stream()
                .filter(tool -> {
                    if (enabledOnly && !tool.isEnabled()) return false;
                    if (category != null && !category.equals(tool.getCategory())) return false;
                    if (keyword != null && !keyword.isEmpty()) {
                        return tool.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                               tool.getDisplayName().toLowerCase().contains(keyword.toLowerCase()) ||
                               tool.getDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                               tool.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(keyword.toLowerCase()));
                    }
                    return true;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 获取工具统计信息
     */
    public Map<String, Object> getToolStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalTools = mcpTools.size();
        long enabledTools = mcpTools.values().stream().mapToLong(t -> t.isEnabled() ? 1 : 0).sum();
        long activeTools = mcpTools.values().stream().mapToLong(t -> "active".equals(t.getStatus()) ? 1 : 0).sum();
        
        Map<String, Long> categoryStats = new HashMap<>();
        Map<String, Long> statusStats = new HashMap<>();
        
        for (McpTool tool : mcpTools.values()) {
            categoryStats.merge(tool.getCategory(), 1L, Long::sum);
            statusStats.merge(tool.getStatus(), 1L, Long::sum);
        }
        
        stats.put("totalTools", totalTools);
        stats.put("enabledTools", enabledTools);
        stats.put("disabledTools", totalTools - enabledTools);
        stats.put("activeTools", activeTools);
        stats.put("categoryStats", categoryStats);
        stats.put("statusStats", statusStats);
        
        return stats;
    }
    
    /**
     * 获取工具性能信息
     */
    public Map<String, Object> getToolPerformance(String toolId) {
        Map<String, Object> performance = new HashMap<>();
        
        try {
            McpTool tool = mcpTools.get(toolId);
            if (tool == null) {
                performance.put("error", "工具不存在: " + toolId);
                performance.put("found", false);
                return performance;
            }
            
            ToolMetrics metrics = tool.getMetrics();
            List<ToolExecutionLog> recentLogs = getToolExecutionLogs(toolId, 10);
            
            // 基本性能指标
            performance.put("toolId", toolId);
            performance.put("toolName", tool.getName());
            performance.put("found", true);
            performance.put("enabled", tool.isEnabled());
            performance.put("status", tool.getStatus());
            
            // 执行统计
            performance.put("totalExecutions", metrics.getTotalExecutions());
            performance.put("successfulExecutions", metrics.getSuccessfulExecutions());
            performance.put("failedExecutions", metrics.getFailedExecutions());
            performance.put("successRate", metrics.getSuccessRate());
            performance.put("avgExecutionTime", metrics.getAvgExecutionTime());
            performance.put("totalExecutionTime", metrics.getTotalExecutionTime());
            performance.put("lastExecutionTime", metrics.getLastExecutionTime());
            
            // 错误统计
            performance.put("errorCounts", metrics.getErrorCounts());
            
            // 最近执行记录分析
            if (!recentLogs.isEmpty()) {
                // 计算最近执行的平均时间
                double recentAvgTime = recentLogs.stream()
                    .filter(log -> log.getDuration() > 0)
                    .mapToLong(ToolExecutionLog::getDuration)
                    .average()
                    .orElse(0.0);
                
                // 计算最近的成功率
                long recentSuccessCount = recentLogs.stream()
                    .mapToLong(log -> log.isSuccess() ? 1 : 0)
                    .sum();
                double recentSuccessRate = recentLogs.size() > 0 ? 
                    (double) recentSuccessCount / recentLogs.size() * 100 : 0;
                
                performance.put("recentAvgExecutionTime", recentAvgTime);
                performance.put("recentSuccessRate", recentSuccessRate);
                performance.put("recentExecutionCount", recentLogs.size());
                
                // 最近错误信息
                List<String> recentErrors = recentLogs.stream()
                    .filter(log -> !log.isSuccess() && log.getErrorMessage() != null)
                    .map(ToolExecutionLog::getErrorMessage)
                    .distinct()
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                performance.put("recentErrors", recentErrors);
            } else {
                performance.put("recentAvgExecutionTime", 0.0);
                performance.put("recentSuccessRate", 0.0);
                performance.put("recentExecutionCount", 0);
                performance.put("recentErrors", new ArrayList<>());
            }
            
            // 性能等级评估
            String performanceLevel = evaluatePerformanceLevel(metrics, recentLogs);
            performance.put("performanceLevel", performanceLevel);
            
            // 配置信息
            ToolConfiguration config = tool.getConfiguration();
            Map<String, Object> configInfo = new HashMap<>();
            configInfo.put("timeout", config.getTimeout());
            configInfo.put("maxRetries", config.getMaxRetries());
            configInfo.put("asyncExecution", config.isAsyncExecution());
            performance.put("configuration", configInfo);
            
            // 时间戳
            performance.put("timestamp", new Date());
            performance.put("lastUpdateTime", tool.getUpdateTime());
            
        } catch (Exception e) {
            performance.put("error", "获取性能信息时发生错误: " + e.getMessage());
            performance.put("found", false);
            performance.put("timestamp", new Date());
        }
        
        return performance;
    }
    
    /**
     * 评估工具性能等级
     */
    private String evaluatePerformanceLevel(ToolMetrics metrics, List<ToolExecutionLog> recentLogs) {
        // 基于成功率和执行时间评估性能等级
        double successRate = metrics.getSuccessRate();
        double avgTime = metrics.getAvgExecutionTime();
        
        if (successRate >= 95 && avgTime <= 1000) {
            return "优秀";
        } else if (successRate >= 90 && avgTime <= 3000) {
            return "良好";
        } else if (successRate >= 80 && avgTime <= 5000) {
            return "一般";
        } else if (successRate >= 60) {
            return "较差";
        } else {
            return "很差";
        }
    }
    
    /**
     * 获取分类列表
     */
    public List<String> getCategories() {
        return new ArrayList<>(toolCategories.keySet());
    }
    
    /**
     * 清理旧的执行日志
     */
    public void cleanOldExecutionLogs(long maxAgeMillis, int maxLogsPerTool) {
        Date cutoffTime = new Date(System.currentTimeMillis() - maxAgeMillis);
        
        for (List<ToolExecutionLog> logs : executionLogs.values()) {
            // 移除过期日志
            logs.removeIf(log -> log.getStartTime().before(cutoffTime));
            
            // 保留最新的日志
            if (logs.size() > maxLogsPerTool) {
                logs.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));
                logs.subList(maxLogsPerTool, logs.size()).clear();
            }
        }
    }
    
    /**
     * 初始化示例工具
     */
    private void initializeSampleTools() {
        // 文件操作工具
        McpTool fileTool = registerTool("file_operations", "文件操作工具", 
                "提供文件读写、复制、移动等操作功能", "文件处理", "1.0.0", "LingQue AI");
        fileTool.getTags().addAll(Arrays.asList("文件", "IO", "操作"));
        fileTool.getMetrics().setTotalExecutions(156);
        fileTool.getMetrics().setSuccessfulExecutions(152);
        fileTool.getMetrics().setFailedExecutions(4);
        fileTool.getMetrics().setAvgExecutionTime(245.6);
        fileTool.getMetrics().setSuccessRate(97.4);
        
        // 数据库查询工具
        McpTool dbTool = registerTool("database_query", "数据库查询工具", 
                "执行SQL查询和数据库操作", "数据库", "2.1.0", "LingQue AI");
        dbTool.getTags().addAll(Arrays.asList("数据库", "SQL", "查询"));
        dbTool.getMetrics().setTotalExecutions(89);
        dbTool.getMetrics().setSuccessfulExecutions(85);
        dbTool.getMetrics().setFailedExecutions(4);
        dbTool.getMetrics().setAvgExecutionTime(1250.3);
        dbTool.getMetrics().setSuccessRate(95.5);
        
        // HTTP请求工具
        McpTool httpTool = registerTool("http_client", "HTTP客户端工具", 
                "发送HTTP请求，支持GET、POST、PUT、DELETE等方法", "网络", "1.5.0", "LingQue AI");
        httpTool.getTags().addAll(Arrays.asList("HTTP", "网络", "API"));
        httpTool.getMetrics().setTotalExecutions(234);
        httpTool.getMetrics().setSuccessfulExecutions(228);
        httpTool.getMetrics().setFailedExecutions(6);
        httpTool.getMetrics().setAvgExecutionTime(856.7);
        httpTool.getMetrics().setSuccessRate(97.4);
        
        // 文本处理工具
        McpTool textTool = registerTool("text_processor", "文本处理工具", 
                "提供文本解析、格式化、转换等功能", "文本处理", "1.2.0", "LingQue AI");
        textTool.getTags().addAll(Arrays.asList("文本", "解析", "格式化"));
        textTool.getMetrics().setTotalExecutions(67);
        textTool.getMetrics().setSuccessfulExecutions(66);
        textTool.getMetrics().setFailedExecutions(1);
        textTool.getMetrics().setAvgExecutionTime(123.4);
        textTool.getMetrics().setSuccessRate(98.5);
        
        // 图像处理工具
        McpTool imageTool = registerTool("image_processor", "图像处理工具", 
                "图像缩放、裁剪、格式转换等操作", "图像处理", "1.0.0", "LingQue AI");
        imageTool.getTags().addAll(Arrays.asList("图像", "处理", "转换"));
        imageTool.getMetrics().setTotalExecutions(45);
        imageTool.getMetrics().setSuccessfulExecutions(43);
        imageTool.getMetrics().setFailedExecutions(2);
        imageTool.getMetrics().setAvgExecutionTime(2340.1);
        imageTool.getMetrics().setSuccessRate(95.6);
        
        // 邮件发送工具
        McpTool emailTool = registerTool("email_sender", "邮件发送工具", 
                "发送邮件通知和消息", "通信", "1.1.0", "LingQue AI");
        emailTool.getTags().addAll(Arrays.asList("邮件", "通知", "发送"));
        emailTool.getMetrics().setTotalExecutions(123);
        emailTool.getMetrics().setSuccessfulExecutions(119);
        emailTool.getMetrics().setFailedExecutions(4);
        emailTool.getMetrics().setAvgExecutionTime(1890.5);
        emailTool.getMetrics().setSuccessRate(96.7);
    }
}