package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 业务管理 - 报价单主表 DO
 *
 * @author luna
 */
@TableName("lunaerp_business_quotation")
@KeySequence("lunaerp_business_quotation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationDO extends TenantBaseDO {

    /**
     * 报价单ID
     */
    @TableId
    private Long id;

    /**
     * 报价单号（QT{yyyy}NNNN，同号多版本共享）
     */
    private String quotationNo;

    /**
     * 版本号（首版为1，修订自增）
     */
    private Integer versionNo;

    /**
     * 客户档案ID
     */
    private Long customerId;

    /**
     * 客户名快照
     */
    private String clientName;

    /**
     * 客户联系人快照
     */
    private String clientContact;

    /**
     * 联系电话快照
     */
    private String phone;

    /**
     * 归属业务员
     */
    private Long salespersonUserId;

    /**
     * 归属部门（服务端根据业务员推导）
     */
    private Long deptId;

    /**
     * 报价日期
     */
    private LocalDate quotationDate;

    /**
     * 有效期自
     */
    private LocalDate validFrom;

    /**
     * 有效期至（null 表示长期有效）
     */
    private LocalDate validTo;

    /**
     * 报价币种（RMB / USD / EUR 等）
     */
    private String currency;

    /**
     * 汇率（1 外币兑换人民币，RMB 为 1）
     */
    private BigDecimal exchangeRate;

    /**
     * 总数量
     */
    private BigDecimal totalQuantity;

    /**
     * 总金额（报价币种）
     */
    private BigDecimal totalAmount;

    /**
     * 总成本额（报价币种，脱敏字段）
     */
    private BigDecimal totalCostAmount;

    /**
     * 整单加权毛利率（脱敏字段）
     */
    private BigDecimal avgMarginRate;

    /**
     * 状态（DRAFT / CONFIRMED / SUPERSEDED / CANCELLED）
     */
    private String status;

    /**
     * 备注
     */
    private String remarks;

}
