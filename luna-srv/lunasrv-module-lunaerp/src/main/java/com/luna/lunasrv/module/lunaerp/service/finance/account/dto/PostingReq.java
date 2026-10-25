package com.luna.lunasrv.module.lunaerp.service.finance.account.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 统一资金过账请求 DTO（ADR-0043 唯一资金写入口契约）
 *
 * @author luna
 */
@Data
@Accessors(chain = true)
public class PostingReq {

    /**
     * 资金账户ID（post 必传；13号往来分录 appendArEntry 为空）
     */
    private Long accountId;

    /**
     * 收支方向（DEBIT 流入 / CREDIT 流出）
     */
    private String direction;

    /**
     * 分录类别（字典 lunaerp_finance_ledger_category，如 IN_CUSTOMER, OUT_SUPPLIER, OUT_EXPENSE 等）
     */
    private String category;

    /**
     * 往来方类型（CUSTOMER / SUPPLIER / EMPLOYEE / NONE 等）
     */
    private String partyType;

    /**
     * 往来方ID
     */
    private Long partyId;

    /**
     * 往来主体名称快照（免 join 查询）
     */
    private String partyName;

    /**
     * 币种（RMB/USD/EUR/HKD 等，须与资金账户一致）
     */
    private String currency;

    /**
     * 原币金额（元，必须 > 0）
     */
    private BigDecimal originalAmount;

    /**
     * 入账汇率（RMB 恒为 1.00000000）
     */
    private BigDecimal exchangeRate;

    /**
     * 业务日期（默认当日）
     */
    private LocalDate bizDate;

    /**
     * 业务回指类型（PAYMENT / EXPENSE / PAYROLL / TRANSFER / OTHER_TXN 等）
     */
    private String refType;

    /**
     * 业务回指主单ID
     */
    private Long refId;

    /**
     * 业务回指明细ID（可选）
     */
    private Long refItemId;

    /**
     * 业务来源单号快照（BX/GZ/GF/ZZ/QT 等）
     */
    private String refNo;

    /**
     * 备注说明
     */
    private String remark;

}
