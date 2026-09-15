package com.example.module.evaluate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单个维度得分（前后端契约见 docs/技术方案.md 第 6.3 节） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionScore {

    /**
     * 维度标识，取值必须是 {@link Dimension#getKey()} 之一
     *
     * <p>保持为 {@code String} 是为了让前后端 JSON 契约（技术方案 6.3）保持字面直观； 合法性由 {@code EvaluationEngine} 通过
     * {@link Dimension#fromKey} 校验。
     */
    private String key;

    /** 维度名称（中文，前端雷达图展示），取自 {@link Dimension#getName()} */
    private String name;

    /** 维度分（0~100） */
    private double score;

    /**
     * 维度权重（百分制占比，来自任务 {@code rubric_json}）
     *
     * <p><b>由 {@code EvaluationEngine} 注入，评测器不应自行设置</b>——评测器只负责产出 {@link #score}。
     */
    private int weight;
}
