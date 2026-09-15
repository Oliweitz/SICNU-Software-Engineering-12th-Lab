package com.example.module.evaluate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 评测引擎：编排聚合各 {@link Evaluator} 实现（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>注入 Spring 容器中全部 Evaluator Bean，按维度加权合并；各实现接入后自动生效，引擎无需改动。
 *
 * <h3>计算规则</h3>
 *
 * <ol>
 *   <li><b>维度分</b> = Σ(各评测器该维度得分 × 其贡献权重) ÷ Σ(已产出该维度分的评测器权重)。 某评测器未接入或未产出该维度分时按剩余权重自动归一化——这就是「百度
 *       API 不可用时回退保底方案」的实现机制。
 *   <li><b>总分</b> = Σ(维度分 × 维度权重) ÷ Σ(维度权重)，权重取自任务 {@code rubric_json}; <b>未产出分数的维度不参与计算</b>（否则迭代 2
 *       尚无教姿教态分时会被记 0 分，不公平）。
 *   <li>评测器返回<b>未声明或未知</b>的维度时告警并忽略该条，防止契约漂移。
 * </ol>
 *
 * <p>TODO 迭代 2 补充：问题列表按级别与维度排序去重;评测结果落库（evaluation_report 表）与异步执行 （见技术方案第 8 节性能对策）。
 */
@Slf4j
@Component
public class EvaluationEngine {

    /**
     * rubric 未配置时的默认维度权重（与 docs/init.sql 中 {@code RUBRIC} 种子模板一致）
     *
     * <p>仅为兜底;正常路径下权重来自任务的 {@code rubric_json}（教师可自定义，见技术方案 2.2 功能 8）。
     */
    private static final Map<Dimension, Integer> DEFAULT_WEIGHTS =
            Map.of(
                    Dimension.CONTENT, 25,
                    Dimension.EXPRESSION, 25,
                    Dimension.POSTURE, 15,
                    Dimension.INTERACTION, 15,
                    Dimension.BOARD, 10,
                    Dimension.TIME, 10);

    private final List<Evaluator> evaluators;

    public EvaluationEngine(List<Evaluator> evaluators) {
        this.evaluators = evaluators;
    }

    /**
     * 执行全维度评测
     *
     * @param ctx 试讲上下文
     * @return 维度得分 + 总分 + 问题建议列表;无任何评测器产出时返回空结果（总分为 0）
     */
    public EvaluationResult evaluate(TrialContext ctx) {
        Map<Dimension, Double> weightedSum = new EnumMap<>(Dimension.class);
        Map<Dimension, Integer> weightSum = new EnumMap<>(Dimension.class);
        List<IssueItem> issues = new ArrayList<>();

        for (Evaluator evaluator : evaluators) {
            EvaluationResult result = evaluator.evaluate(ctx);
            if (result == null) {
                continue;
            }
            if (result.getIssues() != null) {
                issues.addAll(result.getIssues());
            }
            mergeContributions(evaluator, result, weightedSum, weightSum);
        }

        Map<Dimension, Integer> rubricWeights = resolveRubricWeights(ctx);
        List<DimensionScore> dimensions = new ArrayList<>();
        for (Dimension dimension : Dimension.values()) {
            Integer weight = weightSum.get(dimension);
            if (weight == null || weight == 0) {
                // 该维度暂无评测器产出（如迭代 2 的教姿教态）——不参与总分，也不出现在报告中
                continue;
            }
            dimensions.add(
                    DimensionScore.builder()
                            .key(dimension.getKey())
                            .name(dimension.getName())
                            .score(round1(weightedSum.get(dimension) / weight))
                            .weight(rubricWeights.getOrDefault(dimension, 0))
                            .build());
        }

        return EvaluationResult.builder()
                .totalScore(totalScore(dimensions))
                .dimensions(dimensions)
                .issues(issues)
                .build();
    }

    /** 将单个评测器的产出按其声明的贡献权重累加进中间结果，并校验维度声明的一致性 */
    private void mergeContributions(
            Evaluator evaluator,
            EvaluationResult result,
            Map<Dimension, Double> weightedSum,
            Map<Dimension, Integer> weightSum) {
        String name = evaluator.getClass().getSimpleName();
        Map<Dimension, Integer> contributions = evaluator.contributions();
        if (contributions == null || contributions.isEmpty()) {
            log.warn("评测器 {} 未声明任何维度贡献，其评测结果被整体忽略", name);
            return;
        }
        if (result.getDimensions() == null) {
            return;
        }
        for (DimensionScore score : result.getDimensions()) {
            Optional<Dimension> dimension = Dimension.fromKey(score.getKey());
            if (dimension.isEmpty()) {
                log.warn("评测器 {} 返回未知维度 key={}，已忽略该条", name, score.getKey());
                continue;
            }
            Integer weight = contributions.get(dimension.get());
            if (weight == null) {
                log.warn("评测器 {} 返回未声明的维度 {}，已忽略该条（请检查 contributions() 声明）", name, score.getKey());
                continue;
            }
            weightedSum.merge(dimension.get(), score.getScore() * weight, Double::sum);
            weightSum.merge(dimension.get(), weight, Integer::sum);
        }
    }

    /** 维度权重：优先取任务 rubric_json，缺省时回退默认权重 */
    private Map<Dimension, Integer> resolveRubricWeights(TrialContext ctx) {
        Map<Dimension, Integer> weights = ctx.getDimensionWeights();
        return (weights == null || weights.isEmpty()) ? DEFAULT_WEIGHTS : weights;
    }

    /** 总分 = Σ(维度分 × 维度权重) ÷ Σ(维度权重)；无任何维度分时返回 0 */
    private double totalScore(List<DimensionScore> dimensions) {
        double sum = 0;
        int weightTotal = 0;
        for (DimensionScore dimension : dimensions) {
            if (dimension.getWeight() <= 0) {
                continue;
            }
            sum += dimension.getScore() * dimension.getWeight();
            weightTotal += dimension.getWeight();
        }
        return weightTotal == 0 ? 0 : round1(sum / weightTotal);
    }

    /** 分数保留一位小数，与 evaluation_report.total_score 的 DECIMAL(5,1) 对齐 */
    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
