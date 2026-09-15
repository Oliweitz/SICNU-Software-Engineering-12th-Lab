package com.example.module.evaluate.impl;

import com.example.module.evaluate.Dimension;
import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;
import java.util.Map;

/**
 * 大模型评测器（可选）：调用大模型 API 做文本语义评测（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>配置开关见 application.yml 的 evaluate.llm.* 节。
 *
 * <p>与 {@code RuleEvaluator} 在 content / expression / interaction 三个维度上**共同贡献**：
 * 规则引擎擅长可解释的量化指标（知识点命中、口头禅计数），大模型擅长语义层面的判断（讲解是否透彻、
 * 互动反馈是否切题）。二者按权重加权合并，语义分权重低于规则分——保证即使大模型不可用或输出不稳定， 评测结果仍以可解释的规则分为主。
 *
 * <p>TODO 迭代 4（可选）实现。
 */
public class LlmEvaluator implements Evaluator {

    @Override
    public Map<Dimension, Integer> contributions() {
        return Map.of(
                Dimension.CONTENT, 30,
                Dimension.EXPRESSION, 20,
                Dimension.INTERACTION, 30);
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        throw new UnsupportedOperationException("迭代 4(可选)实现：大模型语义评测");
    }
}
