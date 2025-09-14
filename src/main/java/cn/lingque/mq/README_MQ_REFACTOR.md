# MQ队列重构说明

## 重构目标
- 去掉冗余的MQ实现，保留核心的两个队列类
- 优化监听机制，从轮询改为基于事件的监听
- 单独实现延迟队列消费者启动逻辑，提升性能

## 核心变化

### 1. 保留的核心队列类
- **LQUnifiedQueue**: 统一消息队列，支持瞬时和延迟消息
- **LQStreamUnifiedQueue**: 基于Redis Streams的统一消息队列

### 2. 弃用的队列类（向后兼容）
- **LQSequenceQueue**: 顺序队列 → 建议使用 `LQUnifiedQueue` 替代
- **LQLazyQueue**: 延迟队列 → 建议使用 `LQUnifiedQueue` 或 `LQStreamUnifiedQueue` 替代  
- **LQUniqueQueue**: 唯一队列 → 建议使用 `LQUnifiedQueue` 替代
- **LQMQConsumer**: 轮询式消费者 → 已被内置监听机制替代

### 3. 新的监听机制
- **独立线程监听**: 每个队列启动独立的监听线程，避免全局轮询
- **高频监听**: 瞬时消息10ms检查间隔，比原来的50ms轮询快5倍
- **延迟队列优化**: 统一的延迟队列管理器，500ms检查间隔

### 4. 延迟队列管理器
- **LQDelayQueueManager**: 统一管理所有延迟队列处理
- **单独启动**: 延迟队列消费者独立启动，不影响瞬时消息处理
- **智能调度**: 只有注册了延迟队列处理器才会启动管理器

## 性能提升

### 监听机制优化
- **原来**: 全局50ms轮询所有队列
- **现在**: 每个队列独立10ms监听 + 统一500ms延迟队列处理

### 资源消耗优化
- **CPU**: 避免无意义的全局轮询，只处理有消息的队列
- **内存**: 队列状态独立管理，避免全局状态竞争
- **线程**: 按需启动监听线程，不使用的队列不消耗资源

## 使用方式

### 推荐用法（新）
```java
// 使用UNIFIED类型（推荐）
@LqMQListener(key = "user.message", type = LqMqType.UNIFIED)
public void handleUserMessage(UserMessage message) {
    // 处理逻辑
}

// 使用STREAM_UNIFIED类型（高吞吐场景）
@LqMQListener(key = "order.process", type = LqMqType.STREAM_UNIFIED) 
public void handleOrderProcess(OrderMessage message) {
    // 处理逻辑
}
```

### 兼容用法（旧）
```java
// 仍然支持，但会有弃用警告
@LqMQListener(key = "old.queue", type = LqMqType.SEQUENCE)
public void handleOldQueue(String message) {
    // 仍然可以使用，但建议迁移到UNIFIED
}
```

## 迁移指南

### 从旧队列类型迁移
1. **SEQUENCE** → **UNIFIED**: 直接替换类型即可
2. **LAZY** → **UNIFIED**: 使用 `pushDelayMessage()` 方法
3. **UNIQUE** → **UNIFIED**: 业务层面实现去重逻辑

### 性能调优建议
1. **高实时性场景**: 使用 `UNIFIED` 类型
2. **高吞吐场景**: 使用 `STREAM_UNIFIED` 类型
3. **延迟消息**: 两种类型都支持，根据场景选择

## 注意事项
1. 新的监听机制会自动启动，无需手动调用 `LQMQConsumer.start()`
2. 旧的队列类型仍然依赖 `LQMQConsumer.start()`，需要手动启动
3. 建议逐步迁移到新的队列类型，享受性能提升
