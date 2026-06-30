package cn.lingque.cloud.http.interceptor;

import cn.lingque.cloud.http.bean.HttpRequestInfo;
import cn.lingque.cloud.http.bean.HttpResponseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 拦截器管理器
 * 负责管理和执行所有的HTTP客户端拦截器
 */
@Slf4j
@Component
public class InterceptorManager {

    private final List<HttpClientInterceptor> interceptors;

    @Autowired
    public InterceptorManager(List<HttpClientInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
        // 按优先级排序
        this.interceptors.sort(Comparator.comparingInt(HttpClientInterceptor::getOrder));
        
        log.info("Loaded {} HTTP client interceptors", this.interceptors.size());
        for (HttpClientInterceptor interceptor : this.interceptors) {
            log.debug("Interceptor: {} with order: {}", 
                interceptor.getClass().getSimpleName(), interceptor.getOrder());
        }
    }

    /**
     * 执行请求前拦截
     * @param request 请求信息
     * @param method 调用的方法
     * @param args 方法参数
     * @return true继续执行，false中断请求
     */
    public boolean beforeRequest(HttpRequestInfo request, Method method, Object[] args) {
        for (HttpClientInterceptor interceptor : interceptors) {
            try {
                if (!interceptor.beforeRequest(request, method, args)) {
                    log.debug("Request intercepted by: {}", interceptor.getClass().getSimpleName());
                    return false;
                }
            } catch (Exception e) {
                log.error("Error in beforeRequest interceptor: {}", 
                    interceptor.getClass().getSimpleName(), e);
                // 继续执行其他拦截器
            }
        }
        return true;
    }

    /**
     * 执行请求后拦截
     * @param request 请求信息
     * @param response 响应信息
     * @param method 调用的方法
     * @param args 方法参数
     */
    public void afterRequest(HttpRequestInfo request, HttpResponseInfo response, Method method, Object[] args) {
        // 逆序执行afterRequest
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            HttpClientInterceptor interceptor = interceptors.get(i);
            try {
                interceptor.afterRequest(request, response, method, args);
            } catch (Exception e) {
                log.error("Error in afterRequest interceptor: {}", 
                    interceptor.getClass().getSimpleName(), e);
                // 继续执行其他拦截器
            }
        }
    }

    /**
     * 处理异常
     * @param request 请求信息
     * @param exception 异常
     * @param method 调用的方法
     * @param args 方法参数
     * @return 处理后的异常
     */
    public Exception handleException(HttpRequestInfo request, Exception exception, Method method, Object[] args) {
        Exception currentException = exception;
        
        for (HttpClientInterceptor interceptor : interceptors) {
            try {
                Exception handledException = interceptor.handleException(request, currentException, method, args);
                if (handledException != currentException) {
                    currentException = handledException;
                    if (currentException == null) {
                        log.debug("Exception handled by: {}", interceptor.getClass().getSimpleName());
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Error in handleException interceptor: {}", 
                    interceptor.getClass().getSimpleName(), e);
                // 继续执行其他拦截器
            }
        }
        
        return currentException;
    }
}