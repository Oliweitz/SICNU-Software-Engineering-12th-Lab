package com.example.module.evaluate.impl;

import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;

/**
 * 教姿教态评测器（延后项，迭代 5）：基于百度人体关键点识别 （见 docs/技术方案.md 第 2.4/6.4 节）
 *
 * <p>流程：FFmpeg 抽帧（每 2~5s 1 帧）→ 人体关键点 → 躯干直立度、走动幅度、 手势活跃度、肩线朝向 → 换算为教姿教态维度分。
 */
public class PostureEvaluator implements Evaluator {

    @Override
    public String dimension() {
        return "posture";
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        throw new UnsupportedOperationException("迭代 5 实现：百度人体关键点姿态评测");
    }
}
