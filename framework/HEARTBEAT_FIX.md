# 心跳和健康检查修复说明

## 问题描述

服务列表中的节点健康状态不稳定，明明服务正常运行，但健康状态会在"健康"和"异常"之间频繁切换。

## 问题根源

### 1. 心跳超时时间过短
- **心跳间隔**: 2秒（2000毫秒）
- **原超时时间**: 10秒（10000毫秒）
- **问题**: 10秒窗口太小，只有5个心跳周期。如果出现轻微的网络抖动、GC暂停或系统负载，就会导致节点被误判为不健康

### 2. 健康检查逻辑不合理
- 之前依赖`node.isHealthy()`方法，但该方法可能返回不准确的状态
- 没有基于实际的心跳时间戳进行健康判断

## 修复方案

### 1. 延长心跳超时时间
将超时时间从10秒延长到30秒（15个心跳周期）：

```java
// 修改前
.ofZSet().getByScoreRange(currentTime - 10000D, currentTime * 1D, 99999);

// 修改后
.ofZSet().getByScoreRange(currentTime - 30000D, currentTime * 1D, 99999);
```

### 2. 基于心跳时间戳判断健康状态
不再依赖`node.isHealthy()`，而是直接检查心跳时间：

```java
// 检查节点是否真正健康（基于心跳时间）
long heartbeatAge = currentTime - rank.getScore().longValue();
if (heartbeatAge < 30000) { // 30秒内有心跳则认为健康
    nodes.add(node);
}
```

### 3. 修复节点下线逻辑
更新`ServiceCenterService.deregisterNode()`方法，使用nodeId而不是JSON：

```java
// 修改前：使用JSON删除（不可靠）
String nodeJson = JSONUtil.toJsonStr(targetNode);
enhancedNodeService.rd(serviceName).ofZSet().zrem(nodeJson);

// 修改后：使用nodeId删除
enhancedNodeService.rd(serviceName).ofZSet().zrem(nodeId);
enhancedNodeService.rd(serviceName + ":details").ofHash().delete(nodeId);
```

## 修改的文件

1. **framework/src/main/java/cn/lingque/cloud/node/LQEnhancedRegisterCenter.java**
   - `getEnhancedNodeList()`: 延长超时时间，基于心跳时间判断健康
   - `getMCPToolNodes()`: 延长超时时间，基于心跳时间判断健康

2. **lq-admin/src/main/java/com/paypay/lqadmin/service/ServiceCenterService.java**
   - `deregisterNode()`: 使用nodeId删除节点

## 效果

- ✅ 节点健康状态稳定，不再频繁切换
- ✅ 更宽容的超时策略，允许短暂的网络抖动或系统负载
- ✅ 基于实际心跳时间的准确健康判断
- ✅ 节点下线功能正常工作

## 配置建议

当前配置：
- 心跳间隔: 2秒
- 超时时间: 30秒（15个心跳周期）

如果需要调整，建议：
- 超时时间 = 心跳间隔 × 10 ~ 15
- 例如：心跳间隔3秒，超时时间应为30-45秒

## 注意事项

1. 重启服务后，旧的Redis数据可能仍然存在，建议清理：
   ```bash
   redis-cli KEYS "LQ:CLOUD:ENHANCED:NODE:REG:CENTER*" | xargs redis-cli DEL
   ```

2. 如果仍然出现健康状态不稳定，可以进一步延长超时时间到60秒

3. 监控心跳延迟，如果经常超过10秒，需要检查网络或系统性能
