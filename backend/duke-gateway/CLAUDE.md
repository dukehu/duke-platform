# duke-gateway API 网关

端口：8080

## 职责
- 路由转发：/api/{auth,transformer,knowledge-qa}/** → 对应服务
- JWT 认证验证（白名单除外）
- 权限检查
- CORS 统一配置
- Swagger 聚合

## 路由约定
- 新增服务必须在此注册路由，禁止前端直连后端服务
- 内部接口 /internal/** 不经过 Gateway，服务间直连

## CORS 配置
- 文件：config/CorsConfig.java，改 CORS 只改这里
- 允许源：localhost:5173、localhost:3000、127.0.0.1:*
- 允许头：Authorization、Content-Type、X-Requested-With、X-Gateway-Secret
- 禁止用 * 通配符，生产环境必须明确列出域名

## 白名单（不鉴权）
- 配置：WhiteListFilter.java
- 当前：/api/auth/login、/api/auth/register、/api/auth/refresh
- 新增白名单只在此配置，禁止在业务层绕过鉴权