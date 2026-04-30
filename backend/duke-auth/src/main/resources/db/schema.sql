-- RBAC 权限管理系统数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS auth_center DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_center;

-- 1. 应用表
DROP TABLE IF EXISTS sys_app;
CREATE TABLE sys_app (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '应用ID',
    app_id VARCHAR(64) NOT NULL UNIQUE COMMENT '应用标识',
    app_name VARCHAR(100) NOT NULL COMMENT '应用名称',
    app_desc VARCHAR(500) COMMENT '应用描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    INDEX idx_app_id (app_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

-- 2. 部门表
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID 0=根部门',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(64) COMMENT '部门编码',
    ancestors VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖级列表（逗号分隔，如 0,1,2）',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    leader VARCHAR(64) COMMENT '负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 3. 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(100) COMMENT '真实姓名',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(500) COMMENT '头像URL',
    gender TINYINT COMMENT '性别 0=女 1=男 2=未知',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    weixin_openid VARCHAR(100) DEFAULT NULL COMMENT '微信openid（PC扫码登录）',
    weixin_unionid VARCHAR(100) DEFAULT NULL COMMENT '微信unionid',
    github_id    BIGINT       DEFAULT NULL COMMENT 'GitHub用户ID',
    github_login VARCHAR(100) DEFAULT NULL COMMENT 'GitHub登录名',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_weixin_openid (weixin_openid),
    UNIQUE KEY uk_github_id (github_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 4. 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
    data_scope TINYINT NOT NULL DEFAULT 5 COMMENT '数据权限 1=全部 2=自定义 3=本部门 4=本部门及下级 5=仅本人',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    INDEX idx_role_code (role_code),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 5. 菜单表
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID 0=根菜单',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    menu_type TINYINT NOT NULL COMMENT '菜单类型 1=目录 2=菜单 3=按钮',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(200) COMMENT '组件路径',
    permission VARCHAR(100) COMMENT '权限标识',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见 0=隐藏 1=显示',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 6. 按钮表
DROP TABLE IF EXISTS sys_button;
CREATE TABLE sys_button (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '按钮ID',
    menu_id BIGINT NOT NULL COMMENT '所属菜单ID',
    button_name VARCHAR(100) NOT NULL COMMENT '按钮名称',
    button_code VARCHAR(100) NOT NULL COMMENT '按钮编码',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    INDEX idx_menu_id (menu_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按钮表';

-- 7. API表
DROP TABLE IF EXISTS sys_api;
CREATE TABLE sys_api (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'API ID',
    app_id VARCHAR(64) NOT NULL COMMENT '所属应用',
    controller_class VARCHAR(255) NOT NULL COMMENT 'Controller类全限定名',
    controller_name VARCHAR(100) COMMENT 'Controller名称',
    api_name VARCHAR(100) NOT NULL COMMENT 'API名称',
    api_path VARCHAR(500) NOT NULL COMMENT 'API路径',
    api_method VARCHAR(10) NOT NULL COMMENT 'HTTP方法 GET/POST/PUT/DELETE',
    api_desc VARCHAR(500) COMMENT 'API描述',
    permission VARCHAR(100) COMMENT '权限标识',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除',
    UNIQUE KEY uk_method_path (api_method, api_path),
    INDEX idx_app_id (app_id),
    INDEX idx_controller (controller_class),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API表';

-- 8. 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 9. 用户部门关联表
DROP TABLE IF EXISTS sys_user_dept;
CREATE TABLE sys_user_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主部门 0=否 1=是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_dept (user_id, dept_id),
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户部门关联表';

-- 10. 角色菜单关联表
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 11. 角色API关联表
DROP TABLE IF EXISTS sys_role_api;
CREATE TABLE sys_role_api (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    api_id BIGINT NOT NULL COMMENT 'API ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_api (role_id, api_id),
    INDEX idx_role_id (role_id),
    INDEX idx_api_id (api_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色API关联表';

-- 12. 角色部门关联表（数据权限-自定义部门）
DROP TABLE IF EXISTS sys_role_dept;
CREATE TABLE sys_role_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_dept (role_id, dept_id),
    INDEX idx_role_id (role_id),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色部门关联表';

-- 13. 操作日志表
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(64) COMMENT '操作用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(255) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    result TEXT COMMENT '返回结果',
    ip VARCHAR(64) COMMENT 'IP地址',
    location VARCHAR(255) COMMENT 'IP归属地',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=失败 1=成功',
    error_msg TEXT COMMENT '错误信息',
    cost_time INT COMMENT '耗时（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ========== 初始化数据 ==========

-- 初始化应用
INSERT INTO sys_app (app_id, app_name, app_desc, status) VALUES
('duke-auth', '权限管理中心', 'RBAC权限管理系统', 1);

-- 初始化部门（ancestors 格式：根部门为 "0"，子部门追加 "父部门祖链,父部门id"）
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, sort_order, status, ancestors) VALUES
(1, 0, '总公司', 'ROOT', 0, 1, '0'),
(2, 1, '研发部', 'DEV', 1, 1, '0,1'),
(3, 1, '市场部', 'MARKET', 2, 1, '0,1'),
(4, 2, '后端组', 'BACKEND', 1, 1, '0,1,2'),
(5, 2, '前端组', 'FRONTEND', 2, 1, '0,1,2');

-- 初始化角色（密码：admin123）
INSERT INTO sys_role (id, role_name, role_code, data_scope, sort_order, status, remark) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, 0, 1, '拥有所有权限'),
(2, '普通用户', 'USER', 5, 1, 1, '仅查看自己的数据');

-- 初始化用户（密码：admin123，BCrypt加密）
INSERT INTO sys_user (id, username, password, real_name, nickname, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'Admin', 1),
(2, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '普通用户', 'User', 1);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2);

-- 用户部门关联
INSERT INTO sys_user_dept (user_id, dept_id, is_primary) VALUES
(1, 1, 1),
(2, 4, 1);

-- 初始化菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, visible, status) VALUES
(1, 0, '系统管理', 1, '/system', NULL, NULL, 'setting', 1, 1, 1),
(2, 1, '用户管理', 2, '/system/user', 'system/user/index', 'system:user:list', 'user', 1, 1, 1),
(3, 1, '角色管理', 2, '/system/role', 'system/role/index', 'system:role:list', 'team', 2, 1, 1),
(4, 1, '菜单管理', 2, '/system/menu', 'system/menu/index', 'system:menu:list', 'menu', 3, 1, 1),
(5, 1, '部门管理', 2, '/system/dept', 'system/dept/index', 'system:dept:list', 'apartment', 4, 1, 1),
(6, 1, 'API管理', 2, '/system/api', 'system/api/index', 'system:api:list', 'api', 5, 1, 1),
(7, 1, '操作日志', 2, '/system/log', 'system/log/index', 'system:log:list', 'file-text', 6, 1, 1);

-- 角色菜单关联（超级管理员拥有所有菜单）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7);

-- 初始化按钮
INSERT INTO sys_button (menu_id, button_name, button_code, sort_order, status) VALUES
(2, '新增', 'system:user:add', 1, 1),
(2, '编辑', 'system:user:edit', 2, 1),
(2, '删除', 'system:user:delete', 3, 1),
(3, '新增', 'system:role:add', 1, 1),
(3, '编辑', 'system:role:edit', 2, 1),
(3, '删除', 'system:role:delete', 3, 1),
(4, '新增', 'system:menu:add', 1, 1),
(4, '编辑', 'system:menu:edit', 2, 1),
(4, '删除', 'system:menu:delete', 3, 1),
(5, '新增', 'system:dept:add', 1, 1),
(5, '编辑', 'system:dept:edit', 2, 1),
(5, '删除', 'system:dept:delete', 3, 1),
(6, '同步', 'system:api:sync', 1, 1),
(6, '编辑', 'system:api:edit', 2, 1);

-- ========== doc-chat 应用初始化 ==========

-- 注册 doc-chat 应用
INSERT INTO sys_app (app_id, app_name, app_desc, status) VALUES
('doc-chat', '文档问答服务', '本地文件知识库问答服务', 1);

-- doc-chat 菜单目录
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, visible, status)
VALUES (8, 0, '文档问答', 1, '/doc-chat', NULL, NULL, 'ChatDotRound', 2, 1, 1);

-- doc-chat 菜单页面
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, visible, status)
VALUES (9, 8, '问答助手', 2, '/doc-chat/index', 'doc-chat/index', 'doc:chat:view', 'ChatDotRound', 1, 1, 1);

-- 超级管理员角色关联 doc-chat 菜单
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 8), (1, 9);

COMMIT;
