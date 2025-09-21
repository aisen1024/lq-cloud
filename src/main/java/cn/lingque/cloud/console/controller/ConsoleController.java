package cn.lingque.cloud.console.controller;

import cn.lingque.cloud.console.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.*;
import java.util.*;

/**
 * 控制台REST控制器
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/console")
public class ConsoleController {
    
    @Autowired
    private ConsoleAuthService authService;
    
    @Autowired
    private QueueManagementService queueService;
    
    @Autowired
    private ServiceDiscoveryService serviceDiscoveryService;
    
    @Autowired
    private ConfigCenterService configCenterService;
    
    @Autowired
    private McpToolManagementService mcpToolService;
    
    @Autowired
    private QueueMetricsService queueMetricsService;
    
    // ==================== 认证相关 ====================
    
    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            
            ConsoleAuthService.AuthResult authResult = authService.authenticate(username, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", authResult.getToken());
            response.put("message", "登录成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String token) {
        try {
            authService.logout(token.replace("Bearer ", ""));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登出成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 验证令牌
     */
    @GetMapping("/auth/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestHeader("Authorization") String token) {
        try {
            boolean valid = authService.validateToken(token.replace("Bearer ", ""));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", valid);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ==================== 队列管理 ====================
    
    /**
     * 获取所有队列
     */
    @GetMapping("/queues")
    public ResponseEntity<Map<String, Object>> getAllQueues() {
        try {
            Map<String, List<QueueManagementService.QueueInfo>> allQueues = queueService.getAllQueues();
            List<QueueManagementService.QueueInfo> queues = new ArrayList<>();
            for (List<QueueManagementService.QueueInfo> queueList : allQueues.values()) {
                queues.addAll(queueList);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", queues);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取队列统计信息
     */
    @GetMapping("/queues/statistics")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        try {
            Map<String, Object> stats = queueService.getQueueStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建队列
     */
    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> createQueue(@RequestBody Map<String, Object> queueRequest) {
        try {
            String name = (String) queueRequest.get("name");
            String type = (String) queueRequest.get("type");
            String description = (String) queueRequest.get("description");
            
            Map<String, Object> config = new HashMap<>();
            config.put("description", description);
            QueueManagementService.QueueInfo queue = queueService.createQueue(name, type, config);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", queue);
            response.put("message", "队列创建成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除队列
     */
    @DeleteMapping("/queues/{queueId}")
    public ResponseEntity<Map<String, Object>> deleteQueue(@PathVariable String queueId) {
        try {
            boolean deleted = queueService.deleteQueue(queueId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "队列删除成功" : "队列不存在");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ==================== 服务发现 ====================
    
    /**
     * 获取所有服务
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getAllServices() {
        try {
            List<ServiceDiscoveryService.ServiceInstance> services = serviceDiscoveryService.getAllServices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", services);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取服务统计信息
     */
    @GetMapping("/services/statistics")
    public ResponseEntity<Map<String, Object>> getServiceStatistics() {
        try {
            Map<String, Object> stats = serviceDiscoveryService.getServiceStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 注册服务
     */
    @PostMapping("/services")
    public ResponseEntity<Map<String, Object>> registerService(@RequestBody Map<String, Object> serviceRequest) {
        try {
            String serviceName = (String) serviceRequest.get("serviceName");
            String host = (String) serviceRequest.get("host");
            Integer port = (Integer) serviceRequest.get("port");
            String version = (String) serviceRequest.get("version");
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) serviceRequest.get("metadata");
            
            ServiceDiscoveryService.ServiceInstance service = serviceDiscoveryService.registerService(
                    serviceName, host, port, version, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", service);
            response.put("message", "服务注册成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 注销服务
     */
    @DeleteMapping("/services/{instanceId}")
    public ResponseEntity<Map<String, Object>> deregisterService(@PathVariable String instanceId) {
        try {
            boolean deregistered = serviceDiscoveryService.deregisterService(instanceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deregistered);
            response.put("message", deregistered ? "服务注销成功" : "服务实例不存在");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ==================== 配置中心 ====================
    
    /**
     * 获取所有配置
     */
    @GetMapping("/configs")
    public ResponseEntity<Map<String, Object>> getAllConfigs(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String keyword) {
        try {
            List<ConfigCenterService.ConfigItem> configs;
            
            if (keyword != null && !keyword.isEmpty()) {
                configs = configCenterService.searchConfigs(keyword, namespace, group, environment);
            } else if (namespace != null && environment != null) {
                configs = configCenterService.getConfigsByNamespaceAndEnv(namespace, environment);
            } else if (group != null) {
                configs = configCenterService.getConfigsByGroup(group);
            } else {
                configs = configCenterService.getAllConfigs();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", configs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取配置统计信息
     */
    @GetMapping("/configs/statistics")
    public ResponseEntity<Map<String, Object>> getConfigStatistics() {
        try {
            Map<String, Object> stats = configCenterService.getConfigStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建配置
     */
    @PostMapping("/configs")
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody Map<String, Object> configRequest) {
        try {
            String namespace = (String) configRequest.get("namespace");
            String group = (String) configRequest.get("group");
            String key = (String) configRequest.get("key");
            String value = (String) configRequest.get("value");
            String dataType = (String) configRequest.get("dataType");
            String environment = (String) configRequest.get("environment");
            String description = (String) configRequest.get("description");
            String createdBy = (String) configRequest.get("createdBy");
            
            ConfigCenterService.ConfigItem config = configCenterService.createConfig(
                    namespace, group, key, value, dataType, environment, description, createdBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);
            response.put("message", "配置创建成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/configs/{configId}")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String configId,
            @RequestBody Map<String, Object> configRequest) {
        try {
            String value = (String) configRequest.get("value");
            String updatedBy = (String) configRequest.get("updatedBy");
            String reason = (String) configRequest.get("reason");
            
            ConfigCenterService.ConfigItem config = configCenterService.updateConfig(
                    configId, value, updatedBy, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);
            response.put("message", "配置更新成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/configs/{configId}")
    public ResponseEntity<Map<String, Object>> deleteConfig(
            @PathVariable String configId,
            @RequestParam String deletedBy,
            @RequestParam(required = false) String reason) {
        try {
            boolean deleted = configCenterService.deleteConfig(configId, deletedBy, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "配置删除成功" : "配置不存在");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ==================== MCP工具管理 ====================
    
    /**
     * 获取所有MCP工具
     */
    @GetMapping("/mcp-tools")
    public ResponseEntity<Map<String, Object>> getAllMcpTools(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
        try {
            List<McpToolManagementService.McpTool> tools;
            
            if (keyword != null || category != null || enabledOnly) {
                tools = mcpToolService.searchTools(keyword, category, enabledOnly);
            } else {
                tools = mcpToolService.getAllTools();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tools);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取MCP工具统计信息
     */
    @GetMapping("/mcp-tools/statistics")
    public ResponseEntity<Map<String, Object>> getMcpToolStatistics() {
        try {
            Map<String, Object> stats = mcpToolService.getToolStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取工具分类
     */
    @GetMapping("/mcp-tools/categories")
    public ResponseEntity<Map<String, Object>> getToolCategories() {
        try {
            List<String> categories = mcpToolService.getCategories();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 启用/禁用工具
     */
    @PutMapping("/mcp-tools/{toolId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleTool(
            @PathVariable String toolId,
            @RequestBody Map<String, Object> request) {
        try {
            Boolean enabled = (Boolean) request.get("enabled");
            boolean toggled = mcpToolService.toggleToolStatus(toolId, enabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", toggled);
            response.put("message", toggled ? "工具状态更新成功" : "工具不存在");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取工具执行日志
     */
    @GetMapping("/mcp-tools/{toolId}/logs")
    public ResponseEntity<Map<String, Object>> getToolExecutionLogs(
            @PathVariable String toolId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<McpToolManagementService.ToolExecutionLog> logs = 
                    mcpToolService.getToolExecutionLogs(toolId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", logs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ==================== 系统信息 ====================
    
    /**
     * 获取系统概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 队列统计
            Map<String, Object> queueStats = queueService.getQueueStatistics();
            overview.put("queues", queueStats);
            
            // 服务统计
            Map<String, Object> serviceStats = serviceDiscoveryService.getServiceStatistics();
            overview.put("services", serviceStats);
            
            // 配置统计
            Map<String, Object> configStats = configCenterService.getConfigStatistics();
            overview.put("configs", configStats);
            
            // MCP工具统计
            Map<String, Object> toolStats = mcpToolService.getToolStatistics();
            overview.put("mcpTools", toolStats);
            
            // 系统信息
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("timestamp", new Date());
            systemInfo.put("version", "1.0.0");
            systemInfo.put("uptime", System.currentTimeMillis());
            overview.put("system", systemInfo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", overview);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 仪表盘统计数据接口
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 队列统计
            Map<String, Object> queueStats = queueService.getQueueStatistics();
            stats.put("totalQueues", queueStats.get("totalQueues"));
            stats.put("activeQueues", queueStats.get("activeQueues"));
            stats.put("totalMessages", queueStats.get("totalMessages"));
            stats.put("pendingMessages", queueStats.get("pendingMessages"));
            
            // 服务统计
            Map<String, Object> serviceStats = serviceDiscoveryService.getServiceStatistics();
            stats.put("totalServices", serviceStats.get("totalServices"));
            stats.put("healthyServices", serviceStats.get("healthyServices"));
            stats.put("unhealthyServices", serviceStats.get("unhealthyServices"));
            stats.put("totalInstances", serviceStats.get("totalInstances"));
            
            // 配置统计
            Map<String, Object> configStats = configCenterService.getConfigStatistics();
            stats.put("totalConfigs", configStats.get("totalConfigs"));
            stats.put("activeConfigs", configStats.get("activeConfigs"));
            stats.put("configNamespaces", configStats.get("totalNamespaces"));
            stats.put("configGroups", configStats.get("totalGroups"));
            
            // MCP工具统计
            Map<String, Object> toolStats = mcpToolService.getToolStatistics();
            stats.put("totalTools", toolStats.get("totalTools"));
            stats.put("enabledTools", toolStats.get("enabledTools"));
            stats.put("disabledTools", toolStats.get("disabledTools"));
            stats.put("toolCategories", toolStats.get("totalCategories"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 添加系统监控API
    @GetMapping("/system/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // CPU使用率 - JDK17兼容版本
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = getCpuUsage(osBean);
            metrics.put("cpuUsage", cpuUsage);
            
            // 系统负载平均值
            double systemLoadAverage = osBean.getSystemLoadAverage();
            metrics.put("systemLoadAverage", systemLoadAverage);
            
            // 可用处理器数量
            int availableProcessors = osBean.getAvailableProcessors();
            metrics.put("availableProcessors", availableProcessors);
            
            // 内存使用率
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("heapUsed", heapUsage.getUsed());
            memoryInfo.put("heapMax", heapUsage.getMax());
            memoryInfo.put("heapCommitted", heapUsage.getCommitted());
            memoryInfo.put("heapUsagePercent", heapUsage.getMax() > 0 ? 
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100 : 0);
            
            memoryInfo.put("nonHeapUsed", nonHeapUsage.getUsed());
            memoryInfo.put("nonHeapMax", nonHeapUsage.getMax());
            memoryInfo.put("nonHeapCommitted", nonHeapUsage.getCommitted());
            
            metrics.put("memory", memoryInfo);
            
            // JVM信息
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            Map<String, Object> runtimeInfo = new HashMap<>();
            runtimeInfo.put("uptime", runtimeBean.getUptime());
            runtimeInfo.put("startTime", runtimeBean.getStartTime());
            runtimeInfo.put("vmName", runtimeBean.getVmName());
            runtimeInfo.put("vmVersion", runtimeBean.getVmVersion());
            runtimeInfo.put("vmVendor", runtimeBean.getVmVendor());
            runtimeInfo.put("specName", runtimeBean.getSpecName());
            runtimeInfo.put("specVersion", runtimeBean.getSpecVersion());
            metrics.put("runtime", runtimeInfo);
            
            // 线程信息
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            Map<String, Object> threadInfo = new HashMap<>();
            threadInfo.put("threadCount", threadBean.getThreadCount());
            threadInfo.put("peakThreadCount", threadBean.getPeakThreadCount());
            threadInfo.put("daemonThreadCount", threadBean.getDaemonThreadCount());
            threadInfo.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
            metrics.put("threads", threadInfo);
            
            // 垃圾回收信息
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            List<Map<String, Object>> gcInfo = new ArrayList<>();
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                Map<String, Object> gc = new HashMap<>();
                gc.put("name", gcBean.getName());
                gc.put("collectionCount", gcBean.getCollectionCount());
                gc.put("collectionTime", gcBean.getCollectionTime());
                gc.put("memoryPoolNames", Arrays.asList(gcBean.getMemoryPoolNames()));
                gcInfo.add(gc);
            }
            metrics.put("garbageCollectors", gcInfo);
            
            // 内存池信息
            List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();
            List<Map<String, Object>> poolInfo = new ArrayList<>();
            for (MemoryPoolMXBean poolBean : memoryPoolBeans) {
                Map<String, Object> pool = new HashMap<>();
                pool.put("name", poolBean.getName());
                pool.put("type", poolBean.getType().toString());
                
                MemoryUsage usage = poolBean.getUsage();
                if (usage != null) {
                    Map<String, Object> usageInfo = new HashMap<>();
                    usageInfo.put("used", usage.getUsed());
                    usageInfo.put("committed", usage.getCommitted());
                    usageInfo.put("max", usage.getMax());
                    usageInfo.put("init", usage.getInit());
                    pool.put("usage", usageInfo);
                }
                
                MemoryUsage peakUsage = poolBean.getPeakUsage();
                if (peakUsage != null) {
                    Map<String, Object> peakInfo = new HashMap<>();
                    peakInfo.put("used", peakUsage.getUsed());
                    peakInfo.put("committed", peakUsage.getCommitted());
                    peakInfo.put("max", peakUsage.getMax());
                    peakInfo.put("init", peakUsage.getInit());
                    pool.put("peakUsage", peakInfo);
                }
                
                poolInfo.add(pool);
            }
            metrics.put("memoryPools", poolInfo);
            
            // 类加载信息
            ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
            Map<String, Object> classLoadingInfo = new HashMap<>();
            classLoadingInfo.put("loadedClassCount", classLoadingBean.getLoadedClassCount());
            classLoadingInfo.put("totalLoadedClassCount", classLoadingBean.getTotalLoadedClassCount());
            classLoadingInfo.put("unloadedClassCount", classLoadingBean.getUnloadedClassCount());
            metrics.put("classLoading", classLoadingInfo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取系统指标失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取CPU使用率 - JDK17兼容版本
     * 使用反射来安全地访问特定于平台的方法
     */
    private double getCpuUsage(OperatingSystemMXBean osBean) {
        try {
            // 尝试使用com.sun.management.OperatingSystemMXBean的方法
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuLoad = sunOsBean.getCpuLoad();
                if (cpuLoad >= 0) {
                    return cpuLoad * 100;
                }
            }
            
            // 如果上述方法不可用，尝试使用反射
            try {
                java.lang.reflect.Method method = osBean.getClass().getMethod("getCpuLoad");
                method.setAccessible(true);
                Object result = method.invoke(osBean);
                if (result instanceof Double) {
                    double cpuLoad = (Double) result;
                    if (cpuLoad >= 0) {
                        return cpuLoad * 100;
                    }
                }
            } catch (Exception reflectionException) {
                // 反射失败，继续尝试其他方法
            }
            
            // 尝试使用getProcessCpuLoad方法
            try {
                java.lang.reflect.Method method = osBean.getClass().getMethod("getProcessCpuLoad");
                method.setAccessible(true);
                Object result = method.invoke(osBean);
                if (result instanceof Double) {
                    double cpuLoad = (Double) result;
                    if (cpuLoad >= 0) {
                        return cpuLoad * 100;
                    }
                }
            } catch (Exception reflectionException) {
                // 反射失败，返回默认值
            }
            
            // 如果所有方法都失败，返回-1表示不可用
            return -1.0;
            
        } catch (Exception e) {
            // 发生异常时返回-1
            return -1.0;
        }
    }

    // 添加配置验证API
    @PostMapping("/configs/validate")
    public ResponseEntity<Map<String, Object>> validateConfig(@RequestBody Map<String, Object> request) {
        try {
            // 从请求中提取所需参数
            String namespace = (String) request.get("namespace");
            String group = (String) request.get("group");
            String key = (String) request.get("key");
            String value = (String) request.get("value");
            String dataType = (String) request.get("dataType");
            String environment = (String) request.get("environment");
            
            // 执行配置验证逻辑
            Map<String, Object> validationResult = configCenterService.validateConfigData(
                namespace, group, key, value, dataType, environment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", validationResult.get("valid"));
            response.put("errors", validationResult.get("errors"));
            response.put("warnings", validationResult.get("warnings"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 添加MCP工具性能分析API
    @GetMapping("/mcp-tools/{toolId}/performance")
    public ResponseEntity<Map<String, Object>> getToolPerformance(@PathVariable String toolId) {
        try {
            Map<String, Object> performance = mcpToolService.getToolPerformance(toolId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", performance);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取队列指标统计
     */
    @GetMapping("/queues/metrics")
    public ResponseEntity<Map<String, Object>> getQueueMetrics() {
        try {
            Map<String, Object> metrics = queueMetricsService.getAllQueueMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取UNIFIED队列指标
     */
    @GetMapping("/queues/metrics/unified")
    public ResponseEntity<Map<String, Object>> getUnifiedQueueMetrics() {
        try {
            Map<String, Object> metrics = queueMetricsService.getUnifiedQueueMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取STREAM_UNIFIED队列指标
     */
    @GetMapping("/queues/metrics/stream")
    public ResponseEntity<Map<String, Object>> getStreamUnifiedQueueMetrics() {
        try {
            Map<String, Object> metrics = queueMetricsService.getStreamUnifiedQueueMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}