# duke-transformer-web Transformer 可视化
**端口**：5173 | **后端**：duke-transformer

## 功能
- Encoder 可视化（注意力、Embedding、隐藏层）
- Decoder 可视化（token 生成、概率）
- SSE 流（EventSource 实时数据）
- 交互（切换模型、修改输入）

## SSE 集成
```typescript
const source = new EventSource(`/api/transformer/run-encoder?input=...`)
source.onmessage = e => updateVisualization(JSON.parse(e.data))
```

## 状态
input、model、results、loading
startEncoding()、startDecoding()

## 启动
```bash
npm install && npm run dev
npm run build
```
与 duke-auth-web 通过 iframe + postMessage 集成。
