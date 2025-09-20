# LQ Cloud 项目文档

LQ Cloud 是一个基于 Redis 的高性能云框架，提供配置中心、消息队列、节点注册、RPC 调用等功能。经过重构，优化了性能和使用体验。

## 主要特性
- 高性能配置中心
- 统一消息队列支持
- 增强版节点注册中心
- 分布式 RPC 框架
- MCP 工具管理

## 安装
通过 Maven 引入依赖：
```
<dependency>
    <groupId>io.github.aisen1024</groupId>
    <artifactId>lq-cloud</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 相关配置
### 基础配置 (application.yml 示例)
```yaml
server:
  port: 8081

spring:
  application:
    name: lq-cloud

lq:
  ip: 127.0.0.1
  port: 6379
  console:
    enabled: true
    port: 8080
    security:
      username: admin
      password: admin123
```

### 增强版配置中心属性 (LQEnhancedConfigProperties)
- enabled: 是否启用 (默认 true)
- autoCleanup: 自动清理过期配置 (默认 true)
- cleanupInterval: 清理间隔 (默认 300 秒)
- defaultTtl: 默认 TTL (默认 30 分钟)
- maxConfigCount: 最大配置数量 (默认 10000)

### MCP 工具配置 (LQMCPToolProperties)
- enabled: 是否启用 (默认 true)
- server.port: 服务器端口 (默认 8090)
- client.enabled: 客户端启用 (默认 true)

更多配置详见源代码中的属性类。

## 使用案例
### 配置中心示例
```java
// 获取配置
String value = LQEnhancedConfigCenter.getConfig("namespace", "key");

// 设置配置
LQEnhancedConfigCenter.setConfig("namespace", "key", "value", 3600000L); // TTL 1小时

// 监听配置变更
LQEnhancedConfigCenter.addListener("namespace", "key", event -> {
    log.info("配置变更: {} = {}", event.getKey(), event.getNewValue());
});
```

### 消息队列示例
使用 @LqMQListener 注解：
```java
@LqMQListener(key = "user.message", type = LqMqType.UNIFIED)
public void handleUserMessage(UserMessage message) {
    // 处理消息
}
```

### RPC 调用示例
```java
// 服务接口
@LQService(name = "userService")
public interface UserService {
    @LQServiceMethod
    User getUserById(Long id);
}

// 调用
UserService userService = LQDistributedServiceCaller.createProxy(UserService.class);
User user = userService.getUserById(1L);
```

更多示例见 src/test/java 中的测试类，如 LQEnhancedConfigExample.java、LQMCPToolUsageExample.java 等。

## 性能对比
### MQ 队列重构前后对比
- **监听机制**:
  - 旧: 全局 50ms 轮询所有队列
  - 新: 每个队列独立 10ms 监听 (快 5 倍)，延迟队列统一 500ms 处理
- **资源消耗**:
  - CPU: 避免无意义轮询，只处理有消息队列
  - 内存: 独立状态管理，避免全局竞争
  - 线程: 按需启动，不使用队列不消耗资源
- **整体提升**: 响应时间减少 80%，资源利用率提高 50% (基于内部基准测试)

更多性能细节见 src/main/java/cn/lingque/mq/README_MQ_REFACTOR.md。
