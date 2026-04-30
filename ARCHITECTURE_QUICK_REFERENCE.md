# Duke Platform 架构改进快速参考

## 问题 1：跨域配置

### 之前（❌ 重复配置）
```
duke-auth/
  └── config/
      ├── CorsConfig.java
      └── properties/CorsProperties.java

duke-knowledge-qa/
  └── config/
      ├── CorsConfig.java
      └── properties/CorsProperties.java

duke-gateway/
  └── （无CORS配置）
```

### 现在（✅ 统一在网关）
```
duke-gateway/
  └── config/
      └── CorsConfig.java ← 集中管理所有跨域规则

duke-auth/
  └── config/
      └── （无CORS配置）

duke-knowledge-qa/
  └── config/
      └── （无CORS配置）
```

### 网关CORS配置要点
```java
// 位置：duke-gateway/src/main/java/com/duke/gateway/config/CorsConfig.java

// 支持的源
http://localhost:5173       // 默认
http://localhost:3000       // 可扩展
http://127.0.0.1:*

// 支持的请求头
Authorization              // JWT token
Content-Type
X-Requested-With
X-Gateway-Secret          // 内部服务通信

// 支持的方法
GET, POST, PUT, DELETE, OPTIONS, PATCH

// 预检缓存
3600秒
```

### 修改源地址（如有新前端）
编辑 `duke-gateway/src/main/java/com/duke/gateway/config/CorsConfig.java`：
```java
corsConfig.setAllowedOrigins(Arrays.asList(
    "http://localhost:5173",    // Vue
    "http://localhost:3000",     // React
    "http://your-domain.com",    // 新增
    ...
));
```

---

## 问题 2：全局异常处理

### 统一异常处理流程
```
请求
  ↓
业务逻辑处理
  ↓
发生异常
  ↓
GlobalExceptionHandler (framework) ← 自动捕获
  ↓
返回 Result{code, msg, data}
  ↓
响应 JSON
```

### 异常处理器位置
```
duke-framework/
  ├── src/main/java/com/duke/framework/exception/
  │   ├── GlobalExceptionHandler.java    ← 业务异常处理
  │   └── SecurityExceptionHandler.java  ← 安全异常处理
  └── src/main/resources/META-INF/spring/
      └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
          ├── com.duke.framework.config.SwaggerConfig
          ├── com.duke.framework.exception.GlobalExceptionHandler
          └── com.duke.framework.exception.SecurityExceptionHandler
```

### 异常到响应的映射

| 发生的异常 | 处理器 | 返回格式 |
|----------|------|--------|
| 参数验证失败 | GlobalExceptionHandler | `{code: 40000, msg: "参数错误"}` |
| JSON解析失败 | GlobalExceptionHandler | `{code: 40000, msg: "请求体格式错误"}` |
| 业务异常 | GlobalExceptionHandler | `{code: 自定义, msg: 异常消息}` |
| 权限不足 | SecurityExceptionHandler | `{code: 40301, msg: "禁止访问"}` |
| 方法不支持 | GlobalExceptionHandler | `{code: 405, msg: "不支持的请求方式"}` |
| 其他异常 | GlobalExceptionHandler | `{code: 500, msg: "系统内部错误"}` |

### 如何使用

**在服务中抛出业务异常**：
```java
@Service
public class OrderServiceImpl implements IOrderService {
    
    public Order getOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        
        // 自动被GlobalExceptionHandler捕获
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        
        if (order.isExpired()) {
            throw new BusinessException("订单已过期");
        }
        
        return order;
    }
}
```

**自动返回统一格式**：
```json
{
  "code": 40404,
  "msg": "订单不存在",
  "data": null,
  "timestamp": 1630000000000
}
```

### 扩展异常处理

在 `GlobalExceptionHandler` 中添加新的异常处理方法：
```java
@ExceptionHandler(YourCustomException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Result<Void> handleYourException(YourCustomException e) {
    return Result.fail(YOUR_CODE, e.getMessage());
}
```

---

## 验证改进是否生效

### 方式1：运行验证脚本
```bash
cd backend
bash verify-architecture.sh
```

### 方式2：手动验证

**CORS验证**：
```bash
# 测试预检请求
curl -i -X OPTIONS http://localhost:8080/api/auth/list \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET"

# 检查响应头是否包含
# Access-Control-Allow-Origin: http://localhost:5173
```

**异常处理验证**：
```bash
# 发送无效JSON
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{invalid}'

# 应返回Result格式错误
```

---

## 对新服务的影响（使用 Archetype）

新用 `mvn archetype:generate` 生成的服务会自动：
- ✅ 使用网关的CORS配置（无需服务级CORS配置）
- ✅ 使用框架的GlobalExceptionHandler（自动装配）
- ✅ 返回统一的Result错误格式

无需额外配置！

---

## 常见问题

**Q: 如何在本地开发时改变允许的源？**
A: 编辑网关的 `CorsConfig.java`，添加你的本地地址到 `allowedOrigins` 列表。

**Q: 前端收到CORS错误怎么办？**
A: 
1. 检查前端URL是否在网关CORS白名单中
2. 检查浏览器控制台看完整的CORS错误信息
3. 确保网关正在运行

**Q: 如何自定义错误消息？**
A: 在服务中抛出带消息的异常：
   ```java
   throw new BusinessException("自定义错误消息");
   ```

**Q: 某个异常没有被正确处理？**
A: 在 `GlobalExceptionHandler` 中添加对应的 `@ExceptionHandler` 方法。

---

**最后更新**: 2026-04-30
