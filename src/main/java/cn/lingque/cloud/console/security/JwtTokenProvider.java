package cn.lingque.cloud.console.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT令牌提供者（简化版本）
 * 
 * @author LingQue AI
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {
    
    private final String secret;
    private final long tokenValidityInMilliseconds;
    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    
    public JwtTokenProvider(String secret, int tokenExpireHours) {
        this.secret = secret;
        this.tokenValidityInMilliseconds = tokenExpireHours * 60 * 60 * 1000L;
    }
    
    
    /**
     * 令牌信息
     */
    private static class TokenInfo {
        private final String username;
        private final Date expiration;
        private final Map<String, Object> claims;
        
        public TokenInfo(String username, Date expiration, Map<String, Object> claims) {
            this.username = username;
            this.expiration = expiration;
            this.claims = claims != null ? new HashMap<>(claims) : new HashMap<>();
        }
        
        public String getUsername() {
            return username;
        }
        
        public Date getExpiration() {
            return expiration;
        }
        
        public Map<String, Object> getClaims() {
            return claims;
        }
        
        public boolean isExpired() {
            return expiration.before(new Date());
        }
    }
    
    /**
     * 创建令牌
     */
    public String createToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);
        
        // 生成简单的令牌
        String tokenData = username + ":" + validity.getTime();
        String signature = generateSignature(tokenData);
        String token = Base64.getEncoder().encodeToString((tokenData + ":" + signature).getBytes(StandardCharsets.UTF_8));
        
        // 存储令牌信息
        tokenStore.put(token, new TokenInfo(username, validity, claims));
        
        return token;
    }
    
    /**
     * 创建令牌（无额外声明）
     */
    public String createToken(String username) {
        return createToken(username, null);
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            TokenInfo tokenInfo = tokenStore.get(token);
            if (tokenInfo == null) {
                return false;
            }
            
            if (tokenInfo.isExpired()) {
                tokenStore.remove(token);
                return false;
            }
            
            // 验证签名
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                return false;
            }
            
            String tokenData = parts[0] + ":" + parts[1];
            String signature = parts[2];
            
            return signature.equals(generateSignature(tokenData));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        return tokenInfo != null ? tokenInfo.getUsername() : null;
    }
    
    /**
     * 获取声明
     */
    public Map<String, Object> getClaims(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        return tokenInfo != null ? tokenInfo.getClaims() : new HashMap<>();
    }
    
    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        return tokenInfo == null || tokenInfo.isExpired();
    }
    
    /**
     * 刷新令牌
     */
    public String refreshToken(String token) {
        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo == null) {
            throw new RuntimeException("令牌不存在");
        }
        
        // 移除旧令牌
        tokenStore.remove(token);
        
        // 创建新令牌
        return createToken(tokenInfo.getUsername(), tokenInfo.getClaims());
    }
    
    /**
     * 生成签名
     */
    private String generateSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }
    
    /**
     * 清理过期令牌
     */
    public void cleanExpiredTokens() {
        tokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}