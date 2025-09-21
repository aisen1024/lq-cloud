# LQ Cloud 项目文档

LQ Cloud 是一个基于 Redis 的高性能云框架，提供配置中心、消息队列、节点注册、RPC 调用等功能。经过重构，优化了性能和使用体验。

## 主要特性
- 高性能配置中心
- 统一消息队列支持
- 增强版节点注册中心
- 分布式 RPC 框架
- MCP 工具管理
- 模块化组件加载器
- 灵活的配置管理

## 安装
通过 Maven 引入依赖：
```xml
<dependency>
    <groupId>io.github.aisen1024</groupId>
    <artifactId>lq-cloud</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 组件加载器

### LqCloudRunner 组件加载器
LQ Cloud 提供了 `LqCloudRunner` 组件加载器，用于统一管理和启动各个组件。

#### 基本使用
```java
@Configuration
public class LQCloudConfig {
    
    @Bean
    public LqCloudRunner lqCloudRunner(LQProperties lqProperties) {
        return new LqCloudRunner(lqProperties);
    }
}
```

#### 组件启动流程
1. **配置检查** - 自动检查和补全服务配置
2. **基础初始化** - 初始化 Redis 连接和线程池
3. **注册中心启动** - 启动服务注册与发现
4. **消息总线启动** - 启动集群消息总线（可选）
5. **其他组件** - 根据配置启动其他组件

## 配置说明

### 基础配置 (application.yml)
```yaml
server:
  port: 8081                    # 应用服务端口

spring:
  application:
    name: lq-cloud              # 应用名称，用作服务名

# LQ Cloud 基础配置
ling-que:
  ip: 127.0.0.1                # Redis服务器IP地址
  port: 6379                   # Redis服务器端口
  db: 0                        # Redis数据库编号，默认0
  username: default            # Redis用户名，默认default
  password:                    # Redis密码，默认为空
  maxTotal: 50                 # Redis连接池最大连接数
  maxIdle: 20                  # Redis连接池最大空闲连接数
  minIdle: 10                  # Redis连接池最小空闲连接数
  maxWaitMillis: -1            # 获取连接最大等待时间(毫秒)，-1表示无限等待
  timeout: 10000               # Redis连接超时时间(毫秒)
  timeBetweenEvictionRuns: 100 # 空闲连接检查间隔时间(毫秒)
  minEvictableIdleTimeMillis: 60000  # 连接最小空闲时间(毫秒)
  
  # 模式配置
  mode: standalone             # Redis模式：standalone(单机)/cluster(集群)/sentinel(哨兵)
  
  # 哨兵模式配置
  sentinel:
    master: mymaster           # 哨兵主节点名称
    nodes:                     # 哨兵节点列表
      - 127.0.0.1:26379
      - 127.0.0.1:26380
      - 127.0.0.1:26381
  
  # 集群模式配置
  cluster:
    nodes:                     # 集群节点列表
      - 127.0.0.1:7000
      - 127.0.0.1:7001
      - 127.0.0.1:7002
    maxRedirects: 3            # 最大重定向次数
  
  # 线程池配置
  masterPool:                  # 主线程池配置
    corePoolSize: 10           # 核心线程数
    maximumPoolSize: 20        # 最大线程数
    keepAliveTime: 60          # 线程空闲存活时间(秒)
    queueCapacity: 1000        # 队列容量
    threadNamePrefix: "LQ-Master-"  # 线程名前缀
  
  slavePool:                   # 辅助线程池配置
    corePoolSize: 5            # 核心线程数
    maximumPoolSize: 10        # 最大线程数
    keepAliveTime: 60          # 线程空闲存活时间(秒)
    queueCapacity: 500         # 队列容量
    threadNamePrefix: "LQ-Slave-"   # 线程名前缀
  
  # 服务节点配置
  server:
    serverName: ${spring.application.name}  # 服务名称，默认使用应用名
    serverHost:                # 服务主机IP，默认自动获取本机IP
    serverPort: ${server.port} # 服务端口，默认使用应用端口
  
  # 消息总线配置
  bus:
    enable: true               # 是否启用消息总线
    
  # 配置中心配置
  config:
    dataIds:                   # 配置文件ID列表
      - dataId: "app-config"   # 配置文件ID
        group: "DEFAULT_GROUP" # 配置分组
        type: "yaml"           # 配置文件类型：yaml/json/properties
```

### 控制台配置
```yaml
lq:
  console:
    enabled: true              # 是否启用控制台，默认true
    port: 8080                 # 控制台端口，默认8080
    contextPath: /lq-console   # 控制台访问路径前缀
    
    # 安全配置
    security:
      username: admin          # 控制台登录用户名
      password: admin123       # 控制台登录密码
      sessionTimeout: 1800     # 会话超时时间(秒)，默认30分钟
      enableCsrf: false        # 是否启用CSRF保护
      allowedIps:              # 允许访问的IP列表，空表示允许所有
        - 127.0.0.1
        - 192.168.1.0/24
    
    # 会话配置
    session:
      cookieName: "LQ_SESSION" # 会话Cookie名称
      cookiePath: "/"          # Cookie路径
      cookieMaxAge: 1800       # Cookie最大存活时间(秒)
      cookieSecure: false      # 是否仅HTTPS传输Cookie
      cookieHttpOnly: true     # 是否仅HTTP访问Cookie
```

### 增强版配置中心
```yaml
lq:
  config:
    enhanced:
      enabled: true                    # 是否启用增强版配置中心
      autoCleanup: true               # 是否自动清理过期配置
      cleanupInitialDelay: 60         # 清理任务初始延迟(秒)
      cleanupInterval: 300            # 清理任务执行间隔(秒)，默认5分钟
      defaultTtl: 1800000            # 默认配置TTL(毫秒)，默认30分钟
      maxConfigCount: 10000          # 最大配置数量限制
      maxNamespaceCount: 100         # 最大命名空间数量限制
      enableStats: true              # 是否启用统计信息
      statsInterval: 600             # 统计信息打印间隔(秒)，默认10分钟
      enableChangeLog: true          # 是否启用配置变更日志
      enableConfigPush: true         # 是否启用配置推送
      configPushEvent: "lq:config:push"  # 配置推送事件名称
      
      # 初始配置列表
      initialConfigs:
        - namespace: "default"        # 命名空间
          key: "app.name"            # 配置键
          value: "lq-cloud"          # 配置值
          ttl: 3600000               # TTL(毫秒)
        - namespace: "database"
          key: "url"
          value: "jdbc:mysql://localhost:3306/test"
          ttl: -1                    # -1表示永不过期
```

### MCP 工具配置
```yaml
lq:
  mcp:
    enabled: true                      # 是否启用MCP工具模块
    autoDiscovery: true               # 是否启用自动发现工具
    healthCheck: true                 # 是否启用健康检查
    scanPackages:                     # 工具扫描包路径列表
      - cn.lingque
      - com.example.tools
    
    # 服务器配置
    server:
      enabled: true                   # 是否启用MCP服务器
      port: 8090                      # 服务器监听端口
      host: 0.0.0.0                   # 服务器绑定主机，0.0.0.0表示所有接口
      maxConnections: 100             # 最大并发连接数
      connectionTimeout: 30000        # 连接超时时间(毫秒)
      requestTimeout: 60000           # 请求处理超时时间(毫秒)
      
      # 线程池配置
      threadPool:
        coreSize: 10                  # 核心线程数
        maxSize: 50                   # 最大线程数
        queueCapacity: 1000           # 队列容量
        keepAliveSeconds: 60          # 线程空闲存活时间(秒)
    
    # 客户端配置
    client:
      enabled: false                  # 是否启用MCP客户端
      serverHost: localhost           # 目标服务器主机
      serverPort: 8080               # 目标服务器端口
      connectTimeout: 5000           # 连接超时时间(毫秒)
      readTimeout: 30000             # 读取超时时间(毫秒)
      retryCount: 3                  # 重试次数
      retryInterval: 1000            # 重试间隔(毫秒)
    
    # 注册中心配置
    registry:
      enabled: true                   # 是否启用注册中心
      address: localhost:6379         # 注册中心地址(Redis)
      serviceName: mcp-tools          # 服务名称
      serviceVersion: 1.0.0           # 服务版本
      heartbeatInterval: 30000        # 心跳间隔(毫秒)
      weight: 100                     # 服务权重(1-1000)
      tags:                           # 服务标签
        - production
        - mcp-tools
      metadata:                       # 服务元数据
        region: "us-west-1"
        zone: "zone-a"
    
    # 工具配置
    tools:
      # 自定义工具配置
      customTool:
        enabled: true                 # 是否启用该工具
        timeout: 30000               # 工具执行超时时间(毫秒)
        retryCount: 2                # 重试次数
        cacheEnabled: true           # 是否启用结果缓存
        cacheTtl: 300000            # 缓存TTL(毫秒)
```

### RPC 分布式调用配置
```yaml
lq:
  rpc:
    enabled: true                      # 是否启用RPC框架
    
    # 服务端配置
    server:
      port: 9090                      # RPC服务端口
      host: 0.0.0.0                   # 服务绑定主机
      maxConnections: 200             # 最大连接数
      requestTimeout: 30000           # 请求超时时间(毫秒)
      
      # 线程池配置
      threadPool:
        coreSize: 20                  # 核心线程数
        maxSize: 100                  # 最大线程数
        queueCapacity: 2000           # 队列容量
    
    # 客户端配置
    client:
      connectTimeout: 5000            # 连接超时时间(毫秒)
      readTimeout: 30000             # 读取超时时间(毫秒)
      retryCount: 3                  # 重试次数
      retryInterval: 1000            # 重试间隔(毫秒)
      
      # 负载均衡配置
      loadBalance:
        strategy: ROUND_ROBIN         # 负载均衡策略：ROUND_ROBIN/RANDOM/WEIGHTED_ROUND_ROBIN/LEAST_CONNECTIONS/CONSISTENT_HASH
        healthCheckInterval: 30000    # 健康检查间隔(毫秒)
        
      # 熔断器配置
      circuitBreaker:
        enabled: true                 # 是否启用熔断器
        failureThreshold: 5           # 失败阈值
        recoveryTimeout: 60000        # 恢复超时时间(毫秒)
        halfOpenMaxCalls: 3           # 半开状态最大调用次数
```

### HTTP 客户端配置
```yaml
lq:
  http:
    enabled: true                      # 是否启用HTTP客户端，默认true
    
    # 连接池配置
    pool:
      maxTotal: 200                   # 最大连接数
      maxPerRoute: 50                 # 每个路由最大连接数
      connectTimeout: 5000            # 连接超时时间(毫秒)
      socketTimeout: 30000            # Socket超时时间(毫秒)
      connectionRequestTimeout: 3000   # 从连接池获取连接超时时间(毫秒)
      
    # 重试配置
    retry:
      enabled: true                   # 是否启用重试
      maxRetries: 3                   # 最大重试次数
      retryInterval: 1000             # 重试间隔(毫秒)
      
    # 代理配置
    proxy:
      enabled: false                  # 是否启用代理
      host: proxy.example.com         # 代理主机
      port: 8080                      # 代理端口
      username:                       # 代理用户名
      password:                       # 代理密码
```

### 消息队列配置
```yaml
lq:
  mq:
    enabled: true                      # 是否启用消息队列
    
    # 监听器配置
    listener:
      pollInterval: 10                # 轮询间隔(毫秒)
      batchSize: 100                  # 批量处理大小
      maxRetries: 3                   # 最大重试次数
      retryInterval: 1000             # 重试间隔(毫秒)
      
    # 延迟队列配置
    delay:
      enabled: true                   # 是否启用延迟队列
      pollInterval: 500               # 延迟队列轮询间隔(毫秒)
      maxDelayTime: 86400000          # 最大延迟时间(毫秒)，默认24小时
      
    # 死信队列配置
    deadLetter:
      enabled: true                   # 是否启用死信队列
      maxRetries: 5                   # 最大重试次数
      ttl: 604800000                  # 死信消息TTL(毫秒)，默认7天
```

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

### MCP 工具使用示例
```java
// 注册MCP工具
@Component
public class CustomMCPTool {
    
    @LQMCPTool(name = "customTool", description = "自定义工具")
    public String executeCustomLogic(String input) {
        return "处理结果: " + input;
    }
}

// 调用MCP工具
LQMCPToolHandler toolHandler = applicationContext.getBean(LQMCPToolHandler.class);
String result = toolHandler.executeTool("customTool", "测试输入");
```

## 组件启动顺序

1. **Redis 连接初始化** - 建立 Redis 连接池
2. **线程池初始化** - 创建主线程池和辅助线程池
3. **服务注册中心** - 启动节点注册和服务发现
4. **消息总线** - 启动集群消息总线和精准消息总线
5. **配置中心** - 启动增强版配置中心（如果启用）
6. **控制台** - 启动管理控制台（如果启用）
7. **MCP 工具** - 启动 MCP 工具服务器（如果启用）
8. **RPC 框架** - 启动分布式 RPC 服务（如果启用）

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

## 最佳实践

### 1. 配置管理
- 使用命名空间隔离不同环境的配置
- 合理设置配置TTL，避免内存泄漏
- 启用配置变更监听，实现动态配置更新

### 2. 服务注册
- 确保服务名称唯一性
- 配置合适的心跳间隔
- 使用标签进行服务分组

### 3. 消息队列
- 合理设置队列监听间隔
- 使用延迟队列处理定时任务
- 避免消息积压，及时处理消息

### 4. RPC 调用
- 配置合适的超时时间
- 使用负载均衡策略
- 启用熔断器防止雪崩

### 5. MCP 工具
- 合理设置工具扫描包路径
- 启用健康检查监控工具状态
- 使用注册中心实现工具发现

## 故障排查

### 常见问题

1. **Redis 连接失败**
   - 检查 Redis 服务是否启动
   - 验证连接配置（IP、端口、密码）
   - 检查网络连通性

2. **服务注册失败**
   - 确认服务名称配置正确
   - 检查端口是否被占用
   - 验证 Redis 连接状态

3. **配置中心无法获取配置**
   - 检查命名空间和配置键是否正确
   - 验证配置是否已过期
   - 确认配置中心是否启用

4. **MCP 工具调用失败**
   - 检查工具是否已注册
   - 验证工具服务器是否启动
   - 确认工具参数格式正确

### 日志配置
```yaml
logging:
  level:
    cn.lingque: DEBUG
    cn.lingque.cloud: INFO
    cn.lingque.runner: INFO
    cn.lingque.mcp: DEBUG
    cn.lingque.rpc: INFO
```

## 配置参数总结

### 核心配置参数
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `ling-que.ip` | String | localhost | Redis服务器IP |
| `ling-que.port` | String | 6379 | Redis服务器端口 |
| `ling-que.password` | String | null | Redis密码 |
| `ling-que.maxTotal` | int | 50 | 连接池最大连接数 |
| `ling-que.timeout` | int | 10000 | 连接超时时间(毫秒) |

### 组件开关参数
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `lq.console.enabled` | boolean | true | 控制台开关 |
| `lq.config.enhanced.enabled` | boolean | true | 增强配置中心开关 |
| `lq.mcp.enabled` | boolean | true | MCP工具开关 |
| `lq.rpc.enabled` | boolean | false | RPC框架开关 |
| `lq.http.enabled` | boolean | true | HTTP客户端开关 |

### 性能调优参数
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `ling-que.masterPool.corePoolSize` | int | 10 | 主线程池核心线程数 |
| `ling-que.masterPool.maximumPoolSize` | int | 20 | 主线程池最大线程数 |
| `lq.mq.listener.pollInterval` | int | 10 | MQ轮询间隔(毫秒) |
| `lq.config.enhanced.cleanupInterval` | long | 300 | 配置清理间隔(秒) |

更多示例见 src/test/java 中的测试类，如 LQEnhancedConfigExample.java、LQMCPToolUsageExample.java 等。

更多性能细节见 src/main/java/cn/lingque/mq/README_MQ_REFACTOR.md。
