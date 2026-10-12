package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice;

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
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 客户ID（关联 lunaerp_baseinfo_customer.id）
     */
    private Long customerId;

    /**
     * 客户名称快照
     */
    private String customerName;

    /**
     * 币种（CNY / USD / EUR 等）
     */
    private String currency;

    /**
     * 税率（字典 lunaerp_finance_tax_rate：13/9/6/3/1/0/EXEMPT）
     */
    private String taxRate;

    /**
     * 发票种类（字典 lunaerp_finance_invoice_kind：ELEC_SPECIAL/ELEC_NORMAL/VAT_SPECIAL/VAT_NORMAL）
     */
    private String invoiceKind;

    /**
     * 是否电子发票（1是 0否）
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
     * 开户银行及账号快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bankInfo;

    /**
     * 状态（DRAFT/SUBMITTED/ISSUED/REJECTED）
     */
    private String status;

    /**
     * 正式发票号码（登记回填，20位，租户内唯一）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String officialInvoiceNo;

    /**
     * 发票代码（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String invoiceCode;

    /**
     * 开票日期（登记回填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate invoiceDate;

    /**
     * 发票文件/回单附件URL
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String attachmentUrl;

    /**
     * 不含税金额合计（服务端按税率反算）
     */
    private BigDecimal totalAmountExclTax;

    /**
     * 税额合计（服务端按税率反算）
     */
    private BigDecimal totalTax;

    /**
     * 价税合计（源发货单金额汇总）
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
     * 驳回原因
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rejectReason;

    /**
     * 备注说明
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
