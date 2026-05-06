# duke-storage 文件服务

端口：9000 | 路径：/storage

## 职责
- 文件上传（普通上传、分片上传）
- 文件下载和预览
- 秒传功能（基于后端MD5计算）
- 断点续传支持
- 文件管理和查询
- 自动清理过期文件

## 核心规则
- **所有文件MD5必须由后端计算**，禁止前端传递
- 文件服务只管文件，不管业务
- 其他服务通过fileId引用文件
- 支持本地存储和MinIO两种模式，配置切换

## 技术栈
- Java 21 + Spring Boot 3.x
- MyBatis-Plus
- MySQL（元数据）
- MinIO（可选，对象存储）
- Hutool

## 数据库表
- sys_file：文件主表
- sys_file_chunk：分片临时表

## API接口
- POST /files/upload - 普通上传
- GET /files/check/exist - 秒传校验
- POST /files/chunk/upload - 分片上传
- GET /files/chunk/check - 检查分片状态
- POST /files/chunk/merge - 合并分片
- GET /files/{fileId} - 获取文件信息
- GET /files/list - 分页查询
- DELETE /files/{fileId} - 删除文件
- GET /files/download/{fileId} - 下载
- GET /files/preview/{fileId} - 预览

## 存储配置
```yaml
file:
  local-base-path: D:/duke-storage/upload/
  chunk-temp-path: D:/duke-storage/temp/
  max-file-size: 50  # MB
  chunk-expire-day: 7
  delete-expire-day: 30

minio:
  enable: false  # true启用MinIO，false使用本地存储
```

## 定时任务
- 每天凌晨2点：清理7天前的未合并分片
- 每周日凌晨3点：清理30天前逻辑删除的文件

## 注意事项
- 文件按日期分目录存储：yyyy/MM/dd/
- 逻辑删除保留30天后物理删除
- 分片上传完成后自动清理临时数据
- 秒传基于MD5去重实现
- 禁止在文件服务中写业务逻辑
