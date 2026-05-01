# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在此项目中工作提供指导。

## 项目概述

**Duke 平台** 是一个基于微服务的单体仓库，包含认证、API 网关和 Transformer 模型可视化服务以及对应的 Web 前端。

### 项目结构

```
duke-platform/
├── backend/                    # 后端服务目录
│   ├── duke-parent/           # Maven 父 POM，统一版本管理
│   ├── duke-framework/        # 共享框架库（所有服务依赖）
│   ├── duke-auth/             # 认证服务
│   ├── duke-gateway/          # API 网关
│   ├── duke-transformer/      # Transformer 模型可视化服务
│   └── duke-knowledge-qa/     # 知识问答服务（空目录，待实现）
└── frontend/                   # 前端服务目录
    ├── duke-auth-web/         # 认证前端
    ├── duke-transformer-web/  # Transformer 前端
    └── duke-knowledge-qa-web/ # 知识问答前端（空目录，待实现）
```

### 架构

- **后端服务**（Java/Spring Boot）：位于 `/backend` 目录
  - `duke-parent`：Maven 父 POM，统一依赖和插件版本
  - `duke-framework`：共享框架库，包含通用工具、异常、配置等
  - `duke-auth`：用户认证和授权，支持 JWT、OAuth（微信/GitHub）、短信登录
  - `duke-gateway`：API 网关，使用 Spring Cloud Gateway 和 Nacos 服务发现
  - `duke-transformer`：Transformer 模型可视化和教学演示系统（Nacos 注册名：`duke-transformer`）
  - `duke-knowledge-qa`：知识问答服务（待实现）

- **前端服务**（Vue 3/TypeScript）：位于 `/frontend` 目录
  - `duke-auth-web`：认证和授权界面
  - `duke-transformer-web`：Transformer 可视化和学习界面，支持 SSE 流式传输
  - `duke-knowledge-qa-web`：知识问答界面（待实现）

### 核心技术栈

- **后端技术栈**：
  - Java 21
  - Spring Boot 3.2.5
  - Spring Cloud（2023.0.1）
  - Spring Cloud Alibaba（Nacos）
  - MyBatis-Plus
  - MySQL
  - Redis
  - OpenAPI/Swagger
- **前端技术栈**：
  - Vue 3.5
  - TypeScript
  - Vite
  - Element Plus
  - Pinia（状态管理）
  - Axios
- **基础设施**：
  - Nacos（服务发现/配置）
  - MySQL
  - Redis

### 关键基础设施服务

- **Nacos**（127.0.0.1:8848）：服务发现和配置管理
- **MySQL**：用户数据、认证记录
- **Redis**：Token 黑名单（JWT 撤销）、会话存储

## 后端服务（Java/Maven）开发指南

### 常用 Maven 命令

在任何 `/duke-{service}` 目录下执行：

```bash
# 构建
mvn clean install

# 本地运行服务
mvn spring-boot:run

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=AuthServiceTest

# 运行特定测试方法
mvn test -Dtest=AuthServiceTest#testLoginSuccess

# 仅编译（不运行测试）
mvn clean compile

# 构建 JAR 包
mvn package
```

### 运行服务

服务启动时自动向 Nacos 注册并从 Nacos Config 加载配置。配置优先级：

1. 环境变量（开发时使用 `.env` 文件）
2. Nacos Config（在线配置，支持热更新）
3. 本地 `application.yml` 中的属性
4. 代码中的默认值

**本地 application.yml 最小化配置：**
```yaml
server:
  port: 8081
  servlet:
    context-path: /auth

spring:
  application:
    name: duke-auth
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
      config:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
        file-extension: yml
  config:
    import:
      - nacos:duke-common.yml?refreshEnabled=true
      - nacos:duke-auth.yml?refreshEnabled=true
```

**Nacos 中的配置文件：**
- `duke-common.yml`：所有服务共享配置（Redis、JWT、内部通信密钥）
- `duke-auth.yml`：认证服务特定配置（数据库、OAuth）
- `duke-gateway.yml`：网关特定配置（路由、Swagger 聚合）
- `duke-transformer.yml`：Transformer 服务配置
- `duke-knowledge-qa.yml`：知识问答服务配置

**环境变量**（复制 `.env.example` 为 `.env` 并填入实际值）：
```bash
# OAuth 和第三方 API（如果需要）
WEIXIN_APP_ID, WEIXIN_APP_SECRET          # 微信 OAuth
GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET    # GitHub OAuth
```

### 服务端口映射

- `duke-gateway`：8080（API 网关）
- `duke-auth`：8081（上下文路径：`/auth`，Nacos 注册名：`duke-auth`）
- `duke-transformer`：8082（上下文路径：`/transformer`，Nacos 注册名：`duke-transformer`）
- `duke-knowledge-qa`：8083（上下文路径：`/knowledge-qa`，Nacos 注册名：`duke-knowledge-qa`）

### 测试服务

**检查 Nacos 是否运行：**
```bash
curl http://127.0.0.1:8848/nacos/v1/ns/service/list
```

**检查服务注册：**
```bash
curl http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=duke-auth
curl http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=duke-transformer
```

**测试 Swagger/OpenAPI 文档**（服务运行后）：
- `http://localhost:8081/auth/swagger-ui.html`
- `http://localhost:8082/transformer/swagger-ui.html`

## 前端服务（Vue 3/Vite）开发指南

### 前端应用概览

- `duke-auth-web`（应用名：`duke-auth-web`）：认证界面
- `duke-transformer-web`（应用名：`duke-transformer-web`）：Transformer 可视化界面

### 常用 npm 命令

在任何 `/frontend/duke-{service}-web` 目录下执行：

```bash
# 安装依赖
npm install

# 开发服务器（Vite）
npm run dev

# 构建生产版本
npm run build

# 本地预览生产构建
npm run preview

# 类型检查（通过 vue-tsc）
npm run build  # 包含类型检查
```

### Vite 开发服务器

默认运行在 `http://localhost:5173`。特性：
- 热模块替换（HMR）实现即时更新
- TypeScript 支持（通过 `vue-tsc`）
- 自动导入（通过 unplugin-auto-import 和 unplugin-vue-components）

### 前端目录结构

每个前端项目遵循以下布局：
```
src/
├── api/              # API 客户端函数（axios）
├── components/       # 可复用的 Vue 组件
├── layout/          # 布局包装器
├── router/          # Vue Router 配置
├── stores/          # Pinia 状态管理定义
├── styles/          # 全局 SCSS/CSS 样式
├── types/           # TypeScript 类型定义
├── utils/           # 辅助函数
└── views/           # 全页面组件
```

### 开发工作流

1. **启动后端服务**（在 `backend/duke-{service}` 中执行 `mvn spring-boot:run`）
   - 认证服务：`cd backend/duke-auth && mvn spring-boot:run`
   - Transformer 服务：`cd backend/duke-transformer && mvn spring-boot:run`
2. **启动前端开发服务器**（在 `frontend/duke-{service}-web` 中执行 `npm run dev`）
   - 认证前端：`cd frontend/duke-auth-web && npm run dev`
   - Transformer 前端：`cd frontend/duke-transformer-web && npm run dev`
3. **确认 API 配置**：前端通过网关调用后端（API 基础 URL：`http://localhost:8080`）
4. **确保 Nacos 运行**：服务依赖 Nacos 进行服务发现（`http://127.0.0.1:8848`）

## 关键模式和约定

### 后端（Java）

**共享框架库 (`duke-framework`)**：

`duke-framework` 包含所有服务共享的功能：
- 全局异常处理和自定义异常类
- 统一的 API 响应包装（Success/Error）
- 工具类和常用方法
- Spring Boot 自动配置（自动装配到其他服务）
- 拦截器和过滤器配置

**项目结构**（每个服务）：
```
src/main/java/com/duke/{service}/
├── aspect/          # 自定义 AOP 方面和注解
├── common/          # 共享工具和常量
├── config/          # Spring 配置类
├── controller/      # REST 控制器
├── dto/             # 数据传输对象
├── entity/          # JPA/MyBatis 实体
├── enums/           # Java 枚举
├── event/           # 事件监听器
├── exception/       # 自定义异常
├── mapper/          # MyBatis 映射器
├── service/         # 业务逻辑接口
├── service/impl/    # 业务逻辑实现
└── util/            # 工具类
```

**Spring Security 和 JWT**：
- 在 `duke-auth` 中实现，使用自定义安全过滤器
- JWT token 在网关和服务级别验证
- Token 黑名单存储在 Redis 中（TTL = 过期时间）
- Claims 包含用户 ID、用户名、角色

**MyBatis-Plus 配置**：
- 启用逻辑删除（deleted 字段 = 1 表示已删除）
- Mapper XML 文件位于 `src/main/resources/mapper/*.xml`
- 全局 ID 策略：AUTO（自增长）

**API 文档**：
- 所有服务上配置 SpringDoc OpenAPI（Swagger 3）
- 注解：`@Operation`、`@Parameter`、`@Schema` 用于端点文档

### 前端（Vue 3）

**状态管理（Pinia）**：
- Store 定义在 `src/stores/` 中，包含反应式状态和操作
- 使用 `import { useStore } from '@/stores/store-name'`

**路由（Vue Router）**：
- 路由定义在 `src/router/` 中
- 导航守卫用于认证检查

**API 集成（Axios）**：
- API 函数在 `src/api/` 中，返回 Promise
- 拦截器用于 token 附加、错误处理
- 基础 URL 从环境配置读取

**UI 组件**：
- 使用 Element Plus 作为标准 UI 元素
- 通过 unplugin-vue-components 自动导入（无需显式导入）

### 特殊功能：Transformer 演示（SSE 流式传输）

`duke-transformer` 提供实时 Transformer 模型可视化系统：

- **服务名称说明**：
  - 后端目录：`backend/duke-transformer`
  - Nacos 注册名：`duke-transformer`
  - 前端应用名：`duke-transformer-web`（存储库目录：`frontend/duke-transformer-web`）

- **Encoder API**（`GET /run-encoder`）：服务器发送事件（SSE）流，包含计算步骤
- **Decoder API**（`GET /run-autoregressive`）：SSE 流，展示 token 生成过程
- 前端（`duke-transformer-web`）实时可视化矩阵和文本流
- 完整端点规范见 `frontend/duke-transformer-web/` 中的 `API_REFERENCE.md`

## 当前实现状态

### 已实现的服务

- ✅ **duke-auth**：完整的认证服务，支持多种登录方式，内部接口 `/internal/users/{userId}`
- ✅ **duke-gateway**：API 网关，路由转发、JWT 认证、权限检查、Swagger 聚合
- ✅ **duke-transformer**：Transformer 模型可视化系统
- ✅ **duke-knowledge-qa**：知识问答服务骨架（已接入网关、Nacos、OpenFeign）
- ✅ **duke-auth-web**：认证前端界面
- ✅ **duke-transformer-web**：Transformer 前端界面
- ✅ **duke-knowledge-qa-web**：知识问答前端界面（Vue 3 + Vite，iframe 嵌入模式）

### 架构增强（2026.4.30）

- ✅ **Nacos Config**：所有服务配置中心化，支持热更新
- ✅ **OpenFeign**：内部服务间通信规范化
- ✅ **网关路由**：duke-knowledge-qa 已集成网关、Swagger 聚合
- ✅ **内部通信**：基于 `X-Gateway-Secret` 的安全内部接口

### 待完全实现的服务

- ⏳ **duke-knowledge-qa**：业务逻辑开发中

## 开发检查清单

开始功能开发或修复前：

1. **基础设施**：
   - 确保 Nacos 运行在 `127.0.0.1:8848`
   - 确保 MySQL 和 Redis 可访问
   - 复制各服务的 `.env.example` 为 `.env`（可选，用于环保 OAuth 等第三方密钥）

2. **数据库初始化**：
   - 执行 `backend/init.sql` 初始化 `duke_auth` 数据库
   - 命令：`mysql -u root -p123456 duke_auth < backend/init.sql`
   - 脚本包含 14 个表的完整 DDL 和种子数据（用户、角色、菜单、按钮等）
   - 种子数据包括：admin（超级管理员）、xyl、gh_dukehu 三个用户，初始密码为 `admin123`

3. **Nacos 配置**：
   - 在 Nacos 中创建 5 个配置文件（详见 DEVELOPER_GUIDE.md）：
     - `duke-common.yml` - 共享配置
     - `duke-auth.yml` - 认证服务配置
     - `duke-gateway.yml` - 网关配置和路由
     - `duke-transformer.yml` - Transformer 服务配置
     - `duke-knowledge-qa.yml` - 知识问答服务配置

4. **后端依赖**：`cd backend && mvn clean install`
5. **前端依赖**：各前端目录下执行 `npm install`
6. **构建和测试**：按照上述 Maven/npm 命令进行
7. **类型安全**：提交前运行 `npm run build` 捕捉 TypeScript 错误
8. **API 集成**：在前端使用前通过 Swagger UI 或 curl 测试端点
   - 统一网关：`http://localhost:8080/swagger-ui.html`
   - 独立服务：`http://localhost:8081/auth/swagger-ui.html`、`http://localhost:8082/transformer/swagger-ui.html`
9. **跨服务**：前端所有请求都通过网关路由（端口 8080），不直接调用服务端口
10. **服务间通信**：使用 OpenFeign + 内部密钥，不走网关、不传 JWT

## 多服务交互

### 前端 → 后端（通过网关）

所有前端请求都通过 API 网关（端口 8080）转发：

- `duke-auth-web` 调用 `/api/auth/**` → 网关转发到 `lb://duke-auth` → `duke-auth` 服务
- `duke-transformer-web` 调用 `/api/transformer/**` → 网关转发到 `lb://duke-transformer` → `duke-transformer` 服务

### 服务 → 服务（OpenFeign + 内部密钥）

内部服务间通信**不走网关、不传 JWT token**，使用 OpenFeign + 内部密钥机制：

- **OpenFeign 客户端**：在需要调用其他服务的服务中定义 `@FeignClient` 接口
- **内部密钥认证**：所有内部请求自动携带 `X-Gateway-Secret` 请求头
- **内部端点**：被调用服务在 `/internal/**` 路径下提供内部接口，Spring Security 白名单放行
- 例：`duke-knowledge-qa` 通过 `AuthFeignClient` 调用 `duke-auth` 的 `/internal/users/{userId}`

配置方式：
```java
// AuthFeignClient.java
@FeignClient(
    name = "duke-auth",
    contextId = "authFeignClient",
    configuration = InternalFeignConfig.class  // 自动注入内部密钥
)
public interface AuthFeignClient {
    @GetMapping("/internal/users/{userId}")
    Result<Object> getUserById(@PathVariable Long userId);
}
```

### 认证流程

1. 用户在 `duke-auth-web` 登录，调用 `/api/auth/login`
2. 网关转发到 `duke-auth` 服务（`/rbac/login`）
3. 认证服务返回 JWT token
4. 前端保存 token，后续请求在 `Authorization: Bearer {token}` 请求头中包含 token
5. 网关验证 token 签名和过期时间
6. 网关验证通过后转发请求到对应服务
7. 服务验证 token claims 获取用户权限信息

## duke-auth 权限系统参考（数据库：auth_center）

> 编写 SQL 增量脚本时直接参考本节，无需重新探索代码库。

### 数据库表结构

| 表名 | 说明 | 关键列 |
|------|------|--------|
| `sys_app` | 应用 | id, **app_code** VARCHAR(64)（Java 实体字段名 `appCode`）, app_name |
| `sys_menu` | 菜单/目录 | id, parent_id, **app_id** BIGINT（需迁移补充）, menu_name, **menu_type**, path, component, permission, icon, sort_order, visible, status |
| `sys_button` | 页面按钮 | id, menu_id, button_name, button_code, **button_type**（需迁移补充）, sort_order, status |
| `sys_role` | 角色 | id, role_name, role_code, data_scope, status |
| `sys_user` | 用户 | id, username, password(BCrypt), real_name, nickname, status |
| `sys_role_menu` | 角色-菜单关联 | role_id, menu_id |
| `sys_role_button` | 角色-按钮关联 | role_id, button_id |
| `sys_role_api` | 角色-API关联 | role_id, api_id |
| `sys_user_role` | 用户-角色关联 | user_id, role_id |
| `sys_dept` / `sys_user_dept` / `sys_role_dept` | 部门体系 | — |
| `sys_api` | 接口资源 | id, app_id VARCHAR, api_path, api_method, permission |
| `sys_operation_log` | 操作日志 | — |

> DDL 来源：`backend/duke-auth/src/main/resources/db/schema.sql`
> 增量补丁：`backend/duke-auth/src/main/resources/db/migration.sql`

### 已知 Entity ↔ DB 差异（schema.sql 未包含，需迁移）

```sql
-- sys_button 缺少 button_type（Java 实体有此字段）
ALTER TABLE sys_button
    ADD COLUMN IF NOT EXISTS button_type TINYINT NOT NULL DEFAULT 2
    COMMENT '按钮类型 1=头部按钮 2=行操作按钮' AFTER button_code;

-- sys_menu 缺少 app_id（Java 实体有此字段）
ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS app_id BIGINT DEFAULT NULL
    COMMENT '所属应用ID' AFTER parent_id;
```

### 枚举值

**menu_type**：`1`=目录（directory） `2`=页面（page） `3`=按钮（已废弃，现用 sys_button）

**button_type**：`1`=头部按钮（工具栏）`2`=行操作按钮（表格行内）

**data_scope**：`1`=全部 `2`=自定义 `3`=本部门 `4`=本部门及下级 `5`=仅本人

**status**：`0`=禁用 `1`=启用 | **deleted**：`0`=正常 `1`=已删除（MyBatis-Plus 逻辑删除）

### 已有种子数据（关键 ID 速查）

**sys_app**

| id | app_code | app_name |
|----|----------|----------|
| 1 | duke-auth | 权限管理中心 |
| 2 | doc-chat | 文档问答服务 |

**sys_role**

| id | role_code | role_name |
|----|-----------|-----------|
| 1 | SUPER_ADMIN | 超级管理员 |
| 2 | USER | 普通用户 |

**sys_user**（密码均为 `admin123`）

| id | username |
|----|----------|
| 1 | admin |
| 2 | user |

**sys_menu（已有 id 1-11）**

| id | parent_id | menu_name | menu_type | component |
|----|-----------|-----------|-----------|-----------|
| 1 | 0 | 系统管理 | 1(目录) | — |
| 2 | 1 | 用户管理 | 2(页面) | system/user/index |
| 3 | 1 | 角色管理 | 2 | system/role/index |
| 4 | 1 | 菜单管理 | 2 | system/menu/index |
| 5 | 1 | 部门管理 | 2 | system/dept/index |
| 6 | 1 | API管理 | 2 | system/api/index |
| 7 | 1 | 操作日志 | 2 | system/log/index |
| 8 | 0 | 文档问答 | 1(目录) | — |
| 9 | 8 | 问答助手 | 2(页面) | doc-chat/index |
| 10 | 0 | 知识问答 | 1(目录) | — |
| 11 | 10 | 知识问答系统 | 2(页面) | knowledge-qa/index |

> **新模块从 id=12 开始编号**，避免冲突。

### 前端 component 路径映射规则

duke-auth-web 的 permission store 将 `sys_menu.component` 映射到 Vue 组件：

```typescript
// 优先尝试 index.vue 子目录形式，退而使用直接路径
modules[`/src/views/${component}/index.vue`]
|| modules[`/src/views/${component}.vue`]
```

示例：

| DB component 值 | 实际加载文件 |
|----------------|-------------|
| `system/user/index` | `src/views/system/user/index.vue`（退而匹配） |
| `doc-chat/index` | `src/views/doc-chat/index.vue`（退而匹配） |
| `knowledge-qa/index` | `src/views/knowledge-qa/index.vue`（退而匹配） |

### 新增模块 SQL 模板

每次为新模块添加菜单/按钮时，参考此模板（替换 `{MODULE}` 等占位符）：

```sql
USE auth_center;

-- 1. 补丁：确保 entity 所需列存在
ALTER TABLE sys_button ADD COLUMN IF NOT EXISTS button_type TINYINT NOT NULL DEFAULT 2
    COMMENT '按钮类型 1=头部 2=行操作' AFTER button_code;
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS app_id BIGINT DEFAULT NULL
    COMMENT '所属应用ID' AFTER parent_id;

-- 2. 应用（若需独立应用）
INSERT IGNORE INTO sys_app (app_code, app_name, app_desc, status)
VALUES ('{module-id}', '{模块名}', '{描述}', 1);

-- 3. 菜单目录
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, visible, status)
VALUES ({dir_id}, 0, '{模块名}', 1, '/{module}', NULL, '{Icon}', {order}, 1, 1);

-- 4. 页面菜单（component 对应 duke-auth-web/src/views/{component}/index.vue）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, visible, status)
VALUES ({page_id}, {dir_id}, '{页面名}', 2, '/{module}/index', '{module}/index', '{Icon}', 1, 1, 1);

-- 5. 按钮
INSERT IGNORE INTO sys_button (menu_id, button_name, button_code, button_type, sort_order, status) VALUES
({page_id}, '新增', '{module}:add',    1, 1, 1),
({page_id}, '删除', '{module}:delete', 2, 1, 1);

-- 6. 授权（SUPER_ADMIN role_id=1）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, {dir_id}), (1, {page_id});

COMMIT;
```

### iframe 嵌入模式（doc-chat / knowledge-qa 同款）

**通信协议（postMessage）**：
1. iframe 就绪 → `window.parent.postMessage({ type: 'IFRAME_READY' }, '*')`
2. 父窗口 → `postMessage({ type: 'AUTH_TOKEN', token })`
3. iframe 401 → `window.parent.postMessage({ type: 'AUTH_EXPIRED' }, '*')`

**父窗口（duke-auth-web）** 组件模板：`src/views/{module}/index.vue`，参考 `src/views/doc-chat/index.vue`。

**iframe 应用** 的 `utils/request.ts` 使用内存变量存 token（非 sessionStorage），401 时发送 `AUTH_EXPIRED` 而不跳转。

---

**最后更新**：2026 年 5 月 1 日

**版本说明**：
- 2026.4.29：初始版本，基于项目实际结构
- 2026.4.30：新增 Nacos Config（配置中心）、OpenFeign（服务间通信）、duke-knowledge-qa 网关集成
- 2026.5.01：新增 duke-auth 权限系统参考章节；duke-knowledge-qa-web 前端实现完成；新增 init.sql 数据库初始化脚本