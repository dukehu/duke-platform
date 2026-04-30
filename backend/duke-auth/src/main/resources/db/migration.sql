-- 增量补丁：为已有数据库补充新字段
-- 执行前请确认已 USE auth_center;

-- 1. sys_user 表：昵称（schema.sql 中原本定义了但实体漏掉）
ALTER TABLE sys_user
    ADD COLUMN nickname VARCHAR(100) COMMENT '昵称' AFTER real_name;

-- 2. sys_user 表：微信登录字段
ALTER TABLE sys_user
    ADD COLUMN weixin_openid  VARCHAR(100) DEFAULT NULL COMMENT '微信openid（PC扫码登录）' AFTER status,
    ADD COLUMN weixin_unionid VARCHAR(100) DEFAULT NULL COMMENT '微信unionid' AFTER weixin_openid,
    ADD UNIQUE KEY uk_weixin_openid (weixin_openid);

-- 初始化已有部门的 ancestors（根据实际 id 调整）
UPDATE sys_dept SET ancestors = '0'   WHERE parent_id = 0;
UPDATE sys_dept SET ancestors = '0,1' WHERE parent_id = 1;
UPDATE sys_dept SET ancestors = '0,1,2' WHERE parent_id = 2;

-- 3. sys_user 表：GitHub 登录字段
ALTER TABLE sys_user
    ADD COLUMN github_id    BIGINT       DEFAULT NULL COMMENT 'GitHub用户ID'  AFTER weixin_unionid,
    ADD COLUMN github_login VARCHAR(100) DEFAULT NULL COMMENT 'GitHub登录名' AFTER github_id,
    ADD UNIQUE KEY uk_github_id (github_id);
