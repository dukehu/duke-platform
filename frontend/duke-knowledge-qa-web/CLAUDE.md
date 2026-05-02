# duke-knowledge-qa-web 知识问答前端

端口：5173 | 对接后端：duke-knowledge-qa

## 核心页面
- /documents          文档管理（上传、浏览、删除）
- /chat/:sessionId    AI 对话（最核心）
- /search             语义搜索

## SSE 流式对话
- 用原生 EventSource，封装在 composables/useSSE.ts
- 禁止在组件里直接 new EventSource
- 停止生成：同时做两件事：
    1. 调 POST /api/knowledge-qa/questions/stop
    2. eventSource.close()
- 页面销毁时必须 close SSE，禁止泄漏：
  onUnmounted(() => eventSource.close())

## 状态管理
- documentStore：文档列表、上传进度、当前文档
- questionStore：当前会话消息列表、SSE 实例、loading 状态
- searchStore：搜索结果、搜索历史
- 消息列表存 questionStore，禁止用组件本地 state 保存

## 文件上传约定
- 支持格式：PDF、Word、TXT、Markdown
- 上传后轮询 document.status，间隔 2s
- status=2 才可检索，status=3 显示失败，禁止上传完直接搜索
- 大文件列表（>100条）用虚拟滚动，禁止全量渲染

## Markdown 渲染
- AI 回答支持 Markdown，组件：MessageContent.vue
- 用 marked + highlight.js，禁止用 v-html 直接渲染未处理的内容
- 代码块必须有语法高亮和复制按钮

## API 关键约定
- 问答接口返回 SSE 流，不是普通 JSON
- /search/hybrid 混合搜索，优先用这个，不用 /search
- 接口定义统一在 src/api/*.ts