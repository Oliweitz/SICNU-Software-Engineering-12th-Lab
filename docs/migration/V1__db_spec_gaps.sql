-- ============================================================
-- V1 —— 按数据库设计规范补齐执行缺口
-- 环境：MySQL 8.0.x
-- 用法：mysql -u root -p ai_trial_platform < docs/migration/V1__db_spec_gaps.sql
--
-- 适用对象：**已有数据的环境**。全新环境直接跑 docs/init.sql 即可，无需本脚本。
-- 本脚本不含 DROP，只做 ALTER（见 docs/数据库设计规范.md 第 10 节）。
-- ============================================================

-- ⚠️ 与 init.sql 同理：先声明连接字符集，否则 Windows 中文环境下
--    mysql 客户端按 GBK 解读本文件的 UTF-8 中文，COMMENT 会写坏
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. certificate.task_id 是外键列却漏建索引
--    违反规范 §4.1「所有外键列必须建索引（即使不建物理外键）」
--    服务的查询：某任务下的达标名单 / 按任务统计达标率
-- ------------------------------------------------------------
ALTER TABLE certificate
    ADD KEY idx_task (task_id);

-- ------------------------------------------------------------
-- 2. 取值范围 / 空值语义补全
--    违反规范 §7「可空列在 COMMENT 中说明何时为空」与迭代 0 遗留的
--    取值范围缺失。**仅改 COMMENT，不动列类型与约束**。
--    rubric_json 的口径以技术方案 §6.2 为准：总分 = Σ(维度分 × 维度权重)
--    ÷ Σ(维度权重)，引擎按 Σ权重 归一化，故权重之和**不必**为 100，
--    真正的约束是每个维度的 weight 必须 > 0（作分母）。
-- ------------------------------------------------------------
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
