package com.example.module.evaluate;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 评测引擎：编排聚合各 {@link Evaluator} 实现（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>注入 Spring 容器中全部 Evaluator Bean，按维度聚合；各实现接入后自动生效。
 *
 * <p>TODO 迭代 2：
 *
 * <ul>
 *   <li>维度分加权平均（权重来自任务 rubric_json）
 *   <li>问题列表按级别与维度排序去重
 *   <li>评测结果落库（evaluation_report 表）与异步执行（见技术方案第 8 节性能对策）
 * </ul>
 */
@Component
public class EvaluationEngine {

    private final List<Evaluator> evaluators;

    public EvaluationEngine(List<Evaluator> evaluators) {
        this.evaluators = evaluators;
    }

    /**
     * 执行全维度评测
     *
     * <p>骨架阶段：尚无 Evaluator Bean 注册，返回空结果；实现接入后自动聚合。
     */
    public EvaluationResult evaluate(TrialContext ctx) {
        // TODO 迭代 2：编排聚合逻辑
        return EvaluationResult.empty();
    }
}
