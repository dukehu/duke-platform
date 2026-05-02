# Duke Platform - Claude Code 指南

微服务架构。Java 21 + Spring Boot 3.2 + Vue 3。

## 架构
- Gateway 8080：路由、CORS、Swagger 聚合
- duke-auth 8081：认证、权限
- duke-transformer 8082：Transformer 模型可视化演示
- duke-knowledge-qa 8083：知识问答
- 前端 5173：Vue 3，统一走 Gateway

## 关键约定

**后端**
- 异常统一用 GlobalExceptionHandler，返回 Result{code, msg, data}
- CORS 集中在 Gateway，各 service 禁止配置

**前端**
- 所有请求走 Gateway（localhost:8080），禁止直连后端
- 状态管理用 Pinia，接口调用用 Axios

## 禁止事项

❌ 不改：duke-framework 共享配置、Nacos 配置、Spring Security

❌ 不在服务中：添加重复 CORS、捕获全局异常后自定义格式、直接改 JWT 验证逻辑

❌ 不做：自动提交代码、修改 git config、为假设场景添加错误处理

## 认证链路
- Token 由 duke-auth 签发，Gateway 统一校验
- 白名单接口在 Gateway 的 WhiteListFilter 配置
- 服务内获取当前用户：UserContext.getCurrentUser()（duke-framework 提供）

## 新建 service
1. 用 duke-service-archetype 生成骨架
2. 在 duke-parent 的 BOM 注册版本
3. 在 Gateway 添加路由规则
4. 在 Nacos 创建对应的命名空间配置

## 新建代码约定

**后端**：controller → service/impl → mapper/entity/dto → util
```java
throw new BusinessException(ResultCode.BAD_REQUEST, "msg");
```

**前端**：api/components/stores/router → types/utils/views

**评论**：仅记录 WHY（非显然约束、隐蔽不变量、workaround）

## 服务通信

**前端 → 后端**（网关）：/api/{auth,transformer,knowledge-qa}/** → 对应服务

**服务 → 服务**（OpenFeign）：不走网关，自动注入 X-Gateway-Secret
```java
@FeignClient(name = "duke-auth", configuration = InternalFeignConfig.class)
public interface AuthFeignClient {
    @GetMapping("/internal/users/{userId}")
    Result<Object> getUserById(@PathVariable Long userId);
}
```