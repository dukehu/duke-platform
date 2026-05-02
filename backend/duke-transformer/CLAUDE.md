# duke-transformer Transformer 可视化
**端口**：8082 | **路径**：/transformer

## 功能
- Encoder 可视化（SSE 显示计算步骤）
- Decoder 可视化（SSE 显示 token 生成）
- 实时矩阵展示（注意力、Embedding、隐藏层）

## API
GET /api/transformer/run-encoder?input=&model=&layer=
GET /api/transformer/run-autoregressive?input=&model=&max_tokens=
响应：{step, layer/token, attention, logits, ...}

## 前端 SSE 集成
```javascript
const source = new EventSource('/api/transformer/run-encoder?...')
source.onmessage = e => updateVisualization(JSON.parse(e.data))
```

## 启动
```bash
mvn spring-boot:run
curl -N http://localhost:8082/transformer/run-encoder?input=hello
```
