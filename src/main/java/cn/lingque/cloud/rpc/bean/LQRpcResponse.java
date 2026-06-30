package cn.lingque.cloud.rpc.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * LQ RPC响应对象
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Accessors(chain = true)
public class LQRpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 追踪ID */
    private String traceId;
    
    /** 是否成功 */
    private boolean success;
    
    /** 返回结果 */
    private Object result;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 错误代码 */
    private String errorCode;
    
    /** 异常堆栈 */
    private String stackTrace;
    
    /** 响应时间戳 */
    private Long timestamp;
    
    /** 服务器IP */
    private String serverIp;
    
    /** 处理耗时（毫秒） */
    private Long processingTime;
    
    /** 扩展属性 */
    private Map<String, Object> attachments;
    
    /**
     * 创建成功响应
     */
    public static LQRpcResponse success(Object result) {
        return new LQRpcResponse()
                .setSuccess(true)
                .setResult(result)
                .setTimestamp(System.currentTimeMillis());
    }
    
    /**
     * 创建成功响应（带追踪ID）
     */
    public static LQRpcResponse success(String traceId, Object result) {
        return new LQRpcResponse()
                .setTraceId(traceId)
                .setSuccess(true)
                .setResult(result)
                .setTimestamp(System.currentTimeMillis());
    }
    
    /**
     * 创建失败响应
     */
    public static LQRpcResponse failure(String errorMessage) {
        return new LQRpcResponse()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setTimestamp(System.currentTimeMillis());
    }
    
    /**
     * 创建失败响应（带追踪ID）
     */
    public static LQRpcResponse failure(String traceId, String errorMessage) {
        return new LQRpcResponse()
                .setTraceId(traceId)
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setTimestamp(System.currentTimeMillis());
    }
    
    /**
     * 创建失败响应（带错误代码）
     */
    public static LQRpcResponse failure(String traceId, String errorCode, String errorMessage) {
        return new LQRpcResponse()
                .setTraceId(traceId)
                .setSuccess(false)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage)
                .setTimestamp(System.currentTimeMillis());
    }
}