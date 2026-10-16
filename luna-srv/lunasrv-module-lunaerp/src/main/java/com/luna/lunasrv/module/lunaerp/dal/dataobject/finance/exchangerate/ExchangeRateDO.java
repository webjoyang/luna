package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 汇率主档 DO（域公共，SPOT 即期牌价）
 *
 * 表：lunaerp_finance_exchange_rate
 *
 * @author luna
 */
@TableName("lunaerp_finance_exchange_rate")
@KeySequence("lunaerp_finance_exchange_rate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExchangeRateDO extends TenantBaseDO {

    /**
     * 汇率记录ID
     */
    private Long id;

    /**
     * 外币币种（USD/EUR/HKD 等）
     */
    private String currency;

    /**
     * 汇率（1 外币 = rate 本位币）
     */
    private BigDecimal rate;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 状态（ENABLE 启用 / DISABLE 停用）
     */
    private String status;

    /**
     * 备注说明
     */
    private String remark;

}
