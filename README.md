# LQ_Cloud - 灵雀分布式框架

## 介绍
灵雀生态是一个基于Redis的轻量级分布式开发框架，旨在为中小企业提供低成本、高性能的微服务架构解决方案。

## 主要特性
- Redis轻量级操作封装
- 分布式锁
- 微服务注册与发现
- 消息总线
- 多种消息队列实现
  - 延迟消息队列
  - 顺序消息队列
  - 去重消息队列
- 支持Dubbo RPC和SpringCloud生态

## 快速开始

### 1. Maven依赖
```xml
<dependency>
    <groupId>com.lingque</groupId>
    <artifactId>lq-cloud</artifactId>
    <version>1.0.0</version>
</dependency>
```


### 2. 配置文件
```yaml
ling-que:
  # Redis基础配置
  ip: localhost
  port: 6379
  password: yourpassword
  db: 0
  
  # 连接池配置
  maxTotal: 10
  maxIdle: 10
  minIdle: 0
  maxWaitMillis: 60000
  timeout: 18000
  
  # Redis模式配置
  mode: standalone  # standalone/sentinel/cluster
  
  # 哨兵模式配置
  sentinel:
    master: mymaster
    sentinelNodes:
      - 192.168.1.10:26379
      - 192.168.1.11:26379
  
  # 集群模式配置
  cluster:
    clusterNodes:
      - 192.168.1.10:6379
      - 192.168.1.11:6379
  
  # 微服务配置
  serverName: my-service
  serverHost: 192.168.1.100
  serverPort: 8080
  
  # 消息总线配置
  bus:
    serverName: ling-server
    enable: true
  
  # 配置中心配置
  configCenter:
    configId: ${spring.application.name}
    group: ling-que
```

### 3. 缓存Key管理
LQKey提供了统一的缓存key管理机制，包含三个要素：
- 前缀(prefixKey)
- 版本号(version)
- 过期时间(ttl)

#### 常用过期时间常量
QKey.TEN_SECONDS // 10秒
LQKey.ONE_MINUTE // 1分钟
LQKey.FIVE_MINUTE // 5分钟
LQKey.HALF_HOUR // 30分钟
LQKey.ONE_HOUR // 1小时
LQKey.HALF_DAY // 12小时
LQKey.ONE_DAY // 24小时
LQKey.ONE_WEEK // 1周
LQKey.ONE_MONTH // 1月
LQKey.FOREVER // 永久

#### 使用示例

```java
String prefixKey = "my-service";
String version = "1.0.0";
int ttl = QKey.ONE_MINUTE;

String key = LQKey.build(prefixKey, version, ttl);
```

### 4. 消息队列使用
支持多种消息队列类型，使用注解方式简单配置：

```java
@Service
public class MessageHandler {
    
    @LqMQListener(key = "order.create", type = LqMqType.SEQUENCE)
    public void handleOrderCreate(OrderDTO order) {
        // 处理订单创建消息
    }
    
    @LqMQListener(key = "user.login", type = LqMqType.UNIQUE)
    public void handleUserLogin(UserLoginEvent event) {
        // 处理用户登录消息
    }
    
    @LqMQListener(key = "task.delay", type = LqMqType.TIMING_WHEEL)
    public void handleDelayTask(TaskDTO task) {
        // 处理延迟任务
    }
}
```

## 高级特性

### 1. 分布式锁

```java
@LqLock(key = "order.create", ttl = QKey.ONE_MINUTE)
public void handleOrderCreate(OrderDTO order) {
    // 处理订单创建消息
}
```

### 2. 消息总线

```java
@LqMQListener(key = "event.topic")
public void handleEvent(Event event) {
    // 处理事件
}
```

## 最佳实践
1. 合理设置过期时间，避免缓存永久存在
2. 使用版本号管理缓存更新
3. 根据业务场景选择合适的消息队列类型
4. 合理配置连接池参数
5. 在分布式环境下使用哨兵或集群模式

## 注意事项
1. 避免使用永久缓存
2. 注意处理并发情况下的锁超时
3. 消息处理要做好幂等性控制
4. 合理设置重试策略
5. 注意异常处理和日志记录
