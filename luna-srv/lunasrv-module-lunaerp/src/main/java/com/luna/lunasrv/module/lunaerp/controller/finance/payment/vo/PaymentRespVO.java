package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 收付款单 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRespVO {

    @Schema(description = "收付款单ID", example = "100")
    private Long id;

    @Schema(description = "收付款单号", example = "PAY20261011001")
    private String paymentNo;

    @Schema(description = "收付款方向（IN 收款 / OUT 付款）", example = "OUT")
    private String direction;

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "某某织造有限公司")
    private String supplierName;

    @Schema(description = "资金账户ID", example = "2001")
    private Long accountId;

    @Schema(description = "本次资金实收/实付金额（元）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal payAmount;

    @Schema(description = "预收/预付款抵扣金额（元）", example = "2000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal prepayUsedAmount;

    @Schema(description = "状态（COMPLETED 已登记 / REVERSED 已红冲）", example = "COMPLETED")
    private String status;

    @Schema(description = "备注", example = "付款登记")
    private String remark;

    @Schema(description = "创建时间/登记时间")
    private LocalDateTime createTime;

    @Schema(description = "核销明细列表")
    private List<PaymentRefRespVO> items;

}
