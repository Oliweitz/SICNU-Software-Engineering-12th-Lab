-- ============================================================
-- V4__auth.sql —— 认证增强增量迁移
-- 适用版本：V3 之后、未执行 V4 的库（已含 RBAC 四表）
-- 目标版本：与 docs/init.sql 的 sys_user 结构一致
--
-- 变更内容：
--   1. sys_user 增加 phone / email 两列（短信、邮箱验证码登录与找回密码的联系方式）
--   2. 为两列建立唯一索引（未填写时为 NULL，允许多行 NULL）
--   3. 末尾附校验查询，预期均返回 0 行
--
-- 幂等性：全部语句可重复执行（information_schema 判断后执行 DDL）。
-- 不含 DROP DATABASE、不含 DROP TABLE、不删除任何业务数据。
-- 执行：mysql -u root -p ai_trial_platform < docs/migration/V4__auth.sql
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 新增联系方式列
-- ------------------------------------------------------------

SET @has_phone := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'phone');

SET @sql_phone := IF(@has_phone = 0,
    'ALTER TABLE sys_user
       ADD COLUMN phone VARCHAR(20) DEFAULT NULL
       COMMENT ''手机号（用于短信验证码登录/找回密码；接口与日志输出必须脱敏）''
       AFTER grade',
    'SELECT 1');
PREPARE stmt_phone FROM @sql_phone;
EXECUTE stmt_phone;
DEALLOCATE PREPARE stmt_phone;

SET @has_email := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email');

SET @sql_email := IF(@has_email = 0,
    'ALTER TABLE sys_user
       ADD COLUMN email VARCHAR(100) DEFAULT NULL
       COMMENT ''邮箱（用于邮箱验证码登录/找回密码；接口与日志输出必须脱敏）''
       AFTER phone',
    'SELECT 1');
PREPARE stmt_email FROM @sql_email;
EXECUTE stmt_email;
DEALLOCATE PREPARE stmt_email;

-- ------------------------------------------------------------
-- 2. 联系方式唯一索引
--    MySQL 唯一索引允许多个 NULL，未绑定联系方式的账号不受影响。
-- ------------------------------------------------------------

SET @has_uk_phone := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_phone');

SET @sql_uk_phone := IF(@has_uk_phone = 0,
    'ALTER TABLE sys_user ADD UNIQUE KEY uk_phone (phone)',
    'SELECT 1');
PREPARE stmt_uk_phone FROM @sql_uk_phone;
EXECUTE stmt_uk_phone;
DEALLOCATE PREPARE stmt_uk_phone;

SET @has_uk_email := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_email');

SET @sql_uk_email := IF(@has_uk_email = 0,
    'ALTER TABLE sys_user ADD UNIQUE KEY uk_email (email)',
    'SELECT 1');
PREPARE stmt_uk_email FROM @sql_uk_email;
EXECUTE stmt_uk_email;
DEALLOCATE PREPARE stmt_uk_email;

-- ------------------------------------------------------------
-- 3. 校验：以下查询预期均返回 0 行
-- ------------------------------------------------------------

SELECT 'sys_user 缺少 phone 列' AS problem, COUNT(*) AS bad_count
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
   AND COLUMN_NAME = 'phone'
HAVING COUNT(*) = 0;

SELECT 'sys_user 缺少 email 列' AS problem, COUNT(*) AS bad_count
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
   AND COLUMN_NAME = 'email'
HAVING COUNT(*) = 0;

SELECT 'sys_user 缺少 uk_phone 索引' AS problem, COUNT(*) AS bad_count
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_phone'
HAVING COUNT(*) = 0;

SELECT 'sys_user 缺少 uk_email 索引' AS problem, COUNT(*) AS bad_count
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_email'
HAVING COUNT(*) = 0;
