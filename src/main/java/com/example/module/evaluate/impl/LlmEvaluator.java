package com.example.module.evaluate.impl;

import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;

/**
 * 大模型评测器（可选）：调用大模型 API 做文本语义评测（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>配置开关见 application.yml 的 evaluate.llm.* 节。
 *
 * <p>TODO 迭代 4（可选）实现。
 */
public class LlmEvaluator implements Evaluator {

    @Override
    public String dimension() {
        return "text";
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        throw new UnsupportedOperationException("迭代 4(可选)实现：大模型语义评测");
    }
}
