package com.example.module.evaluate;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试讲评测输入上下文（见 docs/技术方案.md 第 6.2 节）
 *
 * <p>由 trial 模块在触发评测时组装：试讲稿 + 任务评分标准 + 各环节实测时长 + 音视频路径（迭代 4/5 供 {@code AsrEvaluator} / {@code
 * PostureEvaluator} 使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialContext {

    /** 试讲任务 id */
    private Long taskId;

    /** 试讲稿全文（含 [环节]、[板书] 标记） */
    private String scriptText;

    /** 任务配置的知识点清单（教学内容维度覆盖度比对） */
    private List<String> knowledgePoints;

    /** 各环节实测时长（秒），key 为环节名：导入/新授/练习/小结/作业 */
    private Map<String, Integer> stageTimes;

    /** 试讲总时长（秒） */
    private Integer durationSeconds;

    /** 试讲音频路径（可选，迭代 4 起） */
    private String audioPath;

    /** 试讲视频路径（可选，迭代 4 起） */
    private String videoPath;
}
