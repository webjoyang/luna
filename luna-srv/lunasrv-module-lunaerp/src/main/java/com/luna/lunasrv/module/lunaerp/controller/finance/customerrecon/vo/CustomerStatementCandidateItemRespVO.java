package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 客户对账单候选分录行 Response VO")
@Data
public class CustomerStatementCandidateItemRespVO {

    @Schema(description = "经营分录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long ledgerEntryId;

    @Schema(description = "往来方向（DEBIT 借 / CREDIT 贷）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEBIT")
    private String direction;

    @Schema(description = "业务日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-15")
    private LocalDate bizDate;

    @Schema(description = "来源单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SHIPMENT_ITEM")
    private String refType;

    @Schema(description = "来源主单ID", example = "201")
    private Long refId;

    @Schema(description = "来源明细ID", example = "301")
    private Long refItemId;

    @Schema(description = "来源业务单号", example = "SH2026090001")
    private String refNo;

    @Schema(description = "摘要（品名或事由）", example = "纯棉斜纹面料 32S")
    private String summary;

    @Schema(description = "分录原金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalAmount;

    @Schema(description = "借方发生额（元，净额）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal debitAmount;

    @Schema(description = "贷方发生额（元）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal creditAmount;

    @Schema(description = "发货单签收状态快照标记", example = "SIGNED")
    private String signStatus;

    @Schema(description = "行备注说明")
    private String remark;

    @Schema(description = "分录类型", example = "发货应收")
    private String entryType;

    @Schema(description = "是否已被对账单占用", example = "false")
    private Boolean isOccupied;

    @Schema(description = "占用单号", example = "CS20260001")
    private String occupiedStatementNo;

    @Schema(description = "占用对账单状态", example = "DRAFT")
    private String occupiedStatus;

    public Boolean getOccupied() {
        return isOccupied;
    }

    public void setOccupied(Boolean occupied) {
        this.isOccupied = occupied;
    }

    public String getOccupyingStatementNo() {
        return occupiedStatementNo;
    }

    public void setOccupyingStatementNo(String occupyingStatementNo) {
        this.occupiedStatementNo = occupyingStatementNo;
    }

}
