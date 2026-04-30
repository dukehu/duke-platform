# 企业级私有知识库问答系统设计文档

## 1. 系统概述

### 1.1 项目背景
Duke 平台知识库问答系统是一个企业级私有知识库解决方案，用于构建组织内部的智能问答系统。系统支持文档上传、向量化存储、语义搜索和智能问答等核心功能。

### 1.2 核心目标
- 提供企业级私有知识库管理能力
- 支持多种文档格式的上传和处理
- 实现高效的语义搜索和问答
- 保证数据安全和隐私性
- 提供友好的用户交互界面

### 1.3 技术选型
- **向量数据库**：Qdrant（高性能向量搜索）
- **嵌入模型**：qwen3-embedding-8b（阿里通义千问嵌入模型）
- **后端框架**：Spring Boot 3.2.5 + Spring Cloud
- **前端框架**：Vue 3.5 + TypeScript + Vite
- **数据库**：MySQL（元数据存储）+ Redis（缓存）

### 1.4 系统架构图
```
┌─────────────────────────────────────────────────────────────┐
│                    前端应用层                                 │
│         duke-knowledge-qa-web (Vue 3 + TypeScript)          │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP/REST
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
│  │ • 向量化处理模块                                       │  │
│  │ • 问答引擎模块                                         │  │
│  │ • 搜索模块                                             │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──┐  ┌──────▼──┐  ┌─────▼──────┐
│  MySQL   │  │  Redis  │  │  Qdrant    │
│ (元数据) │  │ (缓存)  │  │ (向量库)   │
└──────────┘  └─────────┘  └────────────┘
```

## 2. 系统功能模块

### 2.1 文档管理模块
**职责**：处理知识库文档的上传、存储、更新和删除

**主要功能**：
- 支持多种文档格式（PDF、Word、TXT、Markdown 等）
- 文档分类和标签管理
- 文档版本控制
- 文档元数据管理（标题、作者、创建时间等）
- 文档预览和下载

**数据模型**：
```
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

### 2.2 向量化处理模块
**职责**：将文档内容转换为向量表示并存储到 Qdrant

**主要功能**：
- 文档分块处理（Chunking）
- 调用 qwen3-embedding-8b 模型生成向量
- 向量存储到 Qdrant
- 向量更新和删除
- 向量缓存管理

**处理流程**：
```
文档上传 → 文本提取 → 分块处理 → 向量生成 → Qdrant 存储 → 元数据保存
```

**分块策略**：
- 块大小：512 tokens（约 2000 字符）
- 重叠：128 tokens（约 500 字符）
- 保留原文档 ID 和块序号用于追溯

**数据模型**：
```
DocumentChunk (文档块表)
├── id: Long (主键)
├── documentId: Long (关联文档 ID)
├── chunkIndex: Integer (块序号)
├── content: Text (块内容)
├── vectorId: String (Qdrant 中的向量 ID)
├── embedding: Vector (向量表示，冗余存储用于缓存)
├── tokens: Integer (token 数量)
├── createdAt: DateTime (创建时间)
└── deletedAt: DateTime (删除时间，逻辑删除)
```

### 2.3 问答引擎模块
**职责**：处理用户问题，调用 LLM 生成答案

**主要功能**：
- 问题理解和预处理
- 调用 LLM 生成答案（支持多种模型）
- 答案生成历史记录
- 用户反馈收集（答案质量评分）
- 答案缓存管理

**问答流程**：
```
用户提问 → 问题预处理 → 向量搜索 → 检索相关文档 → LLM 生成答案 → 答案后处理 → 返回用户
```

**数据模型**：
```
Question (问题表)
├── id: Long (主键)
├── userId: Long (提问用户 ID)
├── content: String (问题内容)
├── embedding: Vector (问题向量)
├── status: Enum (状态：PENDING/ANSWERED/ARCHIVED)
├── createdAt: DateTime (创建时间)
└── deletedAt: DateTime (删除时间，逻辑删除)

Answer (答案表)
├── id: Long (主键)
├── questionId: Long (关联问题 ID)
├── content: Text (答案内容)
├── sourceChunks: String (来源块 ID 列表，JSON 格式)
├── model: String (使用的 LLM 模型)
├── rating: Integer (用户评分：1-5)
├── feedback: String (用户反馈)
├── createdAt: DateTime (创建时间)
└── updatedAt: DateTime (更新时间)
```

### 2.4 搜索模块
**职责**：实现高效的语义搜索和混合搜索

**主要功能**：
- 向量相似度搜索（Qdrant）
- 关键词搜索（MySQL 全文搜索）
- 混合搜索（向量 + 关键词）
- 搜索结果排序和过滤
- 搜索历史记录

**搜索策略**：
- **向量搜索**：使用 Qdrant 的 ANN 算法，返回 Top-K 相似结果
- **关键词搜索**：使用 MySQL 全文索引，支持布尔查询
- **混合搜索**：结合向量和关键词搜索结果，使用加权融合算法

**搜索配置**：
```
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
```
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
```
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
```
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
```
DELETE /api/knowledge-qa/documents/{documentId}

响应：
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 3.2 问答 API

**提交问题**
```
POST /api/knowledge-qa/questions

请求体：
{
  "content": "如何使用 Spring Boot？",
  "topK": 10,
  "scoreThreshold": 0.5
}

响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "questionId": 1,
    "content": "如何使用 Spring Boot？",
    "status": "PENDING",
    "createdAt": "2026-04-30T10:00:00Z"
  }
}
```

**获取答案**
```
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
    "rating": null,
    "createdAt": "2026-04-30T10:00:00Z"
  }
}
```

**提交答案反馈**
```
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
```
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
```
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
- 文档管理页面：上传、浏览、删除文档
- 问答页面：提交问题、查看答案、反馈评分
- 搜索页面：语义搜索、混合搜索、结果展示
- 知识库首页：统计信息、热门文档、最近问题

### 4.2 核心组件

**文档上传组件**
- 支持拖拽上传
- 显示上传进度
- 支持批量上传
- 文件类型验证

**问答组件**
- 问题输入框（支持多行）
- 实时搜索建议
- 答案展示（支持 Markdown）
- 来源文档链接
- 反馈评分组件

**搜索结果组件**
- 结果列表展示
- 相关度评分显示
- 文档预览
- 分页加载

### 4.3 状态管理（Pinia）

**Store 结构**：
```
stores/
├── documentStore.ts      # 文档管理状态
├── questionStore.ts      # 问答状态
├── searchStore.ts        # 搜索状态
└── uiStore.ts           # UI 状态（加载、错误等）
```

## 5. 后端项目结构

### 5.1 Maven 模块组织

```
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
- 上传文档（支持多种格式）
- 提取文本内容
- 分块处理
- 调用向量化服务
- 保存元数据

**VectorService**：向量化处理业务逻辑
- 调用 qwen3-embedding-8b 模型
- 生成文档块向量
- 存储到 Qdrant
- 向量更新和删除
- 缓存管理

**SearchService**：搜索业务逻辑
- 向量相似度搜索（Qdrant）
- 关键词搜索（MySQL）
- 混合搜索
- 结果排序和过滤

**QuestionService**：问答业务逻辑
- 问题保存
- 调用 LLM 生成答案
- 答案保存
- 反馈收集

### 5.3 外部服务集成

**Qdrant 集成**：
- 依赖：`qdrant-client`
- 配置：Qdrant 服务地址、API Key
- 操作：创建集合、插入向量、搜索、删除

**LLM 集成**：
- 支持多种模型（阿里通义千问、OpenAI 等）
- 配置：模型 API Key、端点
- 调用：生成答案、流式输出

**文件存储**：
- 本地存储或 OSS（阿里云对象存储）
- 配置：存储路径、访问权限

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
  status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
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
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  KEY idx_document_id (document_id),
  KEY idx_vector_id (vector_id),
  FOREIGN KEY (document_id) REFERENCES documents(id)
);
```

**questions 表**（问题表）
```sql
CREATE TABLE questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  status ENUM('PENDING', 'ANSWERED', 'ARCHIVED') DEFAULT 'PENDING',
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
  rating INT,
  feedback TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_question_id (question_id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
);
```

## 7. Qdrant 向量数据库配置

### 7.1 集合设计

**集合名称**：`duke-knowledge-qa`

**向量配置**：
```
{
  "name": "duke-knowledge-qa",
  "vectors": {
    "size": 768,              # qwen3-embedding-8b 输出维度
    "distance": "Cosine"      # 相似度计算方式
  },
  "payload_schema": {
    "document_id": {
      "type": "integer"       # 文档 ID
    },
    "chunk_index": {
      "type": "integer"       # 块序号
    },
    "category": {
      "type": "keyword"       # 文档分类
    },
    "tags": {
      "type": "keyword"       # 文档标签
    },
    "created_at": {
      "type": "integer"       # 创建时间戳
    }
  }
}
```

### 7.2 向量操作

**插入向量**：
```
POST /collections/duke-knowledge-qa/points

{
  "points": [
    {
      "id": "chunk_1_0",
      "vector": [0.1, 0.2, ..., 0.768],
      "payload": {
        "document_id": 1,
        "chunk_index": 0,
        "category": "技术文档",
        "tags": ["Java", "Spring"],
        "created_at": 1714464000
      }
    }
  ]
}
```

**搜索向量**：
```
POST /collections/duke-knowledge-qa/points/search

{
  "vector": [0.1, 0.2, ..., 0.768],
  "limit": 10,
  "score_threshold": 0.5,
  "filter": {
    "must": [
      {
        "key": "category",
        "match": {
          "value": "技术文档"
        }
      }
    ]
  }
}
```

## 8. 缓存策略

### 8.1 Redis 缓存设计

**缓存键设计**：
```
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
- 文档详情：1 小时
- 文档列表：30 分钟
- 搜索结果：1 小时
- 问答数据：2 小时
- 向量缓存：24 小时

### 8.2 缓存更新策略

**主动更新**：
- 文档上传/更新时，清除相关缓存
- 答案反馈时，更新答案缓存

**被动更新**：
- 缓存过期自动删除
- 定期清理过期缓存

## 9. 安全性设计

### 9.1 认证和授权

**认证**：
- 使用 JWT Token（由 duke-auth 服务签发）
- 网关验证 Token 签名和过期时间
- 服务级别验证 Token Claims

**授权**：
- 基于角色的访问控制（RBAC）
- 文档访问权限：
  - 管理员：可上传、编辑、删除所有文档
  - 普通用户：只能查看已发布文档
  - 文档所有者：可编辑自己的文档

**权限检查**：
```
@PreAuthorize("hasRole('ADMIN') or @documentService.isOwner(#documentId, authentication.principal.id)")
public void deleteDocument(Long documentId) {
  // 删除文档
}
```

### 9.2 数据安全

**敏感信息保护**：
- 不存储用户密码（由 duke-auth 管理）
- 不存储 API Key（使用环境变量）
- 文档内容加密存储（可选）

**访问日志**：
- 记录所有文档访问
- 记录问答操作
- 用于审计和安全分析

### 9.3 API 安全

**速率限制**：
- 每个用户每分钟最多 100 个请求
- 文档上传限制：每个用户每天最多 50 个

**输入验证**：
- 文件类型验证
- 文件大小限制（最大 100MB）
- 文本长度限制

**CORS 配置**：
- 只允许来自前端域名的请求
- 支持跨域 Cookie

## 10. 性能优化

### 10.1 向量搜索优化

**Qdrant 优化**：
- 使用 HNSW 索引算法（默认）
- 配置合理的 ef_construct 和 ef_search 参数
- 定期重建索引以优化性能
- 使用向量量化（Quantization）减少内存占用

**查询优化**：
- 使用 payload 过滤减少搜索范围
- 限制返回结果数量（topK）
- 使用缓存避免重复搜索

### 10.2 数据库优化

**索引策略**：
- 在 category、status、created_at 等常用字段建立索引
- 为 document_id、question_id 建立外键索引
- 使用全文索引加速关键词搜索

**查询优化**：
- 使用分页避免一次加载大量数据
- 使用连接池管理数据库连接
- 定期分析慢查询日志

### 10.3 缓存优化

**多层缓存**：
- L1：本地内存缓存（Caffeine）
- L2：Redis 分布式缓存
- L3：Qdrant 向量缓存

**缓存预热**：
- 应用启动时预热热点数据
- 定期更新缓存

## 11. 监控和日志

### 11.1 关键指标

**业务指标**：
- 文档上传数量和大小
- 问题提交数量
- 答案生成时间
- 用户反馈评分

**系统指标**：
- API 响应时间（P50、P95、P99）
- 错误率
- QPS（每秒查询数）
- 缓存命中率

**向量库指标**：
- 向量搜索延迟
- 索引大小
- 内存占用

### 11.2 日志设计

**日志级别**：
- ERROR：系统错误、异常
- WARN：潜在问题、性能警告
- INFO：关键业务事件（文档上传、问答等）
- DEBUG：详细调试信息

**日志内容**：
```
[时间戳] [级别] [服务名] [请求ID] [用户ID] [操作] [结果] [耗时]

示例：
2026-04-30 10:00:00 INFO duke-knowledge-qa req-123 user-456 upload-document success 1234ms
2026-04-30 10:00:01 INFO duke-knowledge-qa req-124 user-456 ask-question success 567ms
2026-04-30 10:00:02 ERROR duke-knowledge-qa req-125 user-789 search-documents failed 100ms
```

### 11.3 告警规则

**关键告警**：
- API 错误率 > 1%
- 平均响应时间 > 1000ms
- Qdrant 连接失败
- MySQL 连接池耗尽
- Redis 连接失败

## 12. 部署架构

### 12.1 服务部署

**开发环境**：
```
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
```
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

# 应用配置
SERVER_PORT=8083
SERVER_SERVLET_CONTEXT_PATH=/knowledge-qa
NACOS_SERVER_ADDR=nacos.duke-platform.svc.cluster.local:8848
```

### 12.3 健康检查

**Liveness Probe**（存活性探针）：
```
GET /knowledge-qa/actuator/health/liveness
期望响应：200 OK
```

**Readiness Probe**（就绪性探针）：
```
GET /knowledge-qa/actuator/health/readiness
期望响应：200 OK
检查项：
- 数据库连接
- Redis 连接
- Qdrant 连接
```

## 13. 前端项目结构

### 13.1 Vue 3 项目组织

```
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
- 文档上传区域（拖拽上传）
- 文档列表（分页、搜索、过滤）
- 文档详情预览
- 删除确认对话框

**问答页面**（QuestionPage.vue）：
- 问题输入框
- 搜索建议下拉框
- 答案展示区域
- 来源文档链接
- 反馈评分组件

**搜索页面**（SearchPage.vue）：
- 搜索条件输入
- 搜索类型选择（向量/关键词/混合）
- 结果列表展示
- 结果详情预览
- 分页加载

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

## 14. 开发工作流

### 14.1 本地开发环境搭建

**前置条件**：
- Java 21
- Node.js 18+
- Maven 3.8+
- Docker（用于运行 MySQL、Redis、Qdrant）

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

volumes:
  mysql_data:
  redis_data:
  qdrant_data:
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
- VS Code + Volar 插件（Vue 3 支持）
- Vue DevTools 浏览器扩展
- TypeScript 类型检查

## 15. 测试策略

### 15.1 后端单元测试

**测试框架**：JUnit 5 + Mockito

**测试覆盖范围**：
- Service 层业务逻辑（>80% 覆盖率）
- Mapper 层数据库操作
- Util 工具类

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
- 数据库操作（真实 MySQL）
- Redis 缓存操作
- Qdrant 向量操作
- API 端点集成

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
- 组件逻辑和交互
- Store 状态管理
- API 函数

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
- 文档上传流程
- 问答完整流程
- 搜索功能

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

## 16. 故障排查指南

### 16.1 常见问题

**问题 1：Qdrant 连接失败**
```
错误信息：Failed to connect to Qdrant at localhost:6333
解决方案：
1. 检查 Qdrant 是否运行：docker ps | grep qdrant
2. 检查端口是否正确：curl http://localhost:6333/health
3. 检查网络连接：ping qdrant（如果使用 Docker Compose）
4. 重启 Qdrant：docker restart qdrant
```

**问题 2：向量搜索返回结果为空**
```
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
```
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

## 17. API 文档参考

### 17.1 完整 API 端点列表

**文档管理**：
- `POST /api/knowledge-qa/documents/upload` - 上传文档
- `GET /api/knowledge-qa/documents` - 获取文档列表
- `GET /api/knowledge-qa/documents/{id}` - 获取文档详情
- `DELETE /api/knowledge-qa/documents/{id}` - 删除文档

**问答**：
- `POST /api/knowledge-qa/questions` - 提交问题
- `GET /api/knowledge-qa/questions/{id}/answer` - 获取答案
- `POST /api/knowledge-qa/answers/{id}/feedback` - 提交反馈

**搜索**：
- `POST /api/knowledge-qa/search` - 向量搜索
- `POST /api/knowledge-qa/search/hybrid` - 混合搜索

**系统**：
- `GET /knowledge-qa/actuator/health` - 健康检查
- `GET /knowledge-qa/swagger-ui.html` - API 文档

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
- 200：成功
- 400：请求参数错误
- 401：未授权
- 403：禁止访问
- 404：资源不存在
- 500：服务器内部错误
- 503：服务不可用

## 18. 总结

### 18.1 项目亮点

1. **企业级架构**：基于微服务架构，支持高可用和水平扩展
2. **向量搜索**：集成 Qdrant 向量数据库，支持高效的语义搜索
3. **智能问答**：集成 LLM，支持自然语言问答
4. **多层缓存**：本地缓存 + Redis 分布式缓存，性能优化
5. **完整的安全体系**：认证、授权、数据加密、审计日志
6. **可观测性**：完善的监控、日志、告警机制

### 18.2 实现路线图

**第一阶段（MVP）**：
- ✅ 文档上传和管理
- ✅ 向量化处理
- ✅ 基础搜索功能
- ✅ 简单问答

**第二阶段（增强）**：
- 混合搜索（向量 + 关键词）
- 用户反馈和评分
- 缓存优化
- 性能监控

**第三阶段（高级）**：
- 多模型支持
- 知识图谱集成
- 个性化推荐
- 实时协作编辑

### 18.3 关键配置清单

部署前检查：
- [ ] MySQL 数据库已创建，初始化脚本已执行
- [ ] Redis 已启动并配置密码
- [ ] Qdrant 已启动，集合已创建
- [ ] Nacos 已启动，服务注册正常
- [ ] 环境变量已配置（.env 文件）
- [ ] JWT 密钥已生成
- [ ] LLM API Key 已配置
- [ ] 嵌入模型 API Key 已配置
- [ ] CORS 跨域配置已设置
- [ ] 文件存储路径已创建

### 18.4 后续优化方向

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

**文档版本**：1.0  
**最后更新**：2026 年 4 月 30 日  
**作者**：Duke 平台团队

