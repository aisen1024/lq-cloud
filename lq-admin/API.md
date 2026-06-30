# LingQue Admin API 文档

参考 Nacos 设计的 RESTful API 接口文档

## 认证接口

### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "success": true,
  "token": "xxx",
  "username": "admin",
  "message": "登录成功"
}
```

### 登出
```
POST /api/auth/logout
Authorization: Bearer {token}

Response:
{
  "success": true,
  "message": "登出成功"
}
```

## 配置中心接口

### 1. 获取命名空间列表
```
GET /api/config/namespaces

Response:
{
  "code": 200,
  "data": [
    {
      "namespace": "default",
      "namespaceShowName": "default",
      "configCount": 10,
      "type": 0
    }
  ]
}
```

### 2. 查询配置列表（分页）
```
GET /api/config/list?namespace=default&pageNo=1&pageSize=10&dataId=xxx&group=DEFAULT_GROUP

Response:
{
  "code": 200,
  "totalCount": 100,
  "pageNumber": 1,
  "pagesAvailable": 10,
  "pageItems": [
    {
      "dataId": "application.properties",
      "group": "DEFAULT_GROUP",
      "content": "server.port=8080",
      "md5": "xxx",
      "type": "properties"
    }
  ]
}
```

### 3. 获取配置详情
```
GET /api/config/detail?dataId=xxx&namespace=default&group=DEFAULT_GROUP

Response:
{
  "code": 200,
  "dataId": "application.properties",
  "group": "DEFAULT_GROUP",
  "content": "server.port=8080",
  "md5": "xxx",
  "type": "properties",
  "namespace": "default"
}
```

### 4. 发布配置
```
POST /api/config/publish
Content-Type: application/json

{
  "dataId": "application.properties",
  "group": "DEFAULT_GROUP",
  "content": "server.port=8080",
  "namespace": "default"
}

Response:
{
  "code": 200,
  "message": "发布成功",
  "data": true
}
```

### 5. 删除配置
```
DELETE /api/config/delete?dataId=xxx&namespace=default

Response:
{
  "code": 200,
  "message": "删除成功",
  "data": true
}
```

### 6. 获取配置历史
```
GET /api/config/history?dataId=xxx&namespace=default&pageNo=1&pageSize=10

Response:
{
  "code": 200,
  "totalCount": 0,
  "pageNumber": 1,
  "pagesAvailable": 0,
  "pageItems": []
}
```

### 7. 添加监听器
```
POST /api/config/listener
Content-Type: application/json

{
  "dataId": "application.properties",
  "namespace": "default"
}

Response:
{
  "code": 200,
  "message": "监听器已添加",
  "data": true
}
```

### 8. 获取统计信息
```
GET /api/config/stats

Response:
{
  "code": 200,
  "namespaceCount": 3,
  "configCount": 100,
  "validConfigCount": 95,
  "expiredConfigCount": 5,
  "listenerCount": 10,
  "healthStatus": "HEALTHY"
}
```

## 服务中心接口

### 1. 获取服务列表（分页）
```
GET /api/service/list?pageNo=1&pageSize=10&serviceName=xxx&groupName=DEFAULT_GROUP

Response:
{
  "code": 200,
  "count": 10,
  "serviceList": [
    {
      "name": "user-service",
      "groupName": "DEFAULT_GROUP",
      "clusterCount": 1,
      "ipCount": 3,
      "healthyInstanceCount": 2,
      "triggerFlag": false
    }
  ]
}
```

### 2. 获取服务详情
```
GET /api/service/detail?serviceName=user-service&groupName=DEFAULT_GROUP

Response:
{
  "code": 200,
  "name": "user-service",
  "groupName": "DEFAULT_GROUP",
  "protectThreshold": 0.0,
  "metadata": {},
  "selector": {},
  "clusters": [
    {
      "name": "DEFAULT",
      "healthChecker": {},
      "defaultPort": 8080,
      "defaultCheckPort": 8080,
      "useIPPort4Check": true,
      "metadata": {}
    }
  ]
}
```

### 3. 获取实例列表
```
GET /api/service/instance/list?serviceName=user-service&groupName=DEFAULT_GROUP&clusterName=DEFAULT&pageNo=1&pageSize=10&healthyOnly=true

Response:
{
  "code": 200,
  "count": 3,
  "hosts": [
    {
      "instanceId": "xxx",
      "ip": "192.168.1.100",
      "port": 8080,
      "weight": 100,
      "healthy": true,
      "enabled": true,
      "ephemeral": true,
      "clusterName": "DEFAULT",
      "serviceName": "user-service",
      "metadata": {
        "protocol": "HTTP",
        "version": "1.0.0",
        "status": "ONLINE"
      },
      "instanceHeartBeatInterval": 30000,
      "instanceHeartBeatTimeOut": 60000,
      "ipDeleteTimeout": 90000
    }
  ]
}
```

### 4. 更新实例
```
PUT /api/service/instance
Content-Type: application/json

{
  "serviceName": "user-service",
  "ip": "192.168.1.100",
  "port": 8080,
  "weight": 100,
  "enabled": true
}

Response:
{
  "code": 200,
  "message": "更新成功"
}
```

### 5. 下线实例
```
DELETE /api/service/instance?serviceName=user-service&ip=192.168.1.100&port=8080

Response:
{
  "code": 200,
  "message": "下线成功"
}
```

### 6. 获取统计信息
```
GET /api/service/stats

Response:
{
  "code": 200,
  "serviceCount": 10,
  "instanceCount": 30,
  "healthyInstanceCount": 25,
  "unhealthyInstanceCount": 5
}
```

## 消息总线接口

### 1. 获取主题列表
```
GET /api/bus/topics

Response: [
  {
    "name": "user.login",
    "subscriberCount": 5,
    "messageCount": 1000
  }
]
```

### 2. 获取订阅者
```
GET /api/bus/topic/{topic}/subscribers

Response: [
  {
    "id": "subscriber-1",
    "topic": "user.login",
    "status": "active"
  }
]
```

### 3. 发布消息
```
POST /api/bus/publish
Content-Type: application/json

{
  "topic": "user.login",
  "message": {"userId": 123}
}

Response:
{
  "success": true,
  "message": "消息发布成功"
}
```

### 4. 获取统计信息
```
GET /api/bus/stats

Response:
{
  "totalMessages": 10000,
  "totalTopics": 20,
  "totalSubscribers": 50
}
```

## 消息队列接口

### 1. 获取队列列表
```
GET /api/mq/queues

Response: [
  {
    "name": "order-queue",
    "type": "LIST",
    "pending": 100,
    "processed": 1000
  }
]
```

### 2. 获取队列信息
```
GET /api/mq/queue/{queue}

Response:
{
  "name": "order-queue",
  "type": "LIST",
  "pending": 100,
  "processed": 1000,
  "failed": 5
}
```

### 3. 发送消息
```
POST /api/mq/send
Content-Type: application/json

{
  "queue": "order-queue",
  "message": {"orderId": 123},
  "delay": 0
}

Response:
{
  "success": true,
  "message": "消息发送成功"
}
```

### 4. 获取统计信息
```
GET /api/mq/stats

Response:
{
  "totalProcessed": 10000,
  "totalFailed": 50,
  "totalQueues": 10,
  "totalPending": 200
}
```

### 5. 清空队列
```
DELETE /api/mq/queue/{queue}

Response:
{
  "success": true,
  "message": "队列清空成功"
}
```

## Dashboard 接口

### 获取总览信息
```
GET /api/dashboard/overview

Response:
{
  "stats": {
    "configCount": 100,
    "serviceCount": 10,
    "busTopicCount": 20,
    "mqQueueCount": 15
  },
  "services": [
    {
      "name": "user-service",
      "totalNodes": 3,
      "healthyNodes": 2
    }
  ],
  "activities": []
}
```

## 错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 认证说明

除了登录接口外，所有接口都需要在请求头中携带 Token：

```
Authorization: Bearer {token}
```

Token 有效期为 24 小时，过期后需要重新登录。
