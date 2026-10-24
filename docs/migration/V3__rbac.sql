-- ============================================================
-- V3__rbac.sql —— RBAC 权限模型增量迁移
-- 适用版本：V2 之后、未执行 V3 的库（已含数据字典两表）
-- 目标版本：与 docs/init.sql 的 RBAC 结构一致
--
-- 变更内容：
--   1. 新建 sys_role / sys_permission / sys_user_role / sys_role_permission 四表
--   2. 写入 3 个角色、19 个权限点与角色-权限授权种子
--   3. 存量 sys_user.role 逐行迁入 sys_user_role（多对多），随后删除该列
--   4. 移除字典 user_role（角色权威源改为 sys_role），新增字典 perm_type
--   5. 末尾附校验查询，预期均返回 0 行
--
-- 幂等性：全部语句可重复执行（CREATE TABLE IF NOT EXISTS / INSERT IGNORE /
--         ON DUPLICATE KEY UPDATE / information_schema 判断后执行 DDL）。
-- 不含 DROP DATABASE、不含 DROP TABLE。
-- 执行：mysql -u root -p ai_trial_platform < docs/migration/V3__rbac.sql
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 建表
-- ------------------------------------------------------------

-- 角色表：角色码为授权单位，与 Java Role 枚举一一对应
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(20)  NOT NULL COMMENT '角色码（STUDENT 师范生 / TEACHER 教师 / ADMIN 管理员），与 Role 枚举一一对应',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称（中文展示名）',
    description VARCHAR(200) DEFAULT NULL COMMENT '职责说明（该角色可做什么，便于评审与运维理解）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB COMMENT ='角色表';

-- 权限点表：权限码是全项目鉴权的唯一判据
CREATE TABLE IF NOT EXISTS sys_permission (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    perm_code  VARCHAR(50) NOT NULL COMMENT '权限码（模块:动作，如 task:manage），@PreAuthorize 与前端按钮共用',
    perm_name  VARCHAR(50) NOT NULL COMMENT '权限名称（中文展示名，如「发布/关闭任务」）',
    module     VARCHAR(20) NOT NULL COMMENT '所属模块（user / clazz / task / trial / report / review / resource / dict / stats / cert）',
    perm_type  VARCHAR(20) NOT NULL COMMENT '权限类型（字典 perm_type，值为字典码：API 接口 / MENU 菜单）',
    sort_no    INT         NOT NULL DEFAULT 0 COMMENT '排序号（管理界面按模块展示时的顺序，升序）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY idx_module (module)
) ENGINE = InnoDB COMMENT ='权限点表';

-- 用户-角色关系表（纯关系表，豁免 updated_at / deleted）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '用户 id（sys_user.id）',
    role_id    BIGINT   NOT NULL COMMENT '角色 id（sys_role.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB COMMENT ='用户-角色关系表';

-- 角色-权限关系表（纯关系表，豁免 updated_at / deleted）
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT   NOT NULL COMMENT '角色 id（sys_role.id）',
    permission_id BIGINT   NOT NULL COMMENT '权限点 id（sys_permission.id）',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE = InnoDB COMMENT ='角色-权限关系表';

-- ------------------------------------------------------------
-- 2. 种子：角色、权限点、角色-权限授权（均按业务码定位，不依赖自增 id）
-- ------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name, description) VALUES
('STUDENT', '师范生', '查看本班任务、提交试讲、查看报告与证书、浏览题库资源'),
('TEACHER', '教师',   '建班与学生管理、发布/关闭任务、批阅点评、查看班级统计与导出'),
('ADMIN',   '管理员', '用户与角色权限维护、字典维护、题库资源维护、全局统计')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

INSERT INTO sys_permission (perm_code, perm_name, module, perm_type, sort_no) VALUES
('user:read',         '查看用户',       'user',     'API',  1),
('user:manage',       '管理用户',       'user',     'API',  2),
('clazz:read',        '查看班级',       'clazz',    'API',  3),
('clazz:manage',      '管理班级',       'clazz',    'API',  4),
('task:read',         '查看任务',       'task',     'API',  5),
('task:manage',       '发布/关闭任务',  'task',     'API',  6),
('trial:submit',      '提交试讲',       'trial',    'API',  7),
('trial:read',        '查看试讲',       'trial',    'API',  8),
('report:read',       '查看评测报告',   'report',   'API',  9),
('review:manage',     '批阅与点评',     'review',   'API', 10),
('resource:read',     '查看题库与资源', 'resource', 'API', 11),
('resource:manage',   '维护题库与资源', 'resource', 'API', 12),
('dict:read',         '查看数据字典',   'dict',     'API', 13),
('dict:manage',       '维护数据字典',   'dict',     'API', 14),
('stats:self',        '查看个人统计',   'stats',    'API', 15),
('stats:class',       '查看班级统计',   'stats',    'API', 16),
('stats:global',      '查看全局统计',   'stats',    'API', 17),
('cert:read',         '查看达标证书',   'cert',     'API', 18),
('permission:manage', '维护角色与权限', 'user',     'API', 19)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), module = VALUES(module),
                        perm_type = VALUES(perm_type), sort_no = VALUES(sort_no);

-- STUDENT：只读任务/试讲/报告/资源/证书 + 提交试讲 + 个人统计
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
  JOIN sys_permission p
    ON p.perm_code IN ('task:read', 'trial:submit', 'trial:read', 'report:read',
                       'resource:read', 'stats:self', 'cert:read')
 WHERE r.role_code = 'STUDENT';

-- TEACHER：班级/任务/批阅管理 + 班级统计
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
  JOIN sys_permission p
    ON p.perm_code IN ('clazz:read', 'clazz:manage', 'task:read', 'task:manage',
                       'trial:read', 'report:read', 'review:manage', 'resource:read',
                       'stats:class', 'cert:read')
 WHERE r.role_code = 'TEACHER';

-- ADMIN：全部权限点
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r
  JOIN sys_permission p ON p.deleted = 0
 WHERE r.role_code = 'ADMIN';

-- ------------------------------------------------------------
-- 3. 存量迁移：sys_user.role → sys_user_role，然后删除该列
--    （列已删除时跳过，保证脚本可重复执行）
-- ------------------------------------------------------------
SET @has_role_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'role');

SET @sql_backfill := IF(@has_role_col > 0,
    'INSERT IGNORE INTO sys_user_role (user_id, role_id)
       SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.role_code = u.role',
    'SELECT 1');
PREPARE stmt_backfill FROM @sql_backfill;
EXECUTE stmt_backfill;
DEALLOCATE PREPARE stmt_backfill;

SET @sql_drop_role := IF(@has_role_col > 0,
    'ALTER TABLE sys_user DROP COLUMN role',
    'SELECT 1');
PREPARE stmt_drop FROM @sql_drop_role;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;

-- 删除依赖 role 列的单列索引（若存在）
SET @has_role_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'idx_role');
SET @sql_drop_idx := IF(@has_role_idx > 0,
    'ALTER TABLE sys_user DROP INDEX idx_role',
    'SELECT 1');
PREPARE stmt_idx FROM @sql_drop_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- ------------------------------------------------------------
-- 4. 字典收敛：移除 user_role，新增 perm_type
-- ------------------------------------------------------------
DELETE FROM sys_dict_item WHERE dict_type = 'user_role';
DELETE FROM sys_dict_type WHERE dict_type = 'user_role';

INSERT IGNORE INTO sys_dict_type (dict_type, dict_name, remark) VALUES
('perm_type', '权限点类型', '服务的列：sys_permission.perm_type（API 接口权限 / MENU 菜单权限）');

INSERT IGNORE INTO sys_dict_item (dict_type, item_value, item_label, sort_no) VALUES
('perm_type', 'API',  '接口权限', 1),
('perm_type', 'MENU', '菜单权限', 2);

-- ------------------------------------------------------------
-- 5. 迁移校验（以下查询预期均返回 0 行）
-- ------------------------------------------------------------
SELECT 'sys_user 仍存在 role 列' AS problem, COUNT(*) AS bad_count
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'role'
UNION ALL
SELECT 'sys_user 存在未分配角色的用户', COUNT(*) FROM sys_user u
  LEFT JOIN sys_user_role ur ON ur.user_id = u.id
 WHERE u.deleted = 0 AND ur.id IS NULL
UNION ALL
SELECT 'sys_user_role 引用不存在的角色', COUNT(*) FROM sys_user_role ur
  LEFT JOIN sys_role r ON r.id = ur.role_id
 WHERE r.id IS NULL
UNION ALL
SELECT 'sys_role_permission 引用不存在的权限点', COUNT(*) FROM sys_role_permission rp
  LEFT JOIN sys_permission p ON p.id = rp.permission_id
 WHERE p.id IS NULL
UNION ALL
SELECT 'perm_type 字典外取值', COUNT(*) FROM sys_permission
 WHERE perm_type NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'perm_type')
UNION ALL
SELECT '三类角色权限点数量异常（应为 7/10/19）', COUNT(*)
  FROM (SELECT r.role_code, COUNT(*) AS cnt FROM sys_role_permission rp
          JOIN sys_role r ON r.id = rp.role_id
         WHERE r.role_code IN ('STUDENT', 'TEACHER', 'ADMIN')
         GROUP BY r.role_code
        HAVING cnt NOT IN (7, 10, 19)) t;
