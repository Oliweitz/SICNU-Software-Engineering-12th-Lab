-- ============================================================
-- 师范专业 AI 试讲台系统 —— 数据库初始化脚本（唯一权威源）
-- 数据库：MySQL 8.0.x（已在 8.0.45 实跑验证）
-- 用法：mysql -u root -p < docs/init.sql
--
-- 组织方式：按数据库设计七阶段编排，章节与内容一一对应
--   一、需求分析        业务目标 / 角色用例 / 数据对象 / 数据量与假设
--   二、概念结构设计    实体、属性、实体间联系（E-R）
--   三、逻辑结构设计    关系模式、主键与唯一键、范式、引用完整性策略
--   四、物理结构设计    存储引擎、字符集、数据类型、索引、审计列
--   五、数据库实施      建库、建表、种子数据
--   六、运行与维护      备份恢复、监控巡检、变更迁移、数据质量校验、版本演进
--   七、安全性设计      账号权限、认证口令、敏感数据、审计与恢复
--
-- 变更说明（2026-10-24）：
--   1. 原 docs/migration/V2__data_dict.sql 的全部内容已合并进本文件
--      （数据字典两表、字典型列收敛为字典码、存量值映射、迁移校验查询），
--      新环境只需执行本文件，无需再跑 V2。
--   2. 本文件是表结构与数据字典取值的唯一权威源；字段增删先改本文件，
--      再按《数据库设计规范》§10 补 docs/migration/V<n>__<描述>.sql。
--   3. 枚举与字典型列的取值权威源是 sys_dict_item 表，不是列 COMMENT；
--      列 COMMENT 只写字典编码（如「字典 subject」），不重复罗列取值。
--   4. V3（RBAC 权限模型）：新增 sys_role / sys_permission / sys_user_role /
--      sys_role_permission 四表，角色改为「用户 N—N 角色」多对多；原
--      sys_user.role 单列与字典 user_role 一并移除，角色取值权威源改为 sys_role。
--
-- ⚠️ 本脚本含 DROP DATABASE，会清空同名库的全部数据！
--    仅用于全新环境初始化；已有数据的环境请勿执行，改用 docs/migration/ 的增量脚本。
-- ============================================================

-- ⚠️ 必须先声明连接字符集！
-- Windows 中文环境下 mysql 客户端默认 character_set_client=gbk，
-- 会把本文件的 UTF-8 中文当作 GBK 解读，导致注释与种子数据全部乱码
-- （或直接报 ERROR 1366 Incorrect string value）。
-- 显式 SET NAMES 后，`mysql -u root -p < docs/init.sql` 无需额外参数即可正确执行。
SET NAMES utf8mb4;

-- ============================================================
-- 一、需求分析
-- ============================================================
-- 1.1 业务目标
--   面向师范专业学生，提供「发布试讲任务 → 提交试讲 → AI 评测 → 教师批阅 →
--   达标发证」的闭环训练平台，覆盖试讲稿、环节时长、六维评分、问题定位与改进建议。
--
-- 1.2 角色与核心用例
--   | 角色   | 核心用例 |
--   | 师范生 | 查看本班任务、提交试讲（稿件/音视频/自评）、查看报告与证书、查询题库与资源 |
--   | 教师   | 建班、管理学生、发布任务（含评分标准）、批阅试讲、查看班级统计 |
--   | 管理员 | 用户与字典维护、题库/资源维护、评测与证书规则配置 |
--
-- 1.3 数据对象（实体）清单（16 张表）
--   系统类（7）：sys_user 用户、sys_role 角色、sys_permission 权限点、
--              sys_user_role 用户-角色、sys_role_permission 角色-权限、
--              sys_dict_type 字典类型、sys_dict_item 字典项
--   业务类（9）：clazz 班级、clazz_student 班级-学生、task 试讲任务、trial 试讲记录、
--           evaluation_report 评测报告、review 教师批阅、certificate 达标证书、
--           question_bank 题库、resource 资源
--   预留（迭代 4，暂不建表）：classroom_session 虚拟课堂会话、
--           student_behavior_event 虚拟学生行为事件
--
-- 1.4 数据量与性能假设（当前阶段）
--   单校/单专业规模：用户 10^3 量级，任务 10^3，试讲与报告 10^4~10^5；
--   以 OLTP 点查与列表查询为主，暂不需要分区/分表；
--   JSON 列仅存评测明细等非检索型数据，检索条件一律走普通列 + 索引。
--
-- 1.5 关键业务约束（需求侧）
--   R-1 一个试讲仅一份 AI 报告（evaluation_report.trial_id 唯一）。
--   R-2 任务/试讲状态流转权限归各自模块，数据库只存状态码。
--   R-3 学生只能看到本班任务；班级成员关系由 clazz_student 维护。
--   R-4 证书按「试讲 + 技能类型」颁发，允许同一学生多次获得同类证书（历史留痕）。
--   R-5 枚举取值统一登记在数据字典，业务列只存字典码。
--   R-6 业务数据一律逻辑删除（deleted），不做物理删除。
--   R-7 权限模型为 RBAC：用户经 sys_user_role 关联多个角色（STUDENT / TEACHER / ADMIN），
--       角色经 sys_role_permission 关联权限点；鉴权只认权限码，不硬编码角色判断。

-- ============================================================
-- 二、概念结构设计（E-R）
-- ============================================================
-- 2.1 实体与主标识
--   sys_user(id)、clazz(id)、clazz_student(id)、task(id)、trial(id)、
--   evaluation_report(id)、review(id)、certificate(id)、question_bank(id)、
--   resource(id)、sys_dict_type(id)、sys_dict_item(id)、
--   sys_role(id)、sys_permission(id)、sys_user_role(id)、sys_role_permission(id)
--
-- 2.2 实体间联系（基数）
--   sys_user(教师) 1 ──< N clazz                    （clazz.teacher_id）
--   clazz 1 ──< N clazz_student >── N sys_user       （M:N，落表 clazz_student）
--   clazz 1 ──< N task                              （task.clazz_id）
--   task  1 ──< N trial                             （trial.task_id）
--   sys_user(学生) 1 ──< N trial                    （trial.student_id）
--   trial 1 ── 1 evaluation_report                  （evaluation_report.trial_id 唯一）
--   trial 1 ──< N review                            （review.trial_id）
--   sys_user(教师) 1 ──< N review                   （review.teacher_id）
--   sys_user(学生) 1 ──< N certificate               （certificate.student_id）
--   task 1 ──< N certificate                        （certificate.task_id）
--   sys_dict_type 1 ──< N sys_dict_item             （按 dict_type 编码关联）
--   sys_user N ──< sys_user_role >── N sys_role     （M:N，一个用户可有多角色）
--   sys_role N ──< sys_role_permission >── N sys_permission（M:N，角色授权的权限点）
--   question_bank / resource 为独立主数据，被引用时以「复制快照」方式进入业务表
--     （resource(type=RUBRIC) → task.rubric_json），不建立强引用。
--
-- 2.3 E-R 示意（文字版）
--   [sys_user]──教师──<[clazz]──<[clazz_student]>──学生──[sys_user]
--        │                 │
--        │                 └──<[task]──<[trial]──1:1──[evaluation_report]
--        │                              │
--        │                              ├──<[review]
--        │                              └──<[certificate]
--        └──<[review]、[certificate]
--   [sys_dict_type]──<[sys_dict_item]
--   [sys_user]──<[sys_user_role]>──[sys_role]──<[sys_role_permission]>──[sys_permission]
--   [question_bank]   [resource]

-- ============================================================
-- 三、逻辑结构设计（关系模式）
-- ============================================================
-- 3.1 关系模式（主键为 id；[UK] 唯一键；{FK} 逻辑外键）
--   sys_user(id, username[UK], password, real_name, major, grade,
--            created_at, updated_at, deleted)          ← 角色移出本表，见 sys_user_role
--   clazz(id, name, course, teacher_id{FK sys_user.id}, created_at, updated_at, deleted)
--   clazz_student(id, clazz_id{FK}, student_id{FK}, created_at, [UK(clazz_id, student_id)])
--   task(id, title, clazz_id{FK}, subject, stage, textbook_version, mode,
--        duration_minutes, deadline, rubric_json, status, created_by{FK},
--        created_at, updated_at, deleted)
--   trial(id, task_id{FK}, student_id{FK}, script_text, audio_path, video_path,
--         self_assessment_json, duration_actual, stage_times_json, status,
--         created_at, updated_at, deleted)
--   evaluation_report(id, trial_id{FK,UK}, total_score, dimension_scores_json,
--         issues_json, suggestions_json, evaluator_type, media_analysis_json,
--         created_at, updated_at, deleted)
--   review(id, trial_id{FK}, teacher_id{FK}, score, comment,
--          created_at, updated_at, deleted)
--   certificate(id, student_id{FK}, task_id{FK}, type, file_path, issued_at,
--               created_at, deleted)
--   question_bank(id, subject, stage, type, content, answer_hint,
--                 created_at, updated_at, deleted)
--   resource(id, type, subject, title, content, file_path,
--            created_at, updated_at, deleted)
--   sys_dict_type(id, dict_type[UK], dict_name, remark, created_at, updated_at, deleted)
--   sys_dict_item(id, dict_type, item_value, item_label, sort_no, enabled,
--                 created_at, updated_at, deleted, [UK(dict_type, item_value)])
--   sys_role(id, role_code[UK], role_name, description, created_at, updated_at, deleted)
--   sys_permission(id, perm_code[UK], perm_name, module, perm_type, sort_no,
--                  created_at, updated_at, deleted)
--   sys_user_role(id, user_id{FK}, role_id{FK}, created_at, [UK(user_id, role_id)])
--   sys_role_permission(id, role_id{FK}, permission_id{FK}, created_at,
--                       [UK(role_id, permission_id)])
--
-- 3.2 范式与反范式
--   全部关系模式满足 3NF：非主属性完全依赖主键，无传递依赖。
--   有意反范式（受控冗余）：
--     * task.subject 与 clazz.course 同域但各自存储——发布时固化，避免班级改课程影响历史任务。
--     * evaluation_report.dimension_scores_json 明细入 JSON——维度集合可配置，拆表会引入
--       动态 DDL；该数据不参与检索，只随报告整体读写。
--     * task.rubric_json 为 resource(type=RUBRIC) 的发布时快照——保证评分标准不随后续模板修改而漂移。
--
-- 3.3 主键与唯一性
--   所有表使用代理主键 id BIGINT AUTO_INCREMENT；
--   业务唯一性用唯一键表达：username、dict_type、(dict_type,item_value)、
--   (clazz_id,student_id)、trial_id、role_code、perm_code、
--   (user_id,role_id)、(role_id,permission_id)。
--   说明：唯一键不感知逻辑删除，删除后同名记录不可再次插入（规范 §4.3 已知限制）。
--
-- 3.4 引用完整性策略：逻辑外键，不建物理 FOREIGN KEY
--   理由（规范 §5.3）：
--     1) 业务使用逻辑删除，物理外键无法表达「父行已删、子行保留」的语义；
--     2) 批量导入/数据修复需要可控的执行顺序，物理外键会增加运维成本；
--     3) 外键校验开销与锁行为不利于后续扩展。
--   替代措施：所有逻辑外键列必须建索引（见 4.4），由应用层在写入前校验父行存在。
--
-- 3.5 命名规范
--   表名/列名小写下划线；系统表 sys_ 前缀；普通索引 idx_ 前缀；唯一键 uk_ 前缀。
--   完整规约见 docs/数据库设计规范.md §1。

-- ============================================================
-- 四、物理结构设计
-- ============================================================
-- 4.1 存储引擎与字符集
--   引擎：InnoDB（事务、行锁、崩溃恢复、MVCC）
--   字符集：utf8mb4；排序规则：utf8mb4_0900_ai_ci（MySQL 8.0 默认，大小写不敏感）
--   库级统一设置，表级继承，不在单表重复声明 CHARSET。
--
-- 4.2 主键与自增
--   BIGINT（8 字节）自增，避免 INT 溢出；自增顺序写入利于 InnoDB 聚簇索引顺序插入。
--
-- 4.3 数据类型选型
--   主键/外键            BIGINT
--   字典码/枚举          VARCHAR(20)，与 sys_dict_item.item_value 对齐；
--                        不用 ENUM（增删取值需 DDL，且与数据字典双轨冲突）
--   名称/标题            VARCHAR(50~100)，按规范 §2.1 长度表
--   长文本              TEXT / MEDIUMTEXT（试讲稿用 MEDIUMTEXT）
--   分数                DECIMAL(5,1)，精确小数，范围 0.0~100.0，禁用 FLOAT
--   时间                DATETIME；业务时间与审计时间分列
--                        （certificate.issued_at / created_at）
--   结构化明细          JSON，仅存非检索数据；需检索的字段必须提升为普通列
--   布尔/状态标记       TINYINT（deleted 0/1、enabled 0/1）
--
-- 4.4 索引设计
--   原则（规范 §4.1）：
--     1) 逻辑外键列必须有索引；
--     2) 复合索引遵循最左前缀，已被覆盖的单列查询不再单建索引；
--     3) 唯一键已覆盖的查询不重复建普通索引；
--     4) 索引服务于已明确的查询，不为「可能用到」预建。
--   索引清单：
--     sys_user            uk_username(username)
--     sys_role            uk_role_code(role_code)
--     sys_permission      uk_perm_code(perm_code)、idx_module(module)
--     sys_user_role       uk_user_role(user_id,role_id)、idx_role_id(role_id)
--     sys_role_permission uk_role_permission(role_id,permission_id)、idx_permission_id(permission_id)
--     clazz               idx_teacher(teacher_id)
--     clazz_student       uk_clazz_student(clazz_id,student_id)、idx_student(student_id)
--     task                idx_clazz(clazz_id)、idx_created_by(created_by)、idx_deadline(deadline)
--     trial               idx_task_student(task_id,student_id)、idx_student_created(student_id,created_at)
--     evaluation_report   uk_trial(trial_id)
--     review              idx_trial(trial_id)、idx_teacher(teacher_id)
--     certificate         idx_student(student_id)、idx_task(task_id)
--     question_bank       idx_subject_stage(subject,stage)
--     resource            idx_type_subject(type,subject)
--     sys_dict_type       uk_dict_type(dict_type)
--     sys_dict_item       uk_type_value(dict_type,item_value)
--   说明：clazz_student 的 uk 最左前缀 clazz_id 已覆盖「按班级查学生」，故不再单建 idx_clazz。
--
-- 4.5 审计列与逻辑删除
--   标准审计列：created_at、updated_at（ON UPDATE CURRENT_TIMESTAMP）、deleted。
--   豁免白名单（规范 §3：纯关系表无 updated_at / deleted）：
--     clazz_student 班级-学生关系；
--     sys_user_role 用户-角色关系；
--     sys_role_permission 角色-权限关系；
--     certificate 只增不改：仅有 updated_at 豁免。
--   逻辑删除：deleted=0 正常、1 已删除；应用查询必须带 deleted=0 条件。

-- ============================================================
-- 五、数据库实施
-- ============================================================

-- ------------------------------------------------------------
-- 5.1 建库
-- ------------------------------------------------------------
DROP DATABASE IF EXISTS ai_trial_platform;
CREATE DATABASE ai_trial_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE ai_trial_platform;

-- ------------------------------------------------------------
-- 5.2 数据字典（sys_ 前缀系统表，取值权威源）
--     通用两表结构：新增字典类型无需 DDL，只加数据行
-- ------------------------------------------------------------

-- 1. 数据字典类型表：登记全项目**有哪些字典**
CREATE TABLE sys_dict_type (
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

-- 2. 数据字典项表：登记每个字典**有哪些取值**
--    业务表存 item_value（字典码），前端展示 item_label（中文标签）
CREATE TABLE sys_dict_item (
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
-- 5.3 业务表
-- ------------------------------------------------------------

-- 3. 用户表：师范生 / 教师 / 管理员
CREATE TABLE sys_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50)  NOT NULL COMMENT '登录名（学号/工号），全局唯一',
    password   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密，定长 60 字符）',
    real_name  VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    major      VARCHAR(20)  DEFAULT NULL COMMENT '专业（字典 major，值为字典码；NULL 表示师范生尚未绑定，见 UC-S01）',
    grade      VARCHAR(20)  DEFAULT NULL COMMENT '年级（字典 grade，值为字典码，取值口径为入学年份；NULL 表示尚未绑定）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：登录时按登录名查用户
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT ='用户表';

-- 4. 班级表：教师建班、关联课程
CREATE TABLE clazz (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(50) NOT NULL COMMENT '班级名称',
    course     VARCHAR(20) NOT NULL COMMENT '课程（字典 subject，值为字典码；课程与 task.subject 同域，共用一套学科字典）',
    teacher_id BIGINT      NOT NULL COMMENT '带班教师（sys_user.id）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：教师查看自己带的班级
    KEY idx_teacher (teacher_id)
) ENGINE = InnoDB COMMENT ='班级表';

-- 5. 班级-学生关系表（多对多）
--    纯关系表：豁免 updated_at / deleted（见 docs/数据库设计规范.md 第 3 节白名单）
CREATE TABLE clazz_student (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    clazz_id   BIGINT   NOT NULL COMMENT '班级 id（clazz.id）',
    student_id BIGINT   NOT NULL COMMENT '学生 id（sys_user.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入班时间',
    PRIMARY KEY (id),
    -- 服务查询：某班全部学生 / 防重复入班（最左前缀覆盖 clazz_id 单列查询）
    UNIQUE KEY uk_clazz_student (clazz_id, student_id),
    -- 服务查询：某学生的全部班级
    KEY idx_student (student_id)
) ENGINE = InnoDB COMMENT ='班级-学生关系表';

-- 6. 试讲任务表：教师发布（课时/学段/教材版本/评分标准/模式）
CREATE TABLE task (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title            VARCHAR(100) NOT NULL COMMENT '任务标题',
    clazz_id         BIGINT       NOT NULL COMMENT '发布到的班级（clazz.id）',
    subject          VARCHAR(20)  NOT NULL COMMENT '学科（字典 subject，值为字典码）',
    stage            VARCHAR(20)  NOT NULL COMMENT '学段（字典 stage，值为字典码）',
    textbook_version VARCHAR(20)  DEFAULT NULL COMMENT '教材版本（字典 textbook_version，值为字典码；NULL 表示不限版本）',
    mode             VARCHAR(20)  NOT NULL COMMENT '训练模式（字典 teach_mode，值为字典码；四要素定义见技术方案 2.2）',
    duration_minutes INT          NOT NULL COMMENT '规定时长（分钟，> 0）',
    deadline         DATETIME     DEFAULT NULL COMMENT '截止时间（NULL 表示不设截止，任务长期有效）',
    rubric_json      JSON         NOT NULL COMMENT '评分标准 JSON（维度 / 权重 / 知识点清单，见技术方案 6.2；可由 resource(type=RUBRIC) 模板复制而来。各维度 weight 必须 > 0，总分按 Σ权重 归一化，权重之和不必为 100）',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态（字典 task_status，值为字典码）',
    created_by       BIGINT       NOT NULL COMMENT '发布教师 id（sys_user.id），固化实际发布人',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：学生查看本班任务
    KEY idx_clazz (clazz_id),
    -- 服务查询：教师查看自己发布的任务
    KEY idx_created_by (created_by),
    -- 服务查询：按截止时间排序 / 即将截止提醒
    KEY idx_deadline (deadline)
) ENGINE = InnoDB COMMENT ='试讲任务表';

-- 7. 试讲记录表：学生提交（试讲稿 + 环节时长 + 自评量表，音视频可选）
CREATE TABLE trial (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id              BIGINT       NOT NULL COMMENT '任务 id（task.id）',
    student_id           BIGINT       NOT NULL COMMENT '学生 id（sys_user.id）',
    script_text          MEDIUMTEXT   NOT NULL COMMENT '试讲稿（含 [环节]、[板书] 标记）',
    audio_path           VARCHAR(255) DEFAULT NULL COMMENT '音频文件路径（可选，迭代 4）',
    video_path           VARCHAR(255) DEFAULT NULL COMMENT '视频文件路径（可选，迭代 4）',
    self_assessment_json JSON         DEFAULT NULL COMMENT '自评量表 JSON（10 项自评；普通话/教姿教态保底方案的数据来源，提交试讲时录入）',
    duration_actual      INT          DEFAULT NULL COMMENT '实际时长（分钟，>= 0；未录入时为 NULL）',
    stage_times_json     JSON         DEFAULT NULL COMMENT '各环节实测时长 JSON（key 为环节名，value 为秒）',
    status               VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态（字典 trial_status，值为字典码）',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：某任务下某学生的试讲记录 / 防同一任务重复提交
    KEY idx_task_student (task_id, student_id),
    -- 服务查询：学生的历史试讲列表与进步曲线（按时间排序）
    KEY idx_student_created (student_id, created_at)
) ENGINE = InnoDB COMMENT ='试讲记录表';

-- 8. 评测报告表：AI 评测引擎生成（试讲 1—1 报告）
CREATE TABLE evaluation_report (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    trial_id              BIGINT        NOT NULL COMMENT '试讲 id（trial.id），一次试讲仅一份报告',
    total_score           DECIMAL(5, 1) NOT NULL COMMENT '总分（0.0~100.0）',
    dimension_scores_json JSON          NOT NULL COMMENT '六维分数 JSON（维度 key / 得分 / 权重）',
    issues_json           JSON          DEFAULT NULL COMMENT '问题定位 JSON（问题码 / 级别 / 定位详情）',
    suggestions_json      JSON          DEFAULT NULL COMMENT '改进建议 JSON（问题码 → 建议模板）',
    evaluator_type        VARCHAR(20)   NOT NULL DEFAULT 'RULE' COMMENT '评测引擎类型（字典 evaluator_type，值为字典码；迭代 5 预留 ASR / POSTURE，与 media_analysis_json 同步启用）',
    media_analysis_json   JSON          DEFAULT NULL COMMENT '语音识别 / 人体关键点原始数据 JSON（迭代 5；API 失败回退时为 NULL）',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评测时间',
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted               TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：按试讲查报告（同时保证一讲一报告）
    UNIQUE KEY uk_trial (trial_id)
) ENGINE = InnoDB COMMENT ='评测报告表';

-- 9. 题库表：试讲题 / 结构化面试题 / 教资真题
CREATE TABLE question_bank (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject     VARCHAR(20) NOT NULL COMMENT '学科（字典 subject，值为字典码）',
    stage       VARCHAR(20) NOT NULL COMMENT '学段（字典 stage，值为字典码）',
    type        VARCHAR(20) NOT NULL COMMENT '类型（字典 question_type，值为字典码）',
    content     TEXT        NOT NULL COMMENT '题目内容',
    answer_hint TEXT        DEFAULT NULL COMMENT '答题要点 / 参考答案提示',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：按学段 + 学科筛选题目
    KEY idx_subject_stage (subject, stage)
) ENGINE = InnoDB COMMENT ='题库表';

-- 10. 资源表：教案/板书模板、优秀试讲范例、评分细则模板
CREATE TABLE resource (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    type       VARCHAR(20)  NOT NULL COMMENT '类型（字典 resource_type，值为字典码；RUBRIC 由管理员维护，教师发布任务时复制进 task.rubric_json）',
    subject    VARCHAR(20)  NOT NULL COMMENT '学科（字典 subject，值为字典码；通用模板填 GENERAL）',
    title      VARCHAR(100) NOT NULL COMMENT '标题',
    content    TEXT         DEFAULT NULL COMMENT '文本内容（RUBRIC 类型时为评分细则 JSON）',
    file_path  VARCHAR(255) DEFAULT NULL COMMENT '附件路径（可选）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：按类型 + 学科列出资源
    KEY idx_type_subject (type, subject)
) ENGINE = InnoDB COMMENT ='资源表';

-- 11. 教师批阅表：试讲 1—N 批阅
CREATE TABLE review (
    id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    trial_id   BIGINT        NOT NULL COMMENT '试讲 id（trial.id）',
    teacher_id BIGINT        NOT NULL COMMENT '批阅教师 id（sys_user.id）',
    score      DECIMAL(5, 1) NOT NULL COMMENT '教师评分（0.0~100.0）',
    comment    TEXT          DEFAULT NULL COMMENT '文字点评',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '批阅时间',
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：查看某次试讲的全部批阅
    KEY idx_trial (trial_id),
    -- 服务查询：教师的批阅历史与批阅工作量统计
    KEY idx_teacher (teacher_id)
) ENGINE = InnoDB COMMENT ='教师批阅表';

-- 12. 达标证书表
--     只增不改的业务表：豁免 updated_at（见 docs/数据库设计规范.md 第 3 节白名单）
CREATE TABLE certificate (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT      NOT NULL COMMENT '学生 id（sys_user.id）',
    task_id    BIGINT      NOT NULL COMMENT '任务 id（task.id）',
    type       VARCHAR(20) NOT NULL COMMENT '证书类型（字典 cert_type，值为字典码）',
    file_path  VARCHAR(255) NOT NULL COMMENT '证书文件路径',
    issued_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '颁发时间（业务时间，补发时可回溯指定；正常颁发时等于 created_at）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（审计列，记录入库时刻）',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：学生的证书列表
    KEY idx_student (student_id),
    -- 服务查询：某任务下的达标名单 / 按任务统计达标率
    KEY idx_task (task_id)
) ENGINE = InnoDB COMMENT ='达标证书表';

-- 13. 角色表（RBAC）：角色码为鉴权与授权的最小单位
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(20)  NOT NULL COMMENT '角色码（STUDENT 师范生 / TEACHER 教师 / ADMIN 管理员），与 Role 枚举一一对应',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称（中文展示名）',
    description VARCHAR(200) DEFAULT NULL COMMENT '职责说明（该角色可做什么，便于评审与运维理解）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：登录后按角色码装配 ROLE_xxx 授权
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB COMMENT ='角色表';

-- 14. 权限点表（RBAC）：权限码是全项目鉴权的唯一判据
CREATE TABLE sys_permission (
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
    -- 服务查询：按权限码鉴权 / 防重复登记
    UNIQUE KEY uk_perm_code (perm_code),
    -- 服务查询：管理界面按模块列出权限点
    KEY idx_module (module)
) ENGINE = InnoDB COMMENT ='权限点表';

-- 15. 用户-角色关系表（多对多）
--     纯关系表：豁免 updated_at / deleted（见 docs/数据库设计规范.md 第 3 节白名单）
CREATE TABLE sys_user_role (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '用户 id（sys_user.id）',
    role_id    BIGINT   NOT NULL COMMENT '角色 id（sys_role.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (id),
    -- 服务查询：某用户的全部角色 / 防重复授权（最左前缀覆盖按 user_id 单列查询）
    UNIQUE KEY uk_user_role (user_id, role_id),
    -- 服务查询：某角色下的全部用户
    KEY idx_role_id (role_id)
) ENGINE = InnoDB COMMENT ='用户-角色关系表';

-- 16. 角色-权限关系表（多对多）：RBAC 的授权落点
--     纯关系表：豁免 updated_at / deleted（见 docs/数据库设计规范.md 第 3 节白名单）
CREATE TABLE sys_role_permission (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT   NOT NULL COMMENT '角色 id（sys_role.id）',
    permission_id BIGINT   NOT NULL COMMENT '权限点 id（sys_permission.id）',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (id),
    -- 服务查询：某角色拥有的权限码 / 防重复授权（最左前缀覆盖按 role_id 单列查询）
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    -- 服务查询：某权限点被哪些角色持有（运维排查越权）
    KEY idx_permission_id (permission_id)
) ENGINE = InnoDB COMMENT ='角色-权限关系表';

-- ------------------------------------------------------------
-- 5.4 种子数据（演示用，迭代 2 起逐步补充）
-- ------------------------------------------------------------

-- 数据字典：全项目枚举与字典型列的取值权威源（列 COMMENT 不再重复罗列取值）
INSERT INTO sys_dict_type (dict_type, dict_name, remark) VALUES
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
('cert_type',        '证书类型',     '服务的列：certificate.type（四类技能取自题目需求 9.1，前两类与评测维度 expression / interaction 对应，控场并入课堂互动维度）'),
('perm_type',        '权限点类型',   '服务的列：sys_permission.perm_type（API 接口权限 / MENU 菜单权限）');

INSERT INTO sys_dict_item (dict_type, item_value, item_label, sort_no) VALUES
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
('cert_type',        'TIME',          '时间管理',     4),
-- 权限点类型
('perm_type',        'API',           '接口权限',     1),
('perm_type',        'MENU',          '菜单权限',     2);

-- RBAC 种子：角色（角色码与 Java Role 枚举一一对应）
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'STUDENT', '师范生', '查看本班任务、提交试讲、查看报告与证书、浏览题库资源'),
(2, 'TEACHER', '教师',   '建班与学生管理、发布/关闭任务、批阅点评、查看班级统计与导出'),
(3, 'ADMIN',   '管理员', '用户与角色权限维护、字典维护、题库资源维护、全局统计');

-- RBAC 种子：权限点（权限码为「模块:动作」，与 PermissionCodes 常量逐一对应）
INSERT INTO sys_permission (id, perm_code, perm_name, module, perm_type, sort_no) VALUES
(1,  'user:read',         '查看用户',       'user',     'API',  1),
(2,  'user:manage',       '管理用户',       'user',     'API',  2),
(3,  'clazz:read',        '查看班级',       'clazz',    'API',  3),
(4,  'clazz:manage',      '管理班级',       'clazz',    'API',  4),
(5,  'task:read',         '查看任务',       'task',     'API',  5),
(6,  'task:manage',       '发布/关闭任务',  'task',     'API',  6),
(7,  'trial:submit',      '提交试讲',       'trial',    'API',  7),
(8,  'trial:read',        '查看试讲',       'trial',    'API',  8),
(9,  'report:read',       '查看评测报告',   'report',   'API',  9),
(10, 'review:manage',     '批阅与点评',     'review',   'API', 10),
(11, 'resource:read',     '查看题库与资源', 'resource', 'API', 11),
(12, 'resource:manage',   '维护题库与资源', 'resource', 'API', 12),
(13, 'dict:read',         '查看数据字典',   'dict',     'API', 13),
(14, 'dict:manage',       '维护数据字典',   'dict',     'API', 14),
(15, 'stats:self',        '查看个人统计',   'stats',    'API', 15),
(16, 'stats:class',       '查看班级统计',   'stats',    'API', 16),
(17, 'stats:global',      '查看全局统计',   'stats',    'API', 17),
(18, 'cert:read',         '查看达标证书',   'cert',     'API', 18),
(19, 'permission:manage', '维护角色与权限', 'user',     'API', 19);

-- RBAC 种子：角色-权限授权
--   STUDENT：只读任务/试讲/报告/资源/证书 + 提交试讲 + 个人统计
--   TEACHER：在 STUDENT 的只读能力基础上，增加班级/任务/批阅管理与班级统计
--   ADMIN  ：持有全部权限点（含权限维护与全局统计），用于后台管理
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
-- STUDENT
(1, 5), (1, 7), (1, 8), (1, 9), (1, 11), (1, 15), (1, 18),
-- TEACHER
(2, 3), (2, 4), (2, 5), (2, 6), (2, 8), (2, 9), (2, 10), (2, 11), (2, 16), (2, 18),
-- ADMIN（全部 19 个权限点）
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6), (3, 7), (3, 8), (3, 9), (3, 10),
(3, 11), (3, 12), (3, 13), (3, 14), (3, 15), (3, 16), (3, 17), (3, 18), (3, 19);

-- 题库：小学数学 / 语文试讲题与结构化面试题示例
INSERT INTO question_bank (subject, stage, type, content, answer_hint) VALUES
('MATH', 'PRIMARY', 'TRIAL', '试讲题目：《分数的初步认识》（人教版三年级上册）\n要求：10 分钟无生试讲，含导入、新授、练习、小结、作业五环节。',
 '导入可用分月饼情境；新授突出"平均分"概念；练习设计由浅入深；注意使用直观教具语言。'),
('MATH', 'PRIMARY', 'STRUCTURED', '结构化面试：上课时有学生当众指出你的板书错误，你怎么办？',
 '答题思路：① 保持冷静、坦然承认；② 表扬学生认真观察；③ 顺势引导学生共同纠错；④ 课后反思备课与板书检查习惯。'),
('CHINESE', 'PRIMARY', 'TRIAL', '试讲题目：《观潮》（人教版四年级上册）第二课时\n要求：8 分钟试讲，重点讲"潮来时"段落，设计一处 [板书]。',
 '抓住"声音—样子"两条线索；重点词句品读（闷雷滚动、白浪翻滚）；朗读指导要有层次。');

-- 资源：教案模板、板书模板、评分细则模板示例
INSERT INTO resource (type, subject, title, content) VALUES
('LESSON_PLAN', 'GENERAL', '试讲教案通用模板', '一、教学目标（知识与技能/过程与方法/情感态度价值观）\n二、教学重难点\n三、教学过程（导入—新授—练习—小结—作业，标注各环节时长）\n四、板书设计\n五、教学反思'),
('BOARD', 'GENERAL', '板书设计要点', '1. 结构清晰：主板书（课题+知识框架）与副板书（临时演算）分区；\n2. 书写规范：笔顺正确、大小适中、不用繁体/异体字；\n3. 与讲解同步：边讲边写，不背对学生长时间书写。'),
('RUBRIC', 'GENERAL', '试讲评分细则默认模板', '{"dimensions":[{"key":"content","name":"教学内容","weight":25},{"key":"expression","name":"教学表达","weight":25},{"key":"posture","name":"教姿教态","weight":15},{"key":"interaction","name":"课堂互动","weight":15},{"key":"board","name":"板书呈现","weight":10},{"key":"time","name":"时间管理","weight":10}]}');

-- ------------------------------------------------------------
-- 5.5 版本演进与预留表
--     迭代 4 预留（虚拟课堂）—— 暂不建表
--     理由：P2 阶段表结构仍可能调整，空表无数据无意义；
--           迭代 4 启动时在本文件补 DDL（并按规范 §10 补增量脚本）。
--   classroom_session      一次虚拟课堂会话（trial_id / student_count / mode / script_json）
--   student_behavior_event 虚拟学生行为事件流（session_id / student_no / behavior / occurred_at）
-- ------------------------------------------------------------

-- ============================================================
-- 六、运行与维护
-- ============================================================

-- 6.1 备份与恢复
--   全量备份（不锁表，保证一致性）：
--     mysqldump --single-transaction --routines --triggers \
--       --default-character-set=utf8mb4 ai_trial_platform > backup_2026-10-24.sql
--   恢复：
--     mysql --default-character-set=utf8mb4 ai_trial_platform < backup_2026-10-24.sql
--   策略：每日全量 + binlog 增量；备份文件加密、异地留存；每学期至少一次恢复演练。
--
-- 6.2 监控与巡检
--   连接与锁：SHOW PROCESSLIST; SHOW ENGINE INNODB STATUS;
--   慢查询：performance_schema.events_statements_summary_by_digest 按总耗时排序；
--   容量：information_schema.TABLES 统计 data_length / index_length；
--   索引有效性：对高频查询执行 EXPLAIN，未命中索引时按规范 §10 走变更流程；
--   数据健康：逻辑删除占比、字典外取值（见 6.4）、唯一键冲突日志。
--
-- 6.3 变更与迁移管理
--   双轨流程（规范 §10）：
--     1) 结构变更先改 docs/init.sql（权威源）；
--     2) 再新增 docs/migration/V<n>__<描述>.sql 供已有数据的库升级，
--        脚本必须幂等、不含 DROP，并在文件头写明适用版本；
--     3) 迁移完成后执行 6.4 校验，预期 0 行。
--   版本沿革：
--     V1__db_spec_gaps.sql  证书外键索引 + 列 COMMENT 补全       → 已并入 init.sql
--     V2__data_dict.sql     数据字典两表 + 字典型列收敛 + 存量值迁移 → 已并入本文件
--     V3__rbac.sql          RBAC 四表 + sys_user.role 数据迁入 sys_user_role
--                           + 移除 role 列与 user_role 字典        → 已并入本文件
--                           （已有数据的库执行 docs/migration/V3__rbac.sql，全新库直接跑本文件）
--   存量值映射（合并自 V2，仅老库升级需要；全新库由种子数据直接写入字典码）：
--     学科：语文→CHINESE 数学→MATH 英语→ENGLISH 思政→POLITICS 幼教→PRESCHOOL
--           科学→SCIENCE 美术→ART 音乐→MUSIC 通用→GENERAL
--     学段：小学→PRIMARY 初中→JUNIOR 高中→SENIOR
--     教材：人教版/人教→PEP 北师大版→BNUP 苏教版→SJEP 外研版→FLTRP
--     专业：汉语言文学→CHINESE_EDU 数学与应用数学→MATH_EDU 英语→ENGLISH_EDU
--           思想政治教育→POLITICS_EDU 学前教育→PRESCHOOL_EDU 小学教育→PRIMARY_EDU
--     年级：'2023级' → '2023'（LEFT(grade,4)）
--     证书：教学表达→EXPRESSION 课堂互动→INTERACTION 控场/课堂控场→CONTROL 时间管理→TIME
--
-- 6.4 数据质量校验（以下查询预期均返回 0 行）
--     有返回行说明存在「不在字典里的取值」或「断链的授权关系」，需人工确认后修复
--     （老库升级时使用；全新库应始终为 0 行）。
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
SELECT 'sys_user_role 引用不存在的用户', ur.id, ur.user_id FROM sys_user_role ur
  LEFT JOIN sys_user u ON u.id = ur.user_id
 WHERE u.id IS NULL
UNION ALL
SELECT 'sys_user_role 引用不存在的角色', ur.id, ur.role_id FROM sys_user_role ur
  LEFT JOIN sys_role r ON r.id = ur.role_id
 WHERE r.id IS NULL
UNION ALL
SELECT 'sys_user 存在未分配角色的用户', u.id, u.username FROM sys_user u
  LEFT JOIN sys_user_role ur ON ur.user_id = u.id
 WHERE u.deleted = 0 AND ur.id IS NULL
UNION ALL
SELECT 'sys_role_permission 引用不存在的角色', rp.id, rp.role_id FROM sys_role_permission rp
  LEFT JOIN sys_role r ON r.id = rp.role_id
 WHERE r.id IS NULL
UNION ALL
SELECT 'sys_role_permission 引用不存在的权限点', rp.id, rp.permission_id FROM sys_role_permission rp
  LEFT JOIN sys_permission p ON p.id = rp.permission_id
 WHERE p.id IS NULL
UNION ALL
SELECT 'sys_permission 的 perm_type 存在字典外取值', id, perm_type FROM sys_permission
 WHERE perm_type NOT IN (SELECT item_value FROM sys_dict_item WHERE dict_type = 'perm_type')
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

-- ============================================================
-- 七、安全性设计
-- ============================================================
-- 7.1 账号与最小权限
--   禁止应用使用 root。按用途分账号，口令由运维替换为强随机值后再执行：
--     CREATE USER 'ai_app'@'10.0.0.%' IDENTIFIED BY '<强随机口令>';
--     GRANT SELECT, INSERT, UPDATE, DELETE ON ai_trial_platform.* TO 'ai_app'@'10.0.0.%';
--     CREATE USER 'ai_ro'@'10.0.0.%' IDENTIFIED BY '<强随机口令>';
--     GRANT SELECT ON ai_trial_platform.* TO 'ai_ro'@'10.0.0.%';
--   应用账号不授予 DROP / ALTER / CREATE / GRANT；DDL 仅由运维账号在变更窗口执行。
--   生产环境限制来源网段，不使用 '%' 通配。
--
-- 7.2 认证与口令
--   sys_user.password 只存 BCrypt 哈希（定长 60 字符），禁止明文或可逆加密；
--   登录失败计数与锁定由应用层实现；本脚本不预置默认管理员账号
--   （BCrypt 无法在 SQL 中静态生成；由应用首次启动的 DataInitializer 创建 admin/admin123）。
--
-- 7.3 敏感数据
--   当前未存手机号/身份证号；如后续新增，必须加密存储、单独授权，
--   且不得以明文进入日志与报表。
--   成绩、评语、证书文件路径属业务敏感数据：接口按角色最小可见，
--   文件路径不直接暴露公网，下载走鉴权后的应用接口。
--
-- 7.4 审计与可恢复性
--   created_at / updated_at 记录写入与变更时刻；逻辑删除保留数据可追溯；
--   关键状态流转（任务发布/关闭、报告生成、证书颁发）建议在应用层写操作审计。
--
-- 7.5 传输与备份安全
--   数据库连接启用 TLS；建议开启 require_secure_transport=ON；
--   备份文件加密存储、与生产库分机保存；恢复演练每学期至少一次。
--
-- 7.6 授权模型（RBAC）
--   用户 ──< sys_user_role >── 角色 ──< sys_role_permission >── 权限点
--   鉴权判据是权限码（sys_permission.perm_code），接口用 @PreAuthorize("hasAuthority('task:manage')")，
--   前端菜单/按钮用 GET /api/v1/rbac/me 返回的权限码集合控制显隐；禁止在业务代码里写死角色判断。
--   角色码 ROLE_xxx 也注入 Spring Security（便于 hasRole 兜底），但仅作粗粒度控制。
--   授权最小化：STUDENT 只读自有数据 + 提交试讲；TEACHER 增加班级/任务/批阅；ADMIN 持有全部权限点。
--   越权排查：按 sys_role_permission 反查「某权限点被哪些角色持有」（idx_permission_id）。
-- ============================================================
-- 脚本结束
-- ============================================================
