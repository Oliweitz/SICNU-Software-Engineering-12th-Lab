package com.example.module.evaluate.impl;

import com.example.module.evaluate.Dimension;
import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;
import java.util.Map;

/**
 * 文本规则评测器：默认实现，零外部依赖、可解释打分（见 docs/技术方案.md 第 6.1/6.2 节）
 *
 * <p>负责五个文本维度：content（教学内容）、expression（教学表达的文本部分）、 interaction（课堂互动）、 board（板书）、time（时间管理）。
 *
 * <p>其中 expression（教学表达）是**多评测器共同贡献**的维度：本评测器贡献口头禅检测与语速估算（权重 60）， 迭代 5 起 {@code AsrEvaluator}
 * 再贡献普通话字错率（权重 40）。ASR 未接入时引擎按剩余权重自动归一化， 教学表达即等于本评测器的文本分——保底逻辑由引擎统一承担，本类无需特判。
 *
 * <p>TODO 迭代 2 实现：
 *
 * <ul>
 *   <li>知识点覆盖度（任务知识点清单 × 学科术语词库命中）
 *   <li>五环节（导入-新授-练习-小结-作业）结构完整度、篇幅合理度
 *   <li>口头禅正则检测、语速估算（字数÷时长，参考 200~260 字/分）、停顿标记
 *   <li>提问句式占比、鼓励语/反馈语/纪律词库命中
 *   <li>[板书] 标记结构与次数、各环节实测时长 vs 标准配比
 * </ul>
 *
 * <p>接入方式：实现完成后添加 {@code @Component} 注解，评测引擎自动发现。
 */
public class RuleEvaluator implements Evaluator {

    /** 教学表达维度内本评测器所占的贡献权重（其余 40 由 AsrEvaluator 的普通话分承担） */
    private static final int EXPRESSION_WEIGHT = 60;

    @Override
    public Map<Dimension, Integer> contributions() {
        return Map.of(
                Dimension.CONTENT, 100,
                Dimension.EXPRESSION, EXPRESSION_WEIGHT,
                Dimension.INTERACTION, 100,
                Dimension.BOARD, 100,
                Dimension.TIME, 100);
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        // TODO 迭代 2：规则引擎实现
        return EvaluationResult.empty();
    }
}
