package com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 发票申请明细行 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemRespVO {

    @Schema(description = "明细ID", example = "10")
    private Long id;

    @Schema(description = "行序号", example = "1")
    private Integer lineNo;

    @Schema(description = "关联发货单ID", example = "200")
    private Long shipmentId;

    @Schema(description = "发货单号快照", example = "SH-20261012-001")
    private String shipmentNo;

    @Schema(description = "发货单价税合计快照（元）", example = "10500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal shipmentAmount;

    @Schema(description = "本次申请开票金额（元）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "行备注", example = "首批货物开票")
    private String remark;

}
