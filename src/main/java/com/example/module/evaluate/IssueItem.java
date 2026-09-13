package com.example.module.evaluate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 评测发现的问题与改进建议（前后端契约见 docs/技术方案.md 第 6.3 节） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueItem {

    /** 所属维度标识 */
    private String dimension;

    /** 级别：INFO / WARN / ERROR */
    private IssueLevel level;

    /** 问题标题，如「口头禅过多」 */
    private String title;

    /** 问题定位详情，如「『那个』出现 23 次」 */
    private String detail;

    /** 改进建议（来自建议模板库，见 docs/技术方案.md 第 6.2 节） */
    private String suggestion;
}
