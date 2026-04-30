# 新建微服务 - 快速入门指南

**目标：5分钟快速构建一个新的微服务**

---

## 前置环境

确保以下工具已安装：
- Java 21+
- Maven 3.6+
- Git
- IntelliJ IDEA（可选但推荐）

运行中的基础设施：
```bash
# Nacos 服务发现
docker run -d -p 8848:8848 -p 9848:9848 \
  -e MODE=standalone nacos/nacos-server:v2.2.1

# MySQL 数据库
docker run -d -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0

# Redis 缓存
docker run -d -p 6379:6379 redis:7
```

---

## 5分钟快速创建

假设要创建一个叫 `duke-api` 的新服务（端口 8084）。

### 步骤 1：复制一个现有服务作为模板

```bash
cd backend
cp -r duke-transformer duke-api
cd duke-api
```

### 步骤 2：修改 pom.xml

```xml
<artifactId>duke-api</artifactId>
<name>duke-api</name>
<description>API 服务</description>
```

删除 transformer 特有的依赖（jieba）。

### 步骤 3：修改应用启动类

```bash
# 重命名文件
mv src/main/java/com/duke/transformer/TransformerApplication.java \
   src/main/java/com/duke/api/ApiApplication.java

# 修改包名和类名
# TransformerApplication → ApiApplication
# com.duke.transformer → com.duke.api
```

**ApiApplication.java：**
```java
package com.duke.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

### 步骤 4：修改 application.yml

```yaml
server:
  port: 8084
  servlet:
    context-path: /api

spring:
  application:
    name: duke-api

  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

  # 禁用数据库自动配置（如果不需要数据库）
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

logging:
  level:
    com.duke.api: debug
```

### 步骤 5：创建第一个控制器

```java
// src/main/java/com/duke/api/controller/TestController.java
package com.duke.api.controller;

import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Tag(name = "测试接口", description = "测试相关")
public class TestController {

    @GetMapping("/{id}")
    @Operation(summary = "获取测试数据")
    public Result<String> getTest(@PathVariable String id) {
        return Result.success("Hello " + id);
    }
}
```

### 步骤 6：更新网关配置

编辑 `duke-gateway/src/main/resources/application.yml`：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... 其他路由 ...

        # 新服务路由
        - id: duke-api
          uri: lb://duke-api
          predicates:
            - Path=/api/api/**
          filters:
            - RewritePath=/api/api/(?<segment>.*), /api/${segment}

        # Swagger 文档聚合
        - id: swagger-api
          uri: lb://duke-api
          predicates:
            - Path=/swagger-api/**
          filters:
            - RewritePath=/swagger-api/(?<segment>.*), /api/${segment}
```

在 Swagger UI 配置中添加：

```yaml
springdoc:
  swagger-ui:
    urls:
      # ... 其他 API ...
      - name: API 服务
        url: /swagger-api/v3/api-docs
```

### 步骤 7：更新网关过滤器白名单

**JwtAuthFilter.java** - 添加到白名单：
```java
"/api/api/**",        // 新服务接口（如果无需认证）
"/swagger-api/**"     // Swagger 文档
```

**ApiPermissionFilter.java** - 添加到白名单：
```java
"/swagger-api/**"
```

### 步骤 8：构建和运行

```bash
# 在 duke-api 目录下
mvn clean install

# 启动服务
mvn spring-boot:run

# 或者在 IDE 中直接运行 ApiApplication.java
```

### 步骤 9：验证

```bash
# 测试服务是否启动
curl http://localhost:8084/api/test/hello

# 测试网关转发
curl http://localhost:8080/api/api/test/hello

# 查看 Swagger（独立访问）
http://localhost:8084/api/swagger-ui.html

# 查看 Swagger（通过网关）
http://localhost:8080/swagger-ui.html
# 右上角下拉菜单选择 "API 服务"
```

---

## 权限控制 - 快速集成

### 什么是权限标识？

权限标识是一个字符串，用来控制用户是否能调用某个 API。格式：

```
模块:操作
例如：user:query、user:create、user:delete
```

### 在你的服务中添加权限控制

#### 1. 在控制器方法上添加 @PreAuthorize

```java
package com.duke.api.controller;

import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    // ✅ 无需权限的接口（所有登录用户都能访问）
    @GetMapping("/profile")
    @Operation(summary = "获取个人信息")
    public Result<Object> getProfile() {
        return Result.success();
    }

    // ✅ 需要特定权限的接口
    @GetMapping("/{id}")
    @Operation(summary = "查询用户")
    @PreAuthorize("hasAuthority('user:query')")
    public Result<Object> getUser(@PathVariable Long id) {
        return Result.success();
    }

    // ✅ 需要创建权限
    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasAuthority('user:create')")
    public Result<Void> createUser(@RequestBody Object req) {
        return Result.success();
    }

    // ✅ 需要多个权限中的任意一个
    @PutMapping("/{id}")
    @Operation(summary = "编辑用户")
    @PreAuthorize("hasAnyAuthority('user:edit', 'user:create')")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Object req) {
        return Result.success();
    }

    // ✅ 需要管理员角色
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        return Result.success();
    }
}
```

### 权限注解对照表

```java
// 需要单个权限
@PreAuthorize("hasAuthority('user:query')")

// 需要多个权限中的任意一个
@PreAuthorize("hasAnyAuthority('user:query', 'user:edit')")

// 需要特定角色
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasRole('USER')")

// 需要多个角色中的任意一个
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")

// 组合多个条件
@PreAuthorize("hasAuthority('user:delete') and hasRole('ADMIN')")
```

### 权限是如何工作的？

```
用户登录
  ↓
Duke-Auth 验证用户，签发 JWT Token
  Token 中包含：userId、username、权限列表
  ↓
前端保存 Token
  ↓
后续请求在 Authorization 请求头中包含 Token
  Authorization: Bearer <token>
  ↓
网关 JwtAuthFilter 验证 Token 签名
  提取权限信息，存入请求上下文
  ↓
服务收到请求，@PreAuthorize 检查权限
  用户有权限？继续处理 ✓
  无权限？返回 403 Forbidden ✗
```

### 权限配置在哪里？

权限配置在 **duke-auth** 的数据库中：

```sql
-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(64) UNIQUE,      -- 权限标识（如 user:query）
    permission_name VARCHAR(255),             -- 权限名称（如 "查询用户"）
    created_at TIMESTAMP
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) UNIQUE,            -- 角色代码（如 ADMIN）
    role_name VARCHAR(255),                  -- 角色名称（如 "管理员"）
    created_at TIMESTAMP
);

-- 角色权限关联
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT,
    permission_id BIGINT,
    UNIQUE KEY (role_id, permission_id)
);

-- 用户角色关联
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    role_id BIGINT,
    UNIQUE KEY (user_id, role_id)
);
```

### 如何添加新的权限？

**方案 1：通过数据库手动添加**

```sql
-- 插入权限
INSERT INTO sys_permission (permission_code, permission_name) 
VALUES ('user:query', '查询用户');

INSERT INTO sys_permission (permission_code, permission_name) 
VALUES ('user:create', '创建用户');

-- 给角色分配权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code = 'user:query';
```

**方案 2：通过 Duke-Auth 的管理后台**（推荐）

1. 打开 duke-auth-web
2. 进入「系统管理 → 权限管理」
3. 点击「新增权限」
4. 输入权限代码（如 `user:query`）和权限名称
5. 点击「保存」

### 测试权限

```bash
# 1. 用有权限的账号登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' | jq '.data.token'

# 2. 使用 token 访问受保护的接口
TOKEN="your_token_here"

# ✅ 有权限：成功
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/1

# ❌ 无权限：403 Forbidden
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/delete/1
```

### 常见权限命名规范

```
# CRUD 操作
模块:query     -- 查询/列表
模块:create    -- 创建
模块:edit      -- 编辑
模块:delete    -- 删除

# 例子
user:query     -- 查询用户
user:create    -- 创建用户
user:edit      -- 编辑用户
user:delete    -- 删除用户

api:sync       -- 扫描 API
api:edit       -- 编辑 API 权限
system:config  -- 系统配置

# 建议：service:action 格式
post:query     -- 查询帖子
post:create    -- 发布帖子
post:delete    -- 删除帖子
comment:query  -- 查询评论
comment:create -- 发布评论
```

---

## 完整配置速查表

| 项目 | 说明 | 示例 |
|------|------|------|
| 端口 | 避免重复 | 8084, 8085, ... |
| 上下文路径 | 对应服务名 | /api |
| 应用名称 | Nacos 注册名 | duke-api |
| 网关路由前缀 | 统一 /api/ | /api/api/** → /api/** |
| POM artifactId | 与目录一致 | duke-api |

---

## 常见错误 & 快速修复

### ❌ 服务在 Nacos 中找不到

```
Error: Connection refused
```

**解决**：
```bash
# 确保 Nacos 运行
docker ps | grep nacos

# 检查 application.yml 中的 Nacos 地址
server-addr: 127.0.0.1:8848
```

### ❌ 启动时数据库错误

```
DataSourceAutoConfiguration: Failed to determine a suitable driver class
```

**解决**：在 `application.yml` 中添加：
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
```

### ❌ 网关返回 404

```
404 Not Found
```

**检查**：
1. 网关中是否添加了路由？
2. 路由的 `RewritePath` 是否正确？
3. 下游服务的 context-path 是否匹配？

例如：
```yaml
predicates:
  - Path=/api/api/**           # ← 网关路径
filters:
  - RewritePath=/api/api/(?<segment>.*), /api/${segment}
  # 变成 /api/test → 转发到 http://duke-api:8084/api/test ✓
```

### ❌ Swagger 返回 401

```
Failed to load API definition. Fetch error response status is 401
```

**解决**：确保白名单中包含：
```java
"/api/api/**",      // 新服务
"/swagger-api/**"   // Swagger 文档
```

### ❌ POM 构建失败

```
Could not transfer artifact...
```

**解决**：
```bash
# 清除 Maven 缓存
rm -rf ~/.m2/repository

# 重新构建
mvn clean install -U
```

---

## 模板项目快速参考

### 最小化 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.duke</groupId>
        <artifactId>duke-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../duke-parent/pom.xml</relativePath>
    </parent>

    <artifactId>duke-api</artifactId>
    <name>duke-api</name>
    <description>新服务</description>

    <dependencies>
        <!-- 必须：共享框架 -->
        <dependency>
            <groupId>com.duke</groupId>
            <artifactId>duke-framework</artifactId>
        </dependency>

        <!-- 服务特有依赖（如果有） -->
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

### 最小化启动类

```java
package com.duke.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

### 最小化控制器

```java
package com.duke.api.controller;

import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
@Tag(name = "示例", description = "示例接口")
public class DemoController {

    @GetMapping("/{id}")
    @Operation(summary = "示例接口")
    public Result<String> getDemo(@PathVariable String id) {
        return Result.success("Hello " + id);
    }
}
```

---

## 清单

新服务创建完成前，确保：

- [ ] 复制了现有服务目录
- [ ] 修改了 `pom.xml` 的 artifactId 和 name
- [ ] 修改了启动类文件名和包名
- [ ] 修改了 `application.yml` 的端口和应用名
- [ ] 创建了至少一个控制器
- [ ] 在网关 `application.yml` 中添加了路由
- [ ] 在网关过滤器中添加了白名单
- [ ] 在网关 Swagger 配置中添加了 URL
- [ ] `mvn clean install` 构建成功
- [ ] 服务能正常启动
- [ ] 网关能正常转发请求
- [ ] Swagger 文档可访问

---

## 总结

| 步骤 | 耗时 | 工作 |
|------|------|------|
| 1. 复制模板 | 30秒 | 复制目录 |
| 2-3. 修改配置 | 2分钟 | pom.xml、启动类、application.yml |
| 4. 创建控制器 | 1分钟 | 写一个简单的 @RestController |
| 5. 配置网关 | 1分钟 | 添加路由和白名单 |
| 6. 构建运行 | 30秒 | `mvn clean install && mvn spring-boot:run` |
| 7. 验证 | 1分钟 | 测试 curl 和 Swagger |

**总计：约 6 分钟**

新手开发者现在可以快速创建一个功能完整的微服务了！🚀

---

**需要帮助？**

遇到问题查看本文档的"常见错误"部分，或者查看 `DEVELOPER_GUIDE.md` 了解更多细节。
