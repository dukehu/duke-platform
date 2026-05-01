-- ============================================================
-- Duke Platform Database Initialization Script
-- Database: duke_auth
-- Created: 2026-05-01
-- ============================================================

-- ============================================================
-- Table: sys_api - API接口资源表
-- ============================================================
CREATE TABLE `sys_api`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`           varchar(20)  NOT NULL COMMENT '所属应用ID',
    `controller_class` varchar(200) NOT NULL COMMENT 'Controller全类名（分组依据）',
    `controller_name`  varchar(100)          DEFAULT NULL COMMENT 'Controller名称（来自@Tag(name)）',
    `api_name`         varchar(100) NOT NULL COMMENT '接口名称（来自@Operation(summary)）',
    `api_path`         varchar(200) NOT NULL COMMENT '接口路径',
    `api_method`       varchar(10)  NOT NULL COMMENT 'HTTP方法',
    `api_desc`         varchar(500)          DEFAULT NULL COMMENT '接口描述',
    `permission`       varchar(100)          DEFAULT NULL COMMENT '权限标识符',
    `status`           tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `create_by`        varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_path_method` (`api_path`,`api_method`),
    KEY                `idx_app_id` (`app_id`),
    KEY                `idx_controller_class` (`controller_class`),
    KEY                `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=292 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API表';

-- ============================================================
-- Table: sys_app - 应用表
-- ============================================================
CREATE TABLE `sys_app`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_code`    varchar(50)  NOT NULL COMMENT '应用编码',
    `app_name`    varchar(100) NOT NULL COMMENT '应用名称',
    `app_desc`    varchar(500)          DEFAULT NULL COMMENT '应用描述',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '排序',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_code` (`app_code`),
    KEY           `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

-- ============================================================
-- Table: sys_button - 按钮表
-- ============================================================
CREATE TABLE `sys_button`
(
    `menu_id`     bigint       NOT NULL COMMENT '所属菜单ID',
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `button_name` varchar(100) NOT NULL COMMENT '按钮名称',
    `button_code` varchar(100) NOT NULL COMMENT '按钮编码',
    `button_type` tinyint      NOT NULL DEFAULT '1' COMMENT '按钮类型：1头部 2行操作',
    `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '排序',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY           `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按钮表';

-- ============================================================
-- Table: sys_dept - 部门表
-- ============================================================
CREATE TABLE `sys_dept`
(
    `parent_id`   bigint       NOT NULL DEFAULT '0' COMMENT '父部门ID，0为根节点',
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dept_name`   varchar(100) NOT NULL COMMENT '部门名称',
    `dept_code`   varchar(50)           DEFAULT NULL COMMENT '部门编码',
    `leader`      varchar(64)           DEFAULT NULL COMMENT '负责人',
    `phone`       varchar(20)           DEFAULT NULL COMMENT '联系电话',
    `email`       varchar(100)          DEFAULT NULL COMMENT '邮箱',
    `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '排序',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `ancestors`   varchar(500)          DEFAULT NULL COMMENT '祖级ID列表（逗号分隔，便于子树查询）',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY           `idx_parent_id` (`parent_id`),
    KEY           `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ============================================================
-- Table: sys_menu - 菜单表
-- ============================================================
CREATE TABLE `sys_menu`
(
    `parent_id`   bigint       NOT NULL DEFAULT '0' COMMENT '父菜单ID，0为根节点',
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`      bigint       NOT NULL COMMENT '所属应用ID',
    `menu_name`   varchar(100) NOT NULL COMMENT '菜单名称',
    `menu_type`   tinyint      NOT NULL COMMENT '菜单类型：1目录 2菜单 3按钮',
    `path`        varchar(200)          DEFAULT NULL COMMENT '路由路径',
    `component`   varchar(200)          DEFAULT NULL COMMENT '组件路径',
    `icon`        varchar(100)          DEFAULT NULL COMMENT '图标',
    `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '排序',
    `visible`     tinyint      NOT NULL DEFAULT '1' COMMENT '是否显示：1显示 0隐藏',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY           `idx_app_id` (`app_id`),
    KEY           `idx_parent_id` (`parent_id`),
    KEY           `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- ============================================================
-- Table: sys_operation_log - 操作日志表
-- ============================================================
CREATE TABLE `sys_operation_log`
(
    `id`             bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module`         varchar(100)      DEFAULT NULL COMMENT '操作模块',
    `operation`      varchar(100)      DEFAULT NULL COMMENT '操作类型',
    `method`         varchar(200)      DEFAULT NULL COMMENT '请求方法',
    `request_url`    varchar(500)      DEFAULT NULL COMMENT '请求URL',
    `request_method` varchar(10)       DEFAULT NULL COMMENT 'HTTP方法',
    `request_params` text COMMENT '请求参数',
    `response_data`  text COMMENT '响应数据',
    `operator`       varchar(64)       DEFAULT NULL COMMENT '操作人',
    `operator_id`    bigint            DEFAULT NULL COMMENT '操作人ID',
    `ip`             varchar(50)       DEFAULT NULL COMMENT '操作IP',
    `status`         tinyint  NOT NULL DEFAULT '1' COMMENT '操作状态：1成功 0失败',
    `error_msg`      varchar(2000)     DEFAULT NULL COMMENT '错误信息',
    `cost_time`      bigint            DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY              `idx_create_time` (`create_time`),
    KEY              `idx_operator` (`operator`)
) ENGINE=InnoDB AUTO_INCREMENT=124 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- Table: sys_role - 角色表
-- ============================================================
CREATE TABLE `sys_role`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`      bigint                DEFAULT NULL COMMENT '所属应用ID',
    `role_code`   varchar(50)  NOT NULL COMMENT '角色编码',
    `role_name`   varchar(100) NOT NULL COMMENT '角色名称',
    `role_desc`   varchar(500)          DEFAULT NULL COMMENT '角色描述',
    `data_scope`  tinyint      NOT NULL DEFAULT '1' COMMENT '数据权限：1全部 2自定义 3本部门 4本部门及下级 5仅本人',
    `status`      tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `sort_order`  int          NOT NULL DEFAULT '0' COMMENT '排序',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY           `idx_app_id` (`app_id`),
    KEY           `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- Table: sys_role_api - 角色API关联表
-- ============================================================
CREATE TABLE `sys_role_api`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `api_id`      bigint   NOT NULL COMMENT 'API ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_api` (`role_id`,`api_id`),
    KEY           `idx_api_id` (`api_id`),
    KEY           `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色API关联表';

-- ============================================================
-- Table: sys_role_button - 角色按钮关联表
-- ============================================================
CREATE TABLE `sys_role_button`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `button_id`   bigint   NOT NULL COMMENT '按钮ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_button` (`role_id`,`button_id`),
    KEY           `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色按钮关联表';

-- ============================================================
-- Table: sys_role_dept - 角色数据权限部门关联表
-- ============================================================
CREATE TABLE `sys_role_dept`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `dept_id`     bigint   NOT NULL COMMENT '部门ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_dept` (`role_id`,`dept_id`),
    KEY           `idx_dept_id` (`dept_id`),
    KEY           `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色数据权限部门关联表';

-- ============================================================
-- Table: sys_role_menu - 角色菜单关联表
-- ============================================================
CREATE TABLE `sys_role_menu`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `menu_id`     bigint   NOT NULL COMMENT '菜单ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
    KEY           `idx_menu_id` (`menu_id`),
    KEY           `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ============================================================
-- Table: sys_user - 用户表
-- ============================================================
CREATE TABLE `sys_user`
(
    `username`       varchar(50)  NOT NULL COMMENT '用户名',
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `password`       varchar(200) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`      varchar(100)          DEFAULT NULL COMMENT '真实姓名',
    `nickname`       varchar(100)          DEFAULT NULL COMMENT '昵称',
    `email`          varchar(100)          DEFAULT NULL COMMENT '邮箱',
    `phone`          varchar(20)           DEFAULT NULL COMMENT '手机号',
    `avatar`         varchar(500)          DEFAULT NULL COMMENT '头像URL',
    `status`         tinyint      NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
    `weixin_openid`  varchar(100)          DEFAULT NULL COMMENT '微信openid（PC扫码登录）',
    `weixin_unionid` varchar(100)          DEFAULT NULL COMMENT '微信unionid',
    `github_id`      bigint                DEFAULT NULL COMMENT 'GitHub用户ID',
    `github_login`   varchar(100)          DEFAULT NULL COMMENT 'GitHub登录名',
    `create_by`      varchar(64)           DEFAULT NULL COMMENT '创建人',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      varchar(64)           DEFAULT NULL COMMENT '更新人',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_github_id` (`github_id`),
    UNIQUE KEY `uk_weixin_openid` (`weixin_openid`),
    KEY              `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- Table: sys_user_dept - 用户部门关联表
-- ============================================================
CREATE TABLE `sys_user_dept`
(
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dept_id`     bigint   NOT NULL COMMENT '部门ID',
    `is_primary`  tinyint  NOT NULL DEFAULT '0' COMMENT '是否主部门：1是 0否',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_dept` (`user_id`,`dept_id`),
    KEY           `idx_dept_id` (`dept_id`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户部门关联表';

-- ============================================================
-- Table: sys_user_role - 用户角色关联表
-- ============================================================
CREATE TABLE `sys_user_role`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
    KEY           `idx_role_id` (`role_id`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- Data: sys_api - API接口资源初始化数据
-- ============================================================
INSERT INTO `sys_api`
VALUES (241, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '获取当前用户信息', '/info', 'GET', '',
        NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (242, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '获取图形验证码', '/captcha', 'GET',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (243, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '短信验证码登录', '/sms/login', 'POST',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (244, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '退出登录', '/logout', 'POST', '',
        NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (245, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '用户登录（账号密码）', '/login', 'POST',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (246, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '修改密码', '/change-password', 'POST',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (247, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', 'GitHub登录回调', '/github/callback',
        'POST', '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (248, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '微信登录回调', '/weixin/callback',
        'POST', '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (249, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '发送短信验证码', '/sms/send', 'POST',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (250, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '获取当前用户菜单树', '/menu', 'GET',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (251, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '获取微信登录二维码URL', '/weixin/url',
        'GET', '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (252, 'duke-auth', 'com.duke.auth.controller.AuthController', '认证管理', '获取GitHub登录授权URL', '/github/url',
        'GET', '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (253, 'duke-auth', 'com.duke.auth.controller.GatewayInternalController', '认证管理', 'check',
        '/internal/gateway/check', 'GET', NULL, NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (254, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', '手动同步API（duke-auth）', '/api/sync',
        'POST', '', 'system:api:sync', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (255, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', '修改API状态', '/api/{id}/status',
        'PUT', '', 'system:api:edit', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (256, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', 'API分页列表', '/api/page', 'GET', '',
        'system:api:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (257, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', '修改API权限标识',
        '/api/{id}/permission', 'PUT', '', 'system:api:edit', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (258, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', 'Controller分组列表',
        '/api/controllers', 'GET', '', 'system:api:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (259, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', '按应用和Controller分组列表',
        '/api/grouped', 'GET', '', 'system:api:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (260, 'duke-auth', 'com.duke.auth.controller.SysAppController', '应用管理', '删除应用', '/app/{id}', 'DELETE',
        '', 'system:app:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (261, 'duke-auth', 'com.duke.auth.controller.SysAppController', '应用管理', '应用分页列表', '/app/page', 'GET',
        '', 'system:app:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (262, 'duke-auth', 'com.duke.auth.controller.SysAppController', '应用管理', '所有启用应用', '/app/list', 'GET',
        '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (263, 'duke-auth', 'com.duke.auth.controller.SysButtonController', '按钮管理', '按钮列表', '/button/list', 'GET',
        '', 'system:menu:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (264, 'duke-auth', 'com.duke.auth.controller.SysButtonController', '按钮管理', '删除按钮', '/button/{id}',
        'DELETE', '', 'system:menu:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02',
        0),
       (265, 'duke-auth', 'com.duke.auth.controller.SysDeptController', '部门管理', '删除部门', '/dept/{id}', 'DELETE',
        '', 'system:dept:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (266, 'duke-auth', 'com.duke.auth.controller.SysDeptController', '部门管理', '部门树', '/dept/tree', 'GET', '',
        'system:dept:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (267, 'duke-auth', 'com.duke.auth.controller.SysMenuController', '菜单管理', '删除菜单', '/menu/{id}', 'DELETE',
        '', 'system:menu:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (268, 'duke-auth', 'com.duke.auth.controller.SysMenuController', '菜单管理', '菜单树', '/menu/tree', 'GET', '',
        'system:menu:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (269, 'duke-auth', 'com.duke.auth.controller.SysMenuController', '菜单管理', '菜单详情', '/menu/{id}', 'GET', '',
        'system:menu:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (270, 'duke-auth', 'com.duke.auth.controller.SysOperationLogController', '操作日志', '删除操作日志', '/log/{id}',
        'DELETE', '', 'system:log:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02',
        0),
       (271, 'duke-auth', 'com.duke.auth.controller.SysOperationLogController', '操作日志', '操作日志分页列表',
        '/log/page', 'GET', '', 'system:log:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (272, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '角色列表（不分页）', '/role/list',
        'GET', '', NULL, 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (273, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '删除角色', '/role/{id}', 'DELETE',
        '', 'system:role:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (274, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '角色分页列表', '/role/page', 'GET',
        '', 'system:role:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (275, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '角色详情', '/role/{id}', 'GET', '',
        'system:role:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (276, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '获取角色已分配API ID',
        '/role/{id}/apis', 'GET', '', 'system:role:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (277, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '获取角色已分配菜单ID',
        '/role/{id}/menus', 'GET', '', 'system:role:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (278, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '分配API权限', '/role/{id}/apis',
        'POST', '', 'system:role:assignApi', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02',
        0),
       (279, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '分配按钮权限',
        '/role/{id}/buttons', 'POST', '', 'system:role:assign', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (280, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '获取角色已分配按钮ID',
        '/role/{id}/buttons', 'GET', '', 'system:role:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu',
        '2026-04-30 00:43:02', 0),
       (281, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '分配菜单权限', '/role/{id}/menus',
        'POST', '', 'system:role:assignMenu', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02',
        0),
       (282, 'duke-auth', 'com.duke.auth.controller.SysRoleController', '角色管理', '设置数据权限',
        '/role/{id}/data-scope', 'POST', '', 'system:role:dataScope', 1, 'gh_dukehu', '2026-04-30 00:43:02',
        'gh_dukehu', '2026-04-30 00:43:02', 0),
       (283, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '删除用户', '/user/{id}', 'DELETE',
        '', 'system:user:delete', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (284, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '修改用户状态', '/user/{id}/status',
        'PUT', '', 'system:user:edit', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (285, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '用户分页列表', '/user/page', 'GET',
        '', 'system:user:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (286, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '用户详情', '/user/{id}', 'GET', '',
        'system:user:list', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (287, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '重置密码', '/user/{id}/password',
        'PUT', '', 'system:user:resetPwd', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02',
        0),
       (288, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '分配部门', '/user/{id}/depts',
        'POST', '', 'system:user:edit', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02', 0),
       (289, 'duke-auth', 'com.duke.auth.controller.SysUserController', '用户管理', '分配角色', '/user/{id}/roles',
        'POST', '', 'system:user:assignRole', 1, 'gh_dukehu', '2026-04-30 00:43:02', 'gh_dukehu', '2026-04-30 00:43:02',
        0),
       (290, 'duke-auth', 'com.duke.auth.controller.SysApiController', 'API管理', '扫描指定应用的API',
        '/api/sync/{appId}', 'POST', '', 'system:api:sync', 1, 'system', '2026-04-30 10:16:37', 'system',
        '2026-04-30 10:16:37', 0),
       (291, 'duke-auth', 'com.duke.auth.controller.InternalUserController',
        'com.duke.auth.controller.InternalUserController', 'getUserById', '/internal/users/{userId}', 'GET', NULL, NULL,
        1, 'system', '2026-04-30 11:22:28', 'system', '2026-04-30 11:22:28', 0);

-- ============================================================
-- Data: sys_app - 应用初始化数据
-- ============================================================
INSERT INTO `sys_app`
VALUES (1, 'auth-center', '权限管理系统', '企业级RBAC权限管理系统', 1, 1, 'system', '2026-04-23 15:06:40', NULL,
        '2026-04-23 15:06:40', 0),
       (6, 'duke-knowledge-qa', '知识问答', '企业级私有知识库问答服务', 1, 0, NULL, '2026-05-01 09:28:35', NULL,
        '2026-05-01 09:28:35', 0);

-- ============================================================
-- Data: sys_button - 按钮初始化数据
-- ============================================================
INSERT INTO `sys_button`
VALUES (2, 6, '新增', 'system:app:add', 1, 1, 1, 'system', '2026-04-23 16:56:07', 'admin', '2026-04-23 16:56:07', 0),
       (2, 7, '编辑', 'system:app:edit', 2, 2, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (2, 8, '删除', 'system:app:delete', 2, 3, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (7, 9, '新增', 'system:dept:add', 1, 1, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (7, 10, '编辑', 'system:dept:edit', 2, 2, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (7, 11, '删除', 'system:dept:delete', 2, 3, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (12, 12, '新增', 'system:user:add', 1, 1, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (12, 13, '编辑', 'system:user:edit', 2, 2, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (12, 14, '删除', 'system:user:delete', 2, 3, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (12, 15, '重置密码', 'system:user:resetPwd', 2, 4, 1, 'system', '2026-04-23 16:56:07', NULL,
        '2026-04-23 16:56:07', 0),
       (12, 16, '分配角色', 'system:user:assignRole', 2, 5, 1, 'system', '2026-04-23 16:56:07', NULL,
        '2026-04-23 16:56:07', 0),
       (19, 17, '新增', 'system:role:add', 1, 1, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (19, 18, '编辑', 'system:role:edit', 2, 2, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (19, 19, '删除', 'system:role:delete', 2, 3, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (19, 20, '分配权限', 'system:role:assignPerm', 2, 4, 1, 'system', '2026-04-23 16:56:07', NULL,
        '2026-04-23 16:56:07', 0),
       (25, 21, '新增', 'system:menu:add', 1, 1, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (25, 22, '编辑', 'system:menu:edit', 2, 2, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (25, 23, '删除', 'system:menu:delete', 2, 3, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (30, 24, '同步API', 'system:api:sync', 1, 1, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (30, 25, '编辑', 'system:api:edit', 2, 2, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (34, 26, '删除', 'system:log:delete', 2, 1, 1, 'system', '2026-04-23 16:56:07', NULL, '2026-04-23 16:56:07', 0),
       (11, 27, '上传文档', 'knowledge:document:upload', 1, 1, 1, NULL, '2026-05-01 09:28:35', NULL,
        '2026-05-01 09:28:35', 0),
       (11, 28, '删除文档', 'knowledge:document:delete', 2, 1, 1, NULL, '2026-05-01 09:28:35', NULL,
        '2026-05-01 09:28:35', 0),
       (11, 29, '提交问题', 'knowledge:question:ask', 1, 2, 1, NULL, '2026-05-01 09:28:35', NULL, '2026-05-01 09:28:35',
        0),
       (11, 30, '提交反馈', 'knowledge:answer:feedback', 2, 2, 1, NULL, '2026-05-01 09:28:35', NULL,
        '2026-05-01 09:28:35', 0),
       (11, 31, '执行搜索', 'knowledge:search:execute', 1, 3, 1, NULL, '2026-05-01 09:28:35', NULL,
        '2026-05-01 09:28:35', 0);

-- ============================================================
-- Data: sys_dept - 部门初始化数据
-- ============================================================
INSERT INTO `sys_dept`
VALUES (0, 1, '总公司', 'ROOT', NULL, NULL, NULL, 1, 1, '0', 'system', '2026-04-23 15:06:40', NULL,
        '2026-04-23 15:06:40', 0),
       (1, 2, '技术部', 'TECH', NULL, NULL, NULL, 1, 1, '0,1', 'system', '2026-04-23 15:06:40', 'admin',
        '2026-04-23 15:06:40', 0),
       (1, 3, '产品部', 'PRODUCT', NULL, NULL, NULL, 2, 1, '0,1', 'system', '2026-04-23 15:06:40', NULL,
        '2026-04-23 15:06:40', 0);

-- ============================================================
-- Data: sys_menu - 菜单初始化数据
-- ============================================================
INSERT INTO `sys_menu`
VALUES (0, 1, 1, '系统管理', 1, '/system', NULL, NULL, 1, 1, 1, NULL, '2026-05-01 09:50:48', NULL,
        '2026-05-01 09:50:48', 0),
       (1, 2, 1, '应用管理', 2, '/system/app', 'system/app/index', 'Grid', 1, 1, 1, 'system', '2026-04-23 15:06:40',
        NULL, '2026-04-23 15:06:40', 0),
       (1, 7, 1, '部门管理', 2, '/system/dept', 'system/dept/index', 'OfficeBuilding', 2, 1, 1, 'system',
        '2026-04-23 15:06:40', NULL, '2026-04-23 15:06:40', 0),
       (0, 10, 6, '知识问答', 1, '/knowledge-qa', NULL, 'ChatDotRound', 3, 1, 1, NULL, '2026-05-01 09:47:03', NULL,
        '2026-05-01 09:47:03', 0),
       (10, 11, 6, '首页', 2, '/knowledge-qa/dashboard', 'knowledge-qa/dashboard', 'HomeFilled', 1, 1, 1, NULL,
        '2026-05-01 09:47:03', NULL, '2026-05-01 09:47:03', 0),
       (1, 12, 1, '用户管理', 2, '/system/user', 'system/user/index', 'User', 3, 1, 1, 'system', '2026-04-23 15:06:40',
        NULL, '2026-04-23 15:06:40', 0),
       (10, 13, 6, '知识问答', 2, '/knowledge-qa/question', 'knowledge-qa/question', 'ChatDotRound', 3, 1, 1, NULL,
        '2026-05-01 09:47:03', NULL, '2026-05-01 09:47:03', 0),
       (10, 14, 6, '语义搜索', 2, '/knowledge-qa/search', 'knowledge-qa/search', 'Search', 4, 1, 1, NULL,
        '2026-05-01 09:47:03', NULL, '2026-05-01 09:47:03', 0),
       (10, 18, 6, '文档管理', 2, '/knowledge-qa/document', 'knowledge-qa/document', 'Document', 2, 1, 1, NULL,
        '2026-05-01 09:47:03', NULL, '2026-05-01 09:47:03', 0),
       (1, 19, 1, '角色管理', 2, '/system/role', 'system/role/index', 'UserFilled', 4, 1, 1, 'system',
        '2026-04-23 15:06:40', NULL, '2026-04-23 15:06:40', 0),
       (1, 25, 1, '菜单管理', 2, '/system/menu', 'system/menu/index', 'Menu', 5, 1, 1, 'system', '2026-04-23 15:06:40',
        NULL, '2026-04-23 15:06:40', 0),
       (1, 34, 1, '操作日志', 2, '/system/log', 'system/log/index', 'Document', 7, 1, 1, 'system',
        '2026-04-23 15:06:40', NULL, '2026-04-23 15:06:40', 0);

-- ============================================================
-- Data: sys_operation_log - 操作日志初始化数据
-- ============================================================
INSERT INTO `sys_operation_log`
VALUES (108, '用户管理', '分配角色', 'com.duke.auth.controller.SysUserController.assignRoles', '/auth/user/3/roles',
        'POST', '[3,[1]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 61, '2026-05-01 09:39:37'),
       (109, '角色管理', '分配按钮权限', 'com.duke.auth.controller.SysRoleController.assignButtons',
        '/auth/role/1/buttons', 'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 6,
        '2026-05-01 09:39:53'),
       (110, '角色管理', '分配API权限', 'com.duke.auth.controller.SysRoleController.assignApis', '/auth/role/1/apis',
        'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-01 09:39:53'),
       (111, '角色管理', '分配菜单权限', 'com.duke.auth.controller.SysRoleController.assignMenus', '/auth/role/1/menus',
        'POST', '[1,[10,11,13,14]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 21, '2026-05-01 09:39:53'),
       (112, '角色管理', '分配API权限', 'com.duke.auth.controller.SysRoleController.assignApis', '/auth/role/1/apis',
        'POST', '[1,[]]', NULL, 'admin', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-01 09:52:07'),
       (113, '角色管理', '分配按钮权限', 'com.duke.auth.controller.SysRoleController.assignButtons',
        '/auth/role/1/buttons', 'POST', '[1,[]]', NULL, 'admin', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 3,
        '2026-05-01 09:52:07'),
       (114, '角色管理', '分配菜单权限', 'com.duke.auth.controller.SysRoleController.assignMenus', '/auth/role/1/menus',
        'POST', '[1,[10]]', NULL, 'admin', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-01 09:52:07'),
       (115, '角色管理', '分配按钮权限', 'com.duke.auth.controller.SysRoleController.assignButtons',
        '/auth/role/1/buttons', 'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 2,
        '2026-05-01 09:52:52'),
       (116, '角色管理', '分配API权限', 'com.duke.auth.controller.SysRoleController.assignApis', '/auth/role/1/apis',
        'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-01 09:52:52'),
       (117, '角色管理', '分配菜单权限', 'com.duke.auth.controller.SysRoleController.assignMenus', '/auth/role/1/menus',
        'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 09:52:52'),
       (118, '角色管理', '分配按钮权限', 'com.duke.auth.controller.SysRoleController.assignButtons',
        '/auth/role/1/buttons', 'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 4,
        '2026-05-01 09:53:23'),
       (119, '角色管理', '分配API权限', 'com.duke.auth.controller.SysRoleController.assignApis', '/auth/role/1/apis',
        'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 3, '2026-05-01 09:53:23'),
       (120, '角色管理', '分配菜单权限', 'com.duke.auth.controller.SysRoleController.assignMenus', '/auth/role/1/menus',
        'POST', '[1,[10,11,18,13,14]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-05-01 09:53:23'),
       (121, '角色管理', '分配API权限', 'com.duke.auth.controller.SysRoleController.assignApis', '/auth/role/1/apis',
        'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-01 09:56:15'),
       (122, '角色管理', '分配按钮权限', 'com.duke.auth.controller.SysRoleController.assignButtons',
        '/auth/role/1/buttons', 'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 3,
        '2026-05-01 09:56:15'),
       (123, '角色管理', '分配菜单权限', 'com.duke.auth.controller.SysRoleController.assignMenus', '/auth/role/1/menus',
        'POST', '[1,[]]', NULL, 'gh_dukehu', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-05-01 09:56:15');

-- ============================================================
-- Data: sys_role - 角色初始化数据
-- ============================================================
INSERT INTO `sys_role`
VALUES (1, 1, 'SUPER_ADMIN', '超级管理员', '拥有所有权限', 1, 1, 1, 'system', '2026-04-23 15:06:40', NULL,
        '2026-04-23 15:06:40', 0),
       (2, 1, 'ROLE_USER', '普通用户', '普通用户', 1, 1, 2, 'system', '2026-04-23 15:06:40', NULL,
        '2026-04-23 15:06:40', 0),
       (3, 1, 'ROLE_TECH_DEPT', '技术部', '普通用户', 1, 1, 0, 'admin', '2026-04-26 23:13:51', 'admin',
        '2026-04-30 09:50:51', 0);

-- ============================================================
-- Data: sys_role_api - 角色API关联初始化数据
-- ============================================================
INSERT INTO `sys_role_api`
VALUES (9, 2, 75, '2026-04-26 22:13:38'),
       (10, 2, 89, '2026-04-26 22:13:38'),
       (31, 3, 77, '2026-04-27 09:58:24'),
       (32, 3, 74, '2026-04-27 09:58:24'),
       (33, 3, 78, '2026-04-27 09:58:24'),
       (34, 3, 76, '2026-04-27 09:58:24'),
       (35, 3, 75, '2026-04-27 09:58:24'),
       (36, 3, 73, '2026-04-27 09:58:24'),
       (37, 3, 82, '2026-04-27 09:58:24'),
       (38, 3, 81, '2026-04-27 09:58:24'),
       (39, 3, 88, '2026-04-27 09:58:24'),
       (40, 3, 89, '2026-04-27 09:58:24');

-- ============================================================
-- Data: sys_role_button - 角色按钮关联初始化数据
-- ============================================================
INSERT INTO `sys_role_button`
VALUES (21, 3, 21, '2026-04-27 09:58:24'),
       (22, 3, 22, '2026-04-27 09:58:24'),
       (23, 3, 23, '2026-04-27 09:58:24'),
       (24, 3, 24, '2026-04-27 09:58:24'),
       (25, 3, 25, '2026-04-27 09:58:24'),
       (26, 3, 26, '2026-04-27 09:58:24');

-- ============================================================
-- Data: sys_role_menu - 角色菜单关联初始化数据
-- ============================================================
INSERT INTO `sys_role_menu`
VALUES (98, 2, 1, '2026-04-26 22:13:38'),
       (99, 2, 30, '2026-04-26 22:13:38'),
       (100, 2, 34, '2026-04-26 22:13:38'),
       (118, 3, 25, '2026-04-27 09:58:24'),
       (119, 3, 30, '2026-04-27 09:58:24'),
       (120, 3, 34, '2026-04-27 09:58:24');

-- ============================================================
-- Data: sys_user - 用户初始化数据
-- ============================================================
INSERT INTO `sys_user`
VALUES ('admin', 1, '$2a$10$jYbg9bFYDfAzz4gXgOAqHOjtyuzKKnNR/BMdTZdOiUW3oTxbAt4AG', '超级管理员', NULL, NULL, NULL,
        NULL, 1, NULL, NULL, NULL, NULL, 'system', '2026-04-23 15:06:40', 'admin', '2026-04-23 15:06:40', 0),
       ('xyl', 2, '$2a$10$73csIA3brN3Ys4EJdoG2MeWPC4uCap73GQeG2RMj8IYCZHOGaQ4Cm', '许一鹭', NULL, '935776769@qq.com',
        '18892088719', NULL, 1, NULL, NULL, NULL, NULL, 'admin', '2026-04-23 17:02:32', 'admin', '2026-04-23 17:02:32',
        0),
       ('gh_dukehu', 3, '$2a$10$D1CIzSoStPUtBoRf8oEBMeS8L6VzBk15B2MMIwKbsHlJEUbw7KFXG', '胡明', 'dukehu',
        '779377254@qq.com', '18829012055', 'https://avatars.githubusercontent.com/u/33944291?v=4', 1, NULL, NULL,
        33944291, 'dukehu', 'system', '2026-04-26 22:55:45', 'system', '2026-04-26 22:55:45', 0);

-- ============================================================
-- Data: sys_user_dept - 用户部门关联初始化数据
-- ============================================================
INSERT INTO `sys_user_dept`
VALUES (1, 1, 1, 1, '2026-04-23 15:06:40');

-- ============================================================
-- Data: sys_user_role - 用户角色关联初始化数据
-- ============================================================
INSERT INTO `sys_user_role`
VALUES (1, 1, 1, '2026-04-23 17:19:05'),
       (7, 2, 2, '2026-04-23 18:38:13'),
       (11, 3, 1, '2026-05-01 09:39:37');
