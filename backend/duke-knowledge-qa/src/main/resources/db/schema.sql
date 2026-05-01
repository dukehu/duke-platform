CREATE DATABASE IF NOT EXISTS knowledge_qa DEFAULT CHARACTER SET utf8mb4;
USE knowledge_qa;

CREATE TABLE IF NOT EXISTS doc_document (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL              COMMENT '文档标题',
    category    VARCHAR(50)   NOT NULL DEFAULT ''   COMMENT '文档分类',
    tags        VARCHAR(500)  NOT NULL DEFAULT '[]' COMMENT '标签 JSON 数组',
    file_type   VARCHAR(20)   NOT NULL DEFAULT ''   COMMENT 'pdf/docx/doc/txt/md',
    file_url    VARCHAR(500)  NOT NULL DEFAULT ''   COMMENT '相对路径，如 /api/knowledge-qa/files/xxx.pdf',
    file_name   VARCHAR(200)  NOT NULL DEFAULT ''   COMMENT '原始文件名',
    file_size   BIGINT        NOT NULL DEFAULT 0    COMMENT '文件大小(bytes)',
    file_md5    VARCHAR(32)   NOT NULL DEFAULT ''   COMMENT '文件 MD5 哈希值',
    status      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
    created_by  BIGINT                              COMMENT '上传者 user_id',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    UNIQUE KEY uk_file_md5 (file_md5),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
