package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理-进度反馈缸表 DO
 *
 * 表：lunaerp_production_progress_feedback
 * 进度反馈的最小单位——某张染色单某个颜色行下投染的一缸（ADR-0031）。
 * 零快照：不含任何名称展示快照列，经染色单主表与颜色行查询取出。
 * 零外键约束：逻辑关联 dyeingOrderId 与 dyeingColorId。
 *
 * @author luna
 */
@TableName("lunaerp_production_progress_feedback")
@KeySequence("lunaerp_production_progress_feedback_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProgressFeedbackDO extends TenantBaseDO {

    /**
     * 反馈缸ID
     */
    private Long id;

    /**
     * 关联染色单ID（lunaerp_production_dyeing_order.id）
     */
    private Long dyeingOrderId;

    /**
     * 关联染色颜色明细ID（lunaerp_production_dyeing_color.id）
     */
    private Long dyeingColorId;

    /**
     * 缸号（同一染色单同一颜色行下由服务层校验唯一）
     */
    private String vatNo;

    /**
     * 本缸数量
     */
    private BigDecimal quantity;

    /**
     * 匹数（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer pieces;

    /**
     * 进度状态（字典 lunaerp_progress_feedback_status：SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）
     */
    private String status;

    /**
     * 排缸日期（阶段一日期戳，建缸缺省当天，可人工修正）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate scheduledDate;

    /**
     * 染色开始日期（阶段二日期戳，可人工修正）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate dyeingStartedDate;

    /**
     * 固色开始日期（阶段三日期戳，可人工修正）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate fixingStartedDate;

    /**
     * 质检日期（阶段四日期戳，可人工修正）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate qualityCheckDate;

    /**
     * 完成日期（阶段五日期戳，可人工修正）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate completedDate;

    /**
     * 延期原因（自由文本，可空可改）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String delayReason;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
