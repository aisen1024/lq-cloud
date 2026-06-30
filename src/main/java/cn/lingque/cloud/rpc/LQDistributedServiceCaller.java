package cn.lingque.cloud.rpc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.node.LQEnhancedRegisterCenter;
import cn.lingque.cloud.node.bean.LQEnhancedNodeInfo;
import cn.lingque.cloud.rpc.annotation.LQService;
import cn.lingque.cloud.rpc.annotation.LQServiceMethod;
import cn.lingque.cloud.rpc.bean.LQRpcRequest;
import cn.lingque.cloud.rpc.bean.LQRpcResponse;
import cn.lingque.cloud.rpc.circuit.LQCircuitBreaker;
import cn.lingque.cloud.rpc.loadbalance.LQLoadBalancer;
import cn.lingque.cloud.rpc.trace.LQTraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LQ分布式服务调用器 - 比Dubbo更强大的微服务调用框架
 * 
 * 核心特性：
 * 1. 基于增强版注册中心的服务发现
 * 2. 多种负载均衡策略
 * 3. 熔断器保护
 * 4. 调用链追踪
 * 5. 自动重试机制
 * 6. 性能监控
 * 7. 多协议支持
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQDistributedServiceCaller {
    
    /** 服务代理缓存 */
    private static final Map<Class<?>, Object> serviceProxyCache = new ConcurrentHashMap<>();
    
    /** 熔断器缓存 */
    private static final Map<String, LQCircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();
    
    /** 负载均衡器 */
    private static final LQLoadBalancer loadBalancer = new LQLoadBalancer();
    
    /** 调用统计 */
    private static final Map<String, AtomicLong> callStats = new ConcurrentHashMap<>();
    
    /** 默认超时时间(毫秒) */
    private static final int DEFAULT_TIMEOUT = 5000;
    
    /** 默认重试次数 */
    private static final int DEFAULT_RETRY_COUNT = 3;
    
    /**
     * 创建服务代理
     * @param serviceInterface 服务接口
     * @param <T> 服务类型
     * @return 服务代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createServiceProxy(Class<T> serviceInterface) {
        return (T) serviceProxyCache.computeIfAbsent(serviceInterface, clazz -> {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(clazz);
            enhancer.setCallback(new ServiceMethodInterceptor(clazz));
            return enhancer.create();
        });
    }
    
    /**
     * 直接调用远程服务
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param args 参数
     * @param returnType 返回类型
     * @param <T> 返回类型
     * @return 调用结果
     */
    public static <T> T callRemoteService(String serviceName, String methodName, Object[] args, Class<T> returnType) {
        return callRemoteService(serviceName, methodName, args, returnType, "HTTP", DEFAULT_TIMEOUT, DEFAULT_RETRY_COUNT);
    }
    
    /**
     * 调用远程服务（完整参数）
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param args 参数
     * @param returnType 返回类型
     * @param protocol 协议类型
     * @param timeout 超时时间
     * @param retryCount 重试次数
     * @param <T> 返回类型
     * @return 调用结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T callRemoteService(String serviceName, String methodName, Object[] args, 
                                         Class<T> returnType, String protocol, int timeout, int retryCount) {
        String traceId = LQTraceContext.generateTraceId();
        LQTraceContext.setTraceId(traceId);
        
        try {
            // 获取熔断器
            String circuitKey = serviceName + ":" + methodName;
            LQCircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(circuitKey);
            
            // 检查熔断器状态
            if (!circuitBreaker.allowRequest()) {
                throw new RuntimeException("服务熔断中: " + circuitKey);
            }
            
            // 服务发现
            List<LQEnhancedNodeInfo> nodes = LQEnhancedRegisterCenter.getNodesByProtocol(serviceName, protocol);
            if (nodes.isEmpty()) {
                throw new RuntimeException("未找到可用的服务实例: " + serviceName);
            }
            
            // 负载均衡选择节点
            LQEnhancedNodeInfo selectedNode = loadBalancer.select(nodes, LQEnhancedRegisterCenter.LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN);
            
            // 构建请求
            LQRpcRequest request = new LQRpcRequest()
                    .setTraceId(traceId)
                    .setServiceName(serviceName)
                    .setMethodName(methodName)
                    .setArgs(args)
                    .setTimestamp(System.currentTimeMillis());
            
            // 执行调用（带重试）
            LQRpcResponse response = executeWithRetry(selectedNode, request, protocol, timeout, retryCount);
            
            // 记录成功调用
            circuitBreaker.recordSuccess();
            recordCallStats(circuitKey, true);
            
            // 处理响应
            if (response.isSuccess()) {
                if (returnType == Void.TYPE || returnType == void.class) {
                    return null;
                }
                return JSONUtil.toBean(JSONUtil.toJsonStr(response.getResult()), returnType);
            } else {
                throw new RuntimeException("远程调用失败: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            // 记录失败调用
            String circuitKey = serviceName + ":" + methodName;
            LQCircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(circuitKey);
            circuitBreaker.recordFailure();
            recordCallStats(circuitKey, false);
            
            log.error("远程服务调用失败 [{}:{}] traceId: {}", serviceName, methodName, traceId, e);
            throw new RuntimeException("远程服务调用失败: " + e.getMessage(), e);
        } finally {
            LQTraceContext.clear();
        }
    }
    
    /**
     * 带重试的执行调用
     */
    private static LQRpcResponse executeWithRetry(LQEnhancedNodeInfo node, LQRpcRequest request, 
                                                 String protocol, int timeout, int retryCount) {
        Exception lastException = null;
        
        for (int i = 0; i <= retryCount; i++) {
            try {
                return executeCall(node, request, protocol, timeout);
            } catch (Exception e) {
                lastException = e;
                if (i < retryCount) {
                    log.warn("调用失败，准备重试 [{}/{}]: {}", i + 1, retryCount, e.getMessage());
                    try {
                        TimeUnit.MILLISECONDS.sleep(100 * (i + 1)); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("重试" + retryCount + "次后仍然失败", lastException);
    }
    
    /**
     * 执行具体调用
     */
    private static LQRpcResponse executeCall(LQEnhancedNodeInfo node, LQRpcRequest request, 
                                           String protocol, int timeout) {
        switch (protocol.toUpperCase()) {
            case "HTTP":
                return executeHttpCall(node, request, timeout);
            case "SOCKET":
                return executeSocketCall(node, request, timeout);
            case "GRPC":
                return executeGrpcCall(node, request, timeout);
            default:
                throw new UnsupportedOperationException("不支持的协议: " + protocol);
        }
    }
    
    /**
     * HTTP调用
     */
    private static LQRpcResponse executeHttpCall(LQEnhancedNodeInfo node, LQRpcRequest request, int timeout) {
        String url = String.format("http://%s:%d/lq-rpc/invoke", node.getNodeIp(), node.getNodePort());
        
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", request.getTraceId())
                .body(JSONUtil.toJsonStr(request))
                .timeout(timeout)
                .execute();
        
        if (response.getStatus() == 200) {
            return JSONUtil.toBean(response.body(), LQRpcResponse.class);
        } else {
            throw new RuntimeException("HTTP调用失败: " + response.getStatus() + " - " + response.body());
        }
    }
    
    /**
     * Socket调用（简化实现）
     */
    private static LQRpcResponse executeSocketCall(LQEnhancedNodeInfo node, LQRpcRequest request, int timeout) {
        // TODO: 实现Socket调用
        throw new UnsupportedOperationException("Socket调用暂未实现");
    }
    
    /**
     * gRPC调用（简化实现）
     */
    private static LQRpcResponse executeGrpcCall(LQEnhancedNodeInfo node, LQRpcRequest request, int timeout) {
        // TODO: 实现gRPC调用
        throw new UnsupportedOperationException("gRPC调用暂未实现");
    }
    
    /**
     * 获取或创建熔断器
     */
    private static LQCircuitBreaker getOrCreateCircuitBreaker(String key) {
        return circuitBreakerCache.computeIfAbsent(key, k -> new LQCircuitBreaker());
    }
    
    /**
     * 记录调用统计
     */
    private static void recordCallStats(String key, boolean success) {
        String statsKey = key + (success ? ":success" : ":failure");
        callStats.computeIfAbsent(statsKey, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 获取调用统计
     */
    public static Map<String, Long> getCallStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        callStats.forEach((key, value) -> stats.put(key, value.get()));
        return stats;
    }
    
    /**
     * 服务方法拦截器
     */
    private static class ServiceMethodInterceptor implements MethodInterceptor {
        private final Class<?> serviceInterface;
        
        public ServiceMethodInterceptor(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }
        
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            // 获取服务注解
            LQService serviceAnnotation = serviceInterface.getAnnotation(LQService.class);
            if (serviceAnnotation == null) {
                throw new RuntimeException("服务接口必须标注@LQService注解: " + serviceInterface.getName());
            }
            
            // 获取方法注解
            LQServiceMethod methodAnnotation = method.getAnnotation(LQServiceMethod.class);
            
            String serviceName = StrUtil.isNotBlank(serviceAnnotation.value()) ? 
                    serviceAnnotation.value() : serviceInterface.getSimpleName();
            String methodName = methodAnnotation != null && StrUtil.isNotBlank(methodAnnotation.value()) ? 
                    methodAnnotation.value() : method.getName();
            String protocol = methodAnnotation != null ? methodAnnotation.protocol() : "HTTP";
            int timeout = methodAnnotation != null ? methodAnnotation.timeout() : DEFAULT_TIMEOUT;
            int retryCount = methodAnnotation != null ? methodAnnotation.retryCount() : DEFAULT_RETRY_COUNT;
            
            return callRemoteService(serviceName, methodName, args, method.getReturnType(), 
                    protocol, timeout, retryCount);
        }
    }
}