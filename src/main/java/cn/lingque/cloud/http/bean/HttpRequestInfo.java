package cn.lingque.cloud.http.bean;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * HTTP请求信息
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Data
@Builder
public class HttpRequestInfo {
    
    /**
     * 请求URL
     */
    private String url;
    
    /**
     * 请求方法
     */
    private String method;
    
    /**
     * 请求头
     */
    private Map<String, String> headers;
    
    /**
     * 请求体
     */
    private Object body;
    
    /**
     * 连接超时时间
     */
    private int connectTimeout;
    
    /**
     * 读取超时时间
     */
    private int readTimeout;
    
    /**
     * 重试次数
     */
    private int retryCount;
}