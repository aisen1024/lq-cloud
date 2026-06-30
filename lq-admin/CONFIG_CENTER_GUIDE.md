# 配置中心使用指南

## 设计理念

LingQue 配置中心参考 Nacos 设计，以配置文件为单位进行管理，而不是简单的 key-value 模式。

## 核心概念

### 1. 命名空间（Namespace/Tenant）

命名空间用于实现多租户隔离，不同命名空间的配置互不影响。

- `public`：公共命名空间（默认）
- 自定义命名空间：如 `dev`、`test`、`prod` 等

### 2. Data ID

配置文件的唯一标识，格式为：`{应用名}-{环境}.{后缀}`

**示例：**
- `user-service-dev.yml` - 用户服务开发环境配置
- `order-service-prod.properties` - 订单服务生产环境配置
- `gateway-test.json` - 网关测试环境配置

**命名规范：**
- 应用名：服务名称，如 `user-service`、`order-service`
- 环境：`dev`、`test`、`prod` 等
- 后缀：`yml`、`yaml`、`properties`、`json`、`xml`、`txt`

### 3. Group

配置分组，用于进一步分类管理，默认为 `DEFAULT_GROUP`。

### 4. 配置格式

支持多种配置格式：

- **YAML** (`.yml`, `.yaml`) - 推荐，层次清晰
- **Properties** (`.properties`) - 传统 Java 配置
- **JSON** (`.json`) - 结构化数据
- **XML** (`.xml`) - XML 格式
- **Text** (`.txt`) - 纯文本

## 使用场景

### 场景 1：新建配置

1. 选择命名空间（如 `public`）
2. 输入 Data ID（如 `user-service-dev.yml`）
3. 选择配置格式（YAML）
4. 编写配置内容
5. 点击发布

**YAML 配置示例：**

```yaml
# user-service-dev.yml
server:
  port: 8080
  servlet:
    context-path: /user

spring:
  application:
    name: user-service
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: root
    password: password

# 自定义配置
app:
  name: user-service
  version: 1.0.0
  features:
    - login
    - register
    - profile
```

### 场景 2：应用拉取配置

应用启动时，根据应用名和环境自动拉取配置：

```java
// 应用配置
spring.application.name=user-service
spring.profiles.active=dev

// 自动拉取配置
// Data ID: user-service-dev.yml
// Namespace: public
// Group: DEFAULT_GROUP
```

### 场景 3：多环境配置

为同一应用创建不同环境的配置：

- `user-service-dev.yml` - 开发环境
- `user-service-test.yml` - 测试环境
- `user-service-prod.yml` - 生产环境

### 场景 4：配置克隆

快速复制配置到其他环境：

```
源配置：user-service-dev.yml
目标配置：user-service-test.yml
```

## 配置验证

系统会自动验证配置格式：

### YAML 验证

```yaml
# 正确的 YAML
server:
  port: 8080
  host: localhost

# 错误的 YAML（缩进错误）
server:
port: 8080  # 缺少缩进
  host: localhost
```

### Properties 验证

```properties
# 正确的 Properties
server.port=8080
server.host=localhost

# 错误的 Properties（格式错误）
server.port 8080  # 缺少等号
```

### JSON 验证

```json
// 正确的 JSON
{
  "server": {
    "port": 8080,
    "host": "localhost"
  }
}

// 错误的 JSON（缺少引号）
{
  server: {
    port: 8080
  }
}
```

## API 使用

### 1. 获取配置列表

```bash
GET /api/config/list?tenant=public&pageNo=1&pageSize=10&appName=user-service

Response:
{
  "code": 200,
  "totalCount": 3,
  "pageItems": [
    {
      "dataId": "user-service-dev.yml",
      "group": "DEFAULT_GROUP",
      "content": "...",
      "type": "yml",
      "appName": "user-service",
      "tenant": "public"
    }
  ]
}
```

### 2. 获取配置详情

```bash
GET /api/config/detail?dataId=user-service-dev.yml&tenant=public

Response:
{
  "code": 200,
  "dataId": "user-service-dev.yml",
  "content": "...",
  "type": "yml",
  "appName": "user-service"
}
```

### 3. 发布配置

```bash
POST /api/config/publish
Content-Type: application/json

{
  "dataId": "user-service-dev.yml",
  "content": "server:\n  port: 8080",
  "type": "yml",
  "tenant": "public",
  "group": "DEFAULT_GROUP"
}

Response:
{
  "code": 200,
  "message": "发布成功",
  "data": true
}
```

### 4. 删除配置

```bash
DELETE /api/config/delete?dataId=user-service-dev.yml&tenant=public

Response:
{
  "code": 200,
  "message": "删除成功"
}
```

### 5. 克隆配置

```bash
POST /api/config/clone
Content-Type: application/json

{
  "srcDataId": "user-service-dev.yml",
  "destDataId": "user-service-test.yml",
  "tenant": "public"
}

Response:
{
  "code": 200,
  "message": "克隆成功"
}
```

## 最佳实践

### 1. 命名规范

- 使用小写字母和连字符
- 应用名与服务名保持一致
- 环境标识清晰明确

**推荐：**
- `user-service-dev.yml`
- `order-service-prod.properties`

**不推荐：**
- `UserService_DEV.yml`
- `config.yml`

### 2. 配置分层

将配置按层次组织：

```yaml
# 基础配置
server:
  port: 8080

# Spring 配置
spring:
  application:
    name: user-service

# 业务配置
app:
  features:
    enabled: true
```

### 3. 敏感信息

敏感信息建议加密存储：

```yaml
# 使用占位符
spring:
  datasource:
    password: ${DB_PASSWORD}

# 或使用加密值
spring:
  datasource:
    password: ENC(encrypted_value)
```

### 4. 配置注释

添加清晰的注释说明：

```yaml
# 服务器配置
server:
  port: 8080  # 服务端口
  
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db  # 数据库连接地址
```

### 5. 版本管理

通过 Data ID 管理版本：

- `user-service-v1.yml`
- `user-service-v2.yml`

## 常见问题

### Q1: 配置格式验证失败？

**A:** 检查配置格式是否正确：
- YAML：注意缩进（使用空格，不要用 Tab）
- Properties：确保使用 `key=value` 格式
- JSON：检查引号和逗号

### Q2: 如何批量导入配置？

**A:** 使用导入功能：
```bash
POST /api/config/import
```

### Q3: 配置更新后应用如何感知？

**A:** 应用需要实现配置监听器，自动刷新配置。

### Q4: 如何备份配置？

**A:** 使用导出功能：
```bash
GET /api/config/export?tenant=public
```

## 与 Nacos 的对比

| 功能 | Nacos | LingQue |
|------|-------|---------|
| 命名空间 | ✅ | ✅ |
| Data ID | ✅ | ✅ |
| Group | ✅ | ✅ |
| 配置格式 | YAML/Properties/JSON/XML | YAML/Properties/JSON/XML/Text |
| 格式验证 | ✅ | ✅ |
| 配置监听 | ✅ | ✅ |
| 历史版本 | ✅ | 🚧 开发中 |
| 灰度发布 | ✅ | 🚧 计划中 |

## 总结

LingQue 配置中心采用与 Nacos 相同的设计理念，以配置文件为单位进行管理，支持多种格式和自动验证，适合微服务架构的配置管理需求。
