-- ============================================================
-- V2 —— 引入数据字典，字典型列一律改存字典码
-- 环境：MySQL 8.0.x
-- 用法：mysql -u root -p ai_trial_platform < docs/migration/V2__data_dict.sql
--
-- 适用对象：**已有数据的环境**。全新环境直接跑 docs/init.sql 即可，无需本脚本。
-- 设计说明见 docs/数据库设计规范.md 第 12 节《数据字典》。
--
-- 本脚本与 V1 的关系：
--   原 docs/migration/V1__db_spec_gaps.sql 的内容已全部并入 docs/init.sql，该脚本已删除。
--   为避免「库建得早、没跑过原 V1」的环境漏变更，本脚本第 1 节以**幂等**方式重建了
--   原 V1 的两项变更（certificate.idx_task 索引、task/trial 列 COMMENT）。
--   故：无论你的库是否执行过原 V1，跑完本脚本都与 init.sql 一致。
--
-- 本脚本不含 DROP，只做 CREATE TABLE / ALTER TABLE / UPDATE；
-- 且**可重复执行**：重复跑不报错，也不会二次改写已迁移的数据。
-- ============================================================

-- ⚠️ 与 init.sql 同理：先声明连接字符集，否则 Windows 中文环境下
--    mysql 客户端按 GBK 解读本文件的 UTF-8 中文，COMMENT 会写坏、UPDATE 也匹配不上
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 原 V1 内容（幂等重建，未跑过原 V1 的库在此补齐）
-- ------------------------------------------------------------

-- 1.1 certificate.task_id 是外键列却漏建索引（规范 §4.1）
--     MySQL 不支持 ADD INDEX IF NOT EXISTS，故先查 information_schema 再决定是否执行
SET @has_idx := (SELECT COUNT(*)
                 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'certificate'
                   AND INDEX_NAME = 'idx_task');
SET @ddl := IF(@has_idx = 0, 'ALTER TABLE certificate ADD KEY idx_task (task_id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 取值范围 / 空值语义补全（仅改 COMMENT，不动列类型与约束）
--     rubric_json 的口径以技术方案 §6.2 为准：总分 = Σ(维度分 × 维度权重) ÷ Σ(维度权重)，
--     引擎按 Σ权重 归一化，故权重之和**不必**为 100，真正的约束是每个维度的 weight 必须 > 0（作分母）
ALTER TABLE task
    MODIFY COLUMN duration_minutes INT NOT NULL
        COMMENT '规定时长（分钟，> 0）',
    MODIFY COLUMN deadline DATETIME DEFAULT NULL
        COMMENT '截止时间（NULL 表示不设截止，任务长期有效）',
    MODIFY COLUMN rubric_json JSON NOT NULL
        COMMENT '评分标准 JSON（维度 / 权重 / 知识点清单，见技术方案 6.2；可由 resource(type=RUBRIC) 模板复制而来。各维度 weight 必须 > 0，总分按 Σ权重 归一化，权重之和不必为 100）';

ALTER TABLE trial
    MODIFY COLUMN duration_actual INT DEFAULT NULL
        COMMENT '实际时长（分钟，>= 0；未录入时为 NULL）';

-- ------------------------------------------------------------
-- 2. 建数据字典两表
--    通用两表结构：新增字典类型**无需 DDL**，只加数据行
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_type  VARCHAR(50)  NOT NULL COMMENT '字典类型编码（全局唯一，小写下划线，如 subject 学科 / stage 学段）',
    dict_name  VARCHAR(50)  NOT NULL COMMENT '字典名称（中文展示名，如「学科」）',
    remark     VARCHAR(200) DEFAULT NULL COMMENT '用途说明（写明本字典服务于哪几列）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：按类型编码取字典类型（同时保证类型编码全局唯一）
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB COMMENT ='数据字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_type  VARCHAR(50) NOT NULL COMMENT '所属字典类型（sys_dict_type.dict_type）',
    item_value VARCHAR(20) NOT NULL COMMENT '字典码（业务表实际存储的值，如 MATH）',
    item_label VARCHAR(50) NOT NULL COMMENT '字典标签（前端展示的中文，如 数学）',
    sort_no    INT         NOT NULL DEFAULT 0 COMMENT '排序号（前端下拉顺序，升序）',
    enabled    TINYINT     NOT NULL DEFAULT 1 COMMENT '启用状态：1 启用 / 0 停用（停用项不可再选，历史数据仍按标签展示）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：同一字典下字典码唯一（防重复登记）；最左前缀已覆盖「按 dict_type 列出全部字典项」，
    -- 依规范 §4.1 最左前缀原则，不得再单建 idx_dict_type（冗余索引徒增写入开销）
    UNIQUE KEY uk_type_value (dict_type, item_value)
) ENGINE = InnoDB COMMENT ='数据字典项表';

-- ------------------------------------------------------------
-- 3. 字典种子数据
--    用 INSERT IGNORE 而非 REPLACE：重复执行不报错，且不会覆盖管理员在界面上做过的调整
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_dict_type (dict_type, dict_name, remark) VALUES
('user_role',        '用户角色',     '服务的列：sys_user.role'),
('major',            '专业',         '服务的列：sys_user.major（师范类专业）'),
('grade',            '年级',         '服务的列：sys_user.grade（取值口径为入学年份）'),
('subject',          '学科',         '服务的列：clazz.course / task.subject / question_bank.subject / resource.subject'),
('stage',            '学段',         '服务的列：task.stage / question_bank.stage'),
('textbook_version', '教材版本',     '服务的列：task.textbook_version'),
('teach_mode',       '训练模式',     '服务的列：task.mode（四要素定义见技术方案 2.2）'),
('task_status',      '任务状态',     '服务的列：task.status（流转权限归 task 模块，见技术方案 5.1 R-2）'),
('trial_status',     '试讲状态',     '服务的列：trial.status（流转权限归 trial 模块，见技术方案 5.1 R-2）'),
('evaluator_type',   '评测引擎类型', '服务的列：evaluation_report.evaluator_type'),
('question_type',    '题目类型',     '服务的列：question_bank.type'),
('resource_type',    '资源类型',     '服务的列：resource.type'),
('cert_type',        '证书类型',     '服务的列：certificate.type（四类技能取自题目需求 9.1，前两类与评测维度 expression / interaction 对应，控场并入课堂互动维度）');

INSERT IGNORE INTO sys_dict_item (dict_type, item_value, item_label, sort_no) VALUES
-- 用户角色
('user_role',        'STUDENT',       '师范生',       1),
('user_role',        'TEACHER',       '教师',         2),
('user_role',        'ADMIN',         '管理员',       3),
-- 专业
('major',            'CHINESE_EDU',   '汉语言文学',   1),
('major',            'MATH_EDU',      '数学与应用数学', 2),
('major',            'ENGLISH_EDU',   '英语',         3),
('major',            'POLITICS_EDU',  '思想政治教育', 4),
('major',            'PRESCHOOL_EDU', '学前教育',     5),
('major',            'PRIMARY_EDU',   '小学教育',     6),
-- 年级（入学年份）
('grade',            '2022',          '2022级',       1),
('grade',            '2023',          '2023级',       2),
('grade',            '2024',          '2024级',       3),
('grade',            '2025',          '2025级',       4),
('grade',            '2026',          '2026级',       5),
-- 学科（clazz.course 与 task.subject 共用；通用模板资源用 GENERAL）
('subject',          'CHINESE',       '语文',         1),
('subject',          'MATH',          '数学',         2),
('subject',          'ENGLISH',       '英语',         3),
('subject',          'POLITICS',      '思政',         4),
('subject',          'PRESCHOOL',     '幼教',         5),
('subject',          'SCIENCE',       '科学',         6),
('subject',          'ART',           '美术',         7),
('subject',          'MUSIC',         '音乐',         8),
('subject',          'GENERAL',       '通用',        99),
-- 学段
('stage',            'PRIMARY',       '小学',         1),
('stage',            'JUNIOR',        '初中',         2),
('stage',            'SENIOR',        '高中',         3),
-- 教材版本
('textbook_version', 'PEP',           '人教版',       1),
('textbook_version', 'BNUP',          '北师大版',     2),
('textbook_version', 'SJEP',          '苏教版',       3),
('textbook_version', 'FLTRP',         '外研版',       4),
-- 训练模式
('teach_mode',       'FREE',          '自由试讲',     1),
('teach_mode',       'SCENARIO',      '情景模拟',     2),
('teach_mode',       'STRUCTURED',    '结构化面试',   3),
('teach_mode',       'EXAM',          '模拟考核',     4),
-- 任务状态
('task_status',      'DRAFT',         '草稿',         1),
('task_status',      'PUBLISHED',     '已发布',       2),
('task_status',      'CLOSED',        '已关闭',       3),
-- 试讲状态
('trial_status',     'DRAFT',         '草稿',         1),
('trial_status',     'SUBMITTED',     '已提交',       2),
('trial_status',     'EVALUATED',     '已评测',       3),
-- 评测引擎类型
('evaluator_type',   'RULE',          '规则引擎',     1),
('evaluator_type',   'LLM',           '大模型',       2),
('evaluator_type',   'ASR',           '语音识别',     3),
('evaluator_type',   'POSTURE',       '姿态识别',     4),
-- 题目类型
('question_type',    'TRIAL',         '试讲题',       1),
('question_type',    'STRUCTURED',    '结构化面试题', 2),
('question_type',    'REAL',          '教资真题',     3),
-- 资源类型
('resource_type',    'LESSON_PLAN',   '教案',         1),
('resource_type',    'BOARD',         '板书模板',     2),
('resource_type',    'EXAMPLE',       '试讲范例',     3),
('resource_type',    'RUBRIC',        '评分细则模板', 4),
-- 证书类型
('cert_type',        'EXPRESSION',    '教学表达',     1),
('cert_type',        'INTERACTION',   '课堂互动',     2),
('cert_type',        'CONTROL',       '课堂控场',     3),
('cert_type',        'TIME',          '时间管理',     4);

-- ------------------------------------------------------------
-- 4. 存量取值迁移：中文自由文本 → 字典码
--    必须在第 5 节「列宽收敛」之前执行。
--    CASE 的 ELSE 分支保留原值不动——**认不出来的取值原样留库**，
--    跑第 6 节的校验语句会把它们列出来，人工确认后再补一条 UPDATE 即可（本脚本可重跑）。
-- ------------------------------------------------------------

-- 4.1 学科：clazz.course / task.subject / question_bank.subject / resource.subject
UPDATE clazz SET course = CASE course
        WHEN '语文' THEN 'CHINESE'  WHEN '数学' THEN 'MATH'      WHEN '英语' THEN 'ENGLISH'
        WHEN '思政' THEN 'POLITICS' WHEN '幼教' THEN 'PRESCHOOL' WHEN '科学' THEN 'SCIENCE'
        WHEN '美术' THEN 'ART'      WHEN '音乐' THEN 'MUSIC'
        ELSE course END
 WHERE course IN ('语文', '数学', '英语', '思政', '幼教', '科学', '美术', '音乐');

UPDATE task SET subject = CASE subject
        WHEN '语文' THEN 'CHINESE'  WHEN '数学' THEN 'MATH'      WHEN '英语' THEN 'ENGLISH'
        WHEN '思政' THEN 'POLITICS' WHEN '幼教' THEN 'PRESCHOOL' WHEN '科学' THEN 'SCIENCE'
        WHEN '美术' THEN 'ART'      WHEN '音乐' THEN 'MUSIC'
        ELSE subject END
 WHERE subject IN ('语文', '数学', '英语', '思政', '幼教', '科学', '美术', '音乐');

UPDATE question_bank SET subject = CASE subject
        WHEN '语文' THEN 'CHINESE'  WHEN '数学' THEN 'MATH'      WHEN '英语' THEN 'ENGLISH'
        WHEN '思政' THEN 'POLITICS' WHEN '幼教' THEN 'PRESCHOOL' WHEN '科学' THEN 'SCIENCE'
        WHEN '美术' THEN 'ART'      WHEN '音乐' THEN 'MUSIC'
        ELSE subject END
 WHERE subject IN ('语文', '数学', '英语', '思政', '幼教', '科学', '美术', '音乐');

-- resource.subject 多一个「通用」取值（通用模板）
UPDATE resource SET subject = CASE subject
        WHEN '语文' THEN 'CHINESE'  WHEN '数学' THEN 'MATH'      WHEN '英语' THEN 'ENGLISH'
        WHEN '思政' THEN 'POLITICS' WHEN '幼教' THEN 'PRESCHOOL' WHEN '科学' THEN 'SCIENCE'
        WHEN '美术' THEN 'ART'      WHEN '音乐' THEN 'MUSIC'     WHEN '通用' THEN 'GENERAL'
        ELSE subject END
 WHERE subject IN ('语文', '数学', '英语', '思政', '幼教', '科学', '美术', '音乐', '通用');

-- 4.2 学段：task.stage / question_bank.stage
UPDATE task SET stage = CASE stage
        WHEN '小学' THEN 'PRIMARY' WHEN '初中' THEN 'JUNIOR' WHEN '高中' THEN 'SENIOR'
        ELSE stage END
 WHERE stage IN ('小学', '初中', '高中');

UPDATE question_bank SET stage = CASE stage
        WHEN '小学' THEN 'PRIMARY' WHEN '初中' THEN 'JUNIOR' WHEN '高中' THEN 'SENIOR'
        ELSE stage END
 WHERE stage IN ('小学', '初中', '高中');

-- 4.3 教材版本
UPDATE task SET textbook_version = CASE textbook_version
        WHEN '人教版' THEN 'PEP'   WHEN '人教' THEN 'PEP'
        WHEN '北师大版' THEN 'BNUP' WHEN '苏教版' THEN 'SJEP' WHEN '外研版' THEN 'FLTRP'
        ELSE textbook_version END
 WHERE textbook_version IN ('人教版', '人教', '北师大版', '苏教版', '外研版');

-- 4.4 专业
UPDATE sys_user SET major = CASE major
        WHEN '汉语言文学' THEN 'CHINESE_EDU'
        WHEN '数学与应用数学' THEN 'MATH_EDU'
        WHEN '英语' THEN 'ENGLISH_EDU'
        WHEN '思想政治教育' THEN 'POLITICS_EDU'
        WHEN '学前教育' THEN 'PRESCHOOL_EDU'
        WHEN '小学教育' THEN 'PRIMARY_EDU'
        ELSE major END
 WHERE major IN ('汉语言文学', '数学与应用数学', '英语', '思想政治教育', '学前教育', '小学教育');

-- 4.5 年级：取值口径统一为**入学年份**，如 '2023级' → '2023'
--     只处理「4 位年份（可带「级」）」的取值，其余原样保留待人工确认
UPDATE sys_user SET grade = LEFT(grade, 4)
 WHERE grade REGEXP '^[0-9]{4}级$';

-- 4.6 证书类型
UPDATE certificate SET type = CASE type
        WHEN '教学表达' THEN 'EXPRESSION'
        WHEN '课堂互动' THEN 'INTERACTION'
        WHEN '控场' THEN 'CONTROL'     WHEN '课堂控场' THEN 'CONTROL'
        WHEN '时间管理' THEN 'TIME'
        ELSE type END
 WHERE type IN ('教学表达', '课堂互动', '控场', '课堂控场', '时间管理');

-- ------------------------------------------------------------
-- 5. 列定义收敛：字典型列一律 VARCHAR(20)（字典码长度）+ COMMENT 改指字典
--    COMMENT 不再罗列取值——取值权威源已唯一收敛到 sys_dict_item，两处罗列必然漂移
-- ------------------------------------------------------------
ALTER TABLE sys_user
    MODIFY COLUMN role VARCHAR(20) NOT NULL
        COMMENT '角色（字典 user_role，值为字典码）',
    MODIFY COLUMN major VARCHAR(20) DEFAULT NULL
        COMMENT '专业（字典 major，值为字典码；NULL 表示师范生尚未绑定，见 UC-S01）',
    MODIFY COLUMN grade VARCHAR(20) DEFAULT NULL
        COMMENT '年级（字典 grade，值为字典码，取值口径为入学年份；NULL 表示尚未绑定）';

ALTER TABLE clazz
    MODIFY COLUMN course VARCHAR(20) NOT NULL
        COMMENT '课程（字典 subject，值为字典码；课程与 task.subject 同域，共用一套学科字典）';

ALTER TABLE task
    MODIFY COLUMN subject VARCHAR(20) NOT NULL
        COMMENT '学科（字典 subject，值为字典码）',
    MODIFY COLUMN stage VARCHAR(20) NOT NULL
        COMMENT '学段（字典 stage，值为字典码）',
    MODIFY COLUMN textbook_version VARCHAR(20) DEFAULT NULL
        COMMENT '教材版本（字典 textbook_version，值为字典码；NULL 表示不限版本）',
    MODIFY COLUMN mode VARCHAR(20) NOT NULL
        COMMENT '训练模式（字典 teach_mode，值为字典码；四要素定义见技术方案 2.2）',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
        COMMENT '状态（字典 task_status，值为字典码）';

ALTER TABLE trial
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
        COMMENT '状态（字典 trial_status，值为字典码）';

ALTER TABLE evaluation_report
    MODIFY COLUMN evaluator_type VARCHAR(20) NOT NULL DEFAULT 'RULE'
        COMMENT '评测引擎类型（字典 evaluator_type，值为字典码；迭代 5 预留 ASR / POSTURE，与 media_analysis_json 同步启用）';

ALTER TABLE question_bank
    MODIFY COLUMN subject VARCHAR(20) NOT NULL
        COMMENT '学科（字典 subject，值为字典码）',
    MODIFY COLUMN stage VARCHAR(20) NOT NULL
        COMMENT '学段（字典 stage，值为字典码）',
    MODIFY COLUMN type VARCHAR(20) NOT NULL
        COMMENT '类型（字典 question_type，值为字典码）';

ALTER TABLE resource
    MODIFY COLUMN type VARCHAR(20) NOT NULL
        COMMENT '类型（字典 resource_type，值为字典码；RUBRIC 由管理员维护，教师发布任务时复制进 task.rubric_json）',
    MODIFY COLUMN subject VARCHAR(20) NOT NULL
        COMMENT '学科（字典 subject，值为字典码；通用模板填 GENERAL）';

ALTER TABLE certificate
    MODIFY COLUMN type VARCHAR(20) NOT NULL
        COMMENT '证书类型（字典 cert_type，值为字典码）',
    MODIFY COLUMN issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '颁发时间（业务时间，补发时可回溯指定；正常颁发时等于 created_at）',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间（审计列，记录入库时刻）';

-- ------------------------------------------------------------
-- 6. 迁移校验：以下查询**预期均返回 0 行**
--    有返回行说明存在「不在字典里的取值」，需人工确认后再补一条 UPDATE（本脚本可重跑）
-- ------------------------------------------------------------
SELECT 'clazz.course 存在字典外取值' AS problem, id, course AS bad_value FROM clazz
 WHERE course NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'subject')
UNION ALL
SELECT 'task.subject 存在字典外取值', id, subject FROM task
 WHERE subject NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'subject')
UNION ALL
SELECT 'task.stage 存在字典外取值', id, stage FROM task
 WHERE stage NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'stage')
UNION ALL
SELECT 'task.mode 存在字典外取值', id, mode FROM task
 WHERE mode NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'teach_mode')
UNION ALL
SELECT 'task.status 存在字典外取值', id, status FROM task
 WHERE status NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'task_status')
UNION ALL
SELECT 'trial.status 存在字典外取值', id, status FROM trial
 WHERE status NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'trial_status')
UNION ALL
SELECT 'sys_user.role 存在字典外取值', id, role FROM sys_user
 WHERE role NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'user_role')
UNION ALL
SELECT 'sys_user.major 存在字典外取值', id, major FROM sys_user
 WHERE major IS NOT NULL AND major NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'major')
UNION ALL
SELECT 'sys_user.grade 存在字典外取值', id, grade FROM sys_user
 WHERE grade IS NOT NULL AND grade NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'grade')
UNION ALL
SELECT 'question_bank.subject 存在字典外取值', id, subject FROM question_bank
 WHERE subject NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'subject')
UNION ALL
SELECT 'question_bank.stage 存在字典外取值', id, stage FROM question_bank
 WHERE stage NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'stage')
UNION ALL
SELECT 'question_bank.type 存在字典外取值', id, type FROM question_bank
 WHERE type NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'question_type')
UNION ALL
SELECT 'resource.type 存在字典外取值', id, type FROM resource
 WHERE type NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'resource_type')
UNION ALL
SELECT 'resource.subject 存在字典外取值', id, subject FROM resource
 WHERE subject NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'subject')
UNION ALL
SELECT 'certificate.type 存在字典外取值', id, type FROM certificate
 WHERE type NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'cert_type')
UNION ALL
SELECT 'evaluation_report.evaluator_type 存在字典外取值', id, evaluator_type FROM evaluation_report
 WHERE evaluator_type NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'evaluator_type');
