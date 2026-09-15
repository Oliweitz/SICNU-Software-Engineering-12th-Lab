package com.example.module.evaluate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测结果聚合体（前后端契约见 docs/技术方案.md 第 6.3 节）
 *
 * <p>由 {@link EvaluationEngine} 加权聚合产出：维度分 = Σ(各评测器得分 × 贡献权重) ÷ Σ(权重)， 总分 = Σ(维度分 × 维度权重) ÷
 * Σ(维度权重)。
 *
 * <h3>「未评测」与「0 分」的区别</h3>
 *
 * 当没有任何评测器产出维度分时（评测器均未接入、或全部无有效输入），{@link #getTotalScore()} 为 0， 但**这不代表被评者得了 0
 * 分，而是「尚未评测」**。二者必须区分，否则：
 *
 * <ul>
 *   <li>进步曲线会出现虚假的 0 分低谷
 *   <li>班级平均分被未评测的试讲拉低
 *   <li>达标判定会把「还没测」误判为「不达标」
 * </ul>
 *
 * 判定方式：{@link #evaluated()}。**调用方在持久化或返回评测报告前必须先判断该值**——未评测时不应写入 {@code evaluation_report} 表（该表
 * {@code total_score} 为 {@code NOT NULL}，一旦写入即被当作真实分数）， 而应提示「评测未完成」。
 *
 * <p>不变量：{@code evaluated() == false} ⟺ {@code dimensions} 为空。任何评测器只要产出了一个维度分， 即使该维度分为 0，也属于「已评测」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    /**
     * 总分（0~100）
     *
     * <p><b>仅当 {@link #evaluated()} 为 true 时才有意义</b>；为 false 时此处的 0 表示「未评测」而非「0 分」。
     */
    private double totalScore;

    /** 各维度得分明细；为空表示未产出任何维度分（即未评测） */
    private List<DimensionScore> dimensions;

    /** 问题定位与建议列表 */
    private List<IssueItem> issues;

    /**
     * 本次评测是否真的产出了分数
     *
     * <p>派生自 {@link #dimensions} 是否为空，不额外序列化到 JSON——避免与 {@code dimensions} 形成冗余状态， 也避免前后端契约（技术方案
     * 6.3）平白多出一个可被误用的字段。
     *
     * @return true 表示至少产出一个维度分；false 表示未评测，此时 {@link #getTotalScore()} 无意义
     */
    @JsonIgnore
    public boolean evaluated() {
        return dimensions != null && !dimensions.isEmpty();
    }

    /** 空结果：未产出任何维度分（评测器未接入或无有效输入），调用方不应据此落库 */
    public static EvaluationResult empty() {
        return EvaluationResult.builder()
                .totalScore(0)
                .dimensions(new ArrayList<>())
                .issues(new ArrayList<>())
                .build();
    }
}
