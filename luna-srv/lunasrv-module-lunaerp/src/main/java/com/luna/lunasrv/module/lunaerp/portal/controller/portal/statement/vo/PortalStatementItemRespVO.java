package com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "客户门户 - 对账单行明细响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalStatementItemRespVO {

    @Schema(description = "明细行ID", example = "201")
    private Long id;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "借贷方向（DEBIT 借方 / CREDIT 贷方）", example = "DEBIT")
    private String direction;

    @Schema(description = "业务日期", example = "2026-10-15")
    private LocalDate bizDate;

    @Schema(description = "源单类型", example = "SHIPMENT")
    private String refType;

    @Schema(description = "单据编号/关联单号", example = "SH-20261015-001")
    private String refNo;

    @Schema(description = "摘要/事由", example = "发货单出库")
    private String summary;

    @Schema(description = "借方金额（本期发货/应收）", example = "50000.00")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额（本期回款/冲减）", example = "0.00")
    private BigDecimal creditAmount;

    @Schema(description = "签收状态标记", example = "SIGNED")
    private String signStatus;

    @Schema(description = "行备注")
    private String remark;

    @Schema(description = "源单红冲标记（SENT期若源分录被红冲则为true）", example = "false")
    private Boolean reversalFlag;

}
