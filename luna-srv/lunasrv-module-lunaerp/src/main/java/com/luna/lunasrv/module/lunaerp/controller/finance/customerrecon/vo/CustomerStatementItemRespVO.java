package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 客户对账单明细行 Response VO")
@Data
public class CustomerStatementItemRespVO {

    @Schema(description = "明细行ID", example = "1")
    private Long id;

    @Schema(description = "所属对账单ID", example = "1001")
    private Long statementId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "分录ID", example = "2001")
    private Long ledgerEntryId;

    @Schema(description = "往来方向（DEBIT 借 / CREDIT 贷）", example = "DEBIT")
    private String direction;

    @Schema(description = "业务日期快照", example = "2026-09-15")
    private LocalDate bizDate;

    @Schema(description = "来源单据类型快照", example = "SHIPMENT_ITEM")
    private String refType;

    @Schema(description = "来源业务单号快照", example = "SH2026090001")
    private String refNo;

    @Schema(description = "摘要（品名或事由）", example = "纯棉斜纹面料 32S")
    private String summary;

    @Schema(description = "借方金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额（元）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal creditAmount;

    @Schema(description = "签收状态快照标记", example = "SIGNED")
    private String signStatus;

    @Schema(description = "行备注说明")
    private String remark;

    @Schema(description = "源单是否红冲/异常（SENT 状态下红冲行标红标记）", example = "false")
    private Boolean hasReversal;

}
