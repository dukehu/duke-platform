# Duke 平台开发指南

完整的项目开发、架构、配置和部署指南。

## 目录

1. [系统架构](#系统架构)
2. [项目结构](#项目结构)
3. [技术栈](#技术栈)
4. [依赖管理](#依赖管理)
5. [配置管理（Nacos Config）](#配置管理nacos-config)
6. [网关配置](#网关配置)
7. [服务间通信（OpenFeign）](#服务间通信openfeign)
8. [API Swagger 管理](#api-swagger-管理)
9. [权限和认证](#权限和认证)
10. [新建微服务](#新建微服务)
11. [开发工作流](#开发工作流)
12. [常见问题](#常见问题)

---

## 系统架构

### 整体设计

```
┌─────────────────────────────────────────────────────────────┐
│                     前端应用 (Vue 3)                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓ (HTTP/REST)
┌─────────────────────────────────────────────────────────────┐
│               API 网关 (Spring Cloud Gateway)                 │
│  - JWT 认证 (JwtAuthFilter)                                  │
│  - 权限检查 (ApiPermissionFilter)                            │
│  - Swagger 聚合管理                                          │
└──────────────────────┬──────────────────────────────────────┘
       │              │               │
       ↓              ↓               ↓
┌─────────┐   ┌─────────────┐   ┌──────────────┐
│ duke-   │   │ duke-       │   │ duke-        │
│ auth    │   │ transformer │   │ knowledge-qa │
│ (8081)  │   │ (8082)      │   │ (待建)       │
└────┬────┘   └─────────────┘   └──────────────┘
     │
     ├─ 系统管理 (权限、API、用户)
     ├─ 中央 API 管理库 (sys_api 表)
     ├─ JWT + Redis Token 黑名单
     └─ Security 权限框架

共享框架库: duke-framework (异常、工具、响应格式、拦截器)
共享配置: Nacos (服务发现、配置管理)
数据库: MySQL、Redis
```

### 服务角色

| 服务 | 端口 | Context Path | 功能 |
|------|------|------|------|
| **duke-gateway** | 8080 | / | API 网关、路由、认证、权限检查 |
| **duke-auth** | 8081 | /auth | 用户认证、权限管理、API 中央管理库 |
| **duke-transformer** | 8082 | /transformer | Transformer 模型可视化 |
| **duke-knowledge-qa** | 8083 | /knowledge-qa | 知识问答服务（待实现） |

---

## 项目结构

```
duke-platform/
├── backend/
│   ├── duke-parent/                    # Maven 父 POM，统一版本管理
│   ├── duke-framework/                 # 共享框架库
│   │   ├── common/                     # 通用响应、异常、常量
│   │   ├── config/                     # 通用配置（拦截器、异常处理）
│   │   ├── exception/                  # 自定义异常类
│   │   ├── util/                       # 工具类
│   │   └── dto/                        # 公共 DTO
│   ├── duke-auth/                      # 认证服务（中央管理）
│   │   ├── controller/                 # REST 接口
│   │   ├── service/                    # 业务逻辑
│   │   ├── mapper/                     # 数据库映射
│   │   ├── entity/                     # 数据库实体
│   │   ├── config/                     # Security 配置
│   │   ├── security/                   # 安全相关
│   │   ├── filter/                     # 自定义过滤器
│   │   ├── util/                       # API 扫描等工具
│   │   ├── event/                      # API 同步事件
│   │   └── resources/
│   │       ├── application.yml         # 应用配置
│   │       └── mapper/                 # MyBatis XML 映射
│   ├── duke-transformer/               # Transformer 服务
│   │   ├── controller/
│   │   ├── service/
│   │   ├── resources/
│   │   │   └── application.yml
│   │   └── ...
│   ├── duke-gateway/                   # API 网关
│   │   ├── filter/                     # 网关过滤器
│   │   │   ├── JwtAuthFilter.java     # JWT 认证
│   │   │   ├── ApiPermissionFilter.java # 权限检查
│   │   │   └── RequestLoggingFilter.java # 日志记录
│   │   ├── config/                     # 网关配置
│   │   ├── client/                     # Feign 客户端
│   │   └── resources/
│   │       └── application.yml         # 网关路由配置
│   └── DEVELOPER_GUIDE.md              # ← 本文件
└── frontend/
    ├── duke-auth-web/                  # 认证前端
    ├── duke-transformer-web/           # Transformer 前端
    └── duke-knowledge-qa-web/          # 知识问答前端（待实现）
```

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 服务发现 |
| Spring Security | 3.2.5 | 权限框架 |
| Spring Cloud Gateway | 2023.0.1 | API 网关 |
| Spring Cloud OpenFeign | 2023.0.1 | 服务间调用（RPC） |
| Spring Cloud Alibaba Nacos Config | 2023.0.1.0 | 配置中心 |
| MyBatis-Plus | 3.5.6 | ORM 框架 |
| MySQL | 8.0+ | 关系数据库 |
| Redis | 6.0+ | 缓存、Session 存储 |
| JJWT | 0.12.5 | JWT Token 处理 |
| SpringDoc OpenAPI | 2.5.0 | Swagger 3 文档 |
| Hutool | 5.8.26 | Java 工具库 |
| Jieba | 1.0.2 | 中文分词 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| TypeScript | 5.3+ | 编程语言 |
| Vite | 5+ | 构建工具 |
| Element Plus | 2.5+ | UI 组件库 |
| Pinia | 2+ | 状态管理 |
| Axios | 1.6+ | HTTP 客户端 |
| TailwindCSS | 3+ | 样式框架（可选） |

---

## 依赖管理

### POM 组织原则

为了避免重复和管理混乱，采用**统一版本管理**模式：

- **Parent POM 直接依赖**：所有服务都需要的依赖（Lombok、framework、测试）
- **Parent POM dependencyManagement**：版本定义，服务按需引入
- **各服务自己的 POM**：只声明该服务特有的依赖

### Parent POM 结构

```xml
<!-- duke-parent/pom.xml -->

<!-- 所有服务都继承这些 -->
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>com.duke</groupId>
        <artifactId>duke-framework</artifactId>
    </dependency>

    <!-- Spring Boot Web (所有非纯网关服务都需要) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Nacos 服务发现 (所有服务都需要) -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Swagger UI (所有服务都需要) -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <!-- 通用工具库 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- AOP (所有服务都可能用到) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 数据库 ORM -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<!-- 版本管理（按需引用） -->
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba BOM -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 其他依赖版本定义... -->
    </dependencies>
</dependencyManagement>
```

### 各服务依赖配置

#### duke-auth (认证服务)

```xml
<parent>
    <groupId>com.duke</groupId>
    <artifactId>duke-parent</artifactId>
    <version>1.0.0</version>
</parent>

<dependencies>
    <!-- 必须：共享框架 -->
    <dependency>
        <groupId>com.duke</groupId>
        <artifactId>duke-framework</artifactId>
    </dependency>

    <!-- auth 特有：Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- auth 特有：JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>

    <!-- auth 特有：Hutool -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- auth 特有：Security 测试 -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### duke-transformer (模型可视化服务)

```xml
<parent>
    <groupId>com.duke</groupId>
    <artifactId>duke-parent</artifactId>
    <version>1.0.0</version>
</parent>

<dependencies>
    <!-- 必须：共享框架 -->
    <dependency>
        <groupId>com.duke</groupId>
        <artifactId>duke-framework</artifactId>
    </dependency>

    <!-- transformer 特有：中文分词 -->
    <dependency>
        <groupId>com.huaban</groupId>
        <artifactId>jieba-analysis</artifactId>
    </dependency>
</dependencies>
```

#### duke-gateway (API 网关)

```xml
<parent>
    <groupId>com.duke</groupId>
    <artifactId>duke-parent</artifactId>
    <version>1.0.0</version>
</parent>

<dependencies>
    <!-- 必须：共享框架 -->
    <dependency>
        <groupId>com.duke</groupId>
        <artifactId>duke-framework</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- gateway 特有：Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- gateway 特有：负载均衡 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <!-- gateway 特有：Swagger UI (WebFlux 版本) -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    </dependency>

    <!-- gateway 特有：JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
</dependencies>
```

### 新增依赖的步骤

1. **在 Parent POM 的 dependencyManagement 中定义版本**：

```xml
<properties>
    <my-lib.version>1.0.0</my-lib.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-lib</artifactId>
            <version>${my-lib.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

2. **在服务的 POM 中引入（不需要指定版本）**：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-lib</artifactId>
    <!-- 版本自动从 parent 继承 -->
</dependency>
```

3. **构建验证**：

```bash
mvn clean install -DskipTests
```

---

## 配置管理（Nacos Config）

### 设计原则

所有服务的配置**不硬写在代码中**，而是集中管理在 Nacos Config 中：

- **本地 application.yml**：只保留最小化配置（端口、服务名、Nacos 地址）
- **Nacos 中的配置**：所有敏感信息和可变配置（数据库、Redis、JWT 密钥、OAuth 等）
- **支持热更新**：无需重启服务即可更新配置

### Spring Boot 3.x 配置导入

使用 `spring.config.import` 从 Nacos 导入配置：

```yaml
# application.yml
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

### Nacos 配置文件结构

**Group**: `DEFAULT_GROUP`（默认分组，不需要修改）

| 配置文件 | 用途 | 内容示例 |
|---------|------|--------|
| `duke-common.yml` | 所有服务共享配置 | Redis、JWT 密钥、内部通信密钥 |
| `duke-auth.yml` | 认证服务专属 | 数据库连接、OAuth 配置 |
| `duke-gateway.yml` | 网关专属 | 路由规则、Swagger 聚合配置 |
| `duke-transformer.yml` | Transformer 服务专属 | 日志级别等 |
| `duke-knowledge-qa.yml` | 知识问答服务专属 | 数据库、向量库、LLM 配置 |

### 热更新

使用 `@ConfigurationProperties` + `@Component` 替代 `@Value` + `@RefreshScope`（更清晰）：

```java
@Component
@ConfigurationProperties(prefix = "app.auth")
@Data
public class AuthProperties {
    private String jwtSecret;
    private Integer expiration;
    // 自动支持热更新，无需额外注解
}
```

在 Nacos 中修改值后，应用会自动重新加载，无需重启。

---

## 网关配置

### 路由配置

网关负责转发前端请求到对应的后端服务。所有配置在 `duke-gateway/src/main/resources/application.yml`。

#### 业务 API 路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 认证服务
        - id: duke-auth
          uri: lb://duke-auth
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/auth/(?<segment>.*), /auth/${segment}

        # Transformer 服务
        - id: duke-transformer
          uri: lb://duke-transformer
          predicates:
            - Path=/api/transformer/**
          filters:
            - RewritePath=/api/transformer/(?<segment>.*), /transformer/${segment}

        # 知识问答服务
        - id: duke-knowledge-qa
          uri: lb://duke-knowledge-qa
          predicates:
            - Path=/api/knowledge-qa/**
          filters:
            - RewritePath=/api/knowledge-qa/(?<segment>.*), /knowledge-qa/${segment}
```

#### Swagger 文档聚合路由

```yaml
        # Swagger 文档聚合路由（通过网关代理下游服务的 API 文档）
        - id: swagger-auth
          uri: lb://duke-auth
          predicates:
            - Path=/swagger-auth/**
          filters:
            - RewritePath=/swagger-auth/(?<segment>.*), /auth/${segment}

        - id: swagger-transformer
          uri: lb://duke-transformer
          predicates:
            - Path=/swagger-transformer/**
          filters:
            - RewritePath=/swagger-transformer/(?<segment>.*), /transformer/${segment}

        - id: swagger-knowledge-qa
          uri: lb://duke-knowledge-qa
          predicates:
            - Path=/swagger-knowledge-qa/**
          filters:
            - RewritePath=/swagger-knowledge-qa/(?<segment>.*), /knowledge-qa/${segment}
```

### 全局过滤器（认证与权限）

网关有三个全局过滤器，按顺序执行：

#### 1. JwtAuthFilter (ORDER = -100)

验证 JWT token，拦截未认证请求。

```java
// duke-gateway/src/main/java/com/duke/gateway/filter/JwtAuthFilter.java

private static final List<String> WHITE_LIST = List.of(
    // 业务接口
    "/api/auth/login",
    "/api/auth/logout",
    "/api/auth/weixin/url",
    "/api/auth/sms/send",
    "/api/auth/sms/login",
    "/api/auth/github/url",
    "/api/auth/github/callback",
    "/api/auth/captcha",
    "/api/transformer/**",
    // 网关 Swagger UI 和 API 文档
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    // 聚合下游服务 API 文档的路由
    "/swagger-auth/**",
    "/swagger-transformer/**"
);
```

需要在白名单中添加无需认证的路径。

#### 2. ApiPermissionFilter (ORDER = -90)

检查用户是否有权限访问该 API（基于 API 权限标识）。

```java
private static final List<String> WHITE_LIST = List.of(
    // 业务接口
    "/api/auth/login",
    "/api/auth/logout",
    // ... 其他无权限限制的路径
    // 网关 Swagger UI 和 API 文档
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-auth/**",
    "/swagger-transformer/**"
);
```

#### 3. RequestLoggingFilter (ORDER = -1)

记录所有请求日志。

### 网关启动配置

本地 `application.yml` 保留最小化配置，路由和 Swagger 聚合配置已移入 Nacos：

```yaml
# application.yml - 本地最小配置
server:
  port: 8080

spring:
  application:
    name: duke-gateway
  main:
    web-application-type: reactive  # 关键：使用 WebFlux 而不是 Servlet

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

  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration

  config:
    import:
      - nacos:duke-common.yml?refreshEnabled=true
      - nacos:duke-gateway.yml?refreshEnabled=true
```

**Nacos 中的 `duke-gateway.yml` 包含：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... 所有路由配置 ...

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tag-sorter: alpha
    operations-sorter: alpha
    enabled: true
    urls:
      - name: 认证服务
        url: /swagger-auth/v3/api-docs
      - name: Transformer 服务
        url: /swagger-transformer/v3/api-docs
      - name: 知识问答服务
        url: /swagger-knowledge-qa/v3/api-docs
  api-docs:
    path: /v3/api-docs

gateway:
  route-prefix-map: "{'duke-auth': '/api/auth', 'duke-transformer': '/api/transformer', 'duke-knowledge-qa': '/api/knowledge-qa'}"
```

---

## 服务间通信（OpenFeign）

### 设计原则

微服务间的调用**不走网关、不传 JWT Token**，而是使用专用的内部通信机制：

- **OpenFeign 客户端**：在调用方定义 `@FeignClient` 接口，自动通过 Nacos 服务发现调用
- **内部密钥认证**：所有内部请求自动携带 `X-Gateway-Secret` 请求头
- **内部接口白名单**：被调用方在 `/internal/**` 路径下提供内部接口，Spring Security 白名单放行
- **性能优势**：直接服务间通信，无网关开销

### 实现步骤

#### 1. 调用方服务（如 duke-knowledge-qa）

**pom.xml 添加 OpenFeign 依赖：**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**主类启用 Feign：**
```java
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class KnowledgeQaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeQaApplication.class, args);
    }
}
```

**定义 Feign 客户端：**
```java
// feign/AuthFeignClient.java
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

**在业务逻辑中使用：**
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final AuthFeignClient authFeignClient;

    public void doSomething(Long userId) {
        // 直接调用 Feign 客户端，自动添加内部密钥
        Result<Object> user = authFeignClient.getUserById(userId);
        // ...
    }
}
```

#### 2. 被调用方服务（如 duke-auth）

**在 SecurityConfig 中白名单 `/internal/**`：**
```java
private static final String[] WHITE_LIST = {
    // ... 其他路由 ...
    "/internal/**"  // 允许所有内部接口无需认证
};
```

**提供内部接口：**
```java
// controller/InternalUserController.java
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final IUserService userService;

    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        return Result.success(userService.getById(userId));
    }
}
```

### 内部密钥配置

**InternalFeignConfig** 在 duke-framework 中定义：

```java
// framework/feign/InternalFeignConfig.java
@Configuration
public class InternalFeignConfig {
    @Value("${gateway.internal-secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalSecretInterceptor() {
        return template -> template.header("X-Gateway-Secret", internalSecret);
    }
}
```

内部密钥定义在 Nacos `duke-common.yml` 中：
```yaml
gateway:
  internal-secret: gateway-internal-secret-change-in-production
```

---

## API Swagger 管理

### 统一 Swagger UI

所有微服务的 API 文档都通过网关统一管理，访问地址：

```
http://localhost:8080/swagger-ui.html
```

右上角的下拉菜单可以切换查看不同服务的 API：
- 认证服务
- Transformer 服务
- 知识问答服务

### 各服务的 Swagger 配置

每个微服务都有自己的 Swagger 配置，用于独立开发和测试。

#### 认证服务配置

```yaml
# duke-auth/src/main/resources/application.yml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
  api-docs:
    path: /v3/api-docs
```

访问地址：`http://localhost:8081/auth/swagger-ui.html`

#### Transformer 服务配置

```yaml
# duke-transformer/src/main/resources/application.yml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

访问地址：`http://localhost:8082/transformer/swagger-ui.html`

### API 注解规范

为了让 Swagger 正确显示 API 信息，遵循以下规范：

```java
@RestController
@RequestMapping("/api")
@Tag(name = "用户管理", description = "用户相关接口")  // 分组名称
public class UserController {

    @GetMapping("/{id}")
    @Operation(
        summary = "获取用户信息",  // API 标题
        description = "根据用户 ID 获取用户详细信息"  // API 描述
    )
    @PreAuthorize("hasAuthority('user:query')")  // 权限标识
    public Result<UserDto> getUserInfo(
        @PathVariable
        @Parameter(description = "用户 ID")
        Long id
    ) {
        // ...
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasAuthority('user:create')")
    public Result<Void> createUser(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户信息")
        CreateUserRequest req
    ) {
        // ...
    }
}
```

### Swagger 故障排除

#### 问题 1：401 Unauthorized

**症状**：Swagger UI 打开了，但显示 "Failed to load API definition. Fetch error response status is 401"

**原因**：网关的认证过滤器拦截了 swagger 请求

**解决**：确保 JwtAuthFilter 和 ApiPermissionFilter 的白名单中包含：
```
/swagger-ui.html
/swagger-ui/**
/v3/api-docs/**
/swagger-auth/**
/swagger-transformer/**
```

#### 问题 2：404 Not Found

**症状**：Swagger UI 报 404 错误

**原因**：路由转写不正确，导致请求路径错误

**解决**：检查 RewritePath 过滤器是否正确转写了路径

```yaml
filters:
  # 错误：会变成 /auth/auth/v3/api-docs
  - RewritePath=/swagger-auth/(?<segment>.*), /auth/${segment}
  
  # 正确：context-path 由服务处理
  - RewritePath=/swagger-auth/(?<segment>.*), /auth/${segment}
```

---

## 权限和认证

### JWT 认证流程

```
前端用户登录
  ↓
POST /api/auth/login (用户名/密码)
  ↓
duke-auth 验证并签发 JWT token
  ↓
前端保存 token 到 localStorage/Cookie
  ↓
后续请求在 Authorization 请求头中包含 token
  Authorization: Bearer <token>
  ↓
网关 JwtAuthFilter 验证 token 签名和过期时间
  ↓
验证通过：提取用户信息，继续转发请求
验证失败：返回 401 Unauthorized
  ↓
服务处理请求，检查 @PreAuthorize 权限
  ↓
权限足够：执行业务逻辑
权限不足：返回 403 Forbidden
```

### 权限标识使用

在控制器方法上使用 `@PreAuthorize` 注解声明权限：

```java
// 需要 user:query 权限
@PreAuthorize("hasAuthority('user:query')")
@GetMapping("/{id}")
public Result<UserDto> getUserInfo(@PathVariable Long id) {
    // ...
}

// 需要 user:create 或 user:edit 权限
@PreAuthorize("hasAnyAuthority('user:create', 'user:edit')")
@PostMapping
public Result<Void> createUser(@RequestBody CreateUserRequest req) {
    // ...
}

// 管理员权限
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    // ...
}
```

### Token 黑名单

当用户登出时，token 加入 Redis 黑名单以禁止其继续使用。

```java
// duke-auth 登出逻辑
@PostMapping("/logout")
@Operation(summary = "登出")
public Result<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        // 将 token 加入黑名单（Redis），TTL = token 过期时间
        tokenBlacklist.add(token);
    }
    return Result.success();
}
```

### API 中央管理库

所有微服务的 API 都在 duke-auth 的 `sys_api` 表中进行中央管理。

#### 数据库结构

```sql
CREATE TABLE sys_api (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id VARCHAR(64),           -- 应用 ID (duke-auth, duke-transformer, ...)
    controller_class VARCHAR(255), -- 控制器类名
    controller_name VARCHAR(255),  -- 控制器显示名称
    api_name VARCHAR(255),         -- API 名称
    api_path VARCHAR(255),         -- API 路径
    api_method VARCHAR(20),        -- HTTP 方法 (GET, POST, ...)
    api_desc TEXT,                 -- API 描述
    permission VARCHAR(255),       -- 权限标识
    status TINYINT,                -- 状态 (0=禁用, 1=启用)
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### API 扫描工作流程

**启动时自动扫描 duke-auth：**

```
1. duke-auth 启动
2. ApplicationReadyEvent 触发
3. ApiServiceImpl.onApplicationReady() 执行
4. ApiScanner 扫描所有 @RestController
5. 提取 API 元数据（路径、方法、权限、描述等）
6. 保存到 sys_api 表
7. 发布 ApiSyncCompletedEvent
8. 网关监听事件，刷新权限规则缓存
```

**前端手动扫描其他应用：**

```
前端打开「API 管理」页面
  ↓
选择要扫描的应用 (如 duke-transformer)
  ↓
点击「扫描」按钮
  ↓
POST /api/auth/api/sync/{appId}
  ↓
ApiServiceImpl.syncAppApis(appId) 执行
  ↓
删除该应用的旧 API 记录
  ↓
通过网关或服务发现获取应用的接口信息 (待完整实现)
  ↓
保存新接口到 sys_api 表
  ↓
发布事件，网关刷新权限规则
  ↓
前端显示更新后的 API 列表
```

#### API 查询接口

```bash
# 查询 API 列表（分页）
GET /api/auth/api/page?pageNum=1&pageSize=10&appId=duke-auth

# 按应用分组查询
GET /api/auth/api/grouped

# 扫描 duke-auth
POST /api/auth/api/sync

# 扫描指定应用
POST /api/auth/api/sync/duke-transformer
```

---

## 新建微服务

### 完整步骤

#### 1. 创建项目目录

```bash
cd backend
mkdir -p duke-knowledge-qa/src/main/{java/com/duke/knowledge_qa,resources}
mkdir -p duke-knowledge-qa/src/test/java
```

#### 2. 创建 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承 parent -->
    <parent>
        <groupId>com.duke</groupId>
        <artifactId>duke-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../duke-parent/pom.xml</relativePath>
    </parent>

    <artifactId>duke-knowledge-qa</artifactId>
    <name>duke-knowledge-qa</name>
    <description>知识问答服务</description>

    <dependencies>
        <!-- 必须：共享框架 -->
        <dependency>
            <groupId>com.duke</groupId>
            <artifactId>duke-framework</artifactId>
        </dependency>

        <!-- 服务特有的依赖... -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 3. 创建应用启动类

```java
// src/main/java/com/duke/knowledge_qa/KnowledgeQaApplication.java
package com.duke.knowledge_qa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KnowledgeQaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeQaApplication.class, args);
    }
}
```

#### 4. 创建应用配置

**本地 application.yml（最小化）：**
```yaml
# src/main/resources/application.yml
server:
  port: 8083
  servlet:
    context-path: /knowledge-qa

spring:
  application:
    name: duke-knowledge-qa

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

  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration

  config:
    import:
      - nacos:duke-common.yml?refreshEnabled=true
      - nacos:duke-knowledge-qa.yml?refreshEnabled=true
```

**Nacos 中的 `duke-knowledge-qa.yml`：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/duke_knowledge_qa?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
  mybatis-plus:
    mapper-locations: classpath:mapper/*.xml
    type-aliases-package: com.duke.knowledgeqa.entity
    global-config:
      db-config:
        logic-delete-field: deleted

qdrant:
  host: localhost
  port: 6333
  api-key: admin_123456
  collection: knowledgeqa

llm:
  model: qwen-max
  endpoint: https://api.aliyun.com/v1/chat/completions

embedding:
  model: qwen3-embedding-8b
  endpoint: https://api.aliyun.com/v1/embeddings

logging:
  level:
    com.duke.knowledgeqa: debug
```

#### 5. 创建控制器

```java
// src/main/java/com/duke/knowledge_qa/controller/QaController.java
package com.duke.knowledge_qa.controller;

import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qa")
@Tag(name = "知识问答", description = "知识问答相关接口")
public class QaController {

    @GetMapping("/{id}")
    @Operation(summary = "获取问答信息")
    public Result<Object> getQa(@PathVariable Long id) {
        return Result.success();
    }
}
```

#### 6. 在网关配置中添加路由

**Nacos 中的 `duke-gateway.yml` 添加：**

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... 其他路由 ...

        # 知识问答服务业务路由
        - id: duke-knowledge-qa
          uri: lb://duke-knowledge-qa
          predicates:
            - Path=/api/knowledge-qa/**
          filters:
            - RewritePath=/api/knowledge-qa/(?<segment>.*), /knowledge-qa/${segment}

        # Swagger 文档聚合路由
        - id: swagger-knowledge-qa
          uri: lb://duke-knowledge-qa
          predicates:
            - Path=/swagger-knowledge-qa/**
          filters:
            - RewritePath=/swagger-knowledge-qa/(?<segment>.*), /knowledge-qa/${segment}

gateway:
  route-prefix-map: "{'duke-auth': '/api/auth', 'duke-transformer': '/api/transformer', 'duke-knowledge-qa': '/api/knowledge-qa'}"

springdoc:
  swagger-ui:
    urls:
      # ... 其他 API ...
      - name: 知识问答服务
        url: /swagger-knowledge-qa/v3/api-docs
```

#### 7. 更新网关过滤器白名单

```java
// duke-gateway/src/main/java/com/duke/gateway/filter/JwtAuthFilter.java
private static final List<String> WHITE_LIST = List.of(
    // ... 其他路由 ...
    "/api/knowledge-qa/**",      // 新增：知识问答服务（如果无需认证）
    "/swagger-knowledge-qa/**"   // 新增：Swagger 文档
);

// duke-gateway/src/main/java/com/duke/gateway/filter/ApiPermissionFilter.java
private static final List<String> WHITE_LIST = List.of(
    // ... 其他路由 ...
    "/api/knowledge-qa/**",      // 新增：知识问答服务
    "/swagger-knowledge-qa/**"   // 新增：Swagger 文档
);
```

#### 8. 构建和测试

```bash
# 在新服务目录构建
cd duke-knowledge-qa
mvn clean install

# 启动服务
mvn spring-boot:run

# 验证 Swagger 文档
# 统一网关：http://localhost:8080/swagger-ui.html (选择"知识问答服务")
# 独立访问：http://localhost:8083/knowledge-qa/swagger-ui.html

# 测试 API
curl http://localhost:8080/api/knowledge-qa/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 开发工作流

### 本地开发环境搭建

#### 1. 启动基础设施

```bash
# Nacos（服务发现）
docker run -d \
  --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=true \
  -e NACOS_AUTH_DEFAULT_LOGIN_ENABLED=true \
  nacos/nacos-server:v2.2.1

# MySQL（数据库）
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  mysql:8.0

# Redis（缓存、Token 黑名单）
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7
```

#### 2. 初始化 Nacos 配置

在 Nacos 控制台（http://127.0.0.1:8848/nacos，username: nacos, password: nacos）中创建配置文件。

**Group**: `DEFAULT_GROUP`，以下 5 个配置文件：

**duke-common.yml** - 所有服务共享：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 3000ms
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

jwt:
  secret: duke-auth-jwt-secret-key-please-change-in-production-environment-256bit
  expiration: 86400

gateway:
  internal-secret: gateway-internal-secret-change-in-production
```

**duke-auth.yml** - 认证服务：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/duke_auth?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  mybatis-plus:
    mapper-locations: classpath:mapper/*.xml
    type-aliases-package: com.duke.auth.entity
    global-config:
      db-config:
        logic-delete-field: deleted
        logic-delete-value: 1
        logic-not-delete-value: 0

cors:
  allowed-origins: http://localhost:3000

weixin:
  app-id: wx8cf74f6236b61560
  app-secret: ${WEIXIN_APP_SECRET:}
  redirect-uri: http://localhost:3000/weixin/callback

github:
  client-id: Ov23lijxNYEMpEoYDapN
  client-secret: ${GITHUB_CLIENT_SECRET:}
  redirect-uri: http://localhost:3000/auth/github/callback

sms:
  expire-seconds: 300
  rate-limit-seconds: 60
```

**duke-gateway.yml** - 网关（参见"网关启动配置"部分）

**duke-transformer.yml** - Transformer 服务：
```yaml
logging:
  level:
    com.duke.transformer: debug
```

**duke-knowledge-qa.yml** - 知识问答服务（参见"新建微服务"部分）

#### 3. 初始化数据库

```bash
# 使用 MySQL 客户端连接
mysql -u root -p123456 -h 127.0.0.1

# 创建数据库和表
CREATE DATABASE duke_auth CHARACTER SET utf8mb4;
USE duke_auth;

-- 执行 SQL 脚本（duke-auth 项目中）
source src/main/resources/sql/schema.sql
source src/main/resources/sql/init-data.sql
```

#### 4. 启动后端服务

**方式 1：IDE 中直接运行**

- 打开 IntelliJ IDEA
- 右键点击 `*Application.java` → Run
- 或 Shift+F10

**方式 2：Maven 命令**

```bash
# 在 duke-auth 目录
mvn spring-boot:run

# 在 duke-transformer 目录
mvn spring-boot:run

# 在 duke-gateway 目录
mvn spring-boot:run
```

#### 5. 启动前端开发服务

```bash
# 在 duke-auth-web 目录
npm install
npm run dev

# 在 duke-transformer-web 目录
npm install
npm run dev

# 默认端口：5173
# 访问：http://localhost:5173
```

### 开发检查清单

开始功能开发或修复前：

- [ ] 基础设施：Nacos、MySQL、Redis 已启动
- [ ] Nacos 配置：5 个配置文件已创建（duke-common.yml, duke-auth.yml, duke-gateway.yml, duke-transformer.yml, duke-knowledge-qa.yml）
- [ ] 数据库：已初始化（执行 schema.sql 和 init-data.sql）
- [ ] 后端依赖：`cd backend && mvn clean install` 成功
- [ ] 前端依赖：各前端目录下 `npm install` 成功
- [ ] 服务启动：gateway、auth、transformer 都能正常启动，无 "Could not resolve placeholder" 错误
- [ ] 网关可访问：`http://localhost:8080`
- [ ] Swagger 可访问：`http://localhost:8080/swagger-ui.html`（三个服务都在下拉菜单中）
- [ ] 前端可访问：`http://localhost:5173`

### 提交代码前

- [ ] 代码格式检查通过
- [ ] 本地测试成功
- [ ] 没有遗留的 `console.log`、`System.out.println`
- [ ] 提交信息清晰有意义
- [ ] 没有提交敏感信息（密码、密钥等）

---

## 常见问题

### 1. 启动 TransformerApplication、GatewayApplication 失败

**错误**：DataSourceAutoConfiguration 尝试配置数据库但没有找到驱动

**解决**：在 `application.yml` 的 `spring.autoconfigure.exclude` 中添加：

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
```

### 2. Swagger 返回 401

**错误**：Failed to load API definition. Fetch error response status is 401

**解决**：确保 `JwtAuthFilter` 和 `ApiPermissionFilter` 的白名单包含 swagger 路径：

```java
"/swagger-ui.html",
"/swagger-ui/**",
"/v3/api-docs/**",
"/swagger-auth/**",
"/swagger-transformer/**"
```

### 3. 网关无法找到下游服务

**错误**：`503 Service Unavailable`

**检查清单**：
- [ ] Nacos 是否运行：`curl http://127.0.0.1:8848/nacos`
- [ ] 下游服务是否启动：查看日志中的 "registered service" 消息
- [ ] Nacos 中是否有服务：访问 http://127.0.0.1:8848/nacos (username: nacos, password: nacos)

### 4. JWT Token 验证失败

**错误**：401 Unauthorized - token 无效或已过期

**解决**：
- 检查 token 是否已过期：JWT 在 Authorization 请求头中
- 检查 JWT_SECRET 环境变量是否正确设置
- 检查 token 黑名单：Redis 中是否存在该 token

### 5. 跨域问题

**错误**：CORS policy: No 'Access-Control-Allow-Origin' header

**解决**：在 `duke-auth/config/SecurityConfig.java` 中配置 CORS：

```java
http.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return config;
}));
```

### 6. Nacos 连接失败

**错误**：Connection refused

**解决**：
```bash
# 检查 Nacos 是否运行
curl http://127.0.0.1:8848/nacos/v1/ns/service/list

# 如果失败，启动 Nacos
docker run -d --name nacos -p 8848:8848 -p 9848:9848 \
  -e MODE=standalone nacos/nacos-server:v2.2.1
```

### 7. 前端 API 请求返回 401

**解决**：
- 确保 token 已保存到 localStorage
- 检查 axios 拦截器是否正确附加了 token
- 检查 token 格式：`Authorization: Bearer <token>`

### 8. 数据库连接失败

**错误**：Communications link failure

**解决**：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/duke_auth?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

## 总结

Duke 平台是一个完整的微服务架构示例，涵盖：

✅ **统一依赖管理** - Parent POM 管理版本，避免重复  
✅ **配置中心化** - Nacos Config 统一管理所有服务配置，支持热更新  
✅ **网关统一入口** - 所有外部请求通过网关，统一认证和权限检查  
✅ **服务间通信规范化** - OpenFeign + 内部密钥，安全高效的微服务调用  
✅ **中央 API 管理** - 所有服务的 API 都在 duke-auth 中央管理  
✅ **Swagger 聚合** - 网关层统一展示所有服务的 API 文档  
✅ **灵活的权限系统** - 支持权限标识和角色的细粒度控制  
✅ **开箱即用** - 新服务只需遵循约定，零改动集成  

按照本文档，可以快速开发新功能、新服务、解决常见问题！

---

**版本历史**：
- 2026.4.29 - 初始版本
- 2026.4.30 - 新增 Nacos Config、OpenFeign、duke-knowledge-qa 网关集成

**最后更新**：2026 年 4 月 30 日  
**维护者**：Duke 平台开发团队
