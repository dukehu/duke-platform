# duke-notification 服务生成指南

欢迎使用 Duke 平台微服务脚手架！此服务已通过 Maven Archetype 生成，包含规范的目录结构和常见配置。


| 参数 | 值 |
|------|-----|
| **serviceName** | duke-notification |
| **servicePort** | 8085 |
| **classPrefix** | Notification |
| **serviceDesc** | 通知服务 |
| **useDatabase** | n |
| **useRedis** | n |
| **useFeign** | y |
| **useSecurity** | n |


根据选择的特性，以下文件可安全删除：

- ✂️ `src/main/java/entity/NotificationEntity.java` — 仅当 useDatabase=n
- ✂️ `src/main/java/mapper/NotificationMapper.java` — 仅当 useDatabase=n
- ✂️ `src/main/resources/mapper/NotificationMapper.xml` — 仅当 useDatabase=n
- ✂️ `src/main/java/config/MybatisPlusConfig.java` — 仅当 useDatabase=n
- ✂️ `src/main/java/config/SecurityConfig.java` — 仅当 useSecurity=n


将以下配置片段添加到 Nacos 中的 `duke-gateway.yml` 配置文件：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # duke-notification 路由
        - id: notification
          uri: lb://duke-notification
          predicates:
            - Path=/api/notification/**
          filters:
            - StripPrefix=2
```

**说明**：
- `id`: 路由唯一标识
- `uri`: 负载均衡到 Nacos 注册的服务名
- `predicates`: 请求路径匹配规则（`/api/notification/**`）
- `filters`: 去掉前两级路径前缀（`/api/notification`），使请求到达服务的 `notification` context-path

**示例调用路径**：
- 前端请求：`http://localhost:8080/api/notification/list`
- 网关转发到：`lb://duke-notificationnotification/list`（根据 Nacos 服务发现）
- 服务接收到：`/notification/list`（context-path 下的相对路径）



```bash
cd duke-notification
mvn clean compile
```


- ℹ️ 当前未启用数据库（useDatabase=n），若需后续启用，请参考 `duke-auth` 服务的数据库配置


- ℹ️ 当前未启用 Redis（useRedis=n），若需后续启用，请参考 `duke-transformer` 服务的 Redis 使用


- ✅ `src/main/java/feign/AuthFeignClient.java` 已配置调用 `duke-auth` 的示例
- ✅ 若需调用其他服务，在 `feign/` 目录下创建新的 `@FeignClient` 接口
- ✅ 确保在 `src/main/java/NotificationApplication.java` 中已配置 `@EnableFeignClients`
- ℹ️ 所有内部 Feign 调用自动注入 `X-Gateway-Secret` 请求头（由 `InternalFeignConfig` 配置）


- ℹ️ 当前未启用 Spring Security（useSecurity=n），若需认证保护，请参考 `duke-auth` 服务的 SecurityConfig


- ✅ 修改 `src/main/java/config/properties/AppProperties.java`，添加服务特定的配置属性
- ✅ 在 Nacos 的 `duke-notification.yml` 中以 `notification:` 前缀配置这些属性
- ✅ 在 Service 或 Controller 中注入 `AppProperties` 使用配置值


创建或更新以下 Nacos 配置文件（管理中心：`http://127.0.0.1:8848/nacos`）：

**duke-notification.yml**（新建）

基础配置：
```yaml
notification:
  example: example-value
```




- ✅ 将上述 **网关路由配置** 片段添加到 Nacos 的 `duke-gateway.yml`


```bash
# 启动服务
mvn spring-boot:run

# 或编译后运行
mvn clean package -DskipTests
java -jar target/duke-notification.jar

# 验证 Nacos 注册
curl http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=duke-notification

# 查看 Swagger 文档
# 独立服务 Swagger：http://localhost:8085/notification/swagger-ui.html
# 网关聚合 Swagger：http://localhost:8080/swagger-ui.html
```


- **Duke 平台文档**：见项目根目录 `CLAUDE.md`
- **duke-framework**：查看共享框架实现（异常、Result、拦截器等）
- **duke-auth**：参考完整的认证服务实现（数据库、Security、内部接口）
- **duke-transformer**：参考 SSE 流式传输和 Redis 使用
- **Nacos 管理中心**：http://127.0.0.1:8848/nacos (nacos/nacos)


**Q: 如何修改默认的 `createBy` 和 `updateBy` 值？**
A: 修改 `src/main/java/config/MybatisPlusConfig.java` 中的 `MetaObjectHandler`，集成 Spring Security 时改为从 `SecurityContextHolder.getContext().getAuthentication()` 获取当前用户名。

**Q: Swagger 在哪里查看？**
A: 
- 单个服务独立 Swagger：`http://localhost:8085/notification/swagger-ui.html`
- 通过网关聚合 Swagger：`http://localhost:8080/swagger-ui.html`（所有注册服务的 API 都会聚合）

**Q: 如何调用其他服务的接口？**
A: 如果启用了 OpenFeign（useFeign=y），在 `src/main/java/feign/` 目录下定义 `@FeignClient` 接口，自动注入内部密钥请求头。参考 `AuthFeignClient.java` 的示例。

**Q: 可以删除不需要的文件吗？**
A: 可以！查看上方的 **可删除文件清单** 部分，根据选择的特性安全删除占位文件。

---

**生成时间**：${__timestamp}
**脚手架版本**：1.0.0

