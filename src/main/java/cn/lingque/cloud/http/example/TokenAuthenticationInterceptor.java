package cn.lingque.cloud.http.example;

import cn.lingque.cloud.http.interceptor.AuthenticationInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Token认证拦截器示例
 * 演示如何实现基于Token的认证
 */
@Slf4j
@Component
public class TokenAuthenticationInterceptor implements AuthenticationInterceptor {

    // 模拟Token存储，实际项目中可能从Redis、数据库或其他地方获取
    private String currentToken = null;

    @Override
    public Map<String, String> getAuthHeaders(Method method, Object[] args) {
        Map<String, String> headers = new HashMap<>();
        
        // 获取当前Token
        String token = getCurrentToken();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
            headers.put("X-Client-Type", "HTTP_CLIENT");
        }
        
        return headers;
    }

    @Override
    public Map<String, Object> getAuthBodyParams(Method method, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        
        // 可以添加一些通用的认证参数
        params.put("clientId", "lq-cloud-client");
        params.put("timestamp", System.currentTimeMillis());
        
        return params;
    }

    @Override
    public boolean requiresAuthentication(Method method, Object[] args) {
        // 检查方法是否需要认证
        // 可以通过注解、方法名等方式判断
        String methodName = method.getName();
        
        // 示例：login方法不需要认证
        if ("login".equals(methodName) || "register".equals(methodName)) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取当前Token
     * 实际项目中可能从以下地方获取：
     * 1. ThreadLocal
     * 2. Redis
     * 3. 数据库
     * 4. 配置文件
     * 5. 其他认证服务
     */
    private String getCurrentToken() {
        if (currentToken == null) {
            // 模拟获取Token的过程
            currentToken = generateOrRefreshToken();
        }
        return currentToken;
    }

    /**
     * 生成或刷新Token
     * 实际项目中这里会调用认证服务
     */
    private String generateOrRefreshToken() {
        try {
            // 模拟调用认证服务获取Token
            log.info("Generating new authentication token...");
            
            // 这里可以调用登录接口获取Token
            // HttpResponse response = HttpUtil.post("http://auth-service/login", loginParams);
            // return parseTokenFromResponse(response);
            
            // 模拟返回Token
            return "mock-token-" + System.currentTimeMillis();
            
        } catch (Exception e) {
            log.error("Failed to generate authentication token", e);
            return null;
        }
    }

    /**
     * 设置Token（供外部调用）
     */
    public void setToken(String token) {
        this.currentToken = token;
        log.info("Authentication token updated");
    }

    /**
     * 清除Token
     */
    public void clearToken() {
        this.currentToken = null;
        log.info("Authentication token cleared");
    }
}