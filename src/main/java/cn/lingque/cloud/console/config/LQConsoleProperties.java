package cn.lingque.cloud.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 控制台配置属性
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "lq.console")
public class LQConsoleProperties {
    
    /**
     * 是否启用控制台
     */
    private boolean enabled = true;
    
    /**
     * 控制台端口
     */
    private int port = 8080;
    
    /**
     * 控制台路径前缀
     */
    private String contextPath = "/lq-console";
    
    /**
     * 安全配置
     */
    private Security security = new Security();
    
    /**
     * 会话配置
     */
    private Session session = new Session();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public Security getSecurity() {
        return security;
    }
    
    public void setSecurity(Security security) {
        this.security = security;
    }
    
    public Session getSession() {
        return session;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }
    
    /**
     * 安全配置
     */
    public static class Security {
        /**
         * 用户名
         */
        private String username = "admin";
        
        /**
         * 密码
         */
        private String password = "admin123";
        
        /**
         * 是否启用认证
         */
        private boolean authEnabled = true;
        
        /**
         * JWT密钥
         */
        private String jwtSecret = "lq-console-secret-key-2024";
        
        /**
         * Token过期时间（小时）
         */
        private int tokenExpireHours = 24;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public boolean isAuthEnabled() {
            return authEnabled;
        }
        
        public void setAuthEnabled(boolean authEnabled) {
            this.authEnabled = authEnabled;
        }
        
        public String getJwtSecret() {
            return jwtSecret;
        }
        
        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }
        
        public int getTokenExpireHours() {
            return tokenExpireHours;
        }
        
        public void setTokenExpireHours(int tokenExpireHours) {
            this.tokenExpireHours = tokenExpireHours;
        }
    }
    
    /**
     * 会话配置
     */
    public static class Session {
        /**
         * 会话超时时间（分钟）
         */
        private int timeoutMinutes = 30;
        
        /**
         * 最大并发会话数
         */
        private int maxConcurrentSessions = 10;
        
        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }
        
        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }
        
        public int getMaxConcurrentSessions() {
            return maxConcurrentSessions;
        }
        
        public void setMaxConcurrentSessions(int maxConcurrentSessions) {
            this.maxConcurrentSessions = maxConcurrentSessions;
        }
    }
}