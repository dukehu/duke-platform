# duke-auth 认证服务

端口：8081 | DB：duke_auth

## API 约定
- 对外：/api/auth/**，经 Gateway 鉴权
- 内部：/internal/**，校验 X-Gateway-Secret，不校验 JWT
- 禁止把内部接口暴露到 /api/** 路径

## 数据库
- 表：sys_user、sys_role、sys_menu、sys_button 及关联表
- menu_type：1=目录 2=页面 | button_type：1=头部 2=行操作
- 禁止物理删除用户，用 status=0 软删除

## 关键约定
- JWT 只在此服务签发和校验，其他服务禁止自己生成 Token
- 密码必须 BCrypt，禁用 MD5/SHA1
- Token 有效期：access_token 2h，refresh_token 7d，不要修改
- RBAC 权限数据由此服务统一维护，其他服务只调接口不查库