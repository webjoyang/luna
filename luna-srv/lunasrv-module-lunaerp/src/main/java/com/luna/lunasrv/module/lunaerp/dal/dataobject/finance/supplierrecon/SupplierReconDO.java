package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务管理 - 供应商对账单主表 DO
 *
 * 表：lunaerp_finance_supplier_recon
 *
 * 单号 SR+YYYYMMDD+3 位日流水，按天取号（ADR-0006 模式），租户内全量唯一含软删行。
 * 三态状态机 DRAFT / CONFIRMED / CANCELLED（字典 2269，缺省 DRAFT）。
 * 不落已申请/已付金额字段（只读实时聚合，杜绝老系统 paidAmount 占位 0 脱节 P0）。
 * 金额一律 numeric(18,2)，仅人民币无币种/汇率列。全部继承 TenantBaseDO。
 *
 * @author luna
 */
@TableName("lunaerp_finance_supplier_recon")
@KeySequence("lunaerp_finance_supplier_recon_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierReconDO extends TenantBaseDO {

    /**
     * 对账单ID
     */
    private Long id;

    /**
     * 对账单号（SR+YYYYMMDD+3位流水，租户内全量唯一含软删行）
     */
    private String reconNo;

    /**
     * 供应商ID（lunaerp_baseinfo_supplier.id）
     */
    private Long supplierId;

    /**
     * 供应商公司名快照
     */
    private String supplierName;

    /**
     * 对账区间起始日期（按源单单据日期过滤，含端点）
     */
    private LocalDate periodStart;

    /**
     * 对账区间截止日期（按源单单据日期过滤，含端点）
     */
    private LocalDate periodEnd;

    /**
     * 状态（字典 lunaerp_supplier_recon_status：DRAFT/CONFIRMED/CANCELLED），缺省 DRAFT
     */
    private String status;

    /**
     * 对账总金额（元，服务端权威重算 = Σ行 line_amount，含负数调整项）
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
