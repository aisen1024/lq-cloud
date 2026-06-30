# LQ-Cloud

> 基于 Redis + Spring Boot 3 的下一代轻量级微服务基础设施框架

[![Maven Central](https://img.shields.io/maven-central/v/io.github.aisen1024/lq-cloud)](https://central.sonatype.com/artifact/io.github.aisen1024/lq-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)

---

## 项目介绍

**LQ-Cloud** 是一个以 Redis 为核心基础设施，面向 Spring Boot 3 微服务体系打造的轻量级云原生框架。它不依赖 Nacos、Eureka、RabbitMQ、Kafka 等中间件，仅通过 Redis 即可实现服务注册与发现、分布式消息队列、消息总线、RPC 远程调用、配置中心、分布式锁等企业级微服务能力。

### 核心优势

- **零依赖中间件**：所有分布式能力均基于 Redis 实现，无需额外部署 Nacos、Zookeeper、Kafka 等
- **开箱即用**：引入依赖，配置 Redis 连接即可启动全部功能
- **高性能**：基于 Lettuce 异步客户端，支持连接池、自动重试
- **易扩展**：模块化设计，各功能模块独立可插拔

---

## 功能模块

| 模块 | 说明 |
|---|---|
| Redis 增强操作 | String/Hash/Set/ZSet/List/Geo 结构化封装 |
| 分布式锁 | 支持可重入、等待超时、延迟释放 |
| 消息队列 (MQ) | 支持 List（顺序）/ ZSet（延迟）/ Stream（可靠）三种模式 |
| 消息总线 (Bus) | 基于 Redis PubSub，支持异步、优先级、条件过滤 |
| 服务注册与发现 | 基于 Redis，支持健康检查、多协议、标签路由 |
| RPC 远程调用 | 支持负载均衡、熔断器、链路追踪、自动重试 |
| HTTP 声明式客户端 | 类 Feign 风格，注解驱动 HTTP 调用 |
| 配置中心 | 支持命名空间、版本管理、实时推送、变更监听 |
| MCP 工具集成 | 支持 AI Agent MCP 工具注册与发现 |

---

## 快速开始

### 1. 引入依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.github.aisen1024</groupId>
    <artifactId>lq-cloud</artifactId>
    <version>1.2.0-high</version>
</dependency>
```

### 2. 配置 Redis 连接

在 `application.yml` 中添加：

```yaml
ling-que:
  ip: 127.0.0.1
  port: 6379
  password: your_password   # 无密码则删除此行
  db: 0

  # 服务节点信息（服务注册 / RPC 必填）
  node:
    server-name: my-service
    server-host: 192.168.1.100
    server-port: 8080

  # 消息总线配置（可选）
  bus:
    server-name: my-service
    enable: true
```

### 3. 启动应用

正常启动 Spring Boot 应用，LQ-Cloud 自动完成 Redis 连接初始化、服务注册、MQ 启动等。

---

## Redis 增强操作

LQ-Cloud 提供以 `LQKey` 为中心的链式 Redis 操作 API。

### 定义 Key

```java
// 三要素：key前缀、版本号、TTL（秒，-1L 表示永不过期）
LQKey USER_INFO = LQKey.key("USER:INFO", 1D, 3600L);
```

### String 操作

```java
// 构建操作实例
LingQueRedis redis = LingQueRedis.ofKey(USER_INFO, userId);

// 写入
redis.ofValue().set("hello");

// 读取
String value = redis.ofValue().get();

// 设置（不存在才写入）
boolean success = redis.ofValue().setNx("value");

// 原子递增
Long count = redis.ofValue().incr();
```

### Hash 操作

```java
LingQueRedis redis = LingQueRedis.ofKey(USER_INFO, userId);

// 写入 field
redis.ofHash().hset("name", "aisen");

// 读取 field
String name = redis.ofHash().hget("name");

// 读取整个 Hash（转为对象）
UserInfo user = redis.ofHash().hgetAll(UserInfo.class);
```

### List 操作

```java
LingQueRedis redis = LingQueRedis.ofKey("ORDER:QUEUE", 30L);

// 从左边推入
redis.ofList().lpush("order1");

// 从右边弹出
String order = redis.ofList().rpop();

// 获取列表长度
Long size = redis.ofList().llen();
```

### Set / ZSet 操作

```java
// Set
redis.ofSet().sadd("tag1", "tag2");
Set<String> members = redis.ofSet().smembers();

// ZSet（有序集合）
redis.ofZSet().zadd(System.currentTimeMillis(), "member1");
List<String> top10 = redis.ofZSet().zrange(0, 9);
```

### Geo 地理位置

```java
LingQueRedis redis = LingQueRedis.ofKey("GEO:STORE", -1L);

// 添加位置
redis.ofGeo().geoadd(116.397128, 39.916527, "store1");

// 查询范围内成员
List<String> nearby = redis.ofGeo().georadius(116.4, 39.9, 5.0, "km");
```

### 原始命令执行

```java
// 执行任意 Redis 命令
redis.execBase(commands -> commands.lpush("my:key", "v1", "v2"));
```

---

## 分布式锁

基于 Redis `SET NX` 实现，支持可重入、等待自旋、延迟释放。

```java
LingQueRedis redis = LingQueRedis.ofKey("LOCK:ORDER", 30L);

// 1. 尝试获取锁，失败立即返回
redis.ofLock().lockFuture(
    () -> { /* 业务逻辑 */ return result; },
    () -> { /* 获取锁失败的处理 */ return null; }
);

// 2. 等待最多 5 秒获取锁
redis.ofLock().lockFuture(5L, () -> {
    // 业务逻辑
    return result;
});

// 3. 执行完后延迟 3 秒释放锁（防止重复操作）
redis.ofLock().lockFutureAndLazy(
    () -> { return result; },
    () -> { return null; },
    3L   // 延迟释放秒数
);

// 4. 手动加锁 / 释放
boolean locked = redis.ofLock().lock(false);
if (locked) {
    try {
        // 业务逻辑
    } finally {
        redis.ofLock().unlock();
    }
}
```

---

## 消息队列 (MQ)

LQ-Cloud MQ 无需部署 RabbitMQ/Kafka，完全基于 Redis 实现三种队列模式。

### 队列类型对比

| 类型 | Redis 结构 | 特点 | 适用场景 |
|---|---|---|---|
| `LIST` | List | 顺序消费、高性能 | 日志收集、简单通知 |
| `ZSET` | ZSet | 支持延迟、可取消 | 订单超时、定时提醒 |
| `STREAM` | Stream | ACK 确认、消息持久化 | 支付回调、可靠业务消息 |

### 方式一：注解驱动（推荐）

**Step 1：定义监听器**

```java
@Component
public class OrderListener {

    // 顺序消息监听
    @LqMQListener(topic = "order:created")
    public void onOrderCreated(String message) {
        // 处理订单创建消息
    }

    // 延迟/ZSet消息监听
    @LqMQListener(topic = "order:timeout")
    public void onOrderTimeout(String message) {
        // 处理订单超时
    }
}
```

**Step 2：发送消息**

```java
@Autowired
private LQMQTemplate mqTemplate;

// 发送顺序消息（List队列）
mqTemplate.sendOrderMessage("order:created", orderObject);

// 发送延迟消息（ZSet队列，3600秒后触发）
mqTemplate.sendDelayMessage("order:timeout", orderObject, 3600L);

// 发送可靠消息（Stream队列）
mqTemplate.sendStreamMessage("order:payment", paymentObject);
```

### 方式二：编程式订阅

```java
// 订阅主题
LQMQTemplate.subscribe("order:created", new ILQMessage<Order>() {
    @Override
    public void handle(Order order) {
        // 处理消息
    }
    @Override
    public Class<Order> getEntityClass() {
        return Order.class;
    }
});

// 启动 MQ（应用启动时调用一次）
LQMQTemplate.start();
```

### 消息取消

```java
// 发送延迟消息并获取消息 ID
String msgId = LQMQTemplate.sendDelayMessage("order:timeout", order, 3600L);

// 用户支付后取消超时消息
LQMQTemplate.cancelMessage(msgId);

// 查询是否已取消
boolean cancelled = LQMQTemplate.isCancelled(msgId);
```

---

## 消息总线 (Bus)

消息总线用于同服务或跨服务的事件广播，基于 Redis PubSub 实现。

### 发布消息

```java
// 广播到所有订阅者
LQEnhancedBus.publish("user:login", userEvent);

// 发送到指定服务组
LQEnhancedBus.publishToGroup("user:login", userEvent, "auth-service");
```

### 订阅消息（注解方式）

```java
@Component
public class UserEventHandler {

    @LQEnhancedBusListener(
        topic = "user:login",
        async = true,             // 异步处理
        priority = 1,             // 优先级（越小越先执行）
        errorStrategy = LQEnhancedBusListener.ErrorHandleStrategy.RETRY,
        maxRetries = 3
    )
    public void onUserLogin(EnhancedBusMessage message) {
        UserEvent event = message.getBody(UserEvent.class);
        // 处理登录事件
    }

    // 带条件过滤（SpEL 表达式）
    @LQEnhancedBusListener(
        topic = "order:status",
        condition = "#message.type == 'PAID'",
        serviceGroup = "order-service"
    )
    public void onOrderPaid(EnhancedBusMessage message) {
        // 只处理支付成功的订单事件
    }
}
```

### 错误处理策略

| 策略 | 说明 |
|---|---|
| `LOG` | 仅记录日志（默认） |
| `RETRY` | 自动重试（配合 `maxRetries` 和 `retryInterval`） |
| `IGNORE` | 忽略错误 |
| `THROW` | 抛出异常 |

---

## 服务注册与发现

LQ-Cloud 内置基于 Redis 的服务注册中心，无需 Nacos 或 Eureka。

### 配置

```yaml
ling-que:
  node:
    server-name: order-service
    server-host: 192.168.1.100
    server-port: 8080
```

### 服务发现

```java
// 获取指定服务的所有节点
List<LQEnhancedNodeInfo> nodes = LQEnhancedRegisterCenter.getServiceNodes("order-service");

// 获取健康节点
List<LQEnhancedNodeInfo> healthyNodes = LQEnhancedRegisterCenter.getHealthyNodes("order-service");

// 获取所有已注册服务列表
Set<String> services = LQEnhancedRegisterCenter.getAllServices();
```

服务注册中心特性：
- 自动心跳维持，节点宕机自动摘除
- 支持多协议（HTTP / Socket / gRPC / MCP）
- 支持标签路由、服务分组
- 支持负载信息上报和智能调度

---

## RPC 远程调用

基于增强版注册中心，无需 Dubbo 即可实现跨服务调用，内置负载均衡与熔断保护。

### 定义服务接口

```java
@LQService(
    value = "OrderService",
    version = "1.0.0",
    group = "default",
    timeout = 5000,
    loadBalance = true,
    circuitBreaker = true
)
public interface OrderService {

    @LQServiceMethod(timeout = 3000, retryCount = 2)
    OrderVO getOrder(String orderId);

    @LQServiceMethod(async = true)
    void createOrder(CreateOrderRequest request);
}
```

### 服务消费端调用

```java
// 获取服务代理（自动负载均衡 + 熔断）
OrderService orderService = LQDistributedServiceCaller.createServiceProxy(OrderService.class);

// 像调用本地方法一样调用远程服务
OrderVO order = orderService.getOrder("ORDER-001");
```

### 核心特性

- **负载均衡**：支持轮询、随机、权重等策略
- **熔断器**：失败率达到阈值自动开启熔断，防止雪崩
- **链路追踪**：自动生成 TraceId，支持跨服务追踪
- **自动重试**：可配置重试次数，支持幂等接口
- **多协议**：默认 HTTP，扩展支持 Socket/gRPC

---

## HTTP 声明式客户端

类 Feign 风格，通过注解定义 HTTP 接口，自动整合服务发现与负载均衡。

### 定义 HTTP 客户端

```java
@HttpClient(
    serviceName = "user-service",   // 服务名（自动发现）
    connectTimeout = 3000,
    readTimeout = 10000,
    loadBalance = true
)
public interface UserHttpClient {

    @Get("/api/user/{id}")
    UserVO getUser(@PathVariable("id") String id);

    @Post("/api/user")
    UserVO createUser(@RequestBody CreateUserRequest request);

    @Put("/api/user/{id}")
    UserVO updateUser(@PathVariable("id") String id, @RequestBody UpdateUserRequest request);

    @Delete("/api/user/{id}")
    void deleteUser(@PathVariable("id") String id);

    @Get("/api/users")
    List<UserVO> listUsers(@RequestParam("page") int page, @RequestParam("size") int size);
}
```

### 注入使用

```java
// 使用 @LqService 注解自动注入
@LqService(serviceName = "user-service")
private UserHttpClient userHttpClient;

// 直接调用
UserVO user = userHttpClient.getUser("user-001");
```

---

## 配置中心

LQ-Cloud 内置轻量级配置中心，支持命名空间、版本管理和实时推送。

### 写入配置

```java
// 写入默认命名空间
LQEnhancedConfigCenter.setConfig("app.timeout", "5000");

// 写入指定命名空间
LQEnhancedConfigCenter.setConfig("production", "db.pool.size", "50");

// 写入对象配置
LQEnhancedConfigCenter.setConfigObject("app.datasource", dataSourceConfig);
```

### 读取配置

```java
// 获取字符串
String timeout = LQEnhancedConfigCenter.getConfig("app.timeout");

// 获取并转换类型（带默认值）
Integer poolSize = LQEnhancedConfigCenter.getConfig("db.pool.size", 10, Integer.class);

// 获取对象
DataSourceConfig config = LQEnhancedConfigCenter.getConfigObject("app.datasource", DataSourceConfig.class);
```

### 配置变更监听

```java
// 注册变更监听器
LQEnhancedConfigCenter.addListener("app.timeout", event -> {
    System.out.println("配置变更：" + event.getOldValue() + " -> " + event.getNewValue());
    // 动态刷新应用配置
});
```

### 配置统计

```java
// 获取配置统计信息
ConfigStats stats = LQEnhancedConfigCenter.getStats();
System.out.println("配置总数: " + stats.getTotalConfigs());
System.out.println("命名空间数: " + stats.getNamespaceCount());
```

---

## MCP 工具集成（AI Agent）

LQ-Cloud 支持将 Spring Bean 注册为 MCP（Model Context Protocol）工具，供 AI Agent 调用。

### 定义 MCP 工具

```java
@MCPTool(
    name = "order-tool",
    description = "订单管理工具",
    version = "1.0.0",
    capabilities = {"query", "create", "cancel"},
    tags = {"order", "e-commerce"}
)
public class OrderMCPTool {

    @MCPToolMethod
    public OrderVO queryOrder(String orderId) {
        // 查询订单逻辑
        return orderService.getOrder(orderId);
    }

    @MCPToolMethod
    public String cancelOrder(String orderId, String reason) {
        // 取消订单逻辑
        orderService.cancel(orderId, reason);
        return "success";
    }
}
```

MCP 工具会自动注册到服务注册中心，其他服务或 AI Agent 可通过服务发现找到并调用。

---

## 线程池工具

LQ-Cloud 提供托管线程池，统一管理异步任务。

```java
// 提交主线程池任务
LQThreadUtil.execute(() -> {
    // 异步业务逻辑
});

// 使用辅助线程池
LQThreadUtil.executeSlave(() -> {
    // 低优先级后台任务
});
```

线程池配置：

```yaml
ling-que:
  master-pool:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 1000
  slave-pool:
    core-pool-size: 5
    max-pool-size: 20
    queue-capacity: 500
```

---

## 完整配置参考

```yaml
ling-que:
  # Redis 连接
  ip: 127.0.0.1
  port: 6379
  db: 0
  username: default
  password: ""
  max-total: 50
  max-idle: 20
  min-idle: 10
  timeout: 10000

  # 部署模式：standalone（单机）| sentinel（哨兵）| cluster（集群）
  mode: standalone

  # 哨兵模式配置
  sentinel:
    master: mymaster
    sentinel-nodes:
      - 127.0.0.1:26379
      - 127.0.0.1:26380

  # 集群模式配置
  cluster:
    cluster-nodes:
      - 127.0.0.1:7001
      - 127.0.0.1:7002
      - 127.0.0.1:7003

  # 服务节点配置
  node:
    server-name: my-service
    server-host: 192.168.1.100
    server-port: 8080

  # 消息总线
  bus:
    server-name: my-service
    enable: true

  # 配置中心
  config:
    namespace: default
    refresh-interval: 30000
```

---

## 项目结构

```
lq-cloud
├── cn.lingque
│   ├── base
│   │   └── LQKey.java                     # Redis Key 定义基类
│   ├── redis
│   │   ├── LingQueRedis.java              # Redis 增强操作入口
│   │   ├── JedisProxy.java                # Lettuce 连接代理
│   │   └── exten
│   │       ├── ValueOpt.java              # String 操作
│   │       ├── HashOpt.java               # Hash 操作
│   │       ├── SetOpt.java                # Set 操作
│   │       ├── SortedSetOpt.java          # ZSet 操作
│   │       ├── ListOpt.java               # List 操作
│   │       ├── GeoOpt.java                # Geo 地理位置操作
│   │       └── LockOpt.java               # 分布式锁
│   ├── mq
│   │   ├── LQMQTemplate.java              # MQ 统一操作模板
│   │   ├── LQMQType.java                  # MQ 类型枚举
│   │   └── core
│   │       ├── ListMQ.java                # 顺序消息队列
│   │       ├── ZSetMQ.java                # 延迟消息队列
│   │       ├── StreamMQ.java              # 可靠流式消息队列
│   │       ├── LQMQStarter.java           # MQ 启动器
│   │       ├── LQMQSubscriptionManager.java # 订阅管理器
│   │       └── MessageCancellationHandler.java # 消息取消处理器
│   ├── bus
│   │   └── enhanced
│   │       ├── LQEnhancedBus.java         # 增强版消息总线
│   │       └── annotation
│   │           └── LQEnhancedBusListener.java # 消息监听注解
│   ├── cloud
│   │   ├── node
│   │   │   ├── LQEnhancedRegisterCenter.java  # 服务注册中心
│   │   │   └── LQRegisterCenter.java          # 基础注册中心
│   │   ├── rpc
│   │   │   ├── LQDistributedServiceCaller.java # 分布式服务调用器
│   │   │   ├── annotation
│   │   │   │   ├── LQService.java             # 服务注解
│   │   │   │   └── LQServiceMethod.java        # 方法注解
│   │   │   ├── circuit
│   │   │   │   └── LQCircuitBreaker.java      # 熔断器
│   │   │   └── loadbalance
│   │   │       └── LQLoadBalancer.java        # 负载均衡器
│   │   ├── http
│   │   │   ├── annotation
│   │   │   │   ├── HttpClient.java            # HTTP 客户端注解
│   │   │   │   ├── Get.java / Post.java ...   # HTTP 方法注解
│   │   │   │   └── PathVariable.java / RequestParam.java ...
│   │   │   └── proxy
│   │   │       └── HttpClientProxyFactory.java # 动态代理工厂
│   │   ├── config
│   │   │   └── enhanced
│   │   │       └── LQEnhancedConfigCenter.java # 配置中心
│   │   └── mcp
│   │       ├── annotation
│   │       │   ├── MCPTool.java               # MCP 工具注解
│   │       │   └── MCPToolMethod.java          # MCP 方法注解
│   │       └── server
│   │           └── LQMCPToolServer.java        # MCP 服务端
│   └── thread
│       ├── LQThread.java                  # 线程封装
│       └── LQThreadUtil.java              # 线程池工具
```

---

## 依赖说明

| 依赖 | 版本 | 说明 |
|---|---|---|
| Spring Boot | 3.2.4 | 基础框架 |
| Lettuce | 6.3.2.RELEASE | Redis 异步客户端 |
| Hutool | 5.8.16 | Java 工具库 |
| Lombok | 1.18.28 | 代码简化 |

---

## 开发者

- **作者**：liming.zheng（aisen1024）
- **邮箱**：aisen1024@163.com
- **GitHub**：[https://github.com/aisen1024/lq-cloud](https://github.com/aisen1024/lq-cloud)

---

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
