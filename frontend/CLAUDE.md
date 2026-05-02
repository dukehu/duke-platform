# Frontend 前端开发规范

各子应用独立：Vue 3 + Vite + TypeScript + Element Plus

## 编码规范
- 统一 <script setup lang="ts">，禁用 Options API
- 组件命名大驼峰：UserCard.vue
- 路由全部懒加载：() => import('./views/xxx.vue')
- CSS 用 scoped，颜色用 CSS 变量，禁止硬编码色值
- 状态管理用 Pinia，禁止 Vuex

## 目录约定
- src/api/          接口定义，按模块拆文件
- src/stores/       Pinia store，按模块拆文件
- src/composables/  组合式函数，use 开头命名
- src/types/        TypeScript 类型定义
- src/views/        页面组件，对应路由

## API 约定
- 基础 URL：http://localhost:8080（Gateway）
- /api/auth/**        → duke-auth
- /api/transformer/** → duke-transformer
- /api/knowledge-qa/** → duke-knowledge-qa
- 响应格式：{code, msg, data, timestamp}
- 禁止在组件里直接写 axios，统一放 src/api/
- 禁止直连后端服务，所有请求走 Gateway

## Token 管理
- 存储：localStorage，key：ACCESS_TOKEN
- 请求拦截器自动注入 Authorization
- 401 自动跳转登录页，禁止组件里手动处理