package cn.lingque.cloud.console.controller;

import cn.lingque.cloud.console.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 控制台REST控制器
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/console")
@CrossOrigin(origins = "*")
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
}