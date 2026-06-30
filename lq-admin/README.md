# LingQue Admin - 管理和治理服务

LingQue Admin 是一个独立的管理和治理服务，提供对 LingQue 框架各个组件的统一管理界面。

## 功能特性

### 0. 安全认证
- 默认账号密码登录
- Token 认证机制
- 自动登出保护

### 1. 配置中心管理
- 查看所有命名空间和配置项
- 动态修改配置
- 配置统计信息

### 2. 服务中心管理
- 查看所有注册的服务
- 查看服务节点状态
- 手动下线节点
- 服务健康检查

### 3. 消息总线管理
- 查看所有主题
- 查看订阅者信息
- 发布测试消息
- 消息统计

### 4. 消息队列管理
- 查看所有队列
- 查看队列详情
- 发送测试消息
- 清空队列

### 5. Dashboard 总览
- 系统整体统计
- 服务状态概览
- 活动记录

## 默认账号

```
用户名: admin
密码: admin123
```

⚠️ **安全提示**：生产环境请修改默认密码！

## 技术架构

- Spring Boot 4.0.3
- Java 17
- LingQue Framework 1.1.8
- Vue 3 (前端)
- Token 认证

## 快速开始

### 启动后端服务

```bash
cd lq-admin
./mvnw spring-boot:run
```

或者使用 Maven：

```bash
mvn spring-boot:run
```

### 启动前端界面

```bash
cd dashboard
npm install
npm run dev
```

### 访问系统

1. 打开浏览器访问: http://localhost:5173
2. 使用默认账号登录:
   - 用户名: `admin`
   - 密码: `admin123`
3. 登录成功后即可使用管理功能

## API 接口

### 配置中心
- `GET /api/config/{namespace}` - 获取命名空间下所有配置
- `GET /api/config/{namespace}/{key}` - 获取指定配置
- `POST /api/config/{namespace}/{key}` - 设置配置
- `DELETE /api/config/{namespace}/{key}` - 删除配置
- `GET /api/config/stats` - 获取统计信息
- `GET /api/config/namespaces` - 获取所有命名空间

### 服务中心
- `GET /api/service/all` - 获取所有服务
- `GET /api/service/{serviceName}/nodes` - 获取服务节点
- `GET /api/service/{serviceName}/healthy` - 获取健康节点
- `DELETE /api/service/{serviceName}/node/{nodeId}` - 下线节点

### 消息总线
- `GET /api/bus/topics` - 获取所有主题
- `GET /api/bus/topic/{topic}/subscribers` - 获取订阅者
- `POST /api/bus/publish` - 发布消息
- `GET /api/bus/stats` - 获取统计信息

### 消息队列
- `GET /api/mq/queues` - 获取所有队列
- `GET /api/mq/queue/{queue}` - 获取队列信息
- `POST /api/mq/send` - 发送消息
- `GET /api/mq/stats` - 获取统计信息
- `DELETE /api/mq/queue/{queue}` - 清空队列

### Dashboard
- `GET /api/dashboard/overview` - 获取总览信息

## 配置说明

在 `application.properties` 中配置：

```properties
# 服务端口
server.port=8080

# LingQue Framework 配置
lingque.config.enabled=true
lingque.register.enabled=true
lingque.bus.enabled=true
lingque.mq.enabled=true
```

## 与 Framework 的关系

- `framework` 模块：提供核心架构能力（配置中心、服务注册、消息总线、消息队列等）
- `lq-admin` 模块：提供管理界面和 API，依赖 framework 模块
- 其他业务服务：依赖 framework 模块获取架构能力

## 开发说明

### 项目结构

```
lq-admin/
├── src/main/java/com/paypay/lqadmin/
│   ├── controller/          # REST API 控制器
│   │   ├── ConfigCenterController.java
│   │   ├── ServiceCenterController.java
│   │   ├── MessageBusController.java
│   │   ├── MessageQueueController.java
│   │   └── DashboardController.java
│   ├── service/             # 业务服务层
│   │   ├── MessageBusService.java
│   │   ├── MessageQueueService.java
│   │   └── DashboardService.java
│   ├── config/              # 配置类
│   │   └── WebConfig.java
│   └── LqAdminApplication.java
└── src/main/resources/
    └── application.properties
```

## 部署

### 打包

```bash
mvn clean package
```

### 运行

```bash
java -jar target/lq-admin-0.0.1-SNAPSHOT.jar
```

## 许可证

与主项目保持一致
