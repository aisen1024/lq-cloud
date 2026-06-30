package cn.lingque.cloud.rpc.trace;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LQ调用链追踪上下文
 * 用于分布式系统的调用链追踪
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class LQTraceContext {
    
    /** 线程本地存储 */
    private static final ThreadLocal<TraceInfo> TRACE_CONTEXT = new ThreadLocal<>();
    
    /** 全局追踪信息存储 */
    private static final Map<String, TraceInfo> GLOBAL_TRACES = new ConcurrentHashMap<>();
    
    /** 追踪信息 */
    public static class TraceInfo {
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String serviceName;
        private String methodName;
        private long startTime;
        private long endTime;
        private Map<String, Object> tags;
        private Map<String, Object> logs;
        
        public TraceInfo() {
            this.tags = new ConcurrentHashMap<>();
            this.logs = new ConcurrentHashMap<>();
            this.startTime = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        
        public String getSpanId() { return spanId; }
        public void setSpanId(String spanId) { this.spanId = spanId; }
        
        public String getParentSpanId() { return parentSpanId; }
        public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public Map<String, Object> getTags() { return tags; }
        public void setTags(Map<String, Object> tags) { this.tags = tags; }
        
        public Map<String, Object> getLogs() { return logs; }
        public void setLogs(Map<String, Object> logs) { this.logs = logs; }
        
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * 生成追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成Span ID
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 设置追踪ID
     */
    public static void setTraceId(String traceId) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.setTraceId(traceId);
        traceInfo.setSpanId(generateSpanId());
    }
    
    /**
     * 获取追踪ID
     */
    public static String getTraceId() {
        TraceInfo traceInfo = TRACE_CONTEXT.get();
        return traceInfo != null ? traceInfo.getTraceId() : null;
    }
    
    /**
     * 设置Span ID
     */
    public static void setSpanId(String spanId) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.setSpanId(spanId);
    }
    
    /**
     * 获取Span ID
     */
    public static String getSpanId() {
        TraceInfo traceInfo = TRACE_CONTEXT.get();
        return traceInfo != null ? traceInfo.getSpanId() : null;
    }
    
    /**
     * 设置父Span ID
     */
    public static void setParentSpanId(String parentSpanId) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.setParentSpanId(parentSpanId);
    }
    
    /**
     * 获取父Span ID
     */
    public static String getParentSpanId() {
        TraceInfo traceInfo = TRACE_CONTEXT.get();
        return traceInfo != null ? traceInfo.getParentSpanId() : null;
    }
    
    /**
     * 设置服务名称
     */
    public static void setServiceName(String serviceName) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.setServiceName(serviceName);
    }
    
    /**
     * 设置方法名称
     */
    public static void setMethodName(String methodName) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.setMethodName(methodName);
    }
    
    /**
     * 添加标签
     */
    public static void addTag(String key, Object value) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.getTags().put(key, value);
    }
    
    /**
     * 添加日志
     */
    public static void addLog(String key, Object value) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        traceInfo.getLogs().put(key, value);
    }
    
    /**
     * 开始新的Span
     */
    public static void startSpan(String serviceName, String methodName) {
        TraceInfo traceInfo = getOrCreateTraceInfo();
        
        // 如果已有traceId，创建子span
        if (traceInfo.getTraceId() != null) {
            traceInfo.setParentSpanId(traceInfo.getSpanId());
            traceInfo.setSpanId(generateSpanId());
        } else {
            traceInfo.setTraceId(generateTraceId());
            traceInfo.setSpanId(generateSpanId());
        }
        
        traceInfo.setServiceName(serviceName);
        traceInfo.setMethodName(methodName);
        traceInfo.setStartTime(System.currentTimeMillis());
        
        // 存储到全局追踪信息中
        GLOBAL_TRACES.put(traceInfo.getTraceId(), traceInfo);
        
        log.debug("开始追踪 traceId: {}, spanId: {}, service: {}, method: {}", 
                traceInfo.getTraceId(), traceInfo.getSpanId(), serviceName, methodName);
    }
    
    /**
     * 结束当前Span
     */
    public static void finishSpan() {
        TraceInfo traceInfo = TRACE_CONTEXT.get();
        if (traceInfo != null) {
            traceInfo.setEndTime(System.currentTimeMillis());
            
            log.debug("结束追踪 traceId: {}, spanId: {}, duration: {}ms", 
                    traceInfo.getTraceId(), traceInfo.getSpanId(), traceInfo.getDuration());
        }
    }
    
    /**
     * 获取当前追踪信息
     */
    public static TraceInfo getCurrentTrace() {
        return TRACE_CONTEXT.get();
    }
    
    /**
     * 根据追踪ID获取追踪信息
     */
    public static TraceInfo getTrace(String traceId) {
        return GLOBAL_TRACES.get(traceId);
    }
    
    /**
     * 清除当前线程的追踪信息
     */
    public static void clear() {
        TRACE_CONTEXT.remove();
    }
    
    /**
     * 清除指定追踪ID的信息
     */
    public static void clearTrace(String traceId) {
        GLOBAL_TRACES.remove(traceId);
    }
    
    /**
     * 获取或创建追踪信息
     */
    private static TraceInfo getOrCreateTraceInfo() {
        TraceInfo traceInfo = TRACE_CONTEXT.get();
        if (traceInfo == null) {
            traceInfo = new TraceInfo();
            TRACE_CONTEXT.set(traceInfo);
        }
        return traceInfo;
    }
    
    /**
     * 获取所有追踪信息
     */
    public static Map<String, TraceInfo> getAllTraces() {
        return new ConcurrentHashMap<>(GLOBAL_TRACES);
    }
    
    /**
     * 清除所有追踪信息
     */
    public static void clearAllTraces() {
        GLOBAL_TRACES.clear();
    }
}