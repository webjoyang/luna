package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 收付款核销明细 Response VO")
@Data
public class PaymentRefRespVO {

    @Schema(description = "核销明细ID", example = "1024")
    private Long id;

    @Schema(description = "所属收付款单ID", example = "2001")
    private Long paymentId;

    @Schema(description = "核销业务类型（PAY_APPLY 付款申请）", example = "PAY_APPLY")
    private String refType;

    @Schema(description = "关联业务单据ID（付款申请ID）", example = "3001")
    private Long refId;

    @Schema(description = "关联付款申请单号快照", example = "FK20261015001")
    private String applyNo;

    @Schema(description = "本次核销分配金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocateAmount;

    @Schema(description = "创建时间", example = "2026-10-15 10:30:00")
    private LocalDateTime createTime;

}
