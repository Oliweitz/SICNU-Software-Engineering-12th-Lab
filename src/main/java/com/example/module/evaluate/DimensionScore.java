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

    /** 维度标识：content / expression / posture / interaction / board / time */
    private String key;

    /** 维度名称（中文，前端雷达图展示） */
    private String name;

    /** 维度分（0~100） */
    private double score;

    /** 权重（百分制占比，来自任务 rubric_json，可配置） */
    private int weight;
}
