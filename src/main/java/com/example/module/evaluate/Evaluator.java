package com.example.module.evaluate;

import java.util.Map;

/**
 * 评测器抽象：评测引擎的可插拔单元（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>实现类注册为 Spring Bean 即自动生效，无需修改 {@link EvaluationEngine}。
 *
 * <h3>维度贡献模型</h3>
 *
 * 一个评测器可以同时负责多个维度，一个维度也可以由多个评测器**共同贡献**—— 例如「教学表达」在迭代 2 由 {@code RuleEvaluator} 按口头禅/语速打分， 迭代 5 起再由
 * {@code AsrEvaluator} 按普通话字错率打分，二者**加权合并**而非二选一。
 *
 * <p>合并由 {@link EvaluationEngine} 执行：同一维度的最终分 = Σ(各评测器该维度得分 × 其贡献权重) ÷ Σ(已产出该维度分的评测器权重)。
 *
 * <p>因此**某评测器未接入或未产出某维度分时，引擎按剩余权重自动归一化**—— 这正是「百度 API 不可用时自动回退保底方案」的实现机制，引擎无需为此写特判。
 *
 * <h3>契约约束</h3>
 *
 * <ul>
 *   <li>{@link #contributions()} 不得为空，key 取自 {@link Dimension}
 *   <li>{@link #evaluate} 返回的 {@link DimensionScore#getKey()} **必须**是本评测器已声明的维度；
 *       返回未声明维度时引擎会告警并忽略该条，以防契约漂移
 *   <li>{@link DimensionScore#getWeight()} 由引擎依据任务 rubric 注入，**评测器不应自行设置**
 * </ul>
 */
public interface Evaluator {

    /**
     * 本评测器负责产出的维度及各自的贡献权重
     *
     * <p>权重是**同一维度内部**的相对比例，不必求和为 100。示例：
     *
     * <ul>
     *   <li>{@code RuleEvaluator} → {@code {CONTENT:100, EXPRESSION:60, INTERACTION:100, BOARD:100,
     *       TIME:100}}
     *   <li>{@code AsrEvaluator} → {@code {EXPRESSION:40}}
     * </ul>
     *
     * 则教学表达 = 文本分 × 60% + 普通话分 × 40%;若 ASR 未接入,自动回退为文本分 × 100%。
     *
     * @return 维度到贡献权重的映射，不得为空
     */
    Map<Dimension, Integer> contributions();

    /**
     * 对试讲上下文执行评测
     *
     * @param ctx 试讲上下文（试讲稿、环节时长、音视频路径等）
     * @return 本评测器负责维度的得分、问题与建议；无有效输入时可返回空结果
     */
    EvaluationResult evaluate(TrialContext ctx);
}
