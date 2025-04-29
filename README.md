# 灵雀分布式框架 (LQ_Cloud) 使用文档

## 目录
1. 简介
2. 快速开始
   - 环境要求
   - Maven依赖
   - 基本配置
3. 基础功能
   - Redis轻量级操作
   - 分布式锁
4. 消息队列
   - 延迟消息队列
   - 顺序消息队列
   - 去重消息队列
5. 消息总线
   - 消息总线配置
   - 消息发送与接收
6. 服务注册与发现
   - 服务注册
   - 基于Forest的服务调用
7. 常见问题

## 1. 简介

灵雀分布式框架（LQ_Cloud）是一个基于Redis的轻量级分布式开发框架，旨在为中小企业提供低成本、高性能的微服务架构解决方案。框架整合了分布式锁、微服务注册与发现、消息总线、多种消息队列实现，并支持Dubbo RPC和SpringCloud生态。

## 2. 快速开始

### 环境要求
- JDK 17+
- Redis 6.0+
- Spring Boot 3.2.4+

### Maven依赖

在项目的pom.xml中添加以下依赖：

```xml
<dependency>
    <groupId>io.github.aisen1024</groupId>
    <artifactId>lq-cloud</artifactId>
    <version>1.0.7</version>
</dependency>
```

### 基本配置

在Spring Boot配置文件(`application.yml`或`application.properties`)中添加灵雀框架配置：

```yaml
ling-que:
  # Redis基础配置
  ip: 127.0.0.1             # Redis服务器地址，默认localhost
  port: 6379                # Redis端口，默认6379
  password: your_password   # Redis密码，默认为空
  db: 0                     # 使用的数据库，默认为0
  username: default         # 连接用户名，默认default
  
  # 连接池配置
  max-total: 50             # 最大连接数，默认50
  max-idle: 20              # 最大空闲连接数，默认20
  min-idle: 10              # 最小空闲连接数，默认10
  max-wait-millis: -1       # 最大等待时间(毫秒)，-1表示无限等待
  timeout: 10000            # 连接超时时间(毫秒)，默认10000
  
  # Redis模式配置
  mode: standalone          # 模式: standalone(单机)、cluster(集群)、sentinel(哨兵)，默认单机
  
  # 哨兵模式配置
  sentinel:
    master: mymaster        # 哨兵主节点名称
    sentinel-nodes:         # 哨兵节点地址列表
      - 127.0.0.1:26379
      - 127.0.0.1:26380
  
  # 集群模式配置
  cluster:
    user: default           # 集群用户
    cluster-nodes:          # 集群节点地址列表
      - 127.0.0.1:6379
      - 127.0.0.1:6380
  
  # 线程池配置
  master-pool:              # 主线程池配置
    core-pool-size: 10      # 核心线程数，默认10
    maximum-pool-size: 50   # 最大线程数，默认50
    keep-alive-time: 18000  # 闲置线程存活时间(毫秒)，默认18000
  
  slave-pool:               # 辅助线程池配置
    core-pool-size: 10      # 核心线程数，默认10
    maximum-pool-size: 50   # 最大线程数，默认50
    keep-alive-time: 18000  # 闲置线程存活时间(毫秒)，默认18000
  
  # 消息总线配置
  bus:
    server-name: ling-server # 服务名称，默认ling-server
    enable: true            # 是否启用消息总线，默认true
  
  # 服务节点配置
  node:
    server-name: user-service  # 服务名称
    server-host: 192.168.1.100 # 服务IP
    server-port: 8080          # 服务端口
```

## 3. 基础功能

### Redis轻量级操作

灵雀框架对Redis操作进行了轻量级封装，使用更加简便。

#### 核心API - LingQueRedis

```java
// 创建Redis操作对象，基于Key
LingQueRedis redis = LingQueRedis.ofKey("your_key", 300L); // 300秒过期

// 基于预定义Key构建
LQKey key = LQKey.key("USER:INFO", 1D, LQKey.ONE_HOUR);
LingQueRedis redis = LingQueRedis.ofKey(key, "userId123");

// 字符串操作
redis.ofValue().set("value");
String value = redis.ofValue().get();

// 列表操作
redis.ofList().rightPush("value1");
redis.ofList().leftPush("value2");
List<String> values = redis.ofList().range(0, -1);

// Hash操作
redis.ofHash().put("field1", "value1");
String fieldValue = redis.ofHash().get("field1");

// Set操作
redis.ofSet().add("value1", "value2");
Set<String> members = redis.ofSet().members();

// ZSet操作
redis.ofZSet().add("member1", 1.0);
redis.ofZSet().add("member2", 2.0);
List<String> zsetMembers = redis.ofZSet().rangeByScore(1.0, 2.0);
```

### 分布式锁

灵雀框架提供了简单易用的分布式锁实现：

```java
// 创建锁
LQKey lockKey = LQKey.key("LOCK:ORDER", 1D, 30L); // 30秒过期
LingQueRedis lock = LingQueRedis.ofKey(lockKey, "order123");

// 简单锁操作
boolean acquired = lock.ofLock().lock(false);
if (acquired) {
    try {
        // 执行业务逻辑
    } finally {
        lock.ofLock().unlock();
    }
}

// 尝试获取锁（带等待时间）
boolean acquired = lock.ofLock().tryLock(5L); // 等待5秒
if (acquired) {
    try {
        // 执行业务逻辑
    } finally {
        lock.ofLock().unlock();
    }
}

// 使用Lambda简化锁操作
String result = lock.ofLock().lockFuture(() -> {
    // 获取锁成功后执行的代码
    return "success";
}, () -> {
    // 获取锁失败后执行的代码
    return "fail";
});

// 锁执行完延迟释放
String result = lock.ofLock().lockFutureAndLazy(() -> {
    // 业务逻辑
    return "success";
}, 10L); // 10秒后释放锁
```

## 4. 消息队列

灵雀框架提供了三种消息队列实现，可以通过`@LqMQListener`注解轻松实现消息的消费：

### 延迟消息队列 (LQLazyQueue)

适用于需要延迟处理的场景，如延迟发送消息、定时任务等。

```java
// 创建延迟队列
LQLazyQueue lazyQueue = LingQueRedis.ofKey("LAZY_QUEUE", LQKey.ONE_DAY).ofLazyQueue();

// 生产消息（10秒后处理）
lazyQueue.push("消息内容", 10L);

// 手动消费消息
List<String> messages = lazyQueue.pop(10); // 一次最多取10条

// 使用注解方式注册消费者
@Component
public class LazyQueueConsumer {
    
    @LqMQListener(key = "LAZY_QUEUE", type = LqMqType.LAZY)
    public void consumeLazyMessage(String message) {
        System.out.println("处理延迟消息: " + message);
        // 处理业务逻辑
    }
}
```

### 顺序消息队列 (LQSequenceQueue)

保证消息按照发送顺序被消费，适用于需要顺序处理的场景。

```java
// 创建顺序队列
LQSequenceQueue sequenceQueue = LingQueRedis.ofKey("SEQ_QUEUE", LQKey.ONE_DAY).ofSequenceQueue();

// 生产消息
sequenceQueue.push("消息1");
sequenceQueue.push("消息2");

// 手动消费消息
List<String> messages = sequenceQueue.pop(10);

// 使用注解方式注册消费者
@Component
public class SequenceQueueConsumer {
    
    @LqMQListener(key = "SEQ_QUEUE", type = LqMqType.SEQUENCE)
    public void consumeSequenceMessage(String message) {
        System.out.println("处理顺序消息: " + message);
        // 处理业务逻辑
    }
}
```

### 去重消息队列 (LQUniqueQueue)

确保相同内容的消息只会被处理一次，适用于防止重复处理的场景。

```java
// 创建去重队列
LQUniqueQueue uniqueQueue = LingQueRedis.ofKey("UNIQUE_QUEUE", LQKey.ONE_DAY).ofUniqueQueue();

// 生产消息
uniqueQueue.push("唯一消息");
uniqueQueue.push("唯一消息"); // 重复消息会被忽略

// 手动消费消息
List<String> messages = uniqueQueue.pop(10);

// 使用注解方式注册消费者
@Component
public class UniqueQueueConsumer {
    
    @LqMQListener(key = "UNIQUE_QUEUE", type = LqMqType.UNIQUE)
    public void consumeUniqueMessage(String message) {
        System.out.println("处理去重消息: " + message);
        // 处理业务逻辑
    }
}
```

## 5. 消息总线 (LQBus)

灵雀框架提供了基于Redis的分布式消息总线，可以通过`@LqEvenBus`注解轻松实现消息的订阅。

### 启动消息总线


### 消息发送

```java
// 发送消息到同一服务的所有节点
LQBus.sendBus("order-service", "order-created", "订单ID:12345", true);

// 发送消息到所有在线的服务节点
LQBus.sendBusAll("global-notification", "系统将在10分钟后维护", true);
```

### 消息订阅

```java
// 使用注解方式订阅消息
@Component
public class OrderEventHandler {
    
    @LqEvenBus(even = "order-created")
    public void handleOrderCreated(String message) {
        System.out.println("收到订单创建消息: " + message);
        // 处理业务逻辑
    }
    
    @LqEvenBus(even = "global-notification")
    public void handleGlobalNotification(String message) {
        System.out.println("收到全局通知: " + message);
        // 处理业务逻辑
    }
}
```

## 6. 服务注册与发现

灵雀框架提供了基于Redis的服务注册中心，结合Forest HTTP客户端实现服务的注册、发现和调用功能，支持分布式环境下的服务间通信和负载均衡。

### 服务注册

灵雀框架会根据配置文件中的`ling-que.server`配置自动注册服务到注册中心，无需手动编写注册代码。配置示例：

```yaml
ling-que:
  # 服务节点配置
  server:
    server-name: user-service  # 服务名称，不配置则取spring.application.name的值；若spring.application.name也为空，则默认为lq_server
    server-host: 192.168.1.100 # 服务IP，不配置则自动获取本机IP
    server-port: 8080          # 服务端口，不配置则使用server.port
```

服务启动时，灵雀框架会自动将服务信息注册到Redis，并维持心跳。如果需要查看注册中心的服务列表，可以使用：

```java
// 获取指定服务的所有节点
Set<LQNodeInfo> nodes = LQRegisterCenter.getSvNodeList("user-service");

// 获取所有在线服务节点
Set<LQNodeInfo> allNodes = LQRegisterCenter.getAllNodeList();

// 判断是否当前节点
boolean isCurrentNode = LQRegisterCenter.isCurrentNode(nodeInfo);
```

### 基于Forest的服务调用

灵雀框架使用`@LqService`注解结合Forest HTTP客户端实现服务发现和负载均衡调用：

#### 1. 配置Forest客户端

```java
@Configuration
@ForestScan(basePackages = "com.example.client")
public class ForestConfig {
}
```

#### 2. 定义服务接口

使用`@LqService`注解标记服务接口，实现服务发现和自动路由：

```java
// 使用@LqService注解定义服务接口，实现服务发现和负载均衡
@LqService(serviceName = "user-service")
public interface UserService {
    
    @Get("/users/{id}")
    UserResponse getUser(@Var("id") Long id);
    
    @Post("/users")
    UserResponse createUser(@JSONBody UserRequest request);
}
```

#### 3. 调用服务

```java
@Service
public class UserServiceClient {
    
    @Resource
    private UserService userService;
    
    public UserResponse getUser(Long id) {
        // 自动路由到user-service服务，实现负载均衡
        return userService.getUser(id);
    }
    
    public UserResponse createUser(UserRequest request) {
        // 自动路由到user-service服务，实现负载均衡
        return userService.createUser(request);
    }
}
```

## 7. 常见问题

### Redis连接问题

如果遇到Redis连接问题，请检查以下配置：
- Redis服务器地址和端口是否正确
- 密码是否正确
- 网络环境是否允许连接
- Redis服务是否正常运行

### 消息队列消费不及时

消息队列消费不及时可能有以下原因：
- 消费者数量不足
- 消息处理逻辑过重
- Redis服务负载过高

可以增加消费者数量或优化处理逻辑来解决。

### 分布式锁获取失败

分布式锁获取失败可能有以下原因：
- 锁已被其他服务持有
- 锁的TTL设置过短，未能完成操作就过期
- Redis网络连接不稳定

建议适当增加锁的等待时间或TTL，并确保网络稳定。

### 服务注册与发现问题

服务注册与发现问题可能有以下原因：
- 服务节点信息不完整
- 心跳更新失败
- Redis连接问题

建议检查服务节点信息和Redis连接，确保心跳正常更新。

---

更多详细信息，请参考[官方文档](https://github.com/aisen1024/lq-cloud)或提交Issue。
