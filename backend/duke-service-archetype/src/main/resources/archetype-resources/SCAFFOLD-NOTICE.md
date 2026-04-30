# ${serviceName} 服务生成指南

欢迎使用 Duke 平台微服务脚手架！此服务已通过 Maven Archetype 生成，包含规范的目录结构和常见配置。

## 📋 生成配置

| 参数 | 值 |
|------|-----|
| **serviceName** | ${serviceName} |
| **servicePort** | ${servicePort} |
| **classPrefix** | ${classPrefix} |
| **serviceDesc** | ${serviceDesc} |
| **useDatabase** | ${useDatabase} |
| **useRedis** | ${useRedis} |
| **useFeign** | ${useFeign} |
| **useSecurity** | ${useSecurity} |

## 🗑️ 可删除文件清单

根据选择的特性，以下文件可安全删除：

#if($useDatabase != "y")
- ✂️ `src/main/java/entity/${classPrefix}Entity.java` — 仅当 useDatabase=n
- ✂️ `src/main/java/mapper/${classPrefix}Mapper.java` — 仅当 useDatabase=n
- ✂️ `src/main/resources/mapper/${classPrefix}Mapper.xml` — 仅当 useDatabase=n
- ✂️ `src/main/java/config/MybatisPlusConfig.java` — 仅当 useDatabase=n
#end
#if($useFeign != "y")
- ✂️ `src/main/java/feign/AuthFeignClient.java` — 仅当 useFeign=n
#end
#if($useSecurity != "y")
- ✂️ `src/main/java/config/SecurityConfig.java` — 仅当 useSecurity=n
#end

## 🌐 跨域配置

⚠️ **CORS 跨域配置已统一在网关处理**，无需在各服务中配置。
- 网关自动处理所有 CORS 预检请求（OPTIONS）
- 允许的源：`http://localhost:5173`, `http://127.0.0.1:5173` 等
- 详见网关配置：`duke-gateway/src/main/java/com/duke/gateway/config/CorsConfig.java`

## 🔀 网关路由配置

将以下配置片段添加到 Nacos 中的 `duke-gateway.yml` 配置文件：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ${serviceName} 路由
        - id: ${classPrefixLower}
          uri: lb://${serviceName}
          predicates:
            - Path=/api/${classPrefixLower}/**
          filters:
            - StripPrefix=2
```

**说明**：
- `id`: 路由唯一标识
- `uri`: 负载均衡到 Nacos 注册的服务名
- `predicates`: 请求路径匹配规则（`/api/${classPrefixLower}/**`）
- `filters`: 去掉前两级路径前缀（`/api/${classPrefixLower}`），使请求到达服务的 `${classPrefixLower}` context-path

**示例调用路径**：
- 前端请求：`http://localhost:8080/api/${classPrefixLower}/list`
- 网关转发到：`lb://${serviceName}${classPrefixLower}/list`（根据 Nacos 服务发现）
- 服务接收到：`/${classPrefixLower}/list`（context-path 下的相对路径）

## ✅ 后续步骤

### 1️⃣ 项目结构验证

```bash
cd ${serviceName}
mvn clean compile
```

### 2️⃣ 数据库配置

#if($useDatabase == "y")
- ✅ 修改 `src/main/java/entity/${classPrefix}Entity.java`，定义实际业务字段
- ✅ 在 MySQL 中创建对应表（可参考 Entity 的 `@TableName`、`@TableField` 注解）
- ✅ 修改 `src/main/java/service/I${classPrefix}Service.java` 和 `impl/${classPrefix}ServiceImpl.java`，实现业务逻辑
- ✅ 如需自定义 SQL，在 `src/main/resources/mapper/${classPrefix}Mapper.xml` 中添加
- ✅ 在 Nacos 的 `${serviceName}.yml` 中配置 MySQL 数据源（URL、username、password）
#else
- ℹ️ 当前未启用数据库（useDatabase=n），若需后续启用，请参考 `duke-auth` 服务的数据库配置
#end

### 3️⃣ Redis 配置

#if($useRedis == "y")
- ✅ 注入 `RedisTemplate` 或 `StringRedisTemplate` 在 Service 中使用缓存
- ✅ 在 Nacos 的 `${serviceName}.yml` 中配置 Redis 连接（host、port、password）
- ✅ 通常在 `src/main/java/service/impl/${classPrefix}ServiceImpl.java` 的 `getById` 等查询方法中添加缓存逻辑
#else
- ℹ️ 当前未启用 Redis（useRedis=n），若需后续启用，请参考 `duke-transformer` 服务的 Redis 使用
#end

### 4️⃣ OpenFeign 配置

#if($useFeign == "y")
- ✅ `src/main/java/feign/AuthFeignClient.java` 已配置调用 `duke-auth` 的示例
- ✅ 若需调用其他服务，在 `feign/` 目录下创建新的 `@FeignClient` 接口
- ✅ 确保在 `src/main/java/${classPrefix}Application.java` 中已配置 `@EnableFeignClients`
- ℹ️ 所有内部 Feign 调用自动注入 `X-Gateway-Secret` 请求头（由 `InternalFeignConfig` 配置）
#else
- ℹ️ 当前未启用 OpenFeign（useFeign=n），若需跨服务调用，请参考 `duke-knowledge-qa` 服务
#end

### 5️⃣ Spring Security & JWT 配置

#if($useSecurity == "y")
- ✅ `src/main/java/config/SecurityConfig.java` 已配置无状态 JWT 认证
- ✅ 修改 `SecurityConfig.WHITE_LIST[]` 以调整公开路径白名单
- ✅ 需在登录端点返回 JWT token 给客户端（参考 `duke-auth` 的实现）
- ✅ 客户端在后续请求的 `Authorization: Bearer {token}` 请求头中包含 token
- ✅ 服务收到的 request 会被网关验证 token，若有效则注入 SecurityContext
- ℹ️ 自定义权限检查可使用 `@PreAuthorize` 注解（已启用 `@EnableMethodSecurity`）
#else
- ℹ️ 当前未启用 Spring Security（useSecurity=n），若需认证保护，请参考 `duke-auth` 服务的 SecurityConfig
#end

### 6️⃣ 自定义配置（AppProperties）

- ✅ 修改 `src/main/java/config/properties/AppProperties.java`，添加服务特定的配置属性
- ✅ 在 Nacos 的 `${serviceName}.yml` 中以 `${classPrefixLower}:` 前缀配置这些属性
- ✅ 在 Service 或 Controller 中注入 `AppProperties` 使用配置值

### 7️⃣ Nacos 配置创建

创建或更新以下 Nacos 配置文件（管理中心：`http://127.0.0.1:8848/nacos`）：

**${serviceName}.yml**（新建）

基础配置：
```yaml
${classPrefixLower}:
  example: example-value
```

#if($useDatabase == "y")
如启用数据库，添加：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/${serviceName}?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true
    username: root
    password: password
```
#end

#if($useRedis == "y")
如启用 Redis，添加：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
```
#end

### 8️⃣ 网关注册

- ✅ 将上述 **网关路由配置** 片段添加到 Nacos 的 `duke-gateway.yml`

### 9️⃣ 启动和验证

```bash
# 启动服务
mvn spring-boot:run

# 或编译后运行
mvn clean package -DskipTests
java -jar target/${serviceName}.jar

# 验证 Nacos 注册
curl http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=${serviceName}

# 查看 Swagger 文档
# 独立服务 Swagger：http://localhost:${servicePort}/${classPrefixLower}/swagger-ui.html
# 网关聚合 Swagger：http://localhost:8080/swagger-ui.html
```

## 📚 参考资源

- **Duke 平台文档**：见项目根目录 `CLAUDE.md`
- **duke-framework**：查看共享框架实现（异常、Result、拦截器等）
- **duke-auth**：参考完整的认证服务实现（数据库、Security、内部接口）
- **duke-transformer**：参考 SSE 流式传输和 Redis 使用
- **Nacos 管理中心**：http://127.0.0.1:8848/nacos (nacos/nacos)

## 🎯 常见问题

**Q: 如何修改默认的 `createBy` 和 `updateBy` 值？**
A: 修改 `src/main/java/config/MybatisPlusConfig.java` 中的 `MetaObjectHandler`，集成 Spring Security 时改为从 `SecurityContextHolder.getContext().getAuthentication()` 获取当前用户名。

**Q: Swagger 在哪里查看？**
A: 
- 单个服务独立 Swagger：`http://localhost:${servicePort}/${classPrefixLower}/swagger-ui.html`
- 通过网关聚合 Swagger：`http://localhost:8080/swagger-ui.html`（所有注册服务的 API 都会聚合）

**Q: 如何调用其他服务的接口？**
A: 如果启用了 OpenFeign（useFeign=y），在 `src/main/java/feign/` 目录下定义 `@FeignClient` 接口，自动注入内部密钥请求头。参考 `AuthFeignClient.java` 的示例。

**Q: 可以删除不需要的文件吗？**
A: 可以！查看上方的 **可删除文件清单** 部分，根据选择的特性安全删除占位文件。

---

**生成时间**：${__timestamp}
**脚手架版本**：1.0.0

