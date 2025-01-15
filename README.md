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
  ip: localhost #redis的IP
  port: 6379 #redis端口
  password: 密码 # 密码
  db: 0 #数据库选择 0～16
   
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
    # 默认是spring.application.name，如果缺省则默认值：ling-server
    serverName: ling-server
    # 是否开启消息总线
    enable: true


  # 主线程池配置 用于底层交互
  masterPool:
    corePoolSize: 10  # 核心线程数
    maximumPoolSize: 20   # 最大线程数
    keepAliveTime: 18000  # 线程空闲时间(毫秒)
    
  # 从线程池配置 用于业务处理
  slavePool:
    corePoolSize: 10  # 核心线程数
    maximumPoolSize: 20   # 最大线程数
    keepAliveTime: 18000  # 线程空闲时间(毫秒)
  

```
### @LQStarterEnable 启动云组件
```java

@LQStarterEnable
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

```

### 3. 缓存Key管理
LQKey提供了统一的缓存key管理机制，包含三个要素：
- 前缀(prefixKey)
- 版本号(version)
- 过期时间(ttl)
```text
🚩说明: 万物皆以LQKey开启，LQKey是灵雀最核心的灵魂，所有的组件核心实现都基于它，比如分布式锁、基本操作、消息队列等等

```
#### 常用过期时间常量
- LQKey.TEN_SECONDS // 10秒
- LQKey.ONE_MINUTE // 1分钟
- LQKey.FIVE_MINUTE // 5分钟
- LQKey.HALF_HOUR // 30分钟
- LQKey.ONE_HOUR // 1小时
- LQKey.HALF_DAY // 12小时
- LQKey.ONE_DAY // 24小时
- LQKey.ONE_WEEK // 1周
- LQKey.ONE_MONTH // 1月
- LQKey.FOREVER // 永久

#### 简单使用示例
### 3.1 定义并使用LQKey

```java
//定义key
LQKey key = LQKey.key("user:token", 1D, LQKey.ONE_MINUTE);
// key.rp(...参数) 简单使用使用key;
key.rp(172918L).ofValue().set("token123");
```



### 3.1 Value操作(ofValue)

Value 类型是最基础的 key-value 结构，支持存储字符串和数值类型。

#### 基础操作
```java
// 定义key
LQKey key = LQKey.key("user:token", 1D, LQKey.ONE_MINUTE);

// 设置值
key.rd("123").ofValue().set("token123");

// 获取值
String value = key.rd("123").ofValue().get();

// 删除值
key.rd("123").ofValue().delete();

// 判断是否存在
boolean exists = key.rd("123").ofValue().exists();
```

#### 过期时间操作
```java
// 设置值同时指定过期时间
key.rd("123").ofValue().set("token123", LQKey.ONE_HOUR);

// 设置过期时间
key.rd("123").ofValue().expire(LQKey.ONE_HOUR);

// 获取剩余过期时间(秒)
Long ttl = key.rd("123").ofValue().ttl();
```

#### 数值操作
```java
// 递增
key.rd("counter").ofValue().incr();  // +1
key.rd("counter").ofValue().incrBy(5);  // +5
key.rd("counter").ofValue().incrByFloat(1.5);  // +1.5

// 递减
key.rd("counter").ofValue().decr();  // -1
key.rd("counter").ofValue().decrBy(3);  // -3
```

#### 批量操作
```java
// 批量设置值
Map<String, String> map = new HashMap<>();
map.put("key1", "value1");
map.put("key2", "value2");
key.rd().ofValue().mset(map);

// 批量获取值
List<String> keys = Arrays.asList("key1", "key2");
List<String> values = key.rd().ofValue().mget(keys);
```

#### 原子操作
```java
// 设置值并返回旧值
String oldValue = key.rd("123").ofValue().getSet("newValue");

// 设置值(仅当key不存在时)
boolean success = key.rd("123").ofValue().setNx("value");

// 设置值(仅当key存在时)
boolean success = key.rd("123").ofValue().setXx("value");
```

#### 高级特性
```java
// 追加字符串
key.rd("msg").ofValue().append("Hello");
key.rd("msg").ofValue().append(" World");  // 结果: "Hello World"

// 获取字符串长度
long length = key.rd("msg").ofValue().strlen();

// 获取指定范围的字符串
String substring = key.rd("msg").ofValue().getRange(0, 4);  // 结果: "Hello"

// 设置新值并返回旧值
String oldValue = key.rd("123").ofValue().getSet("newValue");
```

#### 使用场景示例

1. 缓存用户Token
```java
// 定义Token缓存key
LQKey tokenKey = LQKey.key("user:token", 1D, LQKey.ONE_HOUR);

// 保存Token
public void saveUserToken(Long userId, String token) {
    tokenKey.rd(userId).ofValue().set(token);
}

// 获取Token
public String getUserToken(Long userId) {
    return tokenKey.rd(userId).ofValue().get();
}

// 刷新Token过期时间
public void refreshToken(Long userId) {
    tokenKey.rd(userId).ofValue().expire(LQKey.ONE_HOUR);
}
```

2. 计数器应用
```java
// 定义计数器key
LQKey counterKey = LQKey.key("daily:visits", 1D, LQKey.ONE_DAY);

// 访问计数
public long incrementVisits() {
    return counterKey.rd().ofValue().incr();
}

// 获取当日访问次数
public long getTodayVisits() {
    String count = counterKey.rd().ofValue().get();
    return count != null ? Long.parseLong(count) : 0;
}
```

3. 分布式限流
```java
// 定义限流key
LQKey limitKey = LQKey.key("rate:limit", 1D, LQKey.ONE_MINUTE);

// 简单限流实现
public boolean isAllowed(String userId) {
    String key = limitKey.rd(userId).buildKey();
    long count = limitKey.rd(userId).ofValue().incrBy(1);
    if (count == 1) {
        // 设置过期时间
        limitKey.rd(userId).ofValue().expire(LQKey.ONE_MINUTE);
    }
    return count <= 100; // 每分钟限制100次
}
```

4. 配置缓存
```java
// 定义配置key
LQKey configKey = LQKey.key("sys:config", 1D, LQKey.ONE_DAY);

// 批量更新配置
public void updateConfigs(Map<String, String> configs) {
    configKey.rd().ofValue().mset(configs);
}

// 获取配置
public String getConfig(String configName) {
    return configKey.rd(configName).ofValue().get();
}
```

#### 注意事项
1. 合理设置过期时间，避免长期占用内存
2. 对于高频访问的key，考虑使用本地缓存
3. 大量key批量操作时使用mget/mset
4. 注意处理并发情况下的原子性要求
5. 关键操作需要做好异常处理

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
@LqMQListener(even = "event.topic")
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
