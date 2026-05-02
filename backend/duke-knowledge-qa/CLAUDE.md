# duke-knowledge-qa 知识问答服务

端口：8083 | 路径：/knowledge-qa

## 技术栈
- LLM：Claude，Anthropic SDK，流式用 stream 模式
- Embedding：BAAI/bge-m3，SiliconFlow 接口，OpenAI 兼容格式
  baseUrl: https://api.siliconflow.cn/v1/embeddings
  用 openai-java SDK，baseUrl 指向 SiliconFlow
- 向量库：Qdrant，直接用 QdrantClient
- 切片：LangChain4j TextSplitter
- Rerank：预留，暂未实现

## 核心概念
- Document：用户上传的原始文档
- Chunk：文档切片，size=512 overlap=50，向量化最小单位
- QaSession：问答会话，保存多轮上下文

## Qdrant 约定
- 集合：duke-knowledge-qa，维度：1024，距离：Cosine
- 所有 Qdrant 操作直接用 QdrantClient，不做封装（当前阶段）
- 删除文档必须同时删对应向量：先删 Qdrant，再删 MySQL
- 禁止跨集合查询

## Embedding 约定
- 调用 SiliconFlow 接口，model 固定为 BAAI/bge-m3
- 向量化必须异步执行，禁止在上传接口同步调用（会超时）
- 向量化失败更新 document.status=3，禁止静默失败
- API Key 走 Nacos 配置，禁止硬编码

## LLM 约定
- 用 Anthropic SDK 调 Claude
- 流式响应用 SSE，Controller 返回 SseEmitter
- Prompt 模板放 resources/prompts/，禁止代码里硬编码
- 禁止直接拼接用户输入进 Prompt，防止注入攻击
- API Key 走 Nacos 配置，禁止硬编码

## 数据库（MySQL）
- documents：文档元信息
  status：0=上传中 1=处理中 2=就绪 3=失败
- document_chunks：切片内容 + Qdrant 向量 ID
- questions / answers：问答记录
- 禁止物理删除任何记录，统一软删除

## 检索流程
- 用户问题 → Embedding → Qdrant 相似度检索 → top_k=5
- 相似度阈值：0.75，低于此值不召回
- 检索结果拼入 Prompt → Claude 生成答案 → SSE 流式返回
- Rerank 预留在检索结果和 Prompt 拼接之间

## 易错点
- 维度必须是 1024（bge-m3），不是 768
- 删文档必须先删 Qdrant 再删 MySQL，顺序不能反
- SSE 连接断开时必须手动 close，防止内存泄漏
- 向量化是异步的，上传接口返回成功不代表可以检索