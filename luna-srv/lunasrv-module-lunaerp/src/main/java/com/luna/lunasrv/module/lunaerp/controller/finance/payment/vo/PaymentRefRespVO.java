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

@Schema(description = "管理后台 - 收付款核销明细 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefRespVO {

    @Schema(description = "核销明细ID", example = "10")
    private Long id;

    @Schema(description = "所属收付款单ID", example = "100")
    private Long paymentId;

    @Schema(description = "核销目标类型（PAY_APPLY 付款申请 / AR_ENTRY 应收分录）", example = "PAY_APPLY")
    private String refType;

    @Schema(description = "核销目标记录ID", example = "200")
    private Long refId;

    @Schema(description = "付款申请单号快照", example = "FK20261011001")
    private String applyNo;

    @Schema(description = "本次核销分配金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocateAmount;

}
