package cn.lingque.cloud.http.interceptor;

import cn.lingque.cloud.http.bean.HttpRequestInfo;
import cn.lingque.cloud.http.bean.HttpResponseInfo;

import java.lang.reflect.Method;

/**
 * HTTP客户端拦截器接口
 * 提供请求前后的拦截处理能力
 */
public interface HttpClientInterceptor {

    /**
     * 拦截器优先级，数值越小优先级越高
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }

    /**
     * 请求前拦截
     * @param request 请求信息
     * @param method 调用的方法
     * @param args 方法参数
     * @return true继续执行，false中断请求
     */
    default boolean beforeRequest(HttpRequestInfo request, Method method, Object[] args) {
        return true;
    }

    /**
     * 请求后拦截
     * @param request 请求信息
     * @param response 响应信息
     * @param method 调用的方法
     * @param args 方法参数
     */
    default void afterRequest(HttpRequestInfo request, HttpResponseInfo response, Method method, Object[] args) {
        // 默认空实现
    }

    /**
     * 异常处理
     * @param request 请求信息
     * @param exception 异常
     * @param method 调用的方法
     * @param args 方法参数
     * @return 处理后的异常，返回null表示忽略异常
     */
    default Exception handleException(HttpRequestInfo request, Exception exception, Method method, Object[] args) {
        return exception;
    }
}