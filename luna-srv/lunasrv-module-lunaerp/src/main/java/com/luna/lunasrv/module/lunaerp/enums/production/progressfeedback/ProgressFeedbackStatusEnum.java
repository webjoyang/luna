package com.luna.lunasrv.module.lunaerp.enums.production.progressfeedback;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * 生产管理-进度反馈状态枚举
 * 对应字典：lunaerp_progress_feedback_status (id=2247)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum ProgressFeedbackStatusEnum {

    /**
     * 未排缸（虚拟态，仅用于前端渲染与筛选，不持久化）
     */
    NOT_SCHEDULED("NOT_SCHEDULED", "未排缸"),

    /**
     * 已排缸（建缸缺省态）
     */
    SCHEDULED("SCHEDULED", "已排缸"),

    /**
     * 染色中
     */
    DYEING("DYEING", "染色中"),

    /**
     * 固色中
     */
    FIXING("FIXING", "固色中"),

    /**
     * 质检中
     */
    QUALITY_CHECK("QUALITY_CHECK", "质检中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成");

    private final String value;
    private final String label;

    /**
     * 进行中状态集合（用于统计条进行中缸数）
     */
    public static final Set<String> IN_PROGRESS_STATUSES = Set.of(
            SCHEDULED.getValue(),
            DYEING.getValue(),
            FIXING.getValue(),
            QUALITY_CHECK.getValue()
    );

}
