package com.example.module.evaluate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link EvaluationEngine} 维度合并与总分计算单元测试（见 docs/代码规范.md 第 8 节）
 *
 * <p>刻意使用手写桩 {@link Evaluator} 而非 Mockito：轻量、可读，且规避 JDK 24 下 Mockito inline mock maker 无法
 * self-attach 的环境问题（见 {@code ApplicationSmokeTest} 注释）。
 */
class EvaluationEngineTest {

    // ---------- 桩与构造helper ----------

    /** 构造一个只返回固定结果的评测器桩 */
    private static Evaluator stub(Map<Dimension, Integer> contributions, EvaluationResult result) {
        return new Evaluator() {
            @Override
            public Map<Dimension, Integer> contributions() {
                return contributions;
            }

            @Override
            public EvaluationResult evaluate(TrialContext ctx) {
                return result;
            }
        };
    }

    /** 构造只含单个维度分的评测结果 */
    private static EvaluationResult resultOf(Dimension dimension, double score) {
        return EvaluationResult.builder()
                .dimensions(
                        List.of(
                                DimensionScore.builder()
                                        .key(dimension.getKey())
                                        .name(dimension.getName())
                                        .score(score)
                                        .build()))
                .issues(List.of())
                .build();
    }

    private static EvaluationEngine engineOf(Evaluator... evaluators) {
        return new EvaluationEngine(List.of(evaluators));
    }

    private static TrialContext emptyContext() {
        return TrialContext.builder().build();
    }

    private static DimensionScore dim(EvaluationResult result, Dimension dimension) {
        return result.getDimensions().stream()
                .filter(d -> dimension.getKey().equals(d.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("结果中缺少维度 " + dimension.getKey()));
    }

    // ---------- 用例 ----------

    @Test
    @DisplayName("单评测器产出维度分：得分原样输出，权重由 rubric 注入")
    void singleEvaluator() {
        EvaluationEngine engine =
                engineOf(stub(Map.of(Dimension.CONTENT, 100), resultOf(Dimension.CONTENT, 88)));

        EvaluationResult result = engine.evaluate(emptyContext());

        assertEquals(88.0, dim(result, Dimension.CONTENT).getScore());
        assertEquals(25, dim(result, Dimension.CONTENT).getWeight(), "默认 rubric 中教学内容权重应为 25");
        assertEquals("教学内容", dim(result, Dimension.CONTENT).getName());
    }

    @Test
    @DisplayName("同一维度多评测器共同贡献：按各自声明权重加权合并")
    void multipleEvaluatorsMergeByWeight() {
        // 文本规则：教学表达权重 60、得分 80；语音识别：权重 40、得分 70
        EvaluationEngine engine =
                engineOf(
                        stub(Map.of(Dimension.EXPRESSION, 60), resultOf(Dimension.EXPRESSION, 80)),
                        stub(Map.of(Dimension.EXPRESSION, 40), resultOf(Dimension.EXPRESSION, 70)));

        EvaluationResult result = engine.evaluate(emptyContext());

        // (80×60 + 70×40) ÷ 100 = 76.0
        assertEquals(76.0, dim(result, Dimension.EXPRESSION).getScore());
    }

    @Test
    @DisplayName("评测器未接入时按剩余权重自动归一化（ASR 缺失回退为纯文本分）")
    void missingEvaluatorFallsBackByNormalizingWeight() {
        EvaluationEngine engine =
                engineOf(
                        stub(Map.of(Dimension.EXPRESSION, 60), resultOf(Dimension.EXPRESSION, 80)));

        EvaluationResult result = engine.evaluate(emptyContext());

        // 权重只剩 60，得分应为 80×60÷60 = 80，而不是被 ASR 的缺失拉低为 48
        assertEquals(80.0, dim(result, Dimension.EXPRESSION).getScore());
    }

    @Test
    @DisplayName("未产出分数的维度不出现在结果中，也不参与总分")
    void dimensionsWithoutScoreAreExcluded() {
        EvaluationEngine engine =
                engineOf(stub(Map.of(Dimension.CONTENT, 100), resultOf(Dimension.CONTENT, 90)));

        EvaluationResult result = engine.evaluate(emptyContext());

        assertEquals(1, result.getDimensions().size(), "教姿教态无评测器产出，不应出现在报告中");
        assertEquals(90.0, result.getTotalScore(), "总分应等于唯一维度分，而非被缺失维度记 0 拉低");
    }

    @Test
    @DisplayName("总分按 rubric 权重加权，且只对已产出维度归一化")
    void totalScoreWeightedByRubric() {
        // 默认权重：教学内容 25、时间管理 10
        EvaluationEngine engine =
                engineOf(
                        stub(Map.of(Dimension.CONTENT, 100), resultOf(Dimension.CONTENT, 90)),
                        stub(Map.of(Dimension.TIME, 100), resultOf(Dimension.TIME, 60)));

        EvaluationResult result = engine.evaluate(emptyContext());

        // (90×25 + 60×10) ÷ (25+10) = 2850 ÷ 35 = 81.428… → 81.4
        assertEquals(81.4, result.getTotalScore());
    }

    @Test
    @DisplayName("任务 rubric 自定义权重覆盖默认权重")
    void customRubricWeightsOverrideDefaults() {
        EvaluationEngine engine =
                engineOf(
                        stub(Map.of(Dimension.CONTENT, 100), resultOf(Dimension.CONTENT, 100)),
                        stub(Map.of(Dimension.TIME, 100), resultOf(Dimension.TIME, 50)));
        TrialContext ctx =
                TrialContext.builder()
                        .dimensionWeights(Map.of(Dimension.CONTENT, 80, Dimension.TIME, 20))
                        .build();

        EvaluationResult result = engine.evaluate(ctx);

        assertEquals(80, dim(result, Dimension.CONTENT).getWeight());
        // (100×80 + 50×20) ÷ 100 = 90.0
        assertEquals(90.0, result.getTotalScore());
    }

    @Test
    @DisplayName("评测器返回未声明的维度：忽略该条并保留其余维度")
    void undeclaredDimensionIsIgnored() {
        // 评测器只声明了 CONTENT，却返回了 CONTENT + TIME 两条
        EvaluationResult noisy =
                EvaluationResult.builder()
                        .dimensions(
                                List.of(
                                        DimensionScore.builder()
                                                .key(Dimension.CONTENT.getKey())
                                                .name(Dimension.CONTENT.getName())
                                                .score(90)
                                                .build(),
                                        DimensionScore.builder()
                                                .key(Dimension.TIME.getKey())
                                                .name(Dimension.TIME.getName())
                                                .score(10)
                                                .build()))
                        .issues(List.of())
                        .build();
        EvaluationEngine engine = engineOf(stub(Map.of(Dimension.CONTENT, 100), noisy));

        EvaluationResult result = engine.evaluate(emptyContext());

        assertEquals(1, result.getDimensions().size(), "未声明的 TIME 维度应被忽略");
        assertEquals(90.0, dim(result, Dimension.CONTENT).getScore());
    }

    @Test
    @DisplayName("评测器返回未知维度 key：忽略该条，不抛异常")
    void unknownDimensionKeyIsIgnored() {
        EvaluationResult bogus =
                EvaluationResult.builder()
                        .dimensions(
                                List.of(
                                        DimensionScore.builder()
                                                .key("not_a_dimension")
                                                .score(50)
                                                .build()))
                        .issues(List.of())
                        .build();
        EvaluationEngine engine = engineOf(stub(Map.of(Dimension.CONTENT, 100), bogus));

        EvaluationResult result = engine.evaluate(emptyContext());

        assertTrue(result.getDimensions().isEmpty());
        assertEquals(0.0, result.getTotalScore());
    }

    @Test
    @DisplayName("评测器未声明任何维度贡献：整体忽略其产出")
    void evaluatorWithEmptyContributionsIsIgnored() {
        EvaluationEngine engine = engineOf(stub(Map.of(), resultOf(Dimension.CONTENT, 99)));

        EvaluationResult result = engine.evaluate(emptyContext());

        assertTrue(result.getDimensions().isEmpty());
    }

    @Test
    @DisplayName("无任何评测器注册：返回空结果而非抛异常")
    void noEvaluatorsRegistered() {
        EvaluationResult result = new EvaluationEngine(List.of()).evaluate(emptyContext());

        assertTrue(result.getDimensions().isEmpty());
        assertTrue(result.getIssues().isEmpty());
        assertEquals(0.0, result.getTotalScore());
    }

    @Test
    @DisplayName("问题列表汇总自全部评测器")
    void issuesAreCollectedFromAllEvaluators() {
        IssueItem issue =
                IssueItem.builder()
                        .dimension(Dimension.EXPRESSION.getKey())
                        .level(IssueLevel.WARN)
                        .title("口头禅过多")
                        .build();
        EvaluationResult withIssue =
                EvaluationResult.builder().dimensions(List.of()).issues(List.of(issue)).build();

        EvaluationEngine engine = engineOf(stub(Map.of(Dimension.EXPRESSION, 100), withIssue));

        assertEquals(1, engine.evaluate(emptyContext()).getIssues().size());
    }
}
