package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

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

@Schema(description = "管理后台 - 业务管理 - 销售退货可选发货单候选 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnCandidateRespVO {

    @Schema(description = "发货单ID", example = "100")
    private Long id;

    @Schema(description = "发货单号（SH-YYYYMMDD-NNN）", example = "SH-20261018-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "200")
    private Long customerId;

    @Schema(description = "客户名称", example = "盛泽纺织")
    private String customerName;

    @Schema(description = "销售合同ID", example = "300")
    private Long salesContractId;

    @Schema(description = "销售合同号", example = "CH20260001")
    private String contractNo;

    @Schema(description = "原发货仓库ID", example = "400")
    private Long warehouseId;

    @Schema(description = "原发货仓库名称", example = "柯桥成品一号仓")
    private String warehouseName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "发货日期", example = "2026-10-18")
    private LocalDate shippingDate;

    @Schema(description = "发货总金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "发货总匹数", example = "10")
    private Integer totalPieces;

}
