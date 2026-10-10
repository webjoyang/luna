package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产管理 - 打色指示单主表 DO
 *
 * @author luna
 */
@TableName("lunaerp_production_color_instruction")
@KeySequence("lunaerp_production_color_instruction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionDO extends TenantBaseDO {

    /**
     * 打色指示单ID
     */
    @TableId
    private Long id;

    /**
     * 打色单号（DS+年月日+3位流水，按天取号，租户内全量唯一含已删除行，ADR-0006）
     */
    private String instructionNo;

    /**
     * 客户档案ID（lunaerp_baseinfo_customer.id，逻辑关联）
     */
    private Long customerId;

    /**
     * 客户名称快照
     */
    private String clientName;

    /**
     * 客户联系人（文本，可含电话）
     */
    private String contactPerson;

    /**
     * 产品档案ID（强关联）
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
     * 打色承接方供应商ID（lunaerp_baseinfo_supplier.id，空=本厂打色房自做，ADR-0022 自织先例）
     */
    private Long labFactoryId;

    /**
     * 打色承接方公司名快照
     */
    private String labFactoryName;

    /**
     * 成分快照
     */
    private String composition;

    /**
     * 幅宽快照
     */
    private String width;

    /**
     * 克重快照
     */
    private String gsm;

    /**
     * 单据日期（必填，缺省当天）
     */
    private LocalDate instructionDate;

    /**
     * 要求完成日（可空）
     */
    private LocalDate deliveryDate;

    /**
     * 单据状态（字典 lunaerp_color_instruction_status：PENDING 待打色 / IN_PROGRESS 打色中 / CONFIRMED 已确认 / IN_PRODUCTION 已投产 / VOID 已作废）
     */
    private String status;

    /**
     * 确认类型（CUSTOMER 客户确认 / INTERNAL 内部确认）
     */
    private String confirmType;

    /**
     * 客户反馈意见
     */
    private String customerFeedback;

    /**
     * 确认人用户ID
     */
    private Long confirmedBy;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 作废原因
     */
    private String voidReason;

    /**
     * 备注
     */
    private String remark;

}
