package com.paypay.lqadmin.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 */
@Service
public class AuthService {

    // 默认账号配置
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";
    
    // Token 存储：token -> username
    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    
    // Token 有效期（毫秒）- 24小时
    private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000;

    /**
     * 认证用户
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        
        // 验证默认账号
        return DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password);
    }

    /**
     * 生成 Token
     */
    public String generateToken(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expiryTime = System.currentTimeMillis() + TOKEN_VALIDITY;
        
        tokenStore.put(token, new TokenInfo(username, expiryTime));
        
        // 清理过期 token
        cleanExpiredTokens();
        
        return token;
    }

    /**
     * 验证 Token
     */
    public String validateToken(String token) {
        if (token == null) {
            return null;
        }
        
        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo == null) {
            return null;
        }
        
        // 检查是否过期
        if (tokenInfo.isExpired()) {
            tokenStore.remove(token);
            return null;
        }
        
        return tokenInfo.getUsername();
    }

    /**
     * 使 Token 失效
     */
    public void invalidateToken(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }

    /**
     * 清理过期的 Token
     */
    private void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * Token 信息
     */
    private static class TokenInfo {
        private final String username;
        private final long expiryTime;

        public TokenInfo(String username, long expiryTime) {
            this.username = username;
            this.expiryTime = expiryTime;
        }

        public String getUsername() {
            return username;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long now) {
            return now > expiryTime;
        }
    }
}
