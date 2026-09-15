package com.example.module.evaluate.impl;

import com.example.module.evaluate.Dimension;
import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;
import java.util.Map;

/**
 * 教姿教态评测器（延后项，迭代 5）：基于百度人体关键点识别 （见 docs/技术方案.md 第 2.4/6.4 节）
 *
 * <p>流程：FFmpeg 抽帧（每 2~5s 1 帧）→ 人体关键点 → 躯干直立度、走动幅度、 手势活跃度、肩线朝向 → 换算为教姿教态维度分。
 *
 * <p>本评测器**独占 posture 维度**（权重 100）。未接入时该维度无任何评测器产出， 引擎会将其排除在总分与报告之外（而非记 0 分）——避免迭代 2~4
 * 期间因缺少姿态数据而压低总分。
 */
public class PostureEvaluator implements Evaluator {

    @Override
    public Map<Dimension, Integer> contributions() {
        return Map.of(Dimension.POSTURE, 100);
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        throw new UnsupportedOperationException("迭代 5 实现：百度人体关键点姿态评测");
    }
}
