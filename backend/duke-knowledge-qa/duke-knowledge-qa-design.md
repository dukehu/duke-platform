# 企业级私有知识库问答系统设计文档
## 1. 系统概述
### 1.1 项目背景
Duke 平台知识库问答系统是一个企业级私有知识库解决方案，用于构建组织内部的智能问答系统。系统支持文档上传、向量化存储、语义搜索和智能问答等核心功能。

### 1.2 核心目标
+ 提供企业级私有知识库管理能力
+ 支持多种文档格式的上传和处理
+ 实现高效的语义搜索和问答
+ 保证数据安全和隐私性
+ 提供友好的用户交互界面

### 1.3 技术选型
+ **向量数据库**：Qdrant（高性能向量搜索）
+ **嵌入模型**：qwen3-embedding-8b（阿里通义千问嵌入模型，输出维度 4096）
+ **后端框架**：Spring Boot 3.2.5 + Spring Cloud
+ **前端框架**：Vue 3.5 + TypeScript + Vite
+ **数据库**：MySQL（元数据存储）+ Redis（缓存）
+ **可观测性**：LangFuse（LLM 调用追踪、Prompt 管理、评估分析）

### 1.4 系统架构图
```plain
┌─────────────────────────────────────────────────────────────┐
│                    前端应用层                                 │
│         duke-knowledge-qa-web (Vue 3 + TypeScript)          │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP/REST + SSE（流式输出）
┌────────────────────▼────────────────────────────────────────┐
│                  API 网关层                                   │
│            Spring Cloud Gateway (8080)                       │
└────────────────────┬────────────────────────────────────────┘
                     │ 服务发现 (Nacos)
┌────────────────────▼────────────────────────────────────────┐
│              知识库问答服务层                                  │
│         duke-knowledge-qa (Spring Boot 8083)                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ • 文档管理模块                                         │  │
│  │ • 向量化处理模块（Chunking → Embedding）               │  │
│  │ • 问答引擎模块（查询改写 → 检索 → Rerank → 生成）      │  │
│  │ • 搜索模块（向量 + 关键词混合检索）                    │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────┬─────────────────────────────────┬───────────────┘
           │                                 │ LangFuse SDK（追踪）
     ┌─────┴──────┐                 ┌────────▼────────┐
     │            │                 │    LangFuse      │
  ┌──▼───┐  ┌────▼───┐             │  (LLM 可观测性)  │
  │MySQL │  │ Redis  │             └─────────────────┘
  │(元数│  │ (缓存) │
  │ 据) │  └────────┘
  └──┬───┘
     │
┌────▼─────┐
│  Qdrant  │
│ (向量库) │
└──────────┘
```

## 2. 系统功能模块
### 2.1 文档管理模块
**职责**：处理知识库文档的上传、存储、更新和删除

**主要功能**：

+ 支持多种文档格式（PDF、Word、TXT、Markdown 等）
+ 文档分类和标签管理
+ 文档版本控制
+ 文档元数据管理（标题、作者、创建时间等）
+ 文档预览和下载

**数据模型**：

```plain
Document (文档表)
├── id: Long (主键)
├── title: String (文档标题)
├── category: String (分类)
├── tags: String (标签，JSON 格式)
├── content: Text (文档内容)
├── fileUrl: String (文件存储路径)
├── fileType: String (文件类型)
├── status: Enum (状态：DRAFT/PUBLISHED/ARCHIVED)
├── createdBy: Long (创建者 ID)
├── createdAt: DateTime (创建时间)
├── updatedAt: DateTime (更新时间)
└── deletedAt: DateTime (删除时间，逻辑删除)
```

### 2.2 文档解析方案
#### 2.2.1 方案选型对比
| 方案 | 支持格式 | 优点 | 缺点 | 推荐场景 |
| --- | --- | --- | --- | --- |
| **Apache Tika 3.x** | PDF、Word、Excel、PPT、HTML、TXT、Markdown 等 1000+ 种 | 格式覆盖最全，一个依赖搞定所有格式，自动检测 MIME 类型 | 包体较大（依赖多），复杂 PDF 表格解析能力一般 | **本项目推荐**，企业文档类型多样时首选 |
| Apache PDFBox | 仅 PDF | 纯 Java，PDF 解析质量高，可精确控制布局 | 只支持 PDF | 系统只处理 PDF 时使用 |
| Apache POI | Word、Excel、PPT | Office 格式解析最权威 | 只支持 Office 格式 | Tika 底层已集成，一般不单独使用 |
| Docx4j | Word、PPT | 可处理 OOXML 样式和结构 | API 复杂，学习成本高 | 需要保留文档结构时使用 |


**本项目选型：Apache Tika 3.1.0**，理由：

+ 企业知识库文档格式多样（PDF 技术文档、Word 规范、TXT 日志等），Tika 一个依赖全覆盖
+ 自动 MIME 类型检测，无需前端传递文件类型
+ Spring Boot 3.x + Java 21 环境与 Tika 3.x 兼容良好

#### 2.2.2 Maven 依赖
```xml
<!-- Apache Tika 核心 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>3.1.0</version>
</dependency>
<!-- Tika 标准解析器包（含 PDF、Office、HTML 等所有常用格式） -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>3.1.0</version>
</dependency>

```

> ⚠️ `tika-parsers-standard-package` 依赖较重（含 PDFBox、POI 等），如只需解析 PDF 可单独引入 `tika-parser-pdf-module`；只需 Office 可引入 `tika-parser-microsoft-module`，按需精简。
>

#### 2.2.3 代码示例
```java
@Service
public class DocumentParserService {

    private final Tika tika = new Tika();

    /**
     * 通用文档解析入口：自动检测格式，提取纯文本
     */
    public String parse(InputStream inputStream, String fileName) throws TikaException, IOException {
        // Tika 自动根据文件内容和文件名推断格式，无需手动判断
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        return tika.parseToString(inputStream, metadata, -1); // -1 表示不限长度
    }

    /**
     * 解析并同时获取文档元数据（标题、作者、创建时间等）
     */
    public ParseResult parseWithMetadata(InputStream inputStream, String fileName)
            throws TikaException, IOException, SAXException {

        Parser parser = new AutoDetectParser();
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        ParseContext context = new ParseContext();

        parser.parse(inputStream, handler, metadata, context);

        return ParseResult.builder()
                .content(handler.toString())
                .title(metadata.get(TikaCoreProperties.TITLE))
                .author(metadata.get(TikaCoreProperties.CREATOR))
                .createdAt(metadata.getDate(TikaCoreProperties.CREATED))
                .contentType(metadata.get(Metadata.CONTENT_TYPE))
                .build();
    }

    /**
     * 仅检测文件 MIME 类型，不解析内容（用于文件类型校验）
     */
    public String detectMimeType(InputStream inputStream, String fileName) throws IOException {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        return tika.detect(inputStream, metadata);
    }
}
```

**在文档上传流程中使用**：

```java
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentParserService documentParserService;

    @Autowired
    private ChunkingService chunkingService;

    public Document uploadDocument(MultipartFile file, String title, String category) throws Exception {
        // 1. 解析文档内容
        ParseResult parseResult = documentParserService.parseWithMetadata(
                file.getInputStream(), file.getOriginalFilename());

        // 2. 校验内容非空
        if (StringUtils.isBlank(parseResult.getContent())) {
            throw new BusinessException("文档内容为空，请检查文件是否损坏");
        }

        // 3. 保存文档元数据
        Document document = Document.builder()
                .title(StringUtils.isNotBlank(title) ? title : parseResult.getTitle())
                .category(category)
                .content(parseResult.getContent())
                .fileUrl(storageService.upload(file))
                .fileType(parseResult.getContentType())
                .status(DocumentStatus.PROCESSING)
                .build();
        documentMapper.insert(document);

        // 4. 异步进行分块 + 向量化
        applicationEventPublisher.publishEvent(new DocumentUploadedEvent(document.getId()));

        return document;
    }
}
```

### 2.3 向量化处理模块
**职责**：将文档内容转换为向量表示并存储到 Qdrant

**主要功能**：

+ 文档分块处理（Chunking）
+ 调用 qwen3-embedding-8b 模型生成向量
+ 向量存储到 Qdrant
+ 向量更新和删除
+ 向量缓存管理

**处理流程**：

```plain
文档上传 → 文本提取（Tika）→ 分块处理 → 向量生成 → Qdrant 存储 → 元数据保存
```

#### 2.3.1 分块策略选型对比
| 策略 | 原理 | 优点 | 缺点 | 适用场景 |
| --- | --- | --- | --- | --- |
| **固定大小（Fixed-size）** | 按 token/字符数机械切块，加重叠窗口 | 实现简单，性能最好 | 可能切断语义，忽略文档结构 | 纯文本、日志类文档 |
| **递归字符切分（Recursive）** | 按段落→句子→词的优先级递归切割，尽量在语义边界断开 | 兼顾效率和语义完整性，**最常用** | 仍是启发式规则，复杂文档效果有限 | **本项目主要策略**，适合大多数文档 |
| **语义切分（Semantic）** | 对每句话生成 embedding，相邻句子相似度下降时切块 | 语义边界最准确，召回率最高 | 需调用 embedding 模型，速度慢 3~5 倍，成本高 | 内容密度高、主题频繁切换的长文档 |
| **文档结构感知（Structure-aware）** | 按 Markdown 标题、HTML 标签、PDF 书签等结构切块 | 天然保留文档层次 | 依赖文档格式规范，非结构化文档无效 | 格式规范的技术文档、Wiki |
| **句子窗口（Sentence-window）** | 以句子为最小单位，检索时返回前后 N 句窗口扩展上下文 | 保证 chunk 粒度细，同时扩大上下文返回 | 实现稍复杂，存储多份数据 | 需要精细控制上下文的场景 |


**本项目选型策略**：

+ **默认策略：Recursive（递归字符切分）**，使用 LangChain4j `DocumentSplitters.recursive()`
+ **Markdown/结构化文档：Structure-aware**，先按标题层级切，再对超长节递归切分
+ **重要长文档（如技术规范）：Semantic**，牺牲部分速度换取更高的召回率

#### 2.3.2 Maven 依赖
```xml
<!-- LangChain4j 核心（含 DocumentSplitter） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.14.0</version>
</dependency>

```

#### 2.3.3 代码示例
**策略一：Recursive 递归切分（默认，推荐）**

```java
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

@Service
public class ChunkingService {

    /**
     * Recursive 递归切分：按段落 → 句子 → 词 优先级递归切分
     * 适合大多数文档，兼顾效率与语义完整性
     */
    public List<TextSegment> recursiveChunk(String text, int chunkSize, int overlap) {
        DocumentSplitter splitter = DocumentSplitters.recursive(
                chunkSize,   // 每块最大 token 数，推荐 512
                overlap      // 重叠 token 数，推荐 128
        );
        Document document = Document.from(text);
        return splitter.split(document);
    }
}
```

**策略二：Structure-aware 结构感知切分（Markdown/HTML 文档）**

```java
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;

/**
 * Markdown 结构感知切分：
 * 1. 先按标题（# ## ###）切为大段
 * 2. 超长段再用 recursive 二次切分
 */
public List<TextSegment> structureAwareChunk(String markdownText) {
    List<TextSegment> result = new ArrayList<>();

    // 按 Markdown 标题分割
    String[] sections = markdownText.split("(?m)^#{1,3} ");

    for (String section : sections) {
        if (section.isBlank()) continue;

        String sectionText = section.trim();

        if (tokenCount(sectionText) <= 512) {
            // 小节直接作为一个 chunk
            result.add(TextSegment.from(sectionText));
        } else {
            // 超长节递归切分
            DocumentSplitter splitter = DocumentSplitters.recursive(512, 128);
            result.addAll(splitter.split(Document.from(sectionText)));
        }
    }
    return result;
}

// 简单 token 估算（中文约 1.5 字/token，英文约 4 字符/token）
private int tokenCount(String text) {
    return (int) (text.length() / 1.5);
}
```

**策略三：Semantic 语义切分（高质量长文档）**

语义切分需要对每个句子调用 embedding 模型，在 Java 中通常通过计算相邻句子余弦相似度来决定切块位置：

```java
/**
 * Semantic 语义切分：
 * 1. 按句子分割原文
 * 2. 对每句生成 embedding
 * 3. 相邻句子相似度低于阈值时作为切块边界
 */
public List<String> semanticChunk(String text, double breakpointThreshold) {
    // 1. 切句（简单实现，生产可用 OpenNLP / BreakIterator）
    List<String> sentences = Arrays.asList(text.split("(?<=[。！？.!?])"));

    // 2. 批量生成句子 embedding（调用 qwen3-embedding-8b）
    List<float[]> embeddings = embeddingService.batchEmbed(sentences);

    // 3. 计算相邻句子余弦相似度，找切点
    List<String> chunks = new ArrayList<>();
    StringBuilder currentChunk = new StringBuilder();

    for (int i = 0; i < sentences.size() - 1; i++) {
        currentChunk.append(sentences.get(i));

        double similarity = cosineSimilarity(embeddings.get(i), embeddings.get(i + 1));
        // 相似度低于阈值 = 主题切换，在此断开
        if (similarity < breakpointThreshold) {
            chunks.add(currentChunk.toString().trim());
            currentChunk = new StringBuilder();
        }
    }
    // 加入最后一句
    currentChunk.append(sentences.get(sentences.size() - 1));
    if (!currentChunk.isEmpty()) {
        chunks.add(currentChunk.toString().trim());
    }
    return chunks;
}

private double cosineSimilarity(float[] a, float[] b) {
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

**统一切分入口（根据文档类型路由策略）**：

```java
@Service
public class ChunkingService {

    public List<TextSegment> chunk(String content, String fileType, ChunkingConfig config) {
        return switch (fileType.toLowerCase()) {
            // Markdown 和 HTML 用结构感知切分
            case "text/markdown", "text/html" ->
                    structureAwareChunk(content);

            // 纯文本、PDF 用递归切分
            case "text/plain", "application/pdf" ->
                    recursiveChunk(content, config.getChunkSize(), config.getOverlap());

            // 其他格式默认递归切分
            default -> recursiveChunk(content, config.getChunkSize(), config.getOverlap());
        };
    }
}
```

**分块策略**：

+ 优先按语义边界切块：段落 > 标题 > 代码块 > 句子，避免机械按 token 截断
+ 块大小：512 tokens（约 2000 字符），超长段落再按句子分割
+ 重叠：128 tokens（约 500 字符），保持上下文连续性
+ 保留原文档 ID 和块序号用于追溯
+ 保留父块摘要作为每个子块的前缀上下文（Parent Document 模式）

#### 2.3.4 向量化失败补救机制
**背景**：向量化是异步批处理过程，受 Embedding API 限流、超时、网络抖动、单条文本超长等影响，可能出现部分 chunk 失败的情况（如 `totalChunks: 1748, successChunks: 1628`）。系统需要提供完整的失败检测、重试和状态追踪能力。

**处理流程**：

```plain
文档上传
  ↓
保存原文到 MySQL（status=PROCESSING）
  ↓
所有 chunk 写入 document_chunks（vector_status=PENDING）
  ↓
异步消费：批量调用 Embedding API
  ↓ 成功                    ↓ 失败
vector_status=SUCCESS    retry_count+1，记录 error_msg
vector_id 写入           ↓
                         retry_count < 3：进重试队列（指数退避 1s/5s/30s）
                         retry_count >= 3：vector_status=FAILED，不再自动重试
  ↓
所有 chunk 处理完毕，统计 success_chunks / total_chunks
  ↓
更新 documents.status
  success == total  → PUBLISHED
  0 < success < total → PARTIAL
  success == 0      → FAILED
```

**文档状态流转**：

```plain
DRAFT → PROCESSING → PUBLISHED  (全部成功)
                   → PARTIAL    (部分失败，仍可检索但结果不完整)
                   → FAILED     (全部失败，无法检索)
PARTIAL / FAILED → PROCESSING   (触发重试后重置)
```

**向量化 Service 核心实现**：

```java
@Service
public class VectorService {

    private static final int MAX_RETRY = 3;

    /**
     * 批量向量化，带重试和状态追踪
     */
    @Async
    public void batchEmbedWithRetry(Long documentId) {
        List<DocumentChunk> pending = chunkMapper.selectList(
            new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .eq(DocumentChunk::getVectorStatus, "PENDING")
        );

        int successCount = 0;
        for (DocumentChunk chunk : pending) {
            try {
                float[] vector = embeddingService.embed(chunk.getContent());
                String vectorId = UUID.randomUUID().toString();

                // 写入 Qdrant
                qdrantService.upsert(vectorId, vector, buildPayload(chunk));

                // 更新状态
                chunk.setVectorId(vectorId);
                chunk.setVectorStatus("SUCCESS");
                chunkMapper.updateById(chunk);
                successCount++;

            } catch (Exception e) {
                chunk.setRetryCount(chunk.getRetryCount() + 1);
                chunk.setErrorMsg(e.getMessage());
                chunk.setLastRetryAt(LocalDateTime.now());

                if (chunk.getRetryCount() < MAX_RETRY) {
                    // 未达上限：保持 PENDING，等下次重试（指数退避）
                    scheduleRetry(chunk.getId(), chunk.getRetryCount());
                } else {
                    // 达到上限：标记 FAILED，停止自动重试
                    chunk.setVectorStatus("FAILED");
                }
                chunkMapper.updateById(chunk);
            }
        }

        // 更新文档整体状态
        updateDocumentStatus(documentId);
    }

    private void updateDocumentStatus(Long documentId) {
        int total = chunkMapper.countByDocumentId(documentId);
        int success = chunkMapper.countByDocumentIdAndStatus(documentId, "SUCCESS");

        String status;
        if (success == total)      status = "PUBLISHED";
        else if (success == 0)     status = "FAILED";
        else                       status = "PARTIAL";

        documentMapper.updateStatusAndChunkStats(documentId, status, total, success);
    }

    private void scheduleRetry(Long chunkId, int retryCount) {
        // 指数退避：第1次1s，第2次5s，第3次30s
        long delaySeconds = switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            default -> 30;
        };
        taskScheduler.schedule(
            () -> retryChunk(chunkId),
            Instant.now().plusSeconds(delaySeconds)
        );
    }
}
```

**补救 API**：

```plain
# 重跑指定文档的所有失败/未处理 chunk
POST /api/knowledge-qa/documents/{documentId}/retry-vectors

响应：
{
  "code": 200,
  "data": {
    "documentId": 1,
    "pendingChunks": 120,   // 本次触发重试的 chunk 数
    "status": "PROCESSING"
  }
}

# 全局扫描重跑所有 PARTIAL / FAILED 文档（管理员接口 / 定时任务）
POST /api/knowledge-qa/documents/retry-all-failed
```

**定时任务兜底**（每小时扫描一次，防止遗漏）：

```java
@Scheduled(cron = "0 0 * * * *")
public void retryFailedVectors() {
    // 找出所有 PARTIAL / FAILED 且有 PENDING 或 FAILED chunk 的文档
    List<Long> docIds = documentMapper.selectFailedDocumentIds();
    docIds.forEach(this::retryDocument);
}
```

**数据模型**：

```plain
DocumentChunk (文档块表)
├── id: Long (主键)
├── documentId: Long (关联文档 ID)
├── chunkIndex: Integer (块序号)
├── content: Text (块内容)
├── vectorId: String (Qdrant 中的向量 ID)
├── tokens: Integer (token 数量)
├── vectorStatus: Enum (PENDING / SUCCESS / FAILED)
├── retryCount: Integer (已重试次数)
├── errorMsg: String (失败原因)
├── lastRetryAt: DateTime (最后重试时间)
├── createdAt: DateTime (创建时间)
└── deletedAt: DateTime (删除时间，逻辑删除)
```

> ⚠️ 向量数据统一由 Qdrant 管理，不在 MySQL 中冗余存储 embedding 字段（4096 维 float32 约 16KB/条，MySQL 无法索引）。向量缓存若有需要，通过 Redis 存储序列化二进制。
>

### 2.4 问答引擎模块
**职责**：处理用户问题，调用 LLM 生成答案，并通过 LangFuse 追踪全链路

**主要功能**：

+ 查询改写（Query Rewriting）：用 LLM 将用户口语化问题扩写为检索友好的形式
+ 向量检索 + Rerank：Top-K 粗检索后使用 Reranker 模型精排，送入 LLM 的上下文更精准
+ 流式答案生成（SSE）：基于 Server-Sent Events 实时推送生成内容，避免长时间等待
+ 答案生成历史记录
+ 用户反馈收集（答案质量评分）
+ LangFuse 全链路追踪：记录查询改写、检索、Rerank、LLM 调用各阶段耗时与 token 消耗

**问答流程**：

```plain
用户提问
  ↓
查询改写（LLM，可选）          ← LangFuse Span: query_rewrite
  ↓
向量搜索 Top-K（Qdrant）       ← LangFuse Span: vector_search
  ↓
关键词搜索（MySQL 全文）        ← LangFuse Span: keyword_search
  ↓
混合融合（RRF 算法）
  ↓
Rerank 精排（Top-3~5）         ← LangFuse Span: rerank
  ↓
LLM 生成答案（流式 SSE）        ← LangFuse Generation: llm_generate
  ↓
答案后处理 → 流式返回用户
```

**数据模型**：

```plain
Question (问题表)
├── id: Long (主键)
├── userId: Long (提问用户 ID)
├── content: String (问题内容)
├── rewrittenContent: String (改写后的查询，可为空)
├── status: Enum (状态：PENDING/ANSWERED/FAILED/ARCHIVED)
├── createdAt: DateTime (创建时间)
└── deletedAt: DateTime (删除时间，逻辑删除)

Answer (答案表)
├── id: Long (主键)
├── questionId: Long (关联问题 ID)
├── content: Text (答案内容)
├── sourceChunks: String (来源块 ID 列表，JSON 格式)
├── model: String (使用的 LLM 模型)
├── promptTokens: Integer (消耗的 prompt token 数)
├── completionTokens: Integer (消耗的 completion token 数)
├── langfuseTraceId: String (LangFuse 追踪 ID，用于跳转查看链路详情)
├── rating: Integer (用户评分：1-5)
├── feedback: String (用户反馈)
├── createdAt: DateTime (创建时间)
└── updatedAt: DateTime (更新时间)
```

### 2.5 搜索模块
**职责**：实现高效的语义搜索和混合搜索

**主要功能**：

+ 向量相似度搜索（Qdrant）
+ 关键词搜索（MySQL 全文搜索）
+ 混合搜索（向量 + 关键词）
+ 搜索结果排序和过滤
+ 搜索历史记录

**搜索策略**：

+ **向量搜索**：使用 Qdrant 的 ANN 算法，返回 Top-K 相似结果
+ **关键词搜索**：使用 MySQL 全文索引，支持布尔查询
+ **混合融合**：使用 **RRF（Reciprocal Rank Fusion）算法** 合并两路结果，避免向量相似度与全文检索分数量纲不一致的问题
+ **Rerank**：融合后对 Top-K 结果调用 Reranker 模型（qwen-reranker 或 BGE Reranker）精排，最终取 Top-3~5 送入 LLM

**搜索配置**：

```plain
SearchConfig
├── topK: Integer (返回结果数，默认 10)
├── scoreThreshold: Float (相似度阈值，默认 0.5)
├── vectorWeight: Float (向量搜索权重，默认 0.7)
├── keywordWeight: Float (关键词搜索权重，默认 0.3)
├── filters: Map (过滤条件，如分类、标签等)
└── sortBy: String (排序字段：relevance/date/rating)
```

## 3. 后端 API 设计
### 3.1 文档管理 API
**上传文档**

```plain
POST /api/knowledge-qa/documents/upload
Content-Type: multipart/form-data

请求参数：
- file: File (必需，支持 PDF、Word、TXT、Markdown)
- title: String (必需，文档标题)
- category: String (可选，分类)
- tags: String (可选，标签列表，JSON 数组)

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "documentId": 1,
    "title": "文档标题",
    "status": "PROCESSING",
    "createdAt": "2026-04-30T10:00:00Z"
  }
}
```

**获取文档列表**

```plain
GET /api/knowledge-qa/documents?page=1&pageSize=10&category=&keyword=

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": [
      {
        "id": 1,
        "title": "文档标题",
        "category": "技术文档",
        "tags": ["Java", "Spring"],
        "status": "PUBLISHED",
        "createdAt": "2026-04-30T10:00:00Z",
        "updatedAt": "2026-04-30T10:00:00Z"
      }
    ]
  }
}
```

**获取文档详情**

```plain
GET /api/knowledge-qa/documents/{documentId}

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "文档标题",
    "category": "技术文档",
    "tags": ["Java", "Spring"],
    "content": "文档内容...",
    "status": "PUBLISHED",
    "createdBy": 1,
    "createdAt": "2026-04-30T10:00:00Z",
    "updatedAt": "2026-04-30T10:00:00Z"
  }
}
```

**删除文档**

```plain
DELETE /api/knowledge-qa/documents/{documentId}

响应：
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 3.2 问答 API
**提交问题并获取流式答案（SSE）**

```plain
POST /api/knowledge-qa/questions/stream
Content-Type: application/json
Accept: text/event-stream

请求体：
{
  "content": "如何使用 Spring Boot？",
  "topK": 10,
  "scoreThreshold": 0.5,
  "enableQueryRewrite": true
}

响应（SSE 流）：
event: start
data: {"questionId": 1, "status": "PROCESSING"}

event: delta
data: {"text": "Spring Boot 是"}

event: delta
data: {"text": "一个快速开发框架..."}

event: sources
data: {"sourceChunks": [{"documentId": 1, "chunkIndex": 0, "relevanceScore": 0.95}]}

event: done
data: {"questionId": 1, "answerId": 1, "langfuseTraceId": "trace-xxx", "promptTokens": 512, "completionTokens": 256}

event: error
data: {"code": 500, "message": "LLM 调用失败"}
```

> 前端使用 `EventSource` 或 `fetch` + `ReadableStream` 接收 SSE，实时渲染生成内容。
>

**获取历史答案**

```plain
GET /api/knowledge-qa/questions/{questionId}/answer

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "answerId": 1,
    "questionId": 1,
    "content": "Spring Boot 是一个快速开发框架...",
    "sourceChunks": [
      {
        "documentId": 1,
        "chunkIndex": 0,
        "content": "Spring Boot 简介...",
        "relevanceScore": 0.95
      }
    ],
    "model": "qwen-max",
    "promptTokens": 512,
    "completionTokens": 256,
    "langfuseTraceId": "trace-xxx",
    "rating": null,
    "createdAt": "2026-04-30T10:00:00Z"
  }
}
```

**提交答案反馈**

```plain
POST /api/knowledge-qa/answers/{answerId}/feedback

请求体：
{
  "rating": 5,
  "feedback": "答案很有帮助"
}

响应：
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 3.3 搜索 API
**语义搜索**

```plain
POST /api/knowledge-qa/search

请求体：
{
  "query": "Spring Boot 配置",
  "topK": 10,
  "scoreThreshold": 0.5,
  "filters": {
    "category": "技术文档",
    "tags": ["Java"]
  }
}

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "results": [
      {
        "documentId": 1,
        "chunkIndex": 0,
        "content": "Spring Boot 配置方法...",
        "relevanceScore": 0.95,
        "category": "技术文档",
        "tags": ["Java", "Spring"]
      }
    ],
    "totalCount": 5
  }
}
```

**混合搜索**

```plain
POST /api/knowledge-qa/search/hybrid

请求体：
{
  "query": "Spring Boot 配置",
  "topK": 10,
  "vectorWeight": 0.7,
  "keywordWeight": 0.3,
  "filters": {}
}

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "results": [
      {
        "documentId": 1,
        "chunkIndex": 0,
        "content": "Spring Boot 配置方法...",
        "relevanceScore": 0.92,
        "vectorScore": 0.95,
        "keywordScore": 0.85
      }
    ],
    "totalCount": 5
  }
}
```

## 4. 前端设计
### 4.1 页面结构
**主要页面**：

+ 文档管理页面：上传、浏览、删除文档
+ 问答页面：提交问题、查看答案、反馈评分
+ 搜索页面：语义搜索、混合搜索、结果展示
+ 知识库首页：统计信息、热门文档、最近问题

### 4.2 核心组件
**文档上传组件**

+ 支持拖拽上传
+ 显示上传进度
+ 支持批量上传
+ 文件类型验证

**问答组件**

+ 问题输入框（支持多行）
+ 实时搜索建议
+ 答案展示（支持 Markdown）
+ 来源文档链接
+ 反馈评分组件

**搜索结果组件**

+ 结果列表展示
+ 相关度评分显示
+ 文档预览
+ 分页加载

### 4.3 状态管理（Pinia）
**Store 结构**：

```plain
stores/
├── documentStore.ts      # 文档管理状态
├── questionStore.ts      # 问答状态
├── searchStore.ts        # 搜索状态
└── uiStore.ts           # UI 状态（加载、错误等）
```

## 5. 后端项目结构
### 5.1 Maven 模块组织
```plain
backend/
├── duke-parent/                    # 父 POM（版本管理）
└── duke-knowledge-qa/              # 知识库问答服务
    ├── pom.xml                     # Maven 配置
    ├── src/main/java/com/duke/knowledgeqa/
    │   ├── aspect/                 # AOP 切面
    │   ├── common/                 # 常量和工具类
    │   ├── config/                 # Spring 配置
    │   ├── controller/             # REST 控制器
    │   ├── dto/                    # 数据传输对象
    │   ├── entity/                 # 数据库实体
    │   ├── enums/                  # 枚举类
    │   ├── event/                  # 事件监听
    │   ├── exception/              # 自定义异常
    │   ├── mapper/                 # MyBatis 映射器
    │   ├── service/                # 业务逻辑接口
    │   ├── service/impl/           # 业务逻辑实现
    │   ├── util/                   # 工具类
    │   └── KnowledgeQaApplication.java  # 启动类
    ├── src/main/resources/
    │   ├── application.yml         # 应用配置
    │   ├── application-dev.yml     # 开发环境配置
    │   ├── mapper/                 # MyBatis XML 映射文件
    │   └── db/                     # 数据库初始化脚本
    └── src/test/java/              # 单元测试
```

### 5.2 核心服务类
**DocumentService**：文档管理业务逻辑

+ 上传文档（支持多种格式）
+ 提取文本内容
+ 分块处理
+ 调用向量化服务
+ 保存元数据

**VectorService**：向量化处理业务逻辑

+ 调用 qwen3-embedding-8b 模型
+ 生成文档块向量
+ 存储到 Qdrant
+ 向量更新和删除
+ 缓存管理

**SearchService**：搜索业务逻辑

+ 向量相似度搜索（Qdrant）
+ 关键词搜索（MySQL）
+ 混合搜索
+ 结果排序和过滤

**QuestionService**：问答业务逻辑

+ 问题保存
+ 调用 LLM 生成答案
+ 答案保存
+ 反馈收集

### 5.3 外部服务集成
**Qdrant 集成**：

+ 依赖：`qdrant-client`
+ 配置：Qdrant 服务地址、API Key
+ 操作：创建集合、插入向量、搜索、删除

**LLM 集成**：

+ 支持多种模型（阿里通义千问、OpenAI 等）
+ 配置：模型 API Key、端点
+ 调用：生成答案、流式输出

**文件存储**：

+ 本地存储或 OSS（阿里云对象存储）
+ 配置：存储路径、访问权限

## 6. 数据库设计
### 6.1 核心表结构
**documents 表**（文档表）

```sql
CREATE TABLE documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  category VARCHAR(100),
  tags JSON,
  content LONGTEXT,
  file_url VARCHAR(500),
  file_type VARCHAR(50),
  status ENUM('DRAFT', 'PROCESSING', 'PARTIAL', 'PUBLISHED', 'FAILED', 'ARCHIVED') DEFAULT 'DRAFT',
  -- PROCESSING: 向量化进行中
  -- PARTIAL:    部分 chunk 向量化失败（successChunks < totalChunks）
  -- PUBLISHED:  全部向量化成功，可正常检索
  -- FAILED:     向量化全部失败
  total_chunks INT DEFAULT 0 COMMENT '总 chunk 数',
  success_chunks INT DEFAULT 0 COMMENT '向量化成功 chunk 数',
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  KEY idx_category (category),
  KEY idx_status (status),
  KEY idx_created_at (created_at),
  FULLTEXT KEY ft_title_content (title, content)
);
```

**document_chunks 表**（文档块表）

```sql
CREATE TABLE document_chunks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  vector_id VARCHAR(100),
  tokens INT,
  vector_status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING' COMMENT '向量化状态',
  retry_count INT DEFAULT 0 COMMENT '已重试次数',
  error_msg VARCHAR(500) COMMENT '失败原因',
  last_retry_at DATETIME COMMENT '最后一次重试时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  KEY idx_document_id (document_id),
  KEY idx_vector_id (vector_id),
  KEY idx_vector_status (vector_status),
  FOREIGN KEY (document_id) REFERENCES documents(id)
);
-- 注意：向量数据统一存储在 Qdrant，不在此表冗余存储
```

**questions 表**（问题表）

```sql
CREATE TABLE questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  rewritten_content TEXT,
  status ENUM('PENDING', 'ANSWERED', 'FAILED', 'ARCHIVED') DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  KEY idx_user_id (user_id),
  KEY idx_status (status),
  KEY idx_created_at (created_at)
);
```

**answers 表**（答案表）

```sql
CREATE TABLE answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  content LONGTEXT NOT NULL,
  source_chunks JSON,
  model VARCHAR(100),
  prompt_tokens INT,
  completion_tokens INT,
  langfuse_trace_id VARCHAR(200),
  rating INT,
  feedback TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_question_id (question_id),
  KEY idx_langfuse_trace_id (langfuse_trace_id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
);
```

## 7. Qdrant 向量数据库配置
### 7.1 集合设计策略
#### 7.1.1 单集合的问题
早期设计使用单一集合 `duke-knowledge-qa` 存储所有文档，在业务规模增长后会暴露以下问题：

+ **检索性能下降**：百万级向量后 ANN 搜索耗时增加，payload filter 需全量扫描后过滤，效率低
+ **隔离性差**：不同业务数据混用一个集合，只能靠 payload filter 隔离，无法物理分离
+ **运维困难**：无法对单个业务单独做索引重建、备份、参数调优
+ **参数冲突**：`ef_search`、量化策略等参数调整会影响全部业务

#### 7.1.2 生产集合设计：按业务动态创建
**集合命名规则**：`{env}-knowledge-qa-{kbCode}`

```plain
# 本地开发
dev-knowledge-qa-tech        # 技术部知识库
dev-knowledge-qa-hr          # HR 知识库

# 生产环境
prod-knowledge-qa-tech       # 技术部知识库
prod-knowledge-qa-hr         # HR 知识库
prod-knowledge-qa-product    # 产品部知识库
prod-knowledge-qa-public     # 公共知识库
```

+ 每个知识库对应一个独立的 Qdrant 集合，完全物理隔离
+ `kbCode` 对应 `knowledge_bases.code` 字段（如 `tech`、`hr`）
+ 集合在知识库创建时自动初始化，知识库删除时销毁
+ 不同集合可独立配置向量参数、HNSW 参数、量化策略

#### 7.1.3 集合参数配置
**标准集合配置**（通用）：

```json
{
  "vectors": {
    "size": 4096,
    "distance": "Cosine"
  },
  "hnsw_config": {
    "m": 16,
    "ef_construct": 100
  },
  "payload_schema": {
    "document_id": { "type": "integer" },
    "chunk_index":  { "type": "integer" },
    "content":      { "type": "text"    },
    "category":     { "type": "keyword" },
    "tags":         { "type": "keyword" },
    "created_at":   { "type": "integer" }
  }
}
```

**大数据量集合**（向量数超过 50 万时启用量化）：

```json
{
  "vectors": {
    "size": 4096,
    "distance": "Cosine"
  },
  "hnsw_config": {
    "m": 32,
    "ef_construct": 200
  },
  "quantization_config": {
    "scalar": {
      "type": "int8",
      "quantile": 0.99,
      "always_ram": true
    }
  }
}
```

> `int8` 量化将每个维度从 float32（4 字节）压缩至 int8（1 字节），内存占用降低 **75%**，检索速度提升，精度损失约 1~2%。
>

### 7.2 集合动态管理
`knowledge_bases`** 表新增集合名字段**：

```sql
ALTER TABLE knowledge_bases
    ADD COLUMN qdrant_collection VARCHAR(100) COMMENT 'Qdrant 集合名，如 prod-knowledge-qa-tech';
```

**QdrantCollectionService**：

```java
@Service
public class QdrantCollectionService {

    @Value("${app.env:dev}")
    private String env;

    private static final String PREFIX = "%s-knowledge-qa-";

    /**
     * 知识库创建时调用，初始化对应集合
     */
    public String createCollection(String kbCode) {
        String collectionName = getCollectionName(kbCode);

        qdrantClient.createCollectionAsync(collectionName,
            VectorParams.newBuilder()
                .setSize(4096)
                .setDistance(Distance.Cosine)
                .build()
        ).get();

        // 创建 payload 索引，加速 filter 查询
        qdrantClient.createPayloadIndexAsync(
            collectionName, "document_id", PayloadSchemaType.Integer, null).get();
        qdrantClient.createPayloadIndexAsync(
            collectionName, "category", PayloadSchemaType.Keyword, null).get();
        qdrantClient.createPayloadIndexAsync(
            collectionName, "tags", PayloadSchemaType.Keyword, null).get();

        return collectionName;
    }

    /**
     * 知识库删除时调用，销毁集合及全部向量
     */
    public void deleteCollection(String kbCode) {
        qdrantClient.deleteCollectionAsync(getCollectionName(kbCode)).get();
    }

    /**
     * 对大数据量集合启用 int8 量化
     */
    public void enableQuantization(String kbCode) {
        qdrantClient.updateCollectionAsync(
            getCollectionName(kbCode),
            null,
            HnswConfigDiff.newBuilder().setM(32).setEfConstruct(200).build(),
            ScalarQuantization.newBuilder()
                .setType(QuantizationType.Int8)
                .setQuantile(0.99f)
                .setAlwaysRam(true)
                .build(),
            null, null
        ).get();
    }

    /**
     * 统一入口：根据 kbCode 获取集合名，所有向量操作通过此方法获取，禁止硬编码
     */
    public String getCollectionName(String kbCode) {
        return String.format(PREFIX, env) + kbCode;
    }
}
```

### 7.3 向量操作
所有向量操作不再硬编码集合名，统一通过 `QdrantCollectionService.getCollectionName(kbCode)` 获取：

**插入向量**：

```plain
POST /collections/{env}-knowledge-qa-{kbCode}/points

{
  "points": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "vector": [0.1, 0.2, ..., 0.4096],
      "payload": {
        "document_id": 1,
        "chunk_index": 0,
        "content": "Spring Boot 是一个基于 Spring 框架的快速开发工具，...",
        "category": "技术文档",
        "tags": ["Java", "Spring"],
        "created_at": 1714464000
      }
    }
  ]
}
```

**搜索向量**：

```plain
POST /collections/{env}-knowledge-qa-{kbCode}/points/search

{
  "vector": [0.1, 0.2, ..., 0.4096],
  "limit": 10,
  "score_threshold": 0.5,
  "filter": {
    "must": [
      {
        "key": "category",
        "match": { "value": "技术文档" }
      }
    ]
  }
}
```

> 不再需要在 filter 中加 `kb_id` 做隔离，集合本身即是隔离边界，搜索效率更高。
>

### 7.4 集合生命周期管理
```plain
创建知识库
  → createCollection(kbCode)       初始化集合 + payload 索引
  → knowledge_bases.qdrant_collection 写入集合名

文档上传
  → getCollectionName(kbCode)      获取集合名
  → upsert points                  写入向量

数据量达 50 万+
  → enableQuantization(kbCode)     启用 int8 量化，节省内存

删除知识库
  → deleteCollection(kbCode)       销毁集合，释放存储
```

## 8. 缓存策略
### 8.1 Redis 缓存设计
**缓存键设计**：

```plain
# 文档缓存
doc:{documentId}                    # 文档详情
doc:list:{category}:{page}          # 文档列表
doc:chunks:{documentId}             # 文档块列表

# 搜索缓存
search:{queryHash}:{topK}           # 搜索结果
search:hybrid:{queryHash}:{weights} # 混合搜索结果

# 问答缓存
question:{questionId}               # 问题详情
answer:{answerId}                   # 答案详情

# 向量缓存
vector:{documentId}:{chunkIndex}    # 文档块向量
```

**缓存过期时间**：

+ 文档详情：1 小时
+ 文档列表：30 分钟
+ 搜索结果：1 小时
+ 问答数据：2 小时
+ 向量缓存：24 小时

### 8.2 缓存更新策略
**主动更新**：

+ 文档上传/更新时，清除相关缓存
+ 答案反馈时，更新答案缓存

**被动更新**：

+ 缓存过期自动删除
+ 定期清理过期缓存

## 9. 安全性设计
### 9.1 认证和授权
**认证**：

+ 使用 JWT Token（由 duke-auth 服务签发）
+ 网关验证 Token 签名和过期时间
+ 服务级别验证 Token Claims

**授权**：

+ 基于角色的访问控制（RBAC）
+ 文档访问权限：
    - 管理员：可上传、编辑、删除所有文档
    - 普通用户：只能查看已发布文档
    - 文档所有者：可编辑自己的文档

**权限检查**：

```plain
@PreAuthorize("hasRole('ADMIN') or @documentService.isOwner(#documentId, authentication.principal.id)")
public void deleteDocument(Long documentId) {
  // 删除文档
}
```

### 9.2 数据安全
**敏感信息保护**：

+ 不存储用户密码（由 duke-auth 管理）
+ 不存储 API Key（使用环境变量）
+ 文档内容加密存储（可选）

**访问日志**：

+ 记录所有文档访问
+ 记录问答操作
+ 用于审计和安全分析

### 9.3 API 安全
**速率限制**：

+ 每个用户每分钟最多 100 个请求（在 Spring Cloud Gateway 层用 Redis 令牌桶实现）
+ 文档上传限制：每个用户每天最多 50 个

**输入验证**：

+ 文件类型验证
+ 文件大小限制（最大 100MB）
+ 文本长度限制

**CORS 配置**：

+ 只允许来自前端域名的请求
+ 支持跨域 Cookie

## 10. 性能优化
### 10.1 向量搜索优化
**Qdrant 优化**：

+ 使用 HNSW 索引算法（默认）
+ 配置合理的 ef_construct 和 ef_search 参数
+ 定期重建索引以优化性能
+ 使用向量量化（Quantization）减少内存占用

**查询优化**：

+ 使用 payload 过滤减少搜索范围
+ 限制返回结果数量（topK）
+ 使用缓存避免重复搜索

### 10.2 数据库优化
**索引策略**：

+ 在 category、status、created_at 等常用字段建立索引
+ 为 document_id、question_id 建立外键索引
+ 使用全文索引加速关键词搜索

**查询优化**：

+ 使用分页避免一次加载大量数据
+ 使用连接池管理数据库连接
+ 定期分析慢查询日志

### 10.3 缓存优化
**多层缓存**：

+ L1：本地内存缓存（Caffeine）
+ L2：Redis 分布式缓存
+ L3：Qdrant 向量缓存

**缓存预热**：

+ 应用启动时预热热点数据
+ 定期更新缓存

## 11. 监控和日志
### 11.1 关键指标
**业务指标**：

+ 文档上传数量和大小
+ 问题提交数量
+ 答案生成时间
+ 用户反馈评分

**系统指标**：

+ API 响应时间（P50、P95、P99）
+ 错误率
+ QPS（每秒查询数）
+ 缓存命中率

**向量库指标**：

+ 向量搜索延迟
+ 索引大小
+ 内存占用

### 11.2 日志设计
**日志级别**：

+ ERROR：系统错误、异常
+ WARN：潜在问题、性能警告
+ INFO：关键业务事件（文档上传、问答等）
+ DEBUG：详细调试信息

**日志内容**：

```plain
[时间戳] [级别] [服务名] [请求ID] [用户ID] [操作] [结果] [耗时]

示例：
2026-04-30 10:00:00 INFO duke-knowledge-qa req-123 user-456 upload-document success 1234ms
2026-04-30 10:00:01 INFO duke-knowledge-qa req-124 user-456 ask-question success 567ms
2026-04-30 10:00:02 ERROR duke-knowledge-qa req-125 user-789 search-documents failed 100ms
```

### 11.3 告警规则
**关键告警**：

+ API 错误率 > 1%
+ 平均响应时间 > 1000ms
+ Qdrant 连接失败
+ MySQL 连接池耗尽
+ Redis 连接失败

### 11.3 告警规则
**关键告警**：

+ API 错误率 > 1%
+ 平均响应时间 > 1000ms
+ Qdrant 连接失败
+ MySQL 连接池耗尽
+ Redis 连接失败

## 12. LangFuse 集成
### 12.1 LangFuse 简介
LangFuse 是专为 LLM 应用设计的可观测性平台，提供：

+ **Trace 追踪**：完整记录每次问答的全链路调用（查询改写 → 检索 → Rerank → LLM 生成）
+ **Prompt 管理**：集中版本化管理系统 Prompt，支持 A/B 测试
+ **评估分析**：结合用户评分数据，分析各模型/Prompt 版本的效果
+ **成本统计**：按 token 统计 LLM 调用成本

### 12.2 本地部署（Docker）
在 `docker-compose.yml` 中添加 LangFuse 服务：

```yaml
langfuse-server:
  image: langfuse/langfuse:latest
  environment:
    DATABASE_URL: postgresql://langfuse:langfuse@langfuse-db:5432/langfuse
    NEXTAUTH_SECRET: ${LANGFUSE_SECRET:-your-secret-key}
    NEXTAUTH_URL: http://localhost:3000
    SALT: ${LANGFUSE_SALT:-your-salt}
  ports:
    - "3000:3000"
  depends_on:
    - langfuse-db

langfuse-db:
  image: postgres:15
  environment:
    POSTGRES_USER: langfuse
    POSTGRES_PASSWORD: langfuse
    POSTGRES_DB: langfuse
  volumes:
    - langfuse_db_data:/var/lib/postgresql/data

volumes:
  langfuse_db_data:
```

启动后访问 `http://localhost:3000`，注册账号并创建项目，获取 `Public Key` 和 `Secret Key`。

### 12.3 后端 SDK 集成
**Maven 依赖**：

```xml
<dependency>
  <groupId>com.langfuse</groupId>
  <artifactId>langfuse-java</artifactId>
  <version>1.x.x</version>
</dependency>

```

**环境变量配置**：

```bash
LANGFUSE_PUBLIC_KEY=pk-lf-xxxxxxxx
LANGFUSE_SECRET_KEY=sk-lf-xxxxxxxx
LANGFUSE_HOST=http://localhost:3000
```

**Spring 配置（application.yml）**：

```yaml
langfuse:
  public-key: ${LANGFUSE_PUBLIC_KEY}
  secret-key: ${LANGFUSE_SECRET_KEY}
  host: ${LANGFUSE_HOST:http://localhost:3000}
  enabled: true
```

### 12.4 问答链路追踪示例
每次问答创建一个 Trace，链路内各步骤作为 Span：

```java
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private LangfuseClient langfuse;

    public Flux<String> askQuestion(String content) {
        // 创建顶层 Trace
        Trace trace = langfuse.trace(TraceRequest.builder()
            .name("qa-pipeline")
            .userId(currentUserId())
            .input(content)
            .build());

        // Span 1：查询改写
        Span rewriteSpan = trace.span(SpanRequest.builder()
            .name("query_rewrite")
            .input(content)
            .build());
        String rewrittenQuery = queryRewriteService.rewrite(content);
        rewriteSpan.end(SpanEndRequest.builder().output(rewrittenQuery).build());

        // Span 2：向量检索
        Span retrieveSpan = trace.span(SpanRequest.builder()
            .name("vector_search")
            .input(rewrittenQuery)
            .build());
        List<DocumentChunk> candidates = searchService.vectorSearch(rewrittenQuery, 10);
        retrieveSpan.end(SpanEndRequest.builder()
            .output(candidates.size() + " chunks retrieved").build());

        // Span 3：Rerank
        Span rerankSpan = trace.span(SpanRequest.builder()
            .name("rerank")
            .input(candidates)
            .build());
        List<DocumentChunk> reranked = rerankService.rerank(rewrittenQuery, candidates, 5);
        rerankSpan.end(SpanEndRequest.builder().output(reranked).build());

        // Generation：LLM 调用
        Generation generation = trace.generation(GenerationRequest.builder()
            .name("llm_generate")
            .model(llmConfig.getModel())
            .input(buildPrompt(rewrittenQuery, reranked))
            .build());

        return llmService.streamGenerate(rewrittenQuery, reranked)
            .doOnComplete(() -> {
                generation.end(GenerationEndRequest.builder()
                    .output(fullAnswer)
                    .usage(Usage.builder()
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .build())
                    .build());
                // 保存 traceId 到答案记录，方便后台跳转查看
                answerRepository.updateTraceId(answerId, trace.getId());
            });
    }
}
```

### 12.5 用户评分回写 LangFuse
用户提交答案评分后，同步写回 LangFuse Score，便于统计各 Prompt/模型版本效果：

```java
@PostMapping("/answers/{answerId}/feedback")
public Result submitFeedback(@PathVariable Long answerId, @RequestBody FeedbackRequest req) {
    Answer answer = answerRepository.findById(answerId);
    // 更新本地评分
    answer.setRating(req.getRating());
    answer.setFeedback(req.getFeedback());
    answerRepository.save(answer);

    // 回写 LangFuse Score
    if (answer.getLangfuseTraceId() != null) {
        langfuse.score(ScoreRequest.builder()
            .traceId(answer.getLangfuseTraceId())
            .name("user_rating")
            .value(req.getRating())
            .comment(req.getFeedback())
            .build());
    }
    return Result.success();
}
```

### 12.6 Prompt 版本管理
在 LangFuse 控制台创建 Prompt，后端通过 SDK 拉取，实现无需重启服务修改 Prompt：

```java
// 从 LangFuse 拉取最新 Prompt（带本地缓存，TTL 60s）
Prompt prompt = langfuse.getPrompt("rag-system-prompt");
String systemPrompt = prompt.compile(Map.of(
    "context", formattedContext,
    "question", userQuestion
));
```

在 LangFuse 控制台的 Prompt 模板示例：

```plain
你是企业知识库智能助手，请根据以下参考文档回答用户问题。

参考文档：
{{context}}

用户问题：{{question}}

回答要求：
1. 只基于参考文档内容作答，不要编造信息
2. 如果文档中没有相关信息，请明确告知
3. 回答简洁清晰，必要时引用来源文档
```

## 13. 部署架构
### 12.1 服务部署
**开发环境**：

```plain
本地开发机
├── MySQL（本地或 Docker）
├── Redis（本地或 Docker）
├── Qdrant（本地或 Docker）
├── duke-auth（mvn spring-boot:run）
├── duke-gateway（mvn spring-boot:run）
├── duke-transformer（mvn spring-boot:run）
├── duke-knowledge-qa（mvn spring-boot:run）
├── duke-auth-web（npm run dev）
├── duke-transformer-web（npm run dev）
└── duke-knowledge-qa-web（npm run dev）
```

**生产环境**：

```plain
Kubernetes 集群
├── Namespace: duke-platform
├── Services:
│   ├── duke-auth（副本数：2）
│   ├── duke-gateway（副本数：3）
│   ├── duke-transformer（副本数：2）
│   └── duke-knowledge-qa（副本数：2）
├── StatefulSets:
│   ├── MySQL（副本数：1）
│   ├── Redis（副本数：1）
│   └── Qdrant（副本数：1）
└── ConfigMaps & Secrets:
    ├── 应用配置
    ├── 数据库凭证
    └── API Keys
```

### 12.2 环境变量配置
**duke-knowledge-qa 服务**：

```bash
# 数据库配置
DB_HOST=mysql.duke-platform.svc.cluster.local
DB_PORT=3306
DB_USERNAME=duke_user
DB_PASSWORD=<secret>

# Redis 配置
REDIS_HOST=redis.duke-platform.svc.cluster.local
REDIS_PORT=6379
REDIS_PASSWORD=<secret>

# Qdrant 配置
QDRANT_HOST=qdrant.duke-platform.svc.cluster.local
QDRANT_PORT=6333
QDRANT_API_KEY=<secret>

# LLM 配置
LLM_API_KEY=<secret>
LLM_MODEL=qwen-max
LLM_ENDPOINT=https://api.aliyun.com/v1/chat/completions

# 嵌入模型配置
EMBEDDING_MODEL=qwen3-embedding-8b
EMBEDDING_ENDPOINT=https://api.aliyun.com/v1/embeddings

# LangFuse 配置
LANGFUSE_PUBLIC_KEY=pk-lf-<your-public-key>
LANGFUSE_SECRET_KEY=sk-lf-<your-secret-key>
LANGFUSE_HOST=http://localhost:3000

# 应用配置
SERVER_PORT=8083
SERVER_SERVLET_CONTEXT_PATH=/knowledge-qa
NACOS_SERVER_ADDR=nacos.duke-platform.svc.cluster.local:8848
APP_ENV=prod   # 控制 Qdrant 集合名前缀：prod-knowledge-qa-{kbCode}
```

### 12.3 健康检查
**Liveness Probe**（存活性探针）：

```plain
GET /knowledge-qa/actuator/health/liveness
期望响应：200 OK
```

**Readiness Probe**（就绪性探针）：

```plain
GET /knowledge-qa/actuator/health/readiness
期望响应：200 OK
检查项：
- 数据库连接
- Redis 连接
- Qdrant 连接
```

## 14. 前端项目结构
### 13.1 Vue 3 项目组织
```plain
frontend/duke-knowledge-qa-web/
├── public/                     # 静态资源
├── src/
│   ├── api/                    # API 客户端函数
│   │   ├── document.ts         # 文档相关 API
│   │   ├── question.ts         # 问答相关 API
│   │   └── search.ts           # 搜索相关 API
│   ├── components/             # 可复用组件
│   │   ├── DocumentUpload.vue  # 文档上传组件
│   │   ├── QuestionForm.vue    # 问题表单组件
│   │   ├── AnswerDisplay.vue   # 答案展示组件
│   │   ├── SearchResults.vue   # 搜索结果组件
│   │   └── Feedback.vue        # 反馈组件
│   ├── layout/                 # 布局组件
│   │   ├── Header.vue          # 顶部导航
│   │   ├── Sidebar.vue         # 侧边栏
│   │   └── MainLayout.vue      # 主布局
│   ├── router/                 # 路由配置
│   │   └── index.ts            # 路由定义
│   ├── stores/                 # Pinia 状态管理
│   │   ├── documentStore.ts    # 文档状态
│   │   ├── questionStore.ts    # 问答状态
│   │   ├── searchStore.ts      # 搜索状态
│   │   └── uiStore.ts          # UI 状态
│   ├── styles/                 # 全局样式
│   │   ├── variables.scss      # SCSS 变量
│   │   └── global.scss         # 全局样式
│   ├── types/                  # TypeScript 类型定义
│   │   ├── document.ts         # 文档类型
│   │   ├── question.ts         # 问答类型
│   │   └── search.ts           # 搜索类型
│   ├── utils/                  # 工具函数
│   │   ├── request.ts          # HTTP 请求工具
│   │   ├── format.ts           # 格式化工具
│   │   └── storage.ts          # 本地存储工具
│   ├── views/                  # 页面组件
│   │   ├── DocumentPage.vue    # 文档管理页面
│   │   ├── QuestionPage.vue    # 问答页面
│   │   ├── SearchPage.vue      # 搜索页面
│   │   └── HomePage.vue        # 首页
│   ├── App.vue                 # 根组件
│   └── main.ts                 # 应用入口
├── index.html                  # HTML 模板
├── vite.config.ts              # Vite 配置
├── tsconfig.json               # TypeScript 配置
├── package.json                # 项目依赖
└── .env.example                # 环境变量示例
```

### 13.2 核心页面设计
**文档管理页面**（DocumentPage.vue）：

+ 文档上传区域（拖拽上传）
+ 文档列表（分页、搜索、过滤）
+ 文档详情预览
+ 删除确认对话框

**问答页面**（QuestionPage.vue）：

+ 问题输入框
+ 搜索建议下拉框
+ 答案展示区域
+ 来源文档链接
+ 反馈评分组件

**搜索页面**（SearchPage.vue）：

+ 搜索条件输入
+ 搜索类型选择（向量/关键词/混合）
+ 结果列表展示
+ 结果详情预览
+ 分页加载

### 13.3 API 集成
**Axios 配置**（utils/request.ts）：

```typescript
// 基础 URL 配置
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 请求拦截器：添加 Token
// 响应拦截器：处理错误

// 导出 API 实例
export const apiClient = axios.create({ baseURL })
```

**API 函数示例**（api/document.ts）：

```typescript
// 上传文档
export const uploadDocument = (file: File, title: string, category?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', title)
  if (category) formData.append('category', category)
  return apiClient.post('/api/knowledge-qa/documents/upload', formData)
}

// 获取文档列表
export const getDocuments = (page: number, pageSize: number, filters?: any) => {
  return apiClient.get('/api/knowledge-qa/documents', {
    params: { page, pageSize, ...filters }
  })
}

// 删除文档
export const deleteDocument = (documentId: number) => {
  return apiClient.delete(`/api/knowledge-qa/documents/${documentId}`)
}
```

### 13.4 状态管理示例
**问答 Store**（stores/questionStore.ts）：

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { submitQuestion, getAnswer } from '@/api/question'

export const useQuestionStore = defineStore('question', () => {
  const questions = ref<Question[]>([])
  const currentQuestion = ref<Question | null>(null)
  const currentAnswer = ref<Answer | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const askQuestion = async (content: string) => {
    loading.value = true
    error.value = null
    try {
      const response = await submitQuestion({ content })
      currentQuestion.value = response.data.data
      // 获取答案
      const answerResponse = await getAnswer(currentQuestion.value.id)
      currentAnswer.value = answerResponse.data.data
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  return {
    questions,
    currentQuestion,
    currentAnswer,
    loading,
    error,
    askQuestion
  }
})
```

## 15. 开发工作流
### 14.1 本地开发环境搭建
**前置条件**：

+ Java 21
+ Node.js 18+
+ Maven 3.8+
+ Docker（用于运行 MySQL、Redis、Qdrant）

**启动基础设施**：

```bash
# 使用 Docker Compose 启动所有依赖服务
docker-compose up -d

# 验证服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

**docker-compose.yml 示例**：

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: duke_knowledge_qa
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7.0
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"
    volumes:
      - qdrant_data:/qdrant/storage

  nacos:
    image: nacos/nacos-server:v2.2.0
    environment:
      MODE: standalone
    ports:
      - "8848:8848"

  langfuse-server:
    image: langfuse/langfuse:latest
    environment:
      DATABASE_URL: postgresql://langfuse:langfuse@langfuse-db:5432/langfuse
      NEXTAUTH_SECRET: ${LANGFUSE_SECRET:-change-me-in-production}
      NEXTAUTH_URL: http://localhost:3000
      SALT: ${LANGFUSE_SALT:-change-me-salt}
    ports:
      - "3000:3000"
    depends_on:
      - langfuse-db

  langfuse-db:
    image: postgres:15
    environment:
      POSTGRES_USER: langfuse
      POSTGRES_PASSWORD: langfuse
      POSTGRES_DB: langfuse
    volumes:
      - langfuse_db_data:/var/lib/postgresql/data

volumes:
  mysql_data:
  redis_data:
  qdrant_data:
  langfuse_db_data:
```

### 14.2 后端开发流程
**启动后端服务**：

```bash
# 进入服务目录
cd backend/duke-knowledge-qa

# 复制环境变量文件
cp .env.example .env

# 编辑 .env 文件，填入本地配置
# 构建和运行
mvn clean install
mvn spring-boot:run
```

**验证服务启动**：

```bash
# 检查服务是否注册到 Nacos
curl http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=duke-knowledge-qa

# 访问 Swagger 文档
http://localhost:8083/knowledge-qa/swagger-ui.html

# 检查健康状态
curl http://localhost:8083/knowledge-qa/actuator/health
```

### 14.3 前端开发流程
**启动前端开发服务器**：

```bash
# 进入前端目录
cd frontend/duke-knowledge-qa-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问应用
http://localhost:5173
```

**开发工具**：

+ VS Code + Volar 插件（Vue 3 支持）
+ Vue DevTools 浏览器扩展
+ TypeScript 类型检查

## 16. 测试策略
### 15.1 后端单元测试
**测试框架**：JUnit 5 + Mockito

**测试覆盖范围**：

+ Service 层业务逻辑（>80% 覆盖率）
+ Mapper 层数据库操作
+ Util 工具类

**示例测试**（DocumentServiceTest）：

```java
@SpringBootTest
class DocumentServiceTest {
  @MockBean
  private DocumentMapper documentMapper;
  
  @MockBean
  private VectorService vectorService;
  
  @Autowired
  private DocumentService documentService;
  
  @Test
  void testUploadDocument() {
    // 准备测试数据
    Document doc = new Document();
    doc.setTitle("测试文档");
    
    // 执行测试
    Document result = documentService.uploadDocument(doc);
    
    // 验证结果
    assertNotNull(result.getId());
    assertEquals("测试文档", result.getTitle());
    
    // 验证调用
    verify(vectorService).generateVectors(any());
  }
}
```

**运行测试**：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DocumentServiceTest

# 生成覆盖率报告
mvn test jacoco:report
```

### 15.2 集成测试
**测试框架**：TestContainers（Docker 容器化测试）

**测试范围**：

+ 数据库操作（真实 MySQL）
+ Redis 缓存操作
+ Qdrant 向量操作
+ API 端点集成

**示例集成测试**：

```java
@SpringBootTest
@Testcontainers
class DocumentIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
  
  @Container
  static GenericContainer<?> qdrant = new GenericContainer<>("qdrant/qdrant:latest")
    .withExposedPorts(6333);
  
  @Test
  void testUploadAndSearch() {
    // 上传文档
    Document doc = documentService.uploadDocument(createTestDocument());
    
    // 搜索文档
    List<DocumentChunk> results = searchService.search("测试查询");
    
    // 验证结果
    assertFalse(results.isEmpty());
  }
}
```

### 15.3 前端单元测试
**测试框架**：Vitest + Vue Test Utils

**测试范围**：

+ 组件逻辑和交互
+ Store 状态管理
+ API 函数

**示例组件测试**：

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import QuestionForm from '@/components/QuestionForm.vue'

describe('QuestionForm', () => {
  it('submits question on button click', async () => {
    const wrapper = mount(QuestionForm)
    
    // 输入问题
    await wrapper.find('input').setValue('如何使用 Spring Boot？')
    
    // 点击提交按钮
    await wrapper.find('button').trigger('click')
    
    // 验证提交事件
    expect(wrapper.emitted('submit')).toBeTruthy()
  })
})
```

**运行测试**：

```bash
# 运行所有测试
npm run test

# 监听模式
npm run test:watch

# 生成覆盖率报告
npm run test:coverage
```

### 15.4 E2E 测试
**测试框架**：Playwright

**测试场景**：

+ 文档上传流程
+ 问答完整流程
+ 搜索功能

**示例 E2E 测试**：

```typescript
import { test, expect } from '@playwright/test'

test('complete question-answer flow', async ({ page }) => {
  // 访问应用
  await page.goto('http://localhost:5173')
  
  // 输入问题
  await page.fill('input[placeholder="输入您的问题"]', '如何使用 Spring Boot？')
  
  // 提交问题
  await page.click('button:has-text("提交")')
  
  // 等待答案加载
  await page.waitForSelector('.answer-content')
  
  // 验证答案显示
  const answer = await page.textContent('.answer-content')
  expect(answer).toBeTruthy()
})
```

## 17. 故障排查指南
### 16.1 常见问题
**问题 1：Qdrant 连接失败**

```plain
错误信息：Failed to connect to Qdrant at localhost:6333
解决方案：
1. 检查 Qdrant 是否运行：docker ps | grep qdrant
2. 检查端口是否正确：curl http://localhost:6333/health
3. 检查网络连接：ping qdrant（如果使用 Docker Compose）
4. 重启 Qdrant：docker restart qdrant
```

**问题 2：向量搜索返回结果为空**

```plain
原因分析：
1. 文档未被正确向量化
2. 搜索阈值设置过高
3. 向量库中没有相关数据

解决方案：
1. 检查文档是否成功上传：SELECT * FROM documents WHERE id = ?
2. 检查文档块是否生成：SELECT * FROM document_chunks WHERE document_id = ?
3. 检查 Qdrant 中的向量：curl http://localhost:6333/collections/duke-knowledge-qa/points
4. 降低搜索阈值进行测试
```

**问题 3：LLM 答案生成超时**

```plain
原因分析：
1. LLM API 响应缓慢
2. 网络连接问题
3. 模型过载

解决方案：
1. 增加超时时间配置
2. 检查 LLM API 状态
3. 使用更轻量的模型进行测试
4. 实现重试机制
```

### 16.2 性能诊断
**检查数据库性能**：

```sql
-- 查看慢查询
SELECT * FROM mysql.slow_log;

-- 分析表大小
SELECT table_name, ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'duke_knowledge_qa';

-- 检查索引使用情况
SELECT * FROM performance_schema.table_io_waits_summary_by_index_usage;
```

**检查 Redis 性能**：

```bash
# 连接 Redis
redis-cli

# 查看内存使用
INFO memory

# 查看缓存命中率
INFO stats

# 监控实时命令
MONITOR
```

**检查 Qdrant 性能**：

```bash
# 查看集合统计信息
curl http://localhost:6333/collections/duke-knowledge-qa

# 查看向量数量
curl http://localhost:6333/collections/duke-knowledge-qa/points/count

# 查看索引大小
curl http://localhost:6333/collections/duke-knowledge-qa/snapshots
```

## 18. API 文档参考
### 17.1 完整 API 端点列表
**文档管理**：

+ `POST /api/knowledge-qa/documents/upload` - 上传文档
+ `GET /api/knowledge-qa/documents` - 获取文档列表
+ `GET /api/knowledge-qa/documents/{id}` - 获取文档详情
+ `DELETE /api/knowledge-qa/documents/{id}` - 删除文档
+ `POST /api/knowledge-qa/documents/{id}/retry-vectors` - 重跑指定文档失败 chunk
+ `POST /api/knowledge-qa/documents/retry-all-failed` - 全局重跑所有失败文档（管理员）

**问答**：

+ `POST /api/knowledge-qa/questions/stream` - 提交问题并获取流式答案（SSE）
+ `GET /api/knowledge-qa/questions/{id}/answer` - 获取历史答案
+ `POST /api/knowledge-qa/answers/{id}/feedback` - 提交反馈（同步写回 LangFuse Score）

**搜索**：

+ `POST /api/knowledge-qa/search` - 向量搜索
+ `POST /api/knowledge-qa/search/hybrid` - 混合搜索

**系统**：

+ `GET /knowledge-qa/actuator/health` - 健康检查
+ `GET /knowledge-qa/swagger-ui.html` - API 文档

### 17.2 错误响应格式
**统一错误响应**：

```json
{
  "code": 400,
  "message": "Bad Request",
  "data": null,
  "timestamp": "2026-04-30T10:00:00Z",
  "path": "/api/knowledge-qa/documents/upload"
}
```

**常见错误码**：

+ 200：成功
+ 400：请求参数错误
+ 401：未授权
+ 403：禁止访问
+ 404：资源不存在
+ 500：服务器内部错误
+ 503：服务不可用

## 19. 总结
### 19.1 项目亮点
1. **企业级架构**：基于微服务架构，支持高可用和水平扩展
2. **高质量 RAG 链路**：查询改写 → 混合检索（RRF 融合）→ Rerank 精排 → 流式 LLM 生成，覆盖完整增强检索最佳实践
3. **流式问答体验**：基于 SSE 实时推送生成内容，无需等待全量结果
4. **向量搜索**：集成 Qdrant 向量数据库，使用 qwen3-embedding-8b（4096 维）支持高效语义搜索
5. **LangFuse 可观测性**：全链路追踪 LLM 调用，Prompt 版本管理，用户评分回写，成本统计
6. **多层缓存**：本地缓存 + Redis 分布式缓存，性能优化
7. **完整的安全体系**：认证、授权、数据加密、审计日志

### 19.2 实现路线图
**第一阶段（MVP）**：

+ ✅ 文档上传和管理
+ ✅ 向量化处理
+ ✅ 基础搜索功能
+ ✅ 简单问答

**第二阶段（增强）**：

+ 流式问答（SSE）
+ 查询改写
+ Rerank 精排
+ 混合搜索（向量 + 关键词，RRF 融合）
+ LangFuse 集成（追踪、评估、Prompt 管理）
+ 用户反馈和评分

**第三阶段（高级）**：

+ 多模型支持
+ 知识图谱集成
+ 个性化推荐
+ 实时协作编辑

### 19.3 关键配置清单
部署前检查：

- [ ] MySQL 数据库已创建，初始化脚本已执行
- [ ] Redis 已启动并配置密码
- [ ] Qdrant 已启动，集合已创建（向量维度设为 4096）
- [ ] Nacos 已启动，服务注册正常
- [ ] LangFuse 已启动（`http://localhost:3000`），项目和 API Key 已创建
- [ ] 环境变量已配置（.env 文件，含 LANGFUSE_PUBLIC_KEY / LANGFUSE_SECRET_KEY）
- [ ] JWT 密钥已生成
- [ ] LLM API Key 已配置
- [ ] 嵌入模型 API Key 已配置
- [ ] CORS 跨域配置已设置
- [ ] 文件存储路径已创建

### 19.4 后续优化方向
1. **性能优化**：
    - 实现向量量化减少内存占用
    - 优化数据库查询性能
    - 实现更智能的缓存策略
2. **功能扩展**：
    - 支持更多文档格式
    - 实现文档版本控制
    - 支持多语言问答
3. **用户体验**：
    - 实现实时搜索建议
    - 支持问答历史管理
    - 个性化推荐
4. **运维支持**：
    - 完善的监控告警
    - 自动化部署流程
    - 灾难恢复方案

---

**文档版本**：1.1  
**最后更新**：2026 年 5 月 5 日  
**作者**：Duke 平台团队
