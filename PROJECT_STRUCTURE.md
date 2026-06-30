# LingQue Cloud 项目结构说明

## 整体架构

```
lq_cloud/
├── framework/          # 核心框架模块
├── lq-admin/          # 管理和治理服务
└── dashboard/         # 前端管理界面
```

## 模块说明

### 1. Framework（核心框架）

提供微服务架构的核心能力，是整个系统的基础设施层。

**主要功能：**
- 配置中心：动态配置管理、命名空间、版本控制
- 服务注册中心：服务发现、健康检查、节点管理
- 消息总线：事件驱动通信、主题订阅
- 消息队列：异步消息处理、延迟消息
- HTTP 客户端：声明式 HTTP 调用
- RPC 调用：分布式服务调用
- Redis 增强操作：分布式锁、数据结构操作

**依赖关系：**
- 独立模块，不依赖其他模块
- 发布到 Maven 中央仓库供其他服务使用

### 2. LQ-Admin（管理服务）

独立的管理和治理服务，提供统一的管理界面和 API。

**主要功能：**
- 配置中心管理：查看、修改、删除配置
- 服务中心管理：查看服务、节点状态、手动下线
- 消息总线管理：查看主题、订阅者、发布测试消息
- 消息队列管理：查看队列、发送消息、清空队列
- Dashboard 总览：系统统计、服务状态

**技术栈：**
- Spring Boot 4.0.3
- Java 17
- 依赖 framework 模块

**端口：** 8080

**目录结构：**
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

### 3. Dashboard（前端界面）

基于 Vue 3 的可视化管理界面。

**主要功能：**
- 配置中心界面
- 服务中心界面
- 消息总线界面
- 消息队列界面
- Dashboard 总览

**技术栈：**
- Vue 3
- Vite
- Axios

**端口：** 5173

**目录结构：**
```
dashboard/
├── src/
│   ├── views/               # 页面组件
│   │   ├── ConfigCenter.vue
│   │   ├── ServiceCenter.vue
│   │   ├── MessageBus.vue
│   │   ├── MessageQueue.vue
│   │   └── Dashboard.vue
│   ├── api/                 # API 接口
│   │   └── index.js
│   ├── App.vue
│   └── main.js
├── index.html
├── package.json
└── vite.config.js
```

## 数据流向

```
Dashboard (前端)
    ↓ HTTP 请求
LQ-Admin (管理服务)
    ↓ 调用
Framework (核心框架)
    ↓ 操作
Redis (数据存储)
```

## 部署架构

### 开发环境
```
┌─────────────┐
│  Dashboard  │ :5173
└──────┬──────┘
       │ HTTP
┌──────▼──────┐
│  LQ-Admin   │ :8080
└──────┬──────┘
       │ 依赖
┌──────▼──────┐
│  Framework  │
└──────┬──────┘
       │ 连接
┌──────▼──────┐
│    Redis    │ :6379
└─────────────┘
```

### 生产环境
```
┌─────────────┐
│    Nginx    │ :80
└──────┬──────┘
       │
       ├─────────────┐
       │             │
┌──────▼──────┐ ┌───▼────────┐
│  Dashboard  │ │  LQ-Admin  │
│   (静态)    │ │  (多实例)  │
└─────────────┘ └─────┬──────┘
                      │
                ┌─────▼──────┐
                │  Framework │
                └─────┬──────┘
                      │
                ┌─────▼──────┐
                │Redis Cluster│
                └────────────┘
```

## 使用场景

### 场景 1：业务服务使用框架能力

```xml
<!-- 业务服务的 pom.xml -->
<dependency>
    <groupId>io.github.aisen1024</groupId>
    <artifactId>framework</artifactId>
    <version>1.1.8</version>
</dependency>
```

业务服务直接依赖 framework，获取配置中心、服务注册等能力。

### 场景 2：运维人员管理系统

1. 启动 lq-admin 服务
2. 访问 dashboard 界面
3. 通过界面管理配置、服务、消息等

### 场景 3：开发调试

1. 使用 dashboard 查看服务注册情况
2. 使用 dashboard 修改配置进行测试
3. 使用 dashboard 发送测试消息

## 快速开始

### 一键启动（推荐）

```bash
./start-all.sh
```

### 分别启动

1. 启动管理服务：
```bash
cd lq-admin
./start.sh
```

2. 启动前端界面：
```bash
cd dashboard
./start.sh
```

## 访问地址

- 管理后端 API: http://localhost:8080/api
- 管理前端界面: http://localhost:5173

## 注意事项

1. **Redis 必须先启动**：所有功能都依赖 Redis
2. **端口冲突**：确保 8080 和 5173 端口未被占用
3. **跨域配置**：lq-admin 已配置 CORS，允许前端访问
4. **依赖版本**：确保 Java 17+ 和 Node.js 16+ 已安装
