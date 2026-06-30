# 安全配置指南

## 默认账号

LingQue Admin 提供了一个默认账号用于快速开始：

```
用户名: admin
密码: admin123
```

## 安全建议

### 1. 修改默认密码

⚠️ **重要**：在生产环境中，请务必修改默认密码！

修改方式：编辑 `lq-admin/src/main/java/com/paypay/lqadmin/service/AuthService.java`

```java
// 修改这两个常量
private static final String DEFAULT_USERNAME = "admin";
private static final String DEFAULT_PASSWORD = "your_secure_password";
```

### 2. Token 有效期

默认 Token 有效期为 24 小时，可以在 `AuthService.java` 中修改：

```java
// Token 有效期（毫秒）
private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000;
```

### 3. 使用 HTTPS

生产环境建议使用 HTTPS 协议：

```properties
# application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your_password
server.ssl.key-store-type=PKCS12
```

### 4. 配置防火墙

确保只有授权的 IP 可以访问管理端口：

```bash
# 示例：只允许内网访问
iptables -A INPUT -p tcp --dport 8080 -s 192.168.0.0/16 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j DROP
```

### 5. 使用反向代理

建议使用 Nginx 作为反向代理，添加额外的安全层：

```nginx
server {
    listen 80;
    server_name admin.example.com;
    
    # 限制请求速率
    limit_req_zone $binary_remote_addr zone=admin:10m rate=10r/s;
    limit_req zone=admin burst=20;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 6. 启用日志审计

监控所有管理操作：

```properties
# application.properties
logging.level.com.paypay.lqadmin=DEBUG
logging.file.name=logs/lq-admin.log
```

### 7. 定期更新依赖

定期检查并更新依赖包，修复已知的安全漏洞：

```bash
mvn versions:display-dependency-updates
```

## 未来增强

计划中的安全功能：

- [ ] 多用户支持
- [ ] 角色权限管理（RBAC）
- [ ] 操作审计日志
- [ ] IP 白名单
- [ ] 双因素认证（2FA）
- [ ] 密码复杂度要求
- [ ] 登录失败锁定
- [ ] Session 管理

## 报告安全问题

如果发现安全漏洞，请发送邮件至：security@example.com

请勿公开披露安全问题，我们会尽快响应并修复。
