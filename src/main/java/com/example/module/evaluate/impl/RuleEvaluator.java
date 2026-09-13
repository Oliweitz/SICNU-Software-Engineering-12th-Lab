package com.example.module.evaluate.impl;

import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;

/**
 * 文本规则评测器：默认实现，零外部依赖、可解释打分（见 docs/技术方案.md 第 6.1/6.2 节）
 *
 * <p>负责文本维度组：content（教学内容）、expression（教学表达文本部分）、 interaction（课堂互动）、board（板书）、time（时间管理）。
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

    @Override
    public String dimension() {
        return "text";
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        // TODO 迭代 2：规则引擎实现
        return EvaluationResult.empty();
    }
}
