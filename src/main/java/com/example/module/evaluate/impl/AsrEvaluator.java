package com.example.module.evaluate.impl;

import com.example.module.evaluate.Dimension;
import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;
import java.util.Map;

/**
 * 普通话评测器（延后项，迭代 5）：基于百度语音识别 API（见 docs/技术方案.md 第 2.4/6.4 节）
 *
 * <p>流程：音频切段（≤60s）→ ASR 识别 → 识别文本 vs 试讲稿比对字错率(CER) + 词置信度 + 通顺度 → 规则换算为教学表达维度的普通话分。
 *
 * <p>本评测器**只贡献 expression 维度的一部分**（权重 40），与 {@code RuleEvaluator} 的文本部分（权重 60）加权合并； 本类未注册为 Bean
 * 或返回空结果时，引擎自动按剩余权重归一化，教学表达退回纯文本分，**核心闭环不受影响**。
 *
 * <p>保底策略：API Key 未配置或调用失败时自动回退「自评量表 + 教师评分」 （见技术方案第 6.4 节）。
 */
public class AsrEvaluator implements Evaluator {

    /** 教学表达维度内普通话分所占的贡献权重（其余 60 由 RuleEvaluator 的文本分承担） */
    private static final int EXPRESSION_WEIGHT = 40;

    @Override
    public Map<Dimension, Integer> contributions() {
        return Map.of(Dimension.EXPRESSION, EXPRESSION_WEIGHT);
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        throw new UnsupportedOperationException("迭代 5 实现：百度语音识别普通话评测");
    }
}
