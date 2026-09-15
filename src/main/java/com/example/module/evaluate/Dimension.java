package com.example.module.evaluate;

import java.util.Arrays;
import java.util.Optional;

/**
 * 评测维度（六维，见 docs/技术方案.md 第 6.1 节）
 *
 * <p>本枚举是维度标识的**唯一权威定义**：{@link DimensionScore#getKey()}、任务的 {@code rubric_json}、 前端雷达图的维度 key 均取自
 * {@link #getKey()}。任何位置出现的维度字符串都必须能在此找到对应常量， 禁止在代码中硬编码维度字符串。
 *
 * <p>{@link #getKey()} 为对外契约（前后端 JSON 中的 key，小写下划线），{@link #getName()} 为中文展示名。
 */
public enum Dimension {
    CONTENT("content", "教学内容"),
    EXPRESSION("expression", "教学表达"),
    POSTURE("posture", "教姿教态"),
    INTERACTION("interaction", "课堂互动"),
    BOARD("board", "板书呈现"),
    TIME("time", "时间管理");

    private final String key;
    private final String name;

    Dimension(String key, String name) {
        this.key = key;
        this.name = name;
    }

    /** 对外契约标识（前后端 JSON key），如 {@code "content"} */
    public String getKey() {
        return key;
    }

    /** 中文展示名（前端雷达图标签），如 {@code "教学内容"} */
    public String getName() {
        return name;
    }

    /**
     * 按契约标识解析维度
     *
     * @param key 维度标识，如 {@code "content"}
     * @return 匹配的维度；无匹配时返回 {@link Optional#empty()}，由调用方决定是忽略还是告警
     */
    public static Optional<Dimension> fromKey(String key) {
        return Arrays.stream(values()).filter(d -> d.key.equals(key)).findFirst();
    }
}
