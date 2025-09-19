package cn.lingque.cloud.http.example;

import cn.lingque.cloud.http.annotation.Get;
import cn.lingque.cloud.http.annotation.Post;
import cn.lingque.cloud.http.annotation.RequestBody;
import cn.lingque.cloud.http.annotation.RequestParam;
import cn.lingque.runner.annon.LqService;

/**
 * 外部API客户端示例
 * 展示如何直接使用HTTP域名地址调用异构系统
 */
public class ExternalApiClient {

    /**
     * 单个域名地址示例
     */
    @LqService(serviceName = "https://api.github.com")
    public interface GitHubApiClient {
        
        @Get("/user")
        GitHubUser getCurrentUser();
        
        @Get("/users/{username}")
        GitHubUser getUser(@RequestParam("username") String username);
        
        @Get("/user/repos")
        java.util.List<GitHubRepo> getUserRepos(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "sort", required = false) String sort
        );
    }

    /**
     * 多个域名地址轮询示例
     * 用于高可用场景，当一个域名不可用时自动切换到其他域名
     */
    @LqService(serviceName = "https://api1.example.com,https://api2.example.com,https://api3.example.com")
    public interface HighAvailabilityApiClient {
        
        @Get("/health")
        HealthStatus getHealthStatus();
        
        @Post("/data")
        ApiResponse<String> submitData(@RequestBody DataRequest request);
        
        @Get("/config")
        ConfigResponse getConfiguration();
    }

    /**
     * 混合域名和IP地址示例
     */
    @LqService(serviceName = "http://192.168.1.100:8080,http://backup.example.com:8080")
    public interface InternalApiClient {
        
        @Get("/internal/status")
        SystemStatus getSystemStatus();
        
        @Post("/internal/command")
        CommandResult executeCommand(@RequestBody CommandRequest request);
    }

    /**
     * 第三方支付API示例
     */
    @LqService(serviceName = "https://api.payment-provider.com")
    public interface PaymentApiClient {
        
        @Post("/v1/payments")
        PaymentResponse createPayment(@RequestBody PaymentRequest request);
        
        @Get("/v1/payments/{paymentId}")
        PaymentStatus getPaymentStatus(@RequestParam("paymentId") String paymentId);
        
        @Post("/v1/refunds")
        RefundResponse createRefund(@RequestBody RefundRequest request);
    }

    // ========== 数据模型 ==========

    public static class GitHubUser {
        private String login;
        private Long id;
        private String name;
        private String email;
        private String bio;
        
        // getters and setters
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
    }

    public static class GitHubRepo {
        private String name;
        private String fullName;
        private String description;
        private Boolean isPrivate;
        
        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Boolean getIsPrivate() { return isPrivate; }
        public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
    }

    public static class HealthStatus {
        private String status;
        private Long timestamp;
        private String version;
        
        // getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public static class ApiResponse<T> {
        private Integer code;
        private String message;
        private T data;
        
        // getters and setters
        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    public static class DataRequest {
        private String type;
        private Object payload;
        
        // getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
    }

    public static class ConfigResponse {
        private java.util.Map<String, Object> settings;
        private String environment;
        
        // getters and setters
        public java.util.Map<String, Object> getSettings() { return settings; }
        public void setSettings(java.util.Map<String, Object> settings) { this.settings = settings; }
        
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }

    public static class SystemStatus {
        private String status;
        private Double cpuUsage;
        private Double memoryUsage;
        private Long uptime;
        
        // getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public Long getUptime() { return uptime; }
        public void setUptime(Long uptime) { this.uptime = uptime; }
    }

    public static class CommandRequest {
        private String command;
        private java.util.Map<String, Object> parameters;
        
        // getters and setters
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public java.util.Map<String, Object> getParameters() { return parameters; }
        public void setParameters(java.util.Map<String, Object> parameters) { this.parameters = parameters; }
    }

    public static class CommandResult {
        private Boolean success;
        private String result;
        private String error;
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class PaymentRequest {
        private String amount;
        private String currency;
        private String description;
        private String customerId;
        
        // getters and setters
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
    }

    public static class PaymentResponse {
        private String paymentId;
        private String status;
        private String amount;
        private String currency;
        
        // getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class PaymentStatus {
        private String paymentId;
        private String status;
        private String statusDetail;
        private Long timestamp;
        
        // getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getStatusDetail() { return statusDetail; }
        public void setStatusDetail(String statusDetail) { this.statusDetail = statusDetail; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class RefundRequest {
        private String paymentId;
        private String amount;
        private String reason;
        
        // getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RefundResponse {
        private String refundId;
        private String status;
        private String amount;
        
        // getters and setters
        public String getRefundId() { return refundId; }
        public void setRefundId(String refundId) { this.refundId = refundId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
    }
}