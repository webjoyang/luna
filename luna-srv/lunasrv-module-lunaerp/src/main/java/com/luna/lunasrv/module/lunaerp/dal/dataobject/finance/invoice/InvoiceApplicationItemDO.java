package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;

/**
 * 财务管理 - 发票申请明细行表 DO
 *
 * 表：lunaerp_finance_invoice_application_item
 *
 * @author luna
 */
@TableName("lunaerp_finance_invoice_application_item")
@KeySequence("lunaerp_finance_invoice_application_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceApplicationItemDO extends TenantBaseDO {

    /**
     * 申请明细ID
     */
    private Long id;

    /**
     * 所属发票申请ID（关联 lunaerp_finance_invoice_application.id）
     */
    private Long applicationId;

    /**
     * 明细行序号
     */
    private Integer lineNo;

    /**
     * 关联发货单ID（关联 lunaerp_business_shipment.id）
     */
    private Long shipmentId;

    /**
     * 发货单号快照
     */
    private String shipmentNo;

    /**
     * 发货单价税合计快照（total_amount + freight_fee）
     */
    private BigDecimal shipmentAmount;

    /**
     * 本次申请开票金额（元）
     */
    private BigDecimal amount;

    /**
     * 行备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
