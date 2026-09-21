-- ============================================================
-- 师范专业 AI 试讲台系统 —— 数据库初始化脚本
-- 环境：MySQL 8.0.x（本脚本在 8.0.45 上验证通过）
-- 用法：mysql -u root -p < docs/init.sql
--
-- 说明：
--   1. 建库 ai_trial_platform + 10 张核心表（对应 docs/技术方案.md 第 5 节）
--   2. 本文件是表结构的唯一权威源；字段增删一律先改本文件，再同步技术方案第 5 节摘要
--   3. 设计规约见 docs/数据库设计规范.md（命名 / 类型 / 索引 / 注释 / 逻辑删除）
--   4. sys_user 表不预置账号：管理员账号由应用首次启动时初始化
--      （BCrypt 加密密码无法在 SQL 中静态生成），或手动执行：
--      迭代 1 完成后会提供 DataInitializer 自动创建 admin/admin123
--
-- ⚠️  本脚本含 DROP DATABASE，会清空同名库的全部数据！
--     仅用于全新环境初始化；已有数据的环境请用 docs/migration/ 下的增量脚本
-- ============================================================

-- ⚠️ 必须先声明连接字符集！
-- Windows 中文环境下 mysql 客户端默认 character_set_client=gbk，
-- 会把本文件的 UTF-8 中文当作 GBK 解读，导致注释与种子数据全部乱码
-- （或直接报 ERROR 1366 Incorrect string value）。
-- 显式 SET NAMES 后，`mysql -u root -p < docs/init.sql` 无需额外参数即可正确执行。
SET NAMES utf8mb4;

DROP DATABASE IF EXISTS ai_trial_platform;
CREATE DATABASE ai_trial_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE ai_trial_platform;

-- ------------------------------------------------------------
-- 1. 用户表：师范生 / 教师 / 管理员
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50)  NOT NULL COMMENT '登录名（学号/工号），全局唯一',
    password   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密，定长 60 字符）',
    real_name  VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    role       VARCHAR(20)  NOT NULL COMMENT '角色：STUDENT / TEACHER / ADMIN',
    major      VARCHAR(50)  DEFAULT NULL COMMENT '专业（师范生）',
    grade      VARCHAR(20)  DEFAULT NULL COMMENT '年级',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：登录时按登录名查用户
    UNIQUE KEY uk_username (username),
    -- 服务查询：管理员后台按角色筛选用户
    KEY idx_role (role)
) ENGINE = InnoDB COMMENT ='用户表';

-- ------------------------------------------------------------
-- 2. 班级表：教师建班、关联课程
-- ------------------------------------------------------------
CREATE TABLE clazz (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(50) NOT NULL COMMENT '班级名称',
    course     VARCHAR(50) NOT NULL COMMENT '课程（语文/数学/英语/思政/幼教等）',
    teacher_id BIGINT      NOT NULL COMMENT '带班教师（sys_user.id）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：教师查看自己带的班级
    KEY idx_teacher (teacher_id)
) ENGINE = InnoDB COMMENT ='班级表';

-- ------------------------------------------------------------
-- 3. 班级-学生关系表（多对多）
--    纯关系表：豁免 updated_at / deleted（见 docs/数据库设计规范.md 第 3 节白名单）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 4. 试讲任务表：教师发布（课时/学段/教材版本/评分标准/模式）
-- ------------------------------------------------------------
CREATE TABLE task (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title            VARCHAR(100) NOT NULL COMMENT '任务标题',
    clazz_id         BIGINT       NOT NULL COMMENT '发布到的班级（clazz.id）',
    subject          VARCHAR(50)  NOT NULL COMMENT '学科',
    stage            VARCHAR(20)  NOT NULL COMMENT '学段：小学 / 初中 / 高中',
    textbook_version VARCHAR(50)  DEFAULT NULL COMMENT '教材版本（人教/北师大等）',
    mode             VARCHAR(20)  NOT NULL COMMENT '训练模式：FREE 自由试讲 / SCENARIO 情景模拟 / STRUCTURED 结构化面试 / EXAM 模拟考核',
    duration_minutes INT          NOT NULL COMMENT '规定时长（分钟）',
    deadline         DATETIME     DEFAULT NULL COMMENT '截止时间',
    rubric_json      JSON         NOT NULL COMMENT '评分标准 JSON（维度 / 权重 / 知识点清单，见技术方案 6.2；可由 resource(type=RUBRIC) 模板复制而来）',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT 草稿 / PUBLISHED 已发布 / CLOSED 已关闭',
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

-- ------------------------------------------------------------
-- 5. 试讲记录表：学生提交（试讲稿 + 环节时长 + 自评量表，音视频可选）
-- ------------------------------------------------------------
CREATE TABLE trial (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id              BIGINT       NOT NULL COMMENT '任务 id（task.id）',
    student_id           BIGINT       NOT NULL COMMENT '学生 id（sys_user.id）',
    script_text          MEDIUMTEXT   NOT NULL COMMENT '试讲稿（含 [环节]、[板书] 标记）',
    audio_path           VARCHAR(255) DEFAULT NULL COMMENT '音频文件路径（可选，迭代 4）',
    video_path           VARCHAR(255) DEFAULT NULL COMMENT '视频文件路径（可选，迭代 4）',
    self_assessment_json JSON         DEFAULT NULL COMMENT '自评量表 JSON（10 项自评；普通话/教姿教态保底方案的数据来源，提交试讲时录入）',
    duration_actual      INT          DEFAULT NULL COMMENT '实际时长（分钟）',
    stage_times_json     JSON         DEFAULT NULL COMMENT '各环节实测时长 JSON（key 为环节名，value 为秒）',
    status               VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：DRAFT 草稿 / SUBMITTED 已提交 / EVALUATED 已评测',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：某任务下某学生的试讲记录 / 防同一任务重复提交
    KEY idx_task_student (task_id, student_id),
    -- 服务查询：学生的历史试讲列表与进步曲线（按时间排序）
    KEY idx_student_created (student_id, created_at)
) ENGINE = InnoDB COMMENT ='试讲记录表';

-- ------------------------------------------------------------
-- 6. 评测报告表：AI 评测引擎生成（试讲 1—1 报告）
-- ------------------------------------------------------------
CREATE TABLE evaluation_report (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    trial_id              BIGINT        NOT NULL COMMENT '试讲 id（trial.id），一次试讲仅一份报告',
    total_score           DECIMAL(5, 1) NOT NULL COMMENT '总分（0.0~100.0）',
    dimension_scores_json JSON          NOT NULL COMMENT '六维分数 JSON（维度 key / 得分 / 权重）',
    issues_json           JSON          DEFAULT NULL COMMENT '问题定位 JSON（问题码 / 级别 / 定位详情）',
    suggestions_json      JSON          DEFAULT NULL COMMENT '改进建议 JSON（问题码 → 建议模板）',
    evaluator_type        VARCHAR(20)   NOT NULL DEFAULT 'RULE' COMMENT '评测引擎类型：RULE / LLM（迭代 5 预留 ASR / POSTURE，与 media_analysis_json 同步启用）',
    media_analysis_json   JSON          DEFAULT NULL COMMENT '语音识别 / 人体关键点原始数据 JSON（迭代 5；API 失败回退时为 NULL）',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评测时间',
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted               TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：按试讲查报告（同时保证一讲一报告）
    UNIQUE KEY uk_trial (trial_id)
) ENGINE = InnoDB COMMENT ='评测报告表';

-- ------------------------------------------------------------
-- 7. 题库表：试讲题 / 结构化面试题 / 教资真题
-- ------------------------------------------------------------
CREATE TABLE question_bank (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject     VARCHAR(50)  NOT NULL COMMENT '学科',
    stage       VARCHAR(20)  NOT NULL COMMENT '学段：小学 / 初中 / 高中',
    type        VARCHAR(20)  NOT NULL COMMENT '类型：TRIAL 试讲题 / STRUCTURED 结构化 / REAL 教资真题',
    content     TEXT         NOT NULL COMMENT '题目内容',
    answer_hint TEXT         DEFAULT NULL COMMENT '答题要点 / 参考答案提示',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：按学段 + 学科筛选题目
    KEY idx_subject_stage (subject, stage)
) ENGINE = InnoDB COMMENT ='题库表';

-- ------------------------------------------------------------
-- 8. 资源表：教案/板书模板、优秀试讲范例、评分细则模板
-- ------------------------------------------------------------
CREATE TABLE resource (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    type       VARCHAR(20)  NOT NULL COMMENT '类型：LESSON_PLAN 教案 / BOARD 板书模板 / EXAMPLE 试讲范例 / RUBRIC 评分细则模板（管理员维护，教师发布任务时复制进 task.rubric_json）',
    subject    VARCHAR(50)  NOT NULL COMMENT '学科（通用模板填「通用」）',
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

-- ------------------------------------------------------------
-- 9. 教师批阅表：试讲 1—N 批阅
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 10. 达标证书表
--     只增不改的业务表：豁免 updated_at（见 docs/数据库设计规范.md 第 3 节白名单）
-- ------------------------------------------------------------
CREATE TABLE certificate (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT       NOT NULL COMMENT '学生 id（sys_user.id）',
    task_id    BIGINT       NOT NULL COMMENT '任务 id（task.id）',
    type       VARCHAR(50)  NOT NULL COMMENT '证书类型（教学表达 / 课堂互动 / 控场 / 时间管理）',
    file_path  VARCHAR(255) NOT NULL COMMENT '证书文件路径',
    issued_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '颁发时间',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (id),
    -- 服务查询：学生的证书列表
    KEY idx_student (student_id),
    -- 服务查询：某任务下的达标名单 / 按任务统计达标率
    KEY idx_task (task_id)
) ENGINE = InnoDB COMMENT ='达标证书表';

-- ============================================================
-- 迭代 4 预留表（虚拟课堂）—— 暂不建表
-- 说明：字段设计见 docs/技术方案.md 第 5 节「迭代 4 预留表」。
--       此处不建表的理由：P2 阶段表结构仍可能调整，空表无数据无意义；
--       迭代 4 启动时在本文件中补 DDL（并按数据库设计规范第 10 节补增量脚本）。
--   classroom_session      一次虚拟课堂会话（trial_id / student_count / mode / script_json）
--   student_behavior_event 虚拟学生行为事件流（session_id / student_no / behavior / occurred_at）
-- ============================================================

-- ============================================================
-- 种子数据（演示用，迭代 2 起逐步补充）
-- ============================================================

-- 题库：小学数学 / 语文试讲题与结构化面试题示例
INSERT INTO question_bank (subject, stage, type, content, answer_hint) VALUES
('数学', '小学', 'TRIAL', '试讲题目：《分数的初步认识》（人教版三年级上册）\n要求：10 分钟无生试讲，含导入、新授、练习、小结、作业五环节。',
 '导入可用分月饼情境；新授突出"平均分"概念；练习设计由浅入深；注意使用直观教具语言。'),
('数学', '小学', 'STRUCTURED', '结构化面试：上课时有学生当众指出你的板书错误，你怎么办？',
 '答题思路：① 保持冷静、坦然承认；② 表扬学生认真观察；③ 顺势引导学生共同纠错；④ 课后反思备课与板书检查习惯。'),
('语文', '小学', 'TRIAL', '试讲题目：《观潮》（人教版四年级上册）第二课时\n要求：8 分钟试讲，重点讲"潮来时"段落，设计一处 [板书]。',
 '抓住"声音—样子"两条线索；重点词句品读（闷雷滚动、白浪翻滚）；朗读指导要有层次。');

-- 资源：教案模板、板书模板、评分细则模板示例
INSERT INTO resource (type, subject, title, content) VALUES
('LESSON_PLAN', '通用', '试讲教案通用模板', '一、教学目标（知识与技能/过程与方法/情感态度价值观）\n二、教学重难点\n三、教学过程（导入—新授—练习—小结—作业，标注各环节时长）\n四、板书设计\n五、教学反思'),
('BOARD', '通用', '板书设计要点', '1. 结构清晰：主板书（课题+知识框架）与副板书（临时演算）分区；\n2. 书写规范：笔顺正确、大小适中、不用繁体/异体字；\n3. 与讲解同步：边讲边写，不背对学生长时间书写。'),
('RUBRIC', '通用', '试讲评分细则默认模板', '{"dimensions":[{"key":"content","name":"教学内容","weight":25},{"key":"expression","name":"教学表达","weight":25},{"key":"posture","name":"教姿教态","weight":15},{"key":"interaction","name":"课堂互动","weight":15},{"key":"board","name":"板书呈现","weight":10},{"key":"time","name":"时间管理","weight":10}]}');
