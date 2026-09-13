package com.example.module.evaluate;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测结果聚合体（前后端契约见 docs/技术方案.md 第 6.3 节）
 *
 * <p>totalScore = Σ(维度分 × 权重)，由 {@link EvaluationEngine} 加权聚合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    /** 总分（0~100） */
    private double totalScore;

    /** 六维得分明细 */
    private List<DimensionScore> dimensions;

    /** 问题定位与建议列表 */
    private List<IssueItem> issues;

    /** 空结果（骨架阶段占位，评测器未接入时返回） */
    public static EvaluationResult empty() {
        return EvaluationResult.builder()
                .totalScore(0)
                .dimensions(new ArrayList<>())
                .issues(new ArrayList<>())
                .build();
    }
}
