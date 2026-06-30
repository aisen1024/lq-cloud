package cn.lingque.cloud.http.interceptor;

import cn.lingque.cloud.http.bean.HttpRequestInfo;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 认证拦截器接口
 * 专门处理登录认证相关逻辑
 */
public interface AuthenticationInterceptor extends HttpClientInterceptor {

    /**
     * 获取认证头信息
     * @param method 调用的方法
     * @param args 方法参数
     * @return 认证头信息，key为头名称，value为头值
     */
    Map<String, String> getAuthHeaders(Method method, Object[] args);

    /**
     * 获取认证相关的请求体参数
     * @param method 调用的方法
     * @param args 方法参数
     * @return 认证相关的请求体参数
     */
    default Map<String, Object> getAuthBodyParams(Method method, Object[] args) {
        return null;
    }

    /**
     * 检查是否需要认证
     * @param method 调用的方法
     * @param args 方法参数
     * @return true需要认证，false不需要认证
     */
    default boolean requiresAuthentication(Method method, Object[] args) {
        return true;
    }

    /**
     * 认证失败处理
     * @param request 请求信息
     * @param method 调用的方法
     * @param args 方法参数
     * @return 认证失败时的处理结果
     */
    default Object handleAuthenticationFailure(HttpRequestInfo request, Method method, Object[] args) {
        throw new RuntimeException("Authentication failed");
    }

    @Override
    default boolean beforeRequest(HttpRequestInfo request, Method method, Object[] args) {
        if (!requiresAuthentication(method, args)) {
            return true;
        }

        // 添加认证头
        Map<String, String> authHeaders = getAuthHeaders(method, args);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            request.getHeaders().putAll(authHeaders);
        }

        // 添加认证相关的请求体参数
        Map<String, Object> authBodyParams = getAuthBodyParams(method, args);
        if (authBodyParams != null && !authBodyParams.isEmpty()) {
            // 这里可以根据需要处理请求体参数
            // 例如合并到现有的请求体中
        }

        return true;
    }

    @Override
    default int getOrder() {
        return -100; // 认证拦截器优先级较高
    }
}