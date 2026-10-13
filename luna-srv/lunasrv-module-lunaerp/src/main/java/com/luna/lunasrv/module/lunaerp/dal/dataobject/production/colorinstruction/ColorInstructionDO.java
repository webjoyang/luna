package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产管理 - 打色指示单主表 DO
 *
 * 表：lunaerp_production_color_instruction
 *
 * 客户/产品档案强关联，保存编号与名称快照；
 * 承接方供应商可空（空=本厂打色房自做，同 ADR-0022 自织先例）。
 * 单号按天取号、全量唯一、软删不释放（仿 ADR-0006）。
 * 颜色行子表（ColorInstructionColorDO）随单聚合保存。
 *
 * @author luna
 */
@TableName("lunaerp_production_color_instruction")
@KeySequence("lunaerp_production_color_instruction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ColorInstructionDO extends TenantBaseDO {

    /**
     * 打色指示单ID
     */
    private Long id;

    /**
     * 打色单号（DS+年月日+3位流水，按天取号，租户内全量唯一含已删除行，仿 ADR-0006）
     */
    private String instructionNo;

    /**
     * 客户档案ID（lunaerp_baseinfo_customer.id）
     */
    private Long customerId;

    /**
     * 客户名快照
     */
    private String clientName;

    /**
     * 客户联系人（文本，可含电话）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contactPerson;

    /**
     * 产品档案ID（lunaerp_baseinfo_product.id，强关联）
     */
    private Long productId;

    /**
     * 产品编号快照
     */
    private String productCode;

    /**
     * 产品名称快照
     */
    private String productName;

    /**
     * 打色承接方供应商ID（lunaerp_baseinfo_supplier.id；空=本厂打色房自做）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long labFactoryId;

    /**
     * 承接方公司名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String labFactoryName;

    /**
     * 成分（文本快照）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String composition;

    /**
     * 幅宽（文本快照）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String width;

    /**
     * 克重（文本快照）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gsm;

    /**
     * 单据日期（必填，缺省当天）
     */
    private LocalDate instructionDate;

    /**
     * 要求完成日（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate deliveryDate;

    /**
     * 单据状态：PENDING 待打色 / IN_PROGRESS 打色中 / CONFIRMED 已确认 / IN_PRODUCTION 已投产 / VOID 已作废
     */
    private String status;

    /**
     * 确认类型：CUSTOMER 客户确认 / INTERNAL 内部确认（确认时必填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String confirmType;

    /**
     * 客户反馈意见（确认/驳回时可填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String customerFeedback;

    /**
     * 确认人用户ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long confirmedBy;

    /**
     * 确认时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime confirmedAt;

    /**
     * 作废原因
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String voidReason;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
