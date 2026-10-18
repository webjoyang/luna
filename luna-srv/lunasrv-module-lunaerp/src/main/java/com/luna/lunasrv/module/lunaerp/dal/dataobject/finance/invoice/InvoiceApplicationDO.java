package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice;

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
 * 财务管理 - 发票申请主表 DO
 *
 * 表：lunaerp_finance_invoice_application
 *
 * @author luna
 */
@TableName("lunaerp_finance_invoice_application")
@KeySequence("lunaerp_finance_invoice_application_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceApplicationDO extends TenantBaseDO {

    /**
     * 发票申请ID
     */
    private Long id;

    /**
     * 发票申请单号（IA+YYYY+4位流水，租户内全量唯一含软删行）
     */
    private String invoiceNo;

    /**
     * 客户ID（lunaerp_baseinfo_customer.id）
     */
    private Long customerId;

    /**
     * 客户公司名快照
     */
    private String customerName;

    /**
     * 币种（同发货单币种，禁混选）
     */
    private String currency;

    /**
     * 开票税率百分比（字典 2276，如 13.00, 9.00, 0.00）
     */
    private BigDecimal taxRate;

    /**
     * 发票种类（字典 2275：DIGITAL_SPECIAL/DIGITAL_NORMAL/VAT_SPECIAL/VAT_NORMAL）
     */
    private String invoiceKind;

    /**
     * 是否电子发票（1是 0否纸质）
     */
    private Integer electronic;

    /**
     * 开票抬头快照
     */
    private String invoiceTitle;

    /**
     * 纳税人识别号/统一社会信用代码快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String taxNo;

    /**
     * 开票地址电话快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String address;

    /**
     * 开户行及账号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankInfo;

    /**
     * 状态（DRAFT/SUBMITTED/ISSUED/REJECTED）
     */
    private String status;

    /**
     * 官方发票号码（登记开票时回填，20位数电号码）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String officialInvoiceNo;

    /**
     * 发票代码（传统纸票可选，数电票为空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String invoiceCode;

    /**
     * 开票日期
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate invoiceDate;

    /**
     * 发票电子附件URL
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String attachmentUrl;

    /**
     * 不含税金额合计（元，服务端按税率反算）
     */
    private BigDecimal totalAmountExclTax;

    /**
     * 税额合计（元，服务端反算）
     */
    private BigDecimal totalTax;

    /**
     * 价税合计总金额（元，所选发货单本次开票金额汇总）
     */
    private BigDecimal totalAmountInclTax;

    /**
     * 开票登记人
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String issueBy;

    /**
     * 开票登记时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime issueTime;

    /**
     * 提交人
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String submitBy;

    /**
     * 提交时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime submitTime;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
