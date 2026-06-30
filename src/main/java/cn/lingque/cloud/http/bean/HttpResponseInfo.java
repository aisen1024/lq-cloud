package cn.lingque.cloud.http.bean;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * HTTP响应信息
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Builder
public class HttpResponseInfo {
    
    /**
     * 状态码
     */
    private int statusCode;
    
    /**
     * 响应体
     */
    private String body;
    
    /**
     * 响应头
     */
    private Map<String, String> headers;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}