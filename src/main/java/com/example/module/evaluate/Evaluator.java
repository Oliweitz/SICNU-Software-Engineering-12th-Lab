package com.example.module.evaluate;

/**
 * 评测器抽象：评测引擎的可插拔单元（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>各实现（{@code RuleEvaluator} / {@code LlmEvaluator} / {@code AsrEvaluator} / {@code
 * PostureEvaluator}）输出自身负责维度的分数、问题与建议，由 {@link EvaluationEngine} 按维度编排聚合。实现类接入时注册为 Spring Bean
 * 即自动生效。
 */
public interface Evaluator {

    /** 负责的维度标识：text（文本维度组）/ expression（普通话）/ posture（教姿教态） */
    String dimension();

    /** 对试讲上下文执行评测 */
    EvaluationResult evaluate(TrialContext ctx);
}
