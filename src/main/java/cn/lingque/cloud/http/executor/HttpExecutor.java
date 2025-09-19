package cn.lingque.cloud.http.executor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.http.bean.HttpRequestInfo;
import cn.lingque.cloud.http.bean.HttpResponseInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * HTTP请求执行器
 * 负责实际的HTTP请求执行
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class HttpExecutor {
    
    /**
     * 执行HTTP请求
     */
    public static HttpResponseInfo execute(HttpRequestInfo requestInfo) {
        try {
            HttpRequest request = createRequest(requestInfo);
            HttpResponse response = request.execute();
            
            return HttpResponseInfo.builder()
                    .statusCode(response.getStatus())
                    .body(response.body())
                    .headers(response.headers())
                    .success(response.isOk())
                    .build();
                    
        } catch (Exception e) {
            log.error("HTTP请求执行失败: {}", requestInfo.getUrl(), e);
            return HttpResponseInfo.builder()
                    .statusCode(500)
                    .body(e.getMessage())
                    .success(false)
                    .build();
        }
    }
    
    /**
     * 创建HTTP请求
     */
    private static HttpRequest createRequest(HttpRequestInfo requestInfo) {
        HttpRequest request;
        
        switch (requestInfo.getMethod().toUpperCase()) {
            case "GET":
                request = HttpUtil.createGet(requestInfo.getUrl());
                break;
            case "POST":
                request = HttpUtil.createPost(requestInfo.getUrl());
                if (requestInfo.getBody() != null) {
                    if (requestInfo.getBody() instanceof String) {
                        request.body((String) requestInfo.getBody());
                    } else {
                        request.body(JSONUtil.toJsonStr(requestInfo.getBody()));
                    }
                }
                break;
            case "PUT":
                request = HttpRequest.put(requestInfo.getUrl());
                if (requestInfo.getBody() != null) {
                    if (requestInfo.getBody() instanceof String) {
                        request.body((String) requestInfo.getBody());
                    } else {
                        request.body(JSONUtil.toJsonStr(requestInfo.getBody()));
                    }
                }
                break;
            case "DELETE":
                request = HttpRequest.delete(requestInfo.getUrl());
                break;
            default:
                throw new IllegalArgumentException("不支持的HTTP方法: " + requestInfo.getMethod());
        }
        
        // 设置请求头
        if (requestInfo.getHeaders() != null) {
            for (Map.Entry<String, String> entry : requestInfo.getHeaders().entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        
        // 设置超时时间
        if (requestInfo.getConnectTimeout() > 0) {
            request.timeout(requestInfo.getConnectTimeout());
        }
        
        return request;
    }
}