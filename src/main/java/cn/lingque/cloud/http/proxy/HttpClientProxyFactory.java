package cn.lingque.cloud.http.proxy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.lingque.cloud.http.annotation.*;
import cn.lingque.runner.annon.LqService;
import cn.lingque.cloud.http.bean.HttpRequestInfo;
import cn.lingque.cloud.http.bean.HttpResponseInfo;
import cn.lingque.cloud.http.executor.HttpExecutor;
import cn.lingque.cloud.http.interceptor.InterceptorManager;
import cn.lingque.cloud.http.processor.ResponseProcessorManager;
import cn.lingque.cloud.http.resolver.ServiceResolver;
import cn.lingque.cloud.http.util.LqSpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端代理工厂
 * 负责创建HTTP客户端接口的动态代理
 * 
 * @author aisen
 * @date 2024-12-19
 */
@Slf4j
public class HttpClientProxyFactory {
    
    private static final ServiceResolver serviceResolver = new ServiceResolver();
    
    /**
     * 创建HTTP客户端代理
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("只能为接口创建代理: " + interfaceClass.getName());
        }
        
        HttpClient httpClient = interfaceClass.getAnnotation(HttpClient.class);
        LqService lqService = interfaceClass.getAnnotation(LqService.class);
        
        if (httpClient == null && lqService == null) {
            throw new IllegalArgumentException("接口必须标注@HttpClient或@LqService注解: " + interfaceClass.getName());
        }
        
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new HttpClientInvocationHandler(httpClient, lqService)
        );
    }
    
    /**
     * HTTP客户端调用处理器
     */
    private static class HttpClientInvocationHandler implements InvocationHandler {
        
        private final HttpClient httpClient;
        private final LqService lqService;
        private InterceptorManager interceptorManager;
        private ResponseProcessorManager responseProcessorManager;
        
        public HttpClientInvocationHandler(HttpClient httpClient, LqService lqService) {
            this.httpClient = httpClient;
            this.lqService = lqService;
            try {
                this.interceptorManager = LqSpringUtil.getBean(InterceptorManager.class);
                this.responseProcessorManager = LqSpringUtil.getBean(ResponseProcessorManager.class);
            } catch (Exception e) {
                // 如果获取不到Bean，使用默认实现
                this.interceptorManager = new InterceptorManager(java.util.Collections.emptyList());
                this.responseProcessorManager = new ResponseProcessorManager(java.util.Collections.emptyList());
            }
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // 构建请求信息
            HttpRequestInfo requestInfo = buildRequestInfo(method, args);
            
            try {
                // 执行前置拦截器
                if (!interceptorManager.beforeRequest(requestInfo, method, args)) {
                    throw new RuntimeException("Request intercepted");
                }
                
                // 执行HTTP请求
                HttpResponseInfo responseInfo = HttpExecutor.execute(requestInfo);
                
                // 执行后置拦截器
                interceptorManager.afterRequest(requestInfo, responseInfo, method, args);
                
                // 处理响应
                return handleResponse(responseInfo, method.getGenericReturnType(), method, args);
                
            } catch (Exception e) {
                // 异常处理
                Exception handledException = interceptorManager.handleException(requestInfo, e, method, args);
                if (handledException != null) {
                    throw new RuntimeException(handledException);
                }
                return null;
            }
        }
        
        /**
         * 构建HTTP请求信息
         */
        private HttpRequestInfo buildRequestInfo(Method method, Object[] args) {
            String httpMethod = getHttpMethod(method);
            String path = getPath(method);
            String baseUrl = resolveBaseUrl();
            
            // 构建完整URL
            String url = buildUrl(baseUrl, path, method, args);
            
            // 构建请求头
            Map<String, String> headers = buildHeaders(method);
            
            // 构建请求体
            Object body = buildRequestBody(method, args);
            
            return HttpRequestInfo.builder()
                    .url(url)
                    .method(httpMethod)
                    .headers(headers)
                    .body(body)
                    .connectTimeout(getConnectTimeout(method))
                    .readTimeout(getReadTimeout(method))
                    .retryCount(getRetryCount(method))
                    .build();
        }
        
        /**
         * 获取HTTP方法
         */
        private String getHttpMethod(Method method) {
            if (method.isAnnotationPresent(Get.class)) {
                return "GET";
            } else if (method.isAnnotationPresent(Post.class)) {
                return "POST";
            } else if (method.isAnnotationPresent(Put.class)) {
                return "PUT";
            } else if (method.isAnnotationPresent(Delete.class)) {
                return "DELETE";
            }
            throw new IllegalArgumentException("方法必须标注HTTP方法注解: " + method.getName());
        }
        
        /**
         * 获取请求路径
         */
        private String getPath(Method method) {
            if (method.isAnnotationPresent(Get.class)) {
                return method.getAnnotation(Get.class).value();
            } else if (method.isAnnotationPresent(Post.class)) {
                return method.getAnnotation(Post.class).value();
            } else if (method.isAnnotationPresent(Put.class)) {
                return method.getAnnotation(Put.class).value();
            } else if (method.isAnnotationPresent(Delete.class)) {
                return method.getAnnotation(Delete.class).value();
            }
            return "";
        }
        
        /**
         * 解析基础URL
         */
        private String resolveBaseUrl() {
            // 优先使用HttpClient配置
            if (httpClient != null) {
                if (StrUtil.isNotBlank(httpClient.baseUrl())) {
                    return httpClient.baseUrl();
                }
                if (StrUtil.isNotBlank(httpClient.serviceName())) {
                    return serviceResolver.resolveServiceUrl(httpClient.serviceName());
                }
            }
            
            // 使用LqService配置
            if (lqService != null && StrUtil.isNotBlank(lqService.serviceName())) {
                return serviceResolver.resolveServiceUrl(lqService.serviceName());
            }
            
            throw new IllegalArgumentException("必须指定baseUrl或serviceName");
        }
        
        /**
         * 构建完整URL
         */
        private String buildUrl(String baseUrl, String path, Method method, Object[] args) {
            String url = baseUrl;
            if (StrUtil.isNotBlank(path)) {
                if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
                    url += "/";
                }
                url += path;
            }
            
            // 替换路径变量
            url = replacePathVariables(url, method, args);
            
            // 添加查询参数
            url = addQueryParams(url, method, args);
            
            return url;
        }
        
        /**
         * 替换路径变量
         */
        private String replacePathVariables(String url, Method method, Object[] args) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
                if (pathVariable != null && args[i] != null) {
                    String varName = StrUtil.isNotBlank(pathVariable.value()) ? 
                            pathVariable.value() : parameters[i].getName();
                    url = url.replace("{" + varName + "}", String.valueOf(args[i]));
                }
            }
            return url;
        }
        
        /**
         * 添加查询参数
         */
        private String addQueryParams(String url, Method method, Object[] args) {
            StringBuilder queryParams = new StringBuilder();
            Parameter[] parameters = method.getParameters();
            
            for (int i = 0; i < parameters.length; i++) {
                RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
                if (requestParam != null && args[i] != null) {
                    String paramName = StrUtil.isNotBlank(requestParam.value()) ? 
                            requestParam.value() : parameters[i].getName();
                    
                    if (queryParams.length() > 0) {
                        queryParams.append("&");
                    }
                    queryParams.append(paramName).append("=").append(args[i]);
                }
            }
            
            if (queryParams.length() > 0) {
                url += (url.contains("?") ? "&" : "?") + queryParams.toString();
            }
            
            return url;
        }
        
        /**
         * 构建请求头
         */
        private Map<String, String> buildHeaders(Method method) {
            Map<String, String> headers = new HashMap<>();
            
            // 添加默认Content-Type
            if (method.isAnnotationPresent(Post.class)) {
                headers.put("Content-Type", method.getAnnotation(Post.class).contentType());
            } else if (method.isAnnotationPresent(Put.class)) {
                headers.put("Content-Type", method.getAnnotation(Put.class).contentType());
            }
            
            // 添加自定义请求头
            String[] customHeaders = getCustomHeaders(method);
            for (String header : customHeaders) {
                String[] parts = header.split(":", 2);
                if (parts.length == 2) {
                    headers.put(parts[0].trim(), parts[1].trim());
                }
            }
            
            return headers;
        }
        
        /**
         * 获取自定义请求头
         */
        private String[] getCustomHeaders(Method method) {
            if (method.isAnnotationPresent(Get.class)) {
                return method.getAnnotation(Get.class).headers();
            } else if (method.isAnnotationPresent(Post.class)) {
                return method.getAnnotation(Post.class).headers();
            } else if (method.isAnnotationPresent(Put.class)) {
                return method.getAnnotation(Put.class).headers();
            } else if (method.isAnnotationPresent(Delete.class)) {
                return method.getAnnotation(Delete.class).headers();
            }
            return new String[0];
        }
        
        /**
         * 构建请求体
         */
        private Object buildRequestBody(Method method, Object[] args) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                    return args[i];
                }
            }
            return null;
        }
        
        /**
         * 获取连接超时时间
         */
        private int getConnectTimeout(Method method) {
            int timeout = -1;
            if (method.isAnnotationPresent(Get.class)) {
                timeout = method.getAnnotation(Get.class).connectTimeout();
            } else if (method.isAnnotationPresent(Post.class)) {
                timeout = method.getAnnotation(Post.class).connectTimeout();
            } else if (method.isAnnotationPresent(Put.class)) {
                timeout = method.getAnnotation(Put.class).connectTimeout();
            } else if (method.isAnnotationPresent(Delete.class)) {
                timeout = method.getAnnotation(Delete.class).connectTimeout();
            }
            
            if (timeout > 0) {
                return timeout;
            }
            
            // 使用类级别配置
            if (httpClient != null) {
                return httpClient.connectTimeout();
            }
            
            if (lqService != null) {
                return lqService.connectTimeout();
            }
            
            return 5000; // 默认值
        }
        
        /**
         * 获取读取超时时间
         */
        private int getReadTimeout(Method method) {
            int timeout = -1;
            if (method.isAnnotationPresent(Get.class)) {
                timeout = method.getAnnotation(Get.class).readTimeout();
            } else if (method.isAnnotationPresent(Post.class)) {
                timeout = method.getAnnotation(Post.class).readTimeout();
            } else if (method.isAnnotationPresent(Put.class)) {
                timeout = method.getAnnotation(Put.class).readTimeout();
            } else if (method.isAnnotationPresent(Delete.class)) {
                timeout = method.getAnnotation(Delete.class).readTimeout();
            }
            
            if (timeout > 0) {
                return timeout;
            }
            
            // 使用类级别配置
            if (httpClient != null) {
                return httpClient.readTimeout();
            }
            
            if (lqService != null) {
                return lqService.readTimeout();
            }
            
            return 30000; // 默认值
        }
        
        /**
         * 获取重试次数
         */
        private int getRetryCount(Method method) {
            int retryCount = -1;
            if (method.isAnnotationPresent(Get.class)) {
                retryCount = method.getAnnotation(Get.class).retryCount();
            } else if (method.isAnnotationPresent(Post.class)) {
                retryCount = method.getAnnotation(Post.class).retryCount();
            } else if (method.isAnnotationPresent(Put.class)) {
                retryCount = method.getAnnotation(Put.class).retryCount();
            } else if (method.isAnnotationPresent(Delete.class)) {
                retryCount = method.getAnnotation(Delete.class).retryCount();
            }
            
            if (retryCount >= 0) {
                return retryCount;
            }
            
            // 使用类级别配置
            if (httpClient != null) {
                return httpClient.retryCount();
            }
            
            if (lqService != null) {
                return lqService.retryCount();
            }
            
            return 0; // 默认值
        }
        
        /**
         * 处理响应
         */
        private Object handleResponse(HttpResponseInfo responseInfo, Type returnType, Method method, Object[] args) {
            if (!responseInfo.isSuccess()) {
                throw new RuntimeException("HTTP请求失败: " + responseInfo.getStatusCode() + ", " + responseInfo.getBody());
            }
            
            // 使用响应处理器处理响应
            if (responseProcessorManager != null) {
                return responseProcessorManager.processResponse(responseInfo, method, returnType);
            }
            
            String responseBody = responseInfo.getBody();
            
            if (returnType == void.class || returnType == Void.class) {
                return null;
            }
            
            if (returnType == String.class) {
                return responseBody;
            }
            
            if (returnType == HttpResponseInfo.class) {
                return responseInfo;
            }
            
            // JSON反序列化
            try {
                if (returnType instanceof Class) {
                    return JSONUtil.toBean(responseBody, (Class<?>) returnType);
                } else {
                    // 对于复杂泛型类型，先解析为JSONObject再处理
                    return JSONUtil.parseObj(responseBody);
                }
            } catch (Exception e) {
                log.error("响应反序列化失败: {}", responseBody, e);
                throw new RuntimeException("响应反序列化失败", e);
            }
        }
    }
}