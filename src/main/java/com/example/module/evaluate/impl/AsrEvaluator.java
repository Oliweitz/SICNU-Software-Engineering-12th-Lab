package com.example.module.evaluate.impl;

import com.example.module.evaluate.EvaluationResult;
import com.example.module.evaluate.Evaluator;
import com.example.module.evaluate.TrialContext;

/**
 * 普通话评测器（延后项，迭代 5）：基于百度语音识别 API（见 docs/技术方案.md 第 2.4/6.4 节）
 *
 * <p>流程：音频切段（≤60s）→ ASR 识别 → 识别文本 vs 试讲稿比对字错率(CER) + 词置信度 + 通顺度 → 规则换算为教学表达维度的普通话分。
 *
 * <p>保底策略：API Key 未配置或调用失败时自动回退「自评量表 + 教师评分」 （见技术方案第 6.4 节），不影响核心闭环。
 */
public class AsrEvaluator implements Evaluator {

    @Override
    public String dimension() {
        return "expression";
    }

    @Override
    public EvaluationResult evaluate(TrialContext ctx) {
        throw new UnsupportedOperationException("迭代 5 实现：百度语音识别普通话评测");
    }
}
