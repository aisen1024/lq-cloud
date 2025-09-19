package cn.lingque.cloud.console.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务发现服务
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Service
public class ServiceDiscoveryService {
    
    private final Map<String, ServiceInstance> serviceInstances = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceInstance>> serviceGroups = new ConcurrentHashMap<>();
    private final AtomicLong instanceIdGenerator = new AtomicLong(1);
    
    public ServiceDiscoveryService() {
        // 初始化示例服务
        initializeSampleServices();
    }
    
    /**
     * 服务实例信息
     */
    public static class ServiceInstance {
        private String instanceId;
        private String serviceName;
        private String host;
        private int port;
        private String status; // "up", "down", "starting", "stopping"
        private String version;
        private Map<String, String> metadata;
        private Date registerTime;
        private Date lastHeartbeat;
        private HealthInfo health;
        private ServiceMetrics metrics;
        
        public ServiceInstance() {
            this.registerTime = new Date();
            this.lastHeartbeat = new Date();
            this.metadata = new HashMap<>();
            this.health = new HealthInfo();
            this.metrics = new ServiceMetrics();
        }
        
        // Getters and Setters
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        
        public Date getRegisterTime() { return registerTime; }
        public void setRegisterTime(Date registerTime) { this.registerTime = registerTime; }
        
        public Date getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(Date lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
        
        public HealthInfo getHealth() { return health; }
        public void setHealth(HealthInfo health) { this.health = health; }
        
        public ServiceMetrics getMetrics() { return metrics; }
        public void setMetrics(ServiceMetrics metrics) { this.metrics = metrics; }
        
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
        
        public boolean isHealthy() {
            return "up".equals(status) && health.isHealthy();
        }
    }
    
    /**
     * 健康信息
     */
    public static class HealthInfo {
        private boolean healthy;
        private String status;
        private Map<String, Object> details;
        private Date lastCheckTime;
        
        public HealthInfo() {
            this.healthy = true;
            this.status = "UP";
            this.details = new HashMap<>();
            this.lastCheckTime = new Date();
        }
        
        // Getters and Setters
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        
        public Date getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(Date lastCheckTime) { this.lastCheckTime = lastCheckTime; }
    }
    
    /**
     * 服务指标
     */
    public static class ServiceMetrics {
        private long requestCount;
        private long errorCount;
        private double avgResponseTime;
        private double cpuUsage;
        private double memoryUsage;
        private long uptime;
        
        // Getters and Setters
        public long getRequestCount() { return requestCount; }
        public void setRequestCount(long requestCount) { this.requestCount = requestCount; }
        
        public long getErrorCount() { return errorCount; }
        public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
        
        public double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
        
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
        
        public double getErrorRate() {
            return requestCount > 0 ? (double) errorCount / requestCount * 100 : 0;
        }
    }
    
    /**
     * 获取所有服务实例
     */
    public List<ServiceInstance> getAllServices() {
        return new ArrayList<>(serviceInstances.values());
    }
    
    /**
     * 根据服务名获取实例列表
     */
    public List<ServiceInstance> getServicesByName(String serviceName) {
        return serviceGroups.getOrDefault(serviceName, new ArrayList<>());
    }
    
    /**
     * 根据实例ID获取服务实例
     */
    public ServiceInstance getServiceById(String instanceId) {
        return serviceInstances.get(instanceId);
    }
    
    /**
     * 注册服务实例
     */
    public ServiceInstance registerService(String serviceName, String host, int port, 
                                         String version, Map<String, String> metadata) {
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId("instance_" + instanceIdGenerator.getAndIncrement());
        instance.setServiceName(serviceName);
        instance.setHost(host);
        instance.setPort(port);
        instance.setVersion(version);
        instance.setStatus("up");
        
        if (metadata != null) {
            instance.getMetadata().putAll(metadata);
        }
        
        // 添加到实例映射
        serviceInstances.put(instance.getInstanceId(), instance);
        
        // 添加到服务组
        serviceGroups.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(instance);
        
        return instance;
    }
    
    /**
     * 注销服务实例
     */
    public boolean deregisterService(String instanceId) {
        ServiceInstance instance = serviceInstances.remove(instanceId);
        if (instance != null) {
            List<ServiceInstance> instances = serviceGroups.get(instance.getServiceName());
            if (instances != null) {
                instances.removeIf(i -> i.getInstanceId().equals(instanceId));
                if (instances.isEmpty()) {
                    serviceGroups.remove(instance.getServiceName());
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * 更新服务状态
     */
    public boolean updateServiceStatus(String instanceId, String status) {
        ServiceInstance instance = serviceInstances.get(instanceId);
        if (instance != null) {
            instance.setStatus(status);
            instance.setLastHeartbeat(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * 发送心跳
     */
    public boolean heartbeat(String instanceId) {
        ServiceInstance instance = serviceInstances.get(instanceId);
        if (instance != null) {
            instance.setLastHeartbeat(new Date());
            if (!"up".equals(instance.getStatus())) {
                instance.setStatus("up");
            }
            return true;
        }
        return false;
    }
    
    /**
     * 更新健康状态
     */
    public boolean updateHealthStatus(String instanceId, boolean healthy, String status, 
                                    Map<String, Object> details) {
        ServiceInstance instance = serviceInstances.get(instanceId);
        if (instance != null) {
            HealthInfo health = instance.getHealth();
            health.setHealthy(healthy);
            health.setStatus(status);
            health.setLastCheckTime(new Date());
            if (details != null) {
                health.getDetails().putAll(details);
            }
            return true;
        }
        return false;
    }
    
    /**
     * 更新服务指标
     */
    public boolean updateServiceMetrics(String instanceId, ServiceMetrics metrics) {
        ServiceInstance instance = serviceInstances.get(instanceId);
        if (instance != null) {
            instance.setMetrics(metrics);
            return true;
        }
        return false;
    }
    
    /**
     * 获取服务统计信息
     */
    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalServices = serviceGroups.size();
        long totalInstances = serviceInstances.size();
        long healthyInstances = serviceInstances.values().stream()
                .mapToLong(instance -> instance.isHealthy() ? 1 : 0)
                .sum();
        long upInstances = serviceInstances.values().stream()
                .mapToLong(instance -> "up".equals(instance.getStatus()) ? 1 : 0)
                .sum();
        
        stats.put("totalServices", totalServices);
        stats.put("totalInstances", totalInstances);
        stats.put("healthyInstances", healthyInstances);
        stats.put("upInstances", upInstances);
        stats.put("downInstances", totalInstances - upInstances);
        stats.put("healthRate", totalInstances > 0 ? (double) healthyInstances / totalInstances * 100 : 0);
        stats.put("lastUpdateTime", new Date());
        
        return stats;
    }
    
    /**
     * 获取服务分组统计
     */
    public Map<String, Map<String, Object>> getServiceGroupStatistics() {
        Map<String, Map<String, Object>> groupStats = new HashMap<>();
        
        for (Map.Entry<String, List<ServiceInstance>> entry : serviceGroups.entrySet()) {
            String serviceName = entry.getKey();
            List<ServiceInstance> instances = entry.getValue();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("instanceCount", instances.size());
            stats.put("healthyCount", instances.stream().mapToLong(i -> i.isHealthy() ? 1 : 0).sum());
            stats.put("upCount", instances.stream().mapToLong(i -> "up".equals(i.getStatus()) ? 1 : 0).sum());
            
            groupStats.put(serviceName, stats);
        }
        
        return groupStats;
    }
    
    /**
     * 清理过期实例
     */
    public void cleanExpiredInstances(long timeoutMillis) {
        Date now = new Date();
        List<String> expiredInstances = new ArrayList<>();
        
        for (ServiceInstance instance : serviceInstances.values()) {
            if (now.getTime() - instance.getLastHeartbeat().getTime() > timeoutMillis) {
                expiredInstances.add(instance.getInstanceId());
            }
        }
        
        for (String instanceId : expiredInstances) {
            deregisterService(instanceId);
        }
    }
    
    /**
     * 初始化示例服务
     */
    private void initializeSampleServices() {
        // 用户服务
        Map<String, String> userServiceMeta = new HashMap<>();
        userServiceMeta.put("zone", "us-east-1");
        userServiceMeta.put("environment", "production");
        
        ServiceInstance userService1 = registerService("user-service", "192.168.1.10", 8080, "1.2.0", userServiceMeta);
        userService1.getMetrics().setRequestCount(1250);
        userService1.getMetrics().setErrorCount(5);
        userService1.getMetrics().setAvgResponseTime(120.5);
        userService1.getMetrics().setCpuUsage(45.2);
        userService1.getMetrics().setMemoryUsage(68.7);
        
        ServiceInstance userService2 = registerService("user-service", "192.168.1.11", 8080, "1.2.0", userServiceMeta);
        userService2.getMetrics().setRequestCount(980);
        userService2.getMetrics().setErrorCount(2);
        userService2.getMetrics().setAvgResponseTime(98.3);
        userService2.getMetrics().setCpuUsage(38.9);
        userService2.getMetrics().setMemoryUsage(72.1);
        
        // 订单服务
        Map<String, String> orderServiceMeta = new HashMap<>();
        orderServiceMeta.put("zone", "us-east-1");
        orderServiceMeta.put("environment", "production");
        
        ServiceInstance orderService = registerService("order-service", "192.168.1.20", 8081, "2.1.0", orderServiceMeta);
        orderService.getMetrics().setRequestCount(750);
        orderService.getMetrics().setErrorCount(8);
        orderService.getMetrics().setAvgResponseTime(200.1);
        orderService.getMetrics().setCpuUsage(52.3);
        orderService.getMetrics().setMemoryUsage(81.4);
        
        // 通知服务
        Map<String, String> notificationMeta = new HashMap<>();
        notificationMeta.put("zone", "us-west-2");
        notificationMeta.put("environment", "production");
        
        ServiceInstance notificationService = registerService("notification-service", "192.168.2.10", 8082, "1.0.5", notificationMeta);
        notificationService.getMetrics().setRequestCount(2100);
        notificationService.getMetrics().setErrorCount(15);
        notificationService.getMetrics().setAvgResponseTime(85.7);
        notificationService.getMetrics().setCpuUsage(28.6);
        notificationService.getMetrics().setMemoryUsage(45.2);
    }
}