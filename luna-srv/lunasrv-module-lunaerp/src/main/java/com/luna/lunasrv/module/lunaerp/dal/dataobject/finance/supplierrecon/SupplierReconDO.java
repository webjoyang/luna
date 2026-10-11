package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务管理 - 供应商对账单主表 DO
 *
 * 表：lunaerp_finance_supplier_recon
 *
 * 单号 SR+YYYYMMDD+3位日流水，按天取号，租户内全量唯一含软删行（ADR-0006/0015）；
 * 状态三态：DRAFT 草稿 / CONFIRMED 已确认 / CANCELLED 已作废；
 * 不落已申请/已付金额字段（实时只读聚合，防脱节）。
 *
 * @author luna
 */
@TableName("lunaerp_finance_supplier_recon")
@KeySequence("lunaerp_finance_supplier_recon_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconDO extends TenantBaseDO {

    /**
     * 对账单ID
     */
    private Long id;

    /**
     * 对账单号（SR+YYYYMMDD+3位日流水，租户内全量唯一含软删行）
     */
    private String reconNo;

    /**
     * 供应商ID（关联 lunaerp_baseinfo_supplier.id）
     */
    private Long supplierId;

    /**
     * 供应商名称快照
     */
    private String supplierName;

    /**
     * 对账区间开始日期（含端点，按源单单据日期过滤）
     */
    private LocalDate periodStart;

    /**
     * 对账区间结束日期（含端点，按源单单据日期过滤）
     */
    private LocalDate periodEnd;

    /**
     * 状态（字典 lunaerp_supplier_recon_status：DRAFT/CONFIRMED/CANCELLED）
     */
    private String status;

    /**
     * 确认金额合计（元，服务端权威重算，含负数调整项）
     */
    private BigDecimal totalAmount;

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
     * 作废人用户ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long cancelledBy;

    /**
     * 作废时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime cancelledAt;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
