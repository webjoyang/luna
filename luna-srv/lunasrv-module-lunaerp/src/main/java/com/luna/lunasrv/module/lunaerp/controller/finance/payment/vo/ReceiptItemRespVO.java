package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 收款单核销明细 Response VO")
@Data
public class ReceiptItemRespVO {

    @Schema(description = "核销明细ID", example = "5001")
    private Long id;

    @Schema(description = "所属收款单ID", example = "1024")
    private Long paymentId;

    @Schema(description = "关联的分录ID（ref_id）", example = "2001")
    private Long entryId;

    @Schema(description = "关联的业务单号快照（如发货单号）", example = "SH-20261016-001")
    private String entryNo;

    @Schema(description = "业务日期快照", example = "2026-10-15")
    private LocalDate bizDate;

    @Schema(description = "本次核销分配金额（元）", example = "1500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocateAmount;

    @Schema(description = "本位币核销金额快照（元）", example = "1500.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal baseAmount;

    @Schema(description = "核销分录回指ID", example = "3001")
    private Long ledgerEntryId;

    @Schema(description = "品名/摘要", example = "全棉弹力府绸")
    private String summary;

    @Schema(description = "关联销售合同号", example = "HT-20261010-001")
    private String contractNo;

    @Schema(description = "关联对账单号", example = "CS20260001")
    private String statementNo;

}
