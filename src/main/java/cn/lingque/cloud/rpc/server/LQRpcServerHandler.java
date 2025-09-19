package cn.lingque.cloud.rpc.server;

import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.rpc.annotation.LQService;
import cn.lingque.cloud.rpc.bean.LQRpcRequest;
import cn.lingque.cloud.rpc.bean.LQRpcResponse;
import cn.lingque.cloud.rpc.trace.LQTraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LQ RPC服务端处理器
 * 处理远程调用请求
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
@Component
@RestController
public class LQRpcServerHandler implements ApplicationContextAware {
    
    private ApplicationContext applicationContext;
    
    /** 服务实例缓存 */
    private final Map<String, Object> serviceInstances = new ConcurrentHashMap<>();
    
    /** 服务方法缓存 */
    private final Map<String, Method> serviceMethods = new ConcurrentHashMap<>();
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @PostConstruct
    public void init() {
        // 扫描并注册所有标注了@LQService的服务
        scanAndRegisterServices();
    }
    
    /**
     * RPC调用入口
     */
    @PostMapping("/lq-rpc/invoke")
    public LQRpcResponse invoke(@RequestBody LQRpcRequest request,
                               @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        
        // 设置追踪上下文
        if (traceId != null) {
            LQTraceContext.setTraceId(traceId);
        }
        LQTraceContext.startSpan(request.getServiceName(), request.getMethodName());
        
        try {
            log.info("接收到RPC调用请求: {}", JSONUtil.toJsonStr(request));
            
            // 验证请求
            if (request.getServiceName() == null || request.getMethodName() == null) {
                return LQRpcResponse.failure(request.getTraceId(), "INVALID_REQUEST", "服务名或方法名不能为空");
            }
            
            // 查找服务实例
            Object serviceInstance = serviceInstances.get(request.getServiceName());
            if (serviceInstance == null) {
                return LQRpcResponse.failure(request.getTraceId(), "SERVICE_NOT_FOUND", 
                        "未找到服务: " + request.getServiceName());
            }
            
            // 查找服务方法
            String methodKey = request.getServiceName() + "." + request.getMethodName();
            Method method = serviceMethods.get(methodKey);
            if (method == null) {
                return LQRpcResponse.failure(request.getTraceId(), "METHOD_NOT_FOUND", 
                        "未找到方法: " + request.getMethodName());
            }
            
            // 执行方法调用
            long startTime = System.currentTimeMillis();
            Object result = method.invoke(serviceInstance, request.getArgs());
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 构建成功响应
            LQRpcResponse response = LQRpcResponse.success(request.getTraceId(), result)
                    .setProcessingTime(processingTime)
                    .setServerIp(getLocalIp());
            
            log.info("RPC调用成功: {} 耗时: {}ms", methodKey, processingTime);
            return response;
            
        } catch (Exception e) {
            log.error("RPC调用失败: {}", JSONUtil.toJsonStr(request), e);
            return LQRpcResponse.failure(request.getTraceId(), "INVOKE_ERROR", 
                    "方法调用失败: " + e.getMessage())
                    .setStackTrace(getStackTrace(e))
                    .setServerIp(getLocalIp());
        } finally {
            LQTraceContext.finishSpan();
            LQTraceContext.clear();
        }
    }
    
    /**
     * 扫描并注册服务
     */
    private void scanAndRegisterServices() {
        Map<String, Object> serviceBeans = applicationContext.getBeansWithAnnotation(LQService.class);
        
        for (Map.Entry<String, Object> entry : serviceBeans.entrySet()) {
            Object serviceBean = entry.getValue();
            Class<?> serviceClass = serviceBean.getClass();
            
            // 获取服务注解
            LQService serviceAnnotation = serviceClass.getAnnotation(LQService.class);
            if (serviceAnnotation == null) {
                continue;
            }
            
            // 确定服务名称
            String serviceName = serviceAnnotation.value();
            if (serviceName.isEmpty()) {
                serviceName = serviceClass.getSimpleName();
            }
            
            // 注册服务实例
            serviceInstances.put(serviceName, serviceBean);
            
            // 注册服务方法
            Method[] methods = serviceClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodKey = serviceName + "." + method.getName();
                serviceMethods.put(methodKey, method);
                
                log.info("注册RPC服务方法: {}", methodKey);
            }
            
            log.info("注册RPC服务: {} -> {}", serviceName, serviceClass.getName());
        }
    }
    
    /**
     * 获取本地IP
     */
    private String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 获取服务统计信息
     */
    @PostMapping("/lq-rpc/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("registeredServices", serviceInstances.keySet());
        stats.put("registeredMethods", serviceMethods.keySet());
        stats.put("serviceCount", serviceInstances.size());
        stats.put("methodCount", serviceMethods.size());
        return stats;
    }
    
    /**
     * 健康检查
     */
    @PostMapping("/lq-rpc/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("services", serviceInstances.size());
        return health;
    }
}