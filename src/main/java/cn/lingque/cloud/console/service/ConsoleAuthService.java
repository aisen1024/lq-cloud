package cn.lingque.cloud.console.service;

import cn.lingque.cloud.console.config.LQConsoleProperties;
import cn.lingque.cloud.console.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 控制台认证服务
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Service
public class ConsoleAuthService {
    
    private final LQConsoleProperties properties;
    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    public ConsoleAuthService(LQConsoleProperties properties, JwtTokenProvider jwtTokenProvider) {
        this.properties = properties;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * 用户会话信息
     */
    public static class UserSession {
        private final String username;
        private final String token;
        private final long loginTime;
        private long lastAccessTime;
        
        public UserSession(String username, String token) {
            this.username = username;
            this.token = token;
            this.loginTime = System.currentTimeMillis();
            this.lastAccessTime = loginTime;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getToken() {
            return token;
        }
        
        public long getLoginTime() {
            return loginTime;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public boolean isExpired(int timeoutMinutes) {
            long timeoutMillis = timeoutMinutes * 60 * 1000L;
            return System.currentTimeMillis() - lastAccessTime > timeoutMillis;
        }
    }
    
    /**
     * 登录认证
     */
    public AuthResult authenticate(String username, String password) {
        // 检查认证是否启用
        if (!properties.getSecurity().isAuthEnabled()) {
            String token = jwtTokenProvider.createToken(username);
            UserSession session = new UserSession(username, token);
            activeSessions.put(token, session);
            return AuthResult.success(token, session);
        }
        
        // 验证用户名和密码
        if (!properties.getSecurity().getUsername().equals(username) || 
            !properties.getSecurity().getPassword().equals(password)) {
            return AuthResult.failure("用户名或密码错误");
        }
        
        // 检查并发会话限制
        long currentSessions = activeSessions.values().stream()
                .filter(session -> session.getUsername().equals(username))
                .count();
        
        if (currentSessions >= properties.getSession().getMaxConcurrentSessions()) {
            return AuthResult.failure("超过最大并发会话数限制");
        }
        
        // 创建令牌和会话
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "admin");
        claims.put("loginTime", System.currentTimeMillis());
        
        String token = jwtTokenProvider.createToken(username, claims);
        UserSession session = new UserSession(username, token);
        activeSessions.put(token, session);
        
        return AuthResult.success(token, session);
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        UserSession session = activeSessions.get(token);
        if (session == null) {
            return false;
        }
        
        // 检查会话是否过期
        if (session.isExpired(properties.getSession().getTimeoutMinutes())) {
            activeSessions.remove(token);
            return false;
        }
        
        // 验证JWT令牌
        if (!jwtTokenProvider.validateToken(token)) {
            activeSessions.remove(token);
            return false;
        }
        
        // 更新最后访问时间
        session.updateLastAccessTime();
        return true;
    }
    
    /**
     * 获取用户会话
     */
    public UserSession getUserSession(String token) {
        return activeSessions.get(token);
    }
    
    /**
     * 登出
     */
    public void logout(String token) {
        activeSessions.remove(token);
    }
    
    /**
     * 获取活跃会话列表
     */
    public Map<String, UserSession> getActiveSessions() {
        // 清理过期会话
        cleanExpiredSessions();
        return new HashMap<>(activeSessions);
    }
    
    /**
     * 清理过期会话
     */
    public void cleanExpiredSessions() {
        int timeoutMinutes = properties.getSession().getTimeoutMinutes();
        activeSessions.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(timeoutMinutes));
        
        // 清理JWT令牌
        jwtTokenProvider.cleanExpiredTokens();
    }
    
    /**
     * 刷新令牌
     */
    public String refreshToken(String oldToken) {
        UserSession session = activeSessions.get(oldToken);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        
        // 生成新令牌
        String newToken = jwtTokenProvider.refreshToken(oldToken);
        
        // 更新会话
        activeSessions.remove(oldToken);
        UserSession newSession = new UserSession(session.getUsername(), newToken);
        activeSessions.put(newToken, newSession);
        
        return newToken;
    }
    
    /**
     * 认证结果
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String token;
        private final UserSession session;
        
        private AuthResult(boolean success, String message, String token, UserSession session) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.session = session;
        }
        
        public static AuthResult success(String token, UserSession session) {
            return new AuthResult(true, "登录成功", token, session);
        }
        
        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getToken() {
            return token;
        }
        
        public UserSession getSession() {
            return session;
        }
    }
}