package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 客户对账单明细行响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementItemRespVO {

    @Schema(description = "明细行ID", example = "10")
    private Long id;

    @Schema(description = "对账单ID", example = "100")
    private Long statementId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "关联分录ID", example = "501")
    private Long ledgerEntryId;

    @Schema(description = "借贷方向（DEBIT 借方 / CREDIT 贷方）", example = "DEBIT")
    private String direction;

    @Schema(description = "业务日期", example = "2026-09-15")
    private LocalDate bizDate;

    @Schema(description = "源单类型（SHIPMENT/PAYMENT/RETURN_ORDER 等）", example = "SHIPMENT")
    private String refType;

    @Schema(description = "源单单号快照", example = "SH20260915001")
    private String refNo;

    @Schema(description = "摘要/品名或事由", example = "发货出库 - 纯棉色织布")
    private String summary;

    @Schema(description = "借方发生额", example = "1500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal debitAmount;

    @Schema(description = "贷方发生额", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal creditAmount;

    @Schema(description = "签收状态快照标记", example = "SIGNED")
    private String signStatus;

    @Schema(description = "行备注", example = "顺丰特快")
    private String remark;

}
