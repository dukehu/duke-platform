# duke-auth-web 认证前端

端口：5173 | 对接后端：duke-auth

## 功能
- 登录：用户名、微信、GitHub、短信
- 权限菜单树（动态路由）
- 路由守卫：未登录重定向、权限检查
- 国际化：中文、英文

## API 关键约定
- /api/auth/permission 返回菜单树结构，登录成功后立即调用，结果用于生成动态路由
- Token 刷新在请求拦截器里静默处理，禁止弹窗提示用户
- 接口定义统一在 src/api/*.ts，禁止组件里直接写请求

## authStore 结构
- 字段固定：user、token、permissions，禁止随意新增字段
- login 方法签名：async (username, password) => {}
- 禁止在组件里直接修改 store 字段，必须调 action

## 菜单映射
- sys_menu.component → src/views/{component}/index.vue
- 例：system/user/index → src/views/system/user/index.vue
- 动态路由在登录后根据权限数据生成，不要写死路由表

## iframe 嵌入
- 通过 postMessage 通信
- 禁止用 window.parent 直接操作父页面 DOM