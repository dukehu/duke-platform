# Duke Storage - 文件存储服务

## 项目简介

Duke Storage 是一个独立的文件微服务，提供文件上传、下载、预览、秒传、断点续传等功能。

## 核心特性

- ✅ **后端计算 MD5**：所有文件 MD5 由后端计算，确保安全防篡改
- ✅ **秒传功能**：通过 MD5 去重实现秒传，节省存储空间
- ✅ **断点续传**：支持大文件分片上传，可断点续传
- ✅ **多种存储方式**：支持本地存储和 MinIO 对象存储，可配置切换
- ✅ **自动清理**：定时任务自动清理过期分片和已删除文件
- ✅ **目录归档**：按年/月/日自动分目录存储，避免单目录文件过多
- ✅ **逻辑删除**：支持文件逻辑删除，保留一定时间后物理删除

## 技术栈

- Java 21
- Spring Boot 3.x
- MyBatis-Plus
- MySQL
- MinIO（可选）
- Hutool

## 快速开始

### 1. 数据库初始化

执行 `src/main/resources/db/schema.sql` 创建数据库表：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS duke_storage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE duke_storage;

-- 执行建表脚本
-- (复制 schema.sql 内容执行)
```

### 2. 配置文件

修改 `application.yml` 中的配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/duke_storage?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

# 文件存储路径配置
file:
  local-base-path: D:/duke-storage/upload/    # 本地存储路径
  chunk-temp-path: D:/duke-storage/temp/      # 分片临时存储路径
  max-file-size: 50                            # 最大文件大小(MB)
  chunk-expire-day: 7                          # 分片保留天数
  delete-expire-day: 30                        # 删除文件保留天数

# MinIO配置（如不使用MinIO，保持 enable: false）
minio:
  enable: false
  endpoint: http://127.0.0.1:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: duke-file-bucket
```

### 3. 启动服务

```bash
mvn clean package
java -jar target/duke-storage-1.0.0.jar
```

## API 接口文档

### 1. 普通文件上传

**接口**：`POST /files/upload`

**请求**：
- Content-Type: multipart/form-data
- 参数：file (MultipartFile)

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "originalName": "test.pdf",
    "fileSuffix": "pdf",
    "fileSize": 1024000,
    "formattedSize": "1000.00 KB",
    "mimeType": "application/pdf",
    "storageMode": "local",
    "fileUrl": "/api/storage/files/2026/05/06/1234567890_abc.pdf",
    "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
    "createTime": "2026-05-06T10:00:00"
  }
}
```

### 2. 校验文件是否存在（秒传）

**接口**：`GET /files/check/exist`

**请求参数**：
- fileName: 文件名
- fileSize: 文件大小

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "exists": true,
    "fileInfo": {
      "id": 1,
      "originalName": "test.pdf",
      ...
    }
  }
}
```

### 3. 上传分片

**接口**：`POST /files/chunk/upload`

**请求**：
- Content-Type: multipart/form-data
- 参数：
  - chunkId: 分片ID（前端生成的唯一标识）
  - fileName: 文件名
  - chunkIndex: 当前分片序号（从1开始）
  - chunkTotal: 总分片数
  - chunkSize: 当前分片大小
  - fileSize: 文件总大小
  - chunk: 分片文件

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 4. 检查分片上传状态

**接口**：`GET /files/chunk/check`

**请求参数**：
- chunkId: 分片ID
- chunkTotal: 总分片数

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "uploadedChunks": [1, 2, 3, 5],
    "completed": false
  }
}
```

### 5. 合并分片

**接口**：`POST /files/chunk/merge`

**请求体**：
```json
{
  "chunkId": "unique-chunk-id-12345",
  "fileName": "largefile.pdf",
  "chunkTotal": 10,
  "fileSize": 52428800
}
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2,
    "originalName": "largefile.pdf",
    ...
  }
}
```

### 6. 获取文件信息

**接口**：`GET /files/{fileId}`

**响应**：返回文件详细信息

### 7. 分页查询文件列表

**接口**：`GET /files/list`

**请求参数**：
- current: 当前页（默认1）
- size: 每页大小（默认10）
- keyword: 文件名关键字（可选）
- fileType: 文件类型（可选）

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "records": [...]
  }
}
```

### 8. 删除文件

**接口**：`DELETE /files/{fileId}`

**说明**：逻辑删除，文件会在30天后物理删除

### 9. 下载文件

**接口**：`GET /files/download/{fileId}`

**响应**：文件流下载

### 10. 预览文件

**接口**：`GET /files/preview/{fileId}`

**说明**：支持 PDF、图片等格式在线预览

## 架构设计

### 存储模式

系统支持两种存储模式，通过配置切换：

1. **本地存储**（默认）
   - 文件存储在本地磁盘
   - 按日期分目录：`yyyy/MM/dd/filename`
   - 适合小规模应用

2. **MinIO 存储**
   - 文件存储在 MinIO 对象存储
   - 支持分布式部署
   - 适合大规模应用

### 秒传机制

1. 用户上传文件时，后端接收文件流
2. 后端计算文件 MD5
3. 查询数据库是否存在相同 MD5 的文件
4. 如果存在，直接返回已有文件信息（秒传）
5. 如果不存在，保存文件并记录元数据

### 断点续传流程

1. 前端将大文件分割成多个分片
2. 为每个文件生成唯一的 chunkId
3. 逐个上传分片到 `/files/chunk/upload`
4. 上传前可调用 `/files/chunk/check` 检查已上传的分片
5. 所有分片上传完成后，调用 `/files/chunk/merge` 合并
6. 后端合并分片后计算完整文件 MD5
7. 检查是否可秒传，否则保存为新文件
8. 清理分片临时数据

### 定时任务

系统包含两个定时任务：

1. **清理过期分片**（每天凌晨2点）
   - 清理超过7天未合并的分片
   - 删除分片文件和数据库记录

2. **清理已删除文件**（每周日凌晨3点）
   - 清理逻辑删除超过30天的文件
   - 删除物理文件和数据库记录

## 注意事项

### 安全限制

- ❌ 禁止前端传递 MD5，必须由后端计算
- ✅ 文件后缀白名单校验
- ✅ 文件大小限制
- ✅ 路径穿越防护

### 性能优化

- 大文件建议使用分片上传
- 秒传功能减少重复存储
- 定期清理垃圾数据

### 扩展性

- 文件服务与业务完全解耦
- 其他服务通过 fileId 引用文件
- 可轻松切换存储方式（本地/MinIO）

## 常见问题

### Q1: 如何切换到 MinIO 存储？

修改 `application.yml`：
```yaml
minio:
  enable: true
  endpoint: http://your-minio-server:9000
  access-key: your-access-key
  secret-key: your-secret-key
  bucket-name: your-bucket-name
```

### Q2: 如何调整文件大小限制？

修改配置：
```yaml
file:
  max-file-size: 100  # 单位：MB

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

### Q3: 分片上传失败如何处理？

- 调用 `/files/chunk/check` 检查已上传的分片
- 重新上传失败的分片
- 系统会自动清理7天前的未合并分片

## 开发规范

- 所有文件 MD5 必须由后端计算
- 禁止在文件服务中添加业务逻辑
- 文件状态由业务服务管理
- 统一使用逻辑删除

## 许可证

MIT License
