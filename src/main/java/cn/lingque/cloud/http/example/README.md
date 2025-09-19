# HTTP客户端框架使用示例

本文档展示如何使用灵雀云HTTP客户端框架，包括拦截器、响应处理器等高级功能。

## 1. 基本使用

### 1.1 定义服务接口

```java
@LqService(serviceName = "user-api-service")
public interface UserApiClient {
    
    @Get("/api/user/{id}")
    ApiResult<User> getUserById(@PathVariable("id") Long userId);
    
    @Post("/api/user")
    ApiResult<User> createUser(@RequestBody CreateUserRequest request);
}
```

### 1.2 注入和使用

```java
@Service
public class UserService {
    
    @Autowired
    private UserApiClient userApiClient;
    
    public User getUser(Long userId) {
        ApiResult<User> result = userApiClient.getUserById(userId);
        if (result.getSuccess()) {
            return result.getData();
        }
        throw new RuntimeException(result.getMessage());
    }
}
```

## 2. 认证拦截器

### 2.1 实现认证拦截器

```java
@Component
public class TokenAuthenticationInterceptor implements AuthenticationInterceptor {
    
    private static final String TOKEN_STORAGE_KEY = "auth_token";
    
    @Override
    public Map<String, String> getAuthHeaders(Method method, Object[] args) {
        String token = getStoredToken();
        if (token != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            return headers;
        }
        return Collections.emptyMap();
    }
    
    @Override
    public Map<String, Object> getAuthBodyParams(Method method, Object[] args) {
        // 可以添加通用的body参数，如时间戳、签名等
        Map<String, Object> params = new HashMap<>();
        params.put("timestamp", System.currentTimeMillis());
        return params;
    }
    
    @Override
    public boolean requiresAuthentication(Method method, Object[] args) {
        // 登录接口不需要认证
        return !"login".equals(method.getName());
    }
    
    @Override
    public void handleAuthenticationFailure(HttpResponseInfo response, Method method, Object[] args) {
        if (response.getStatusCode() == 401) {
            // 清除过期token
            clearStoredToken();
            log.warn("认证失败，已清除过期token");
        }
    }
    
    private String getStoredToken() {
        // 从缓存、数据库或其他存储中获取token
        return "your_stored_token";
    }
    
    private void clearStoredToken() {
        // 清除存储的token
    }
}
```

### 2.2 自定义拦截器

```java
@Component
public class LoggingInterceptor implements HttpClientInterceptor {
    
    @Override
    public void beforeRequest(HttpRequestInfo request, Method method, Object[] args) {
        log.info("发送请求: {} {}", request.getMethod(), request.getUrl());
        log.debug("请求头: {}", request.getHeaders());
        log.debug("请求体: {}", request.getBody());
    }
    
    @Override
    public void afterRequest(HttpResponseInfo response, Method method, Object[] args) {
        log.info("收到响应: {} - {}", response.getStatusCode(), response.getStatusText());
        log.debug("响应体: {}", response.getBody());
    }
    
    @Override
    public void handleException(Exception exception, Method method, Object[] args) {
        log.error("请求异常: {}", exception.getMessage(), exception);
    }
    
    @Override
    public int getOrder() {
        return 100; // 较低优先级，在认证拦截器之后执行
    }
}
```

## 3. 响应处理器

### 3.1 API响应处理器

```java
@Component
public class ApiResponseProcessor implements ResponseProcessor<Object> {
    
    @Override
    public boolean supports(Type returnType) {
        // 支持ApiResult<T>类型的响应
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            Type rawType = parameterizedType.getRawType();
            
            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                return "ApiResult".equals(rawClass.getSimpleName());
            }
        }
        return false;
    }
    
    @Override
    public Object processResponse(HttpResponseInfo response, Method method, Type returnType) {
        String responseBody = response.getBody();
        
        try {
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            
            Integer code = jsonResponse.getInt("code");
            String message = jsonResponse.getStr("message", "Unknown");
            Object data = jsonResponse.get("data");
            
            // 检查业务状态码
            if (code == null || code != 200) {
                return createErrorResult(message, code);
            }
            
            // 处理泛型类型
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                
                if (actualTypeArguments.length > 0) {
                    Type dataType = actualTypeArguments[0];
                    Object convertedData = convertData(data, dataType);
                    return createSuccessResult(convertedData, message);
                }
            }
            
            return createSuccessResult(data, message);
            
        } catch (Exception e) {
            return createErrorResult("Failed to parse response: " + e.getMessage());
        }
    }
}
```

### 3.2 自定义响应处理器

```java
@Component
public class XmlResponseProcessor implements ResponseProcessor<Object> {
    
    @Override
    public boolean supports(Type returnType) {
        // 支持XML响应的特定类型
        return returnType.getTypeName().contains("XmlResponse");
    }
    
    @Override
    public Object processResponse(HttpResponseInfo response, Method method, Type returnType) {
        String responseBody = response.getBody();
        
        // 处理XML响应
        // 这里可以使用XML解析库如JAXB、Jackson XML等
        
        return responseBody; // 简化示例
    }
    
    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
```

## 4. 配置

### 4.1 自动配置

框架会自动扫描并注册所有的拦截器和响应处理器：

```java
@Configuration
public class HttpClientAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public InterceptorManager interceptorManager(List<HttpClientInterceptor> interceptors) {
        return new InterceptorManager(interceptors);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ResponseProcessorManager responseProcessorManager(List<ResponseProcessor<?>> processors) {
        return new ResponseProcessorManager(processors);
    }
}
```

### 4.2 属性配置

```properties
# HTTP客户端配置
http.client.default.connectTimeout=5000
http.client.default.readTimeout=30000
http.client.default.retryCount=3

# 启用HTTP客户端框架
lq.http.client.enabled=true
```

## 5. 高级特性

### 5.1 条件拦截

```java
@Component
public class ConditionalInterceptor implements HttpClientInterceptor {
    
    @Override
    public void beforeRequest(HttpRequestInfo request, Method method, Object[] args) {
        // 只对特定方法进行拦截
        if (method.isAnnotationPresent(RequiresAuth.class)) {
            // 添加认证信息
            request.addHeader("Authorization", "Bearer " + getToken());
        }
    }
}
```

### 5.2 异步处理

```java
@Service
public class AsyncUserService {
    
    @Autowired
    private UserApiClient userApiClient;
    
    @Async
    public CompletableFuture<User> getUserAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            ApiResult<User> result = userApiClient.getUserById(userId);
            return result.getData();
        });
    }
}
```

### 5.3 缓存支持

```java
@Component
public class CacheInterceptor implements HttpClientInterceptor {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void beforeRequest(HttpRequestInfo request, Method method, Object[] args) {
        if (method.isAnnotationPresent(Cacheable.class)) {
            String cacheKey = generateCacheKey(method, args);
            Object cachedResult = redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                // 返回缓存结果（需要特殊处理）
            }
        }
    }
}
```

## 6. 最佳实践

1. **拦截器顺序**：认证拦截器应该有较高的优先级（较小的order值）
2. **异常处理**：在拦截器中妥善处理异常，避免影响正常请求流程
3. **性能考虑**：避免在拦截器中执行耗时操作
4. **日志记录**：合理使用日志级别，避免敏感信息泄露
5. **配置管理**：将可配置的参数提取到配置文件中

## 7. 故障排查

### 7.1 常见问题

1. **拦截器不生效**：检查是否正确注册为Spring Bean
2. **响应解析失败**：检查响应处理器的supports方法
3. **认证失败**：检查token获取和设置逻辑

### 7.2 调试技巧

```java
// 启用详细日志
logging.level.cn.lingque.cloud.http=DEBUG

// 在拦截器中添加调试日志
log.debug("拦截器执行: {}", this.getClass().getSimpleName());
```

通过以上示例，您可以充分利用HTTP客户端框架的拦截器和响应处理器功能，实现灵活的HTTP客户端调用。