package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 生产管理-进度反馈记录流水表 DO
 *
 * 表：lunaerp_production_progress_feedback_note
 * 反馈缸下的跟单沟通流水（ADR-0014 独立子资源）：记录文字内容、问题标记、闭环时间。
 *
 * @author luna
 */
@TableName("lunaerp_production_progress_feedback_note")
@KeySequence("lunaerp_production_progress_feedback_note_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProgressFeedbackNoteDO extends TenantBaseDO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 所属反馈缸ID（lunaerp_production_progress_feedback.id）
     */
    private Long feedbackId;

    /**
     * 反馈沟通内容（非空文本）
     */
    private String content;

    /**
     * 是否标记为问题（true 是，false 否）
     */
    private Boolean issueFlag;

    /**
     * 问题闭环时间（可空，闭环后写入）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime resolvedAt;

}
