package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 收付款单 Response VO")
@Data
public class PaymentRespVO {

    @Schema(description = "收付款单ID", example = "1024")
    private Long id;

    @Schema(description = "收付款单号（PAY+YYYYMMDD+3位流水）", example = "PAY20261015001")
    private String paymentNo;

    @Schema(description = "收付款方向（IN 收款 / OUT 付款）", example = "OUT")
    private String direction;

    @Schema(description = "供应商ID（OUT 付款）", example = "1001")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "某某织造有限公司")
    private String supplierName;

    @Schema(description = "客户ID（IN 收款）", example = "2001")
    private Long customerId;

    @Schema(description = "资金账户ID", example = "3001")
    private Long accountId;

    @Schema(description = "本次资金实收/实付金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal payAmount;

    @Schema(description = "预收/预付抵扣金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal prepayUsedAmount;

    @Schema(description = "状态（COMPLETED 登记完成 / REVERSED 红冲取消预留）", example = "COMPLETED")
    private String status;

    @Schema(description = "备注", example = "10月第一笔货款结算")
    private String remark;

    @Schema(description = "登记人/创建者", example = "admin")
    private String creator;

    @Schema(description = "登记时间/创建时间", example = "2026-10-15 10:30:00")
    private LocalDateTime createTime;

    @Schema(description = "核销申请明细列表")
    private List<PaymentRefRespVO> items;

}
