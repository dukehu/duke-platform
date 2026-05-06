# 权限控制架构说明

## 📋 概述

duke-platform 采用**双层权限控制架构**，网关负责粗粒度的 API 级别权限检查，微服务负责细粒度的业务级权限控制。

```
┌──────────────────────────────────────────────┐
│          第一层：网关（粗粒度）                 │
│  - JWT Token 验证                            │
│  - API 级别权限检查（基于 sys_api 表）         │
│  - 快速拦截未授权请求                         │
└──────────────────────────────────────────────┘
                      ↓
┌──────────────────────────────────────────────┐
│          第二层：微服务（细粒度）               │
│  - 方法级权限控制（@PreAuthorize）             │
│  - 业务级权限逻辑（如：只能操作自己的数据）      │
│  - 灵活的权限表达式                           │
└──────────────────────────────────────────────┘
```

## 🔐 第一层：网关权限控制

### 组件
- **JwtAuthFilter**: JWT Token 验证
- **ApiPermissionFilter**: API 权限检查

### 职责
1. 验证 JWT Token 的有效性
2. 检查用户是否有访问该 API 的权限
3. 获取用户权限列表并传递给微服务
4. 快速拦截未授权请求，保护微服务

### 配置位置
- `sys_api` 表：定义 API 和权限标识的映射
- `sys_role_api` 表：定义角色和 API 的关联
- `sys_user_role` 表：定义用户和角色的关联

### 示例
```sql
-- API 定义
INSERT INTO sys_api (app_id, api_path, api_method, permission) 
VALUES ('duke-storage', '/files/list', 'GET', 'storage:file:list');

-- 角色权限分配
INSERT INTO sys_role_api (role_id, api_id) VALUES (1, 306);
```

## 🎯 第二层：微服务权限控制

### 组件
- **GatewayAuthFilter**: 从 Header 提取用户信息并填充 SecurityContext
- **SecurityConfig**: 统一的 Spring Security 配置
- **@PreAuthorize**: 方法级权限注解

### 职责
1. 接收网关传递的用户信息和权限列表
2. 填充 SecurityContext
3. 执行方法级权限检查
4. 实现业务级细粒度权限控制

### 使用方式

#### 1. 引入依赖
```xml
<dependency>
    <groupId>com.duke</groupId>
    <artifactId>duke-framework</artifactId>
</dependency>
```

#### 2. 配置启动类
```java
@SpringBootApplication(scanBasePackages = {"com.duke.storage", "com.duke.framework"})
public class StorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }
}
```

#### 3. 使用 @PreAuthorize
```java
@RestController
@RequestMapping("/files")
public class FileController {
    
    // API 级别权限检查
    @PreAuthorize("hasAuthority('storage:file:list')")
    @GetMapping("/list")
    public Result<List<FileVO>> list() {
        return Result.success(fileService.list());
    }
    
    // 业务级权限检查（在方法内部实现）
    @PreAuthorize("hasAuthority('storage:file:delete')")
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        SysFile file = fileService.getById(fileId);
        
        // 业务级检查：只能删除自己上传的文件
        Long currentUserId = getCurrentUserId();
        if (!file.getCreateBy().equals(currentUserId)) {
            throw new BusinessException("无权删除他人文件");
        }
        
        fileService.delete(fileId);
        return Result.success();
    }
}
```

## 📊 权限检查流程

### 完整流程
```
1. 前端请求
   Header: Authorization: Bearer xxx
   
2. 网关 - JwtAuthFilter
   ├─ 验证 JWT Token ✓
   ├─ 提取 userId, username
   └─ 放入 Request Header
   
3. 网关 - ApiPermissionFilter
   ├─ 调用 auth-center 检查 API 权限
   ├─ 获取用户权限列表
   └─ 放入 Header: X-User-Permissions
   
4. 微服务 - GatewayAuthFilter
   ├─ 从 Header 提取用户信息
   ├─ 解析权限列表
   └─ 填充 SecurityContext
   
5. 微服务 - @PreAuthorize
   ├─ 检查方法级权限
   ├─ 执行业务逻辑
   └─ 可选：业务级权限检查
```

## 🎨 权限标识命名规范

格式：`{模块}:{资源}:{操作}`

示例：
- `storage:file:upload` - 文件上传
- `storage:file:list` - 文件列表
- `storage:file:delete` - 文件删除
- `storage:file:download` - 文件下载
- `auth:user:create` - 用户创建
- `auth:role:update` - 角色更新

## ⚡ 性能优化

### 1. 网关缓存
- API 规则缓存在内存中（`GatewayPermissionServiceImpl.cachedRules`）
- 避免每次都查询数据库

### 2. 微服务缓存（可选）
可以在微服务中添加用户权限缓存：
```java
@Service
public class PermissionCacheService {
    
    @Cacheable(value = "userPermissions", key = "#userId")
    public List<String> getUserPermissions(Long userId) {
        return apiMapper.selectApiPermissionsByUserId(userId);
    }
}
```

### 3. 白名单机制
- 公开接口无需权限检查
- 减少不必要的权限验证

## 🔧 常见问题

### Q1: 什么时候用网关权限，什么时候用微服务权限？
**A:** 
- **网关权限**：API 级别的访问控制（能否访问这个接口）
- **微服务权限**：业务级别的细粒度控制（能否操作这条数据）

### Q2: 可以只用一层权限控制吗？
**A:** 可以，根据项目需求选择：
- **只用网关**：适合简单的 CRUD 系统
- **只用微服务**：适合需要复杂业务权限的系统
- **双层控制**：适合企业级应用（推荐）

### Q3: 如何调试权限问题？
**A:** 
1. 检查网关日志：查看 API 权限检查结果
2. 检查微服务日志：查看 @PreAuthorize 执行情况
3. 检查数据库：确认权限配置是否正确
4. 检查 Header：确认权限列表是否正确传递

## 📝 最佳实践

1. **明确分工**：网关做粗粒度控制，微服务做细粒度控制
2. **统一命名**：遵循 `{模块}:{资源}:{操作}` 的命名规范
3. **合理缓存**：对频繁查询的权限数据进行缓存
4. **详细日志**：记录权限检查的关键步骤，便于调试
5. **文档同步**：权限配置变更后及时更新文档

## 🚀 新增微服务指南

1. 引入 `duke-framework` 依赖
2. 启动类添加 `scanBasePackages = {"com.duke.xxx", "com.duke.framework"}`
3. 在 `sys_api` 表中注册 API 和权限
4. 在 Controller 中使用 `@PreAuthorize` 注解
5. 无需创建 SecurityConfig（使用框架提供的统一配置）

---

**最后更新**: 2026-05-06  
**维护者**: Duke Platform Team
