-- ============================================================
-- 师范专业 AI 试讲台系统 —— 数据库初始化脚本
-- 环境：MySQL 8.x
-- 用法：mysql -u root -p < docs/init.sql
-- 说明：
--   1. 建库 ai_trial_platform + 10 张核心表（对应 docs/技术方案.md 第 5 节）
--   2. 逻辑删除统一使用 deleted 列（配合 MyBatis-Plus 配置）
--   3. sys_user 表不预置账号：管理员账号由应用首次启动时初始化
--      （BCrypt 加密密码无法在 SQL 中静态生成），或手动执行：
--      迭代 1 完成后会提供 DataInitializer 自动创建 admin/admin123
-- ============================================================

DROP DATABASE IF EXISTS ai_trial_platform;
CREATE DATABASE ai_trial_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_trial_platform;

-- ------------------------------------------------------------
-- 1. 用户表：师范生 / 教师 / 管理员
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名（学号/工号），唯一',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    real_name   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL COMMENT '角色：STUDENT / TEACHER / ADMIN',
    major       VARCHAR(50)  DEFAULT NULL COMMENT '专业（师范生）',
    grade       VARCHAR(20)  DEFAULT NULL COMMENT '年级',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT ='用户表';

-- ------------------------------------------------------------
-- 2. 班级表：教师建班、关联课程
-- ------------------------------------------------------------
CREATE TABLE clazz (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50) NOT NULL COMMENT '班级名称',
    course      VARCHAR(50) NOT NULL COMMENT '课程（语文/数学/英语/思政/幼教等）',
    teacher_id  BIGINT      NOT NULL COMMENT '带班教师（sys_user.id）',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_teacher (teacher_id)
) ENGINE = InnoDB COMMENT ='班级表';

-- ------------------------------------------------------------
-- 3. 班级-学生关系表（多对多）
-- ------------------------------------------------------------
CREATE TABLE clazz_student (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    clazz_id   BIGINT   NOT NULL COMMENT '班级 id',
    student_id BIGINT   NOT NULL COMMENT '学生 id（sys_user.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入班时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_clazz_student (clazz_id, student_id),
    KEY idx_student (student_id)
) ENGINE = InnoDB COMMENT ='班级-学生关系表';

-- ------------------------------------------------------------
-- 4. 试讲任务表：教师发布（课时/学段/教材版本/评分标准/模式）
-- ------------------------------------------------------------
CREATE TABLE task (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    title             VARCHAR(100)  NOT NULL COMMENT '任务标题',
    clazz_id          BIGINT        NOT NULL COMMENT '发布到的班级',
    subject           VARCHAR(50)   NOT NULL COMMENT '学科',
    stage             VARCHAR(20)   NOT NULL COMMENT '学段（小学/初中/高中）',
    textbook_version  VARCHAR(50)   DEFAULT NULL COMMENT '教材版本（人教/北师大等）',
    mode              VARCHAR(20)   NOT NULL COMMENT '训练模式：FREE 自由试讲 / SCENARIO 情景模拟 / STRUCTURED 结构化面试 / EXAM 模拟考核',
    duration_minutes  INT           NOT NULL COMMENT '规定时长（分钟）',
    deadline          DATETIME      DEFAULT NULL COMMENT '截止时间',
    rubric_json       TEXT          NOT NULL COMMENT '评分标准 JSON（维度/权重/知识点清单，见 docs/技术方案.md 6.2）',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT / PUBLISHED / CLOSED',
    created_by        BIGINT        NOT NULL COMMENT '发布教师 id',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_clazz (clazz_id),
    KEY idx_deadline (deadline)
) ENGINE = InnoDB COMMENT ='试讲任务表';

-- ------------------------------------------------------------
-- 5. 试讲记录表：学生提交（试讲稿 + 环节时长，音视频可选）
-- ------------------------------------------------------------
CREATE TABLE trial (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id           BIGINT       NOT NULL COMMENT '任务 id',
    student_id        BIGINT       NOT NULL COMMENT '学生 id',
    script_text       MEDIUMTEXT   NOT NULL COMMENT '试讲稿（含 [环节]、[板书] 标记）',
    audio_path        VARCHAR(255) DEFAULT NULL COMMENT '音视频文件路径（可选，迭代 4）',
    duration_actual   INT          DEFAULT NULL COMMENT '实际时长（分钟）',
    stage_times_json  TEXT         DEFAULT NULL COMMENT '各环节实测时长 JSON',
    status            VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：DRAFT / SUBMITTED / EVALUATED',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_task_student (task_id, student_id),
    KEY idx_student_created (student_id, created_at)
) ENGINE = InnoDB COMMENT ='试讲记录表';

-- ------------------------------------------------------------
-- 6. 评测报告表：AI 规则引擎生成（试讲 1—1 报告）
-- ------------------------------------------------------------
CREATE TABLE evaluation_report (
    id                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    trial_id             BIGINT         NOT NULL COMMENT '试讲 id（唯一）',
    total_score          DECIMAL(5, 1)  NOT NULL COMMENT '总分',
    dimension_scores_json TEXT          NOT NULL COMMENT '六维分数 JSON（维度/得分/权重）',
    issues_json          TEXT           DEFAULT NULL COMMENT '问题定位 JSON',
    suggestions_json     TEXT           DEFAULT NULL COMMENT '改进建议 JSON',
    evaluator_type       VARCHAR(20)    NOT NULL DEFAULT 'RULE' COMMENT '评测引擎类型：RULE / LLM',
    created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评测时间',
    updated_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trial (trial_id)
) ENGINE = InnoDB COMMENT ='评测报告表';

-- ------------------------------------------------------------
-- 7. 题库表：试讲题 / 结构化面试题 / 教资真题
-- ------------------------------------------------------------
CREATE TABLE question_bank (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject     VARCHAR(50)  NOT NULL COMMENT '学科',
    stage       VARCHAR(20)  NOT NULL COMMENT '学段',
    type        VARCHAR(20)  NOT NULL COMMENT '类型：TRIAL 试讲题 / STRUCTURED 结构化 / REAL 教资真题',
    content     TEXT         NOT NULL COMMENT '题目内容',
    answer_hint TEXT         DEFAULT NULL COMMENT '答题要点/参考答案提示',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_subject_stage (subject, stage)
) ENGINE = InnoDB COMMENT ='题库表';

-- ------------------------------------------------------------
-- 8. 资源表：教案/板书模板、优秀试讲范例
-- ------------------------------------------------------------
CREATE TABLE resource (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    type       VARCHAR(20)  NOT NULL COMMENT '类型：LESSON_PLAN 教案 / BOARD 板书模板 / EXAMPLE 试讲范例',
    subject    VARCHAR(50)  NOT NULL COMMENT '学科',
    title      VARCHAR(100) NOT NULL COMMENT '标题',
    content    TEXT         DEFAULT NULL COMMENT '文本内容',
    file_path  VARCHAR(255) DEFAULT NULL COMMENT '附件路径（可选）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type_subject (type, subject)
) ENGINE = InnoDB COMMENT ='资源表';

-- ------------------------------------------------------------
-- 9. 教师批阅表：试讲 1—N 批阅
-- ------------------------------------------------------------
CREATE TABLE review (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    trial_id   BIGINT       NOT NULL COMMENT '试讲 id',
    teacher_id BIGINT       NOT NULL COMMENT '批阅教师 id',
    score      DECIMAL(5, 1) NOT NULL COMMENT '教师评分',
    comment    TEXT         DEFAULT NULL COMMENT '文字点评',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '批阅时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_trial (trial_id)
) ENGINE = InnoDB COMMENT ='教师批阅表';

-- ------------------------------------------------------------
-- 10. 达标证书表
-- ------------------------------------------------------------
CREATE TABLE certificate (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT       NOT NULL COMMENT '学生 id',
    task_id    BIGINT       NOT NULL COMMENT '任务 id',
    type       VARCHAR(50)  NOT NULL COMMENT '证书类型（教学表达/课堂互动/控场/时间管理）',
    file_path  VARCHAR(255) NOT NULL COMMENT '证书文件路径',
    issued_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '颁发时间',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_student (student_id)
) ENGINE = InnoDB COMMENT ='达标证书表';

-- ============================================================
-- 种子数据（演示用，迭代 2 起逐步补充）
-- ============================================================

-- 题库：小学数学试讲题示例
INSERT INTO question_bank (subject, stage, type, content, answer_hint) VALUES
('数学', '小学', 'TRIAL', '试讲题目：《分数的初步认识》（人教版三年级上册）\n要求：10 分钟无生试讲，含导入、新授、练习、小结、作业五环节。',
 '导入可用分月饼情境；新授突出"平均分"概念；练习设计由浅入深；注意使用直观教具语言。'),
('数学', '小学', 'STRUCTURED', '结构化面试：上课时有学生当众指出你的板书错误，你怎么办？',
 '答题思路：① 保持冷静、坦然承认；② 表扬学生认真观察；③ 顺势引导学生共同纠错；④ 课后反思备课与板书检查习惯。'),
('语文', '小学', 'TRIAL', '试讲题目：《观潮》（人教版四年级上册）第二课时\n要求：8 分钟试讲，重点讲"潮来时"段落，设计一处 [板书]。',
 '抓住"声音—样子"两条线索；重点词句品读（闷雷滚动、白浪翻滚）；朗读指导要有层次。');

-- 资源：教案模板与板书模板示例
INSERT INTO resource (type, subject, title, content) VALUES
('LESSON_PLAN', '通用', '试讲教案通用模板', '一、教学目标（知识与技能/过程与方法/情感态度价值观）\n二、教学重难点\n三、教学过程（导入—新授—练习—小结—作业，标注各环节时长）\n四、板书设计\n五、教学反思'),
('BOARD', '通用', '板书设计要点', '1. 结构清晰：主板书（课题+知识框架）与副板书（临时演算）分区；\n2. 书写规范：笔顺正确、大小适中、不用繁体/异体字；\n3. 与讲解同步：边讲边写，不背对学生长时间书写。');
