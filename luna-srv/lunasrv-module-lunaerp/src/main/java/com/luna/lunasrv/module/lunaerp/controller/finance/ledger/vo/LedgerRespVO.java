package com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务管理 - 资金流水 Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金流水 Response VO")
@Data
public class LedgerRespVO {

    @Schema(description = "分录ID", example = "1001")
    private Long id;

    @Schema(description = "业务日期", example = "2026-10-19")
    private LocalDate bizDate;

    @Schema(description = "往来方类型（CUSTOMER 客户 / SUPPLIER 供应商）", example = "SUPPLIER")
    private String partyType;

    @Schema(description = "往来方ID", example = "2001")
    private Long partyId;

    @Schema(description = "往来主体名称快照", example = "盛泽染织一厂")
    private String partyName;

    @Schema(description = "资金账户ID", example = "10")
    private Long accountId;

    @Schema(description = "分录类别（字典 lunaerp_finance_ledger_category）", example = "OUT_EXPENSE")
    private String category;

    @Schema(description = "收支方向（DEBIT 借方流入 / CREDIT 贷方流出）", example = "CREDIT")
    private String direction;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "入账汇率", example = "1.00000000")
    private BigDecimal exchangeRate;

    @Schema(description = "原币金额", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "本位币金额", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal baseAmount;

    @Schema(description = "业务回指类型（EXPENSE / PAYROLL / PAYMENT / RECEIPT / TRANSFER / OTHER_TXN 等）", example = "EXPENSE")
    private String refType;

    @Schema(description = "业务回指主单ID", example = "3001")
    private Long refId;

    @Schema(description = "业务回指明细ID", example = "4001")
    private Long refItemId;

    @Schema(description = "业务来源单号快照", example = "BX202610190001")
    private String refNo;

    @Schema(description = "红冲回指原分录ID", example = "500")
    private Long reversalOfId;

    @Schema(description = "备注说明", example = "出差差旅费报销")
    private String remark;

    @Schema(description = "来源单据跳转前端路由", example = "/finance/expense?id=3001")
    private String sourceRoute;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 根据业务回指类型与主单ID解析前端跳转路由
     */
    public static String resolveSourceRoute(String refType, Long refId) {
        if (StrUtil.isBlank(refType)) {
            return null;
        }
        String suffix = refId != null ? "?id=" + refId : "";
        switch (refType.toUpperCase()) {
            case "EXPENSE":
                return "/finance/expense" + suffix;
            case "PAYROLL":
                return "/finance/payroll" + suffix;
            case "TRANSFER":
                return "/finance/account?tab=transfer";
            case "OTHER_TXN":
                return "/finance/account?tab=other-txn";
            case "PAYMENT":
                return "/finance/payment" + suffix;
            case "RECEIPT":
                return "/finance/receipt" + suffix;
            case "SUPPLIER_RECON":
                return "/finance/supplier-recon" + suffix;
            case "CUSTOMER_RECON":
                return "/finance/customer-recon" + suffix;
            case "SHIPMENT":
            case "SHIPMENT_ITEM":
            case "SHIPMENT_FREIGHT":
                return "/business/shipment" + suffix;
            case "SALES_RETURN":
                return "/business/sales-return" + suffix;
            default:
                return null;
        }
    }

}
