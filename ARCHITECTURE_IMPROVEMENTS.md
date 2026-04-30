# Duke Platform 架构优化日志

## 2026-04-30 系统级改进

### 1. 跨域配置统一在网关处理

**问题**: 之前各服务（duke-auth, duke-knowledge-qa）都需要单独配置 CORS，导致重复配置和维护困难。

**解决方案**:
- ✅ 在网关（duke-gateway）中添加集中式 CORS 配置：`CorsConfig.java`
- ✅ 删除各服务的 CORS 配置文件：
  - `duke-auth/src/main/java/com/duke/auth/config/CorsConfig.java`
  - `duke-auth/src/main/java/com/duke/auth/config/properties/CorsProperties.java`
  - `duke-knowledge-qa/src/main/java/com/duke/knowledgeqa/config/CorsConfig.java`
  - `duke-knowledge-qa/src/main/java/com/duke/knowledgeqa/config/properties/CorsProperties.java`

**网关 CORS 支持的源**:
```
http://localhost:5173       # Vue 前端开发
http://localhost:3000       # React 前端开发
http://127.0.0.1:5173
http://127.0.0.1:3000
```

**允许的请求头**:
- Authorization
- Content-Type
- X-Requested-With
- X-Gateway-Secret（内部服务通信）

**好处**:
- ✅ 统一管理跨域策略
- ✅ 减少各服务配置重复
- ✅ 提高安全性：集中验证源地址
- ✅ 易于扩展：添加新源地址只需修改网关配置
- ✅ 性能优化：网关层统一处理 OPTIONS 预检请求

---

### 2. 全局异常处理统一定义在框架

**现状**: GlobalExceptionHandler 已定义在 `duke-framework` 中，现已显式注册为自动装配组件。

**改进内容**:
- ✅ 在 `duke-framework/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册异常处理器
  - `com.duke.framework.exception.GlobalExceptionHandler`
  - `com.duke.framework.exception.SecurityExceptionHandler`

**覆盖的异常类型**:

| 异常类型 | HTTP 状态 | 响应码 | 说明 |
|--------|---------|-------|------|
| `BusinessException` | 200 | 自定义 | 业务异常（如数据验证失败） |
| `MethodArgumentNotValidException` | 400 | 400 | 请求体参数验证失败 |
| `ConstraintViolationException` | 400 | 400 | 约束验证失败 |
| `HttpMessageNotReadableException` | 400 | 400 | JSON 解析失败 |
| `MissingServletRequestParameterException` | 400 | 400 | 缺少必填参数 |
| `HttpRequestMethodNotSupportedException` | 405 | 405 | 请求方法不支持 |
| `IllegalArgumentException` | 400 | 400 | 非法参数 |
| 其他 `Exception` | 500 | - | 系统内部错误 |

**统一响应格式**:
```json
{
  "code": 40000,
  "msg": "参数错误",
  "data": null,
  "timestamp": 1630000000000
}
```

**好处**:
- ✅ 统一错误处理：所有服务返回一致的错误格式
- ✅ 自动装配：无需各服务手动配置异常处理
- ✅ 易于扩展：框架中添加新异常处理，所有服务自动受益
- ✅ 专业的错误提示：明确的错误消息和状态码

---

### 3. Maven Archetype 更新

**相关文件更新**:
- ✅ `SCAFFOLD-NOTICE.md` - 已添加说明：CORS 由网关处理，新生成的服务无需配置跨域
- ✅ 新生成的服务将自动：
  - 使用框架中的 `GlobalExceptionHandler`
  - 通过网关处理跨域请求
  - 无需额外的 CORS 配置

---

## 配置检查清单

部署时验证以下配置是否正确：

### 网关配置
- [ ] 网关已编译：`mvn clean compile -DskipTests`
- [ ] 网关运行：`mvn spring-boot:run`
- [ ] 验证 CORS：浏览器发送预检请求，网关返回 CORS 响应头

### 各服务配置
- [ ] duke-auth 已删除 CORS 配置，编译无错
- [ ] duke-knowledge-qa 已删除 CORS 配置，编译无错
- [ ] 框架已注册异常处理器自动装配

### 测试验证

**跨域请求测试**:
```bash
curl -i -X OPTIONS http://localhost:8080/swagger-ui.html \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET"
```

预期响应包含：
```
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: Authorization, Content-Type, X-Requested-With, X-Gateway-Secret
Access-Control-Allow-Credentials: true
```

**异常处理测试**:
```bash
# 测试参数验证
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{invalid json}'

# 预期返回 Result 格式的错误响应
```

---

## 后续优化方向

1. **网关配置中心化**：将 CORS 源地址配置到 Nacos 中，支持热更新
2. **请求日志记录**：在网关层统一记录所有请求/响应
3. **速率限制**：在网关层统一实现请求速率限制
4. **全局响应包装**：考虑在网关层统一包装响应格式

---

**更新日期**: 2026-04-30
**更新者**: Claude Code
