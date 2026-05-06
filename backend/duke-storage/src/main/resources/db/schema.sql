-- 创建数据库
CREATE DATABASE IF NOT EXISTS duke_storage 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE duke_storage;

-- 文件服务数据库表结构

-- 文件主表
CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_suffix VARCHAR(50) NOT NULL COMMENT '文件后缀',
    file_size BIGINT NOT NULL COMMENT '文件大小字节',
    mime_type VARCHAR(100) COMMENT '文件mime类型',
    storage_mode VARCHAR(20) NOT NULL COMMENT '存储类型 local / minio',
    save_path VARCHAR(500) NOT NULL COMMENT '物理存储路径',
    file_url VARCHAR(500) COMMENT '访问预览地址',
    file_md5 VARCHAR(32) NOT NULL COMMENT '后端计算完整文件MD5，用于秒传去重',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标识 0-未删除 1-已删除',
    INDEX idx_md5 (file_md5),
    INDEX idx_deleted (deleted),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用文件主表';

-- 断点续传分片临时表
CREATE TABLE IF NOT EXISTS sys_file_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    chunk_id VARCHAR(100) NOT NULL COMMENT '前端生成全局唯一文件任务ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_suffix VARCHAR(50) NOT NULL COMMENT '文件后缀',
    chunk_index INT NOT NULL COMMENT '当前分片序号，从1开始',
    chunk_total INT NOT NULL COMMENT '总分片数',
    chunk_size BIGINT NOT NULL COMMENT '单分片大小字节',
    file_size BIGINT NOT NULL COMMENT '文件总大小字节',
    chunk_path VARCHAR(500) NOT NULL COMMENT '分片临时存放路径',
    storage_mode VARCHAR(20) NOT NULL COMMENT '存储模式 local/minio',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_chunk_unique (chunk_id, chunk_index),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='断点续传分片临时表';
