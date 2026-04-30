# duke-order 服务生成指南

欢迎使用 Duke 平台微服务脚手架！此服务已通过 Maven Archetype 生成，包含规范的目录结构和常见配置。


| 参数 | 值 |
|------|-----|
| **serviceName** | duke-order |
| **servicePort** | 8084 |
| **classPrefix** | Order |
| **serviceDesc** | 订单服务 |
| **useDatabase** | y |
| **useRedis** | y |
| **useFeign** | y |
| **useSecurity** | y |


根据选择的特性，以下文件可安全删除：



将以下配置片段添加到 Nacos 中的 `duke-gateway.yml` 配置文件：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # duke-order 路由
        - id: order
          uri: lb://duke-order
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=2
```

**说明**：
- `id`: 路由唯一标识
- `uri`: 负载均衡到 Nacos 注册的服务名
- `predicates`: 请求路径匹配规则（`/api/order/**`）
- `filters`: 去掉前两级路径前缀（`/api/order`），使请求到达服务的 `order` context-path

**示例调用路径**：
- 前端请求：`http://localhost:8080/api/order/list`
- 网关转发到：`lb://duke-orderorder/list`（根据 Nacos 服务发现）
- 服务接收到：`/order/list`（context-path 下的相对路径）



```bash
cd duke-order
mvn clean compile
```


- ✅ 修改 `src/main/java/entity/OrderEntity.java`，定义实际业务字段
- ✅ 在 MySQL 中创建对应表（可参考 Entity 的 `@TableName`、`@TableField` 注解）
- ✅ 修改 `src/main/java/service/IOrderService.java` 和 `impl/OrderServiceImpl.java`，实现业务逻辑
- ✅ 如需自定义 SQL，在 `src/main/resources/mapper/OrderMapper.xml` 中添加
- ✅ 在 Nacos 的 `duke-order.yml` 中配置 MySQL 数据源（URL、username、password）


- ✅ 注入 `RedisTemplate` 或 `StringRedisTemplate` 在 Service 中使用缓存
- ✅ 在 Nacos 的 `duke-order.yml` 中配置 Redis 连接（host、port、password）
- ✅ 通常在 `src/main/java/service/impl/OrderServiceImpl.java` 的 `getById` 等查询方法中添加缓存逻辑


- ✅ `src/main/java/feign/AuthFeignClient.java` 已配置调用 `duke-auth` 的示例
- ✅ 若需调用其他服务，在 `feign/` 目录下创建新的 `@FeignClient` 接口
- ✅ 确保在 `src/main/java/OrderApplication.java` 中已配置 `@EnableFeignClients`
- ℹ️ 所有内部 Feign 调用自动注入 `X-Gateway-Secret` 请求头（由 `InternalFeignConfig` 配置）


- ✅ `src/main/java/config/SecurityConfig.java` 已配置无状态 JWT 认证
- ✅ 修改 `SecurityConfig.WHITE_LIST[]` 以调整公开路径白名单
- ✅ 需在登录端点返回 JWT token 给客户端（参考 `duke-auth` 的实现）
- ✅ 客户端在后续请求的 `Authorization: Bearer {token}` 请求头中包含 token
- ✅ 服务收到的 request 会被网关验证 token，若有效则注入 SecurityContext
- ℹ️ 自定义权限检查可使用 `@PreAuthorize` 注解（已启用 `@EnableMethodSecurity`）


- ✅ 修改 `src/main/java/config/properties/AppProperties.java`，添加服务特定的配置属性
- ✅ 在 Nacos 的 `duke-order.yml` 中以 `order:` 前缀配置这些属性
- ✅ 在 Service 或 Controller 中注入 `AppProperties` 使用配置值


创建或更新以下 Nacos 配置文件（管理中心：`http://127.0.0.1:8848/nacos`）：

**duke-order.yml**（新建）

基础配置：
```yaml
order:
  example: example-value
```

如启用数据库，添加：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/duke-order?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true
    username: root
    password: password
```

如启用 Redis，添加：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
```


- ✅ 将上述 **网关路由配置** 片段添加到 Nacos 的 `duke-gateway.yml`


```bash
# 启动服务
mvn spring-boot:run

# 或编译后运行
mvn clean package -DskipTests
java -jar target/duke-order.jar

# 验证 Nacos 注册
curl http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=duke-order

# 查看 Swagger 文档
# 独立服务 Swagger：http://localhost:8084/order/swagger-ui.html
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
- 单个服务独立 Swagger：`http://localhost:8084/order/swagger-ui.html`
- 通过网关聚合 Swagger：`http://localhost:8080/swagger-ui.html`（所有注册服务的 API 都会聚合）

**Q: 如何调用其他服务的接口？**
A: 如果启用了 OpenFeign（useFeign=y），在 `src/main/java/feign/` 目录下定义 `@FeignClient` 接口，自动注入内部密钥请求头。参考 `AuthFeignClient.java` 的示例。

**Q: 可以删除不需要的文件吗？**
A: 可以！查看上方的 **可删除文件清单** 部分，根据选择的特性安全删除占位文件。

---

**生成时间**：${__timestamp}
**脚手架版本**：1.0.0

