package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货发起制单取数详情 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnShipmentDetailRespVO {

    @Schema(description = "发货单ID", example = "100")
    private Long shipmentId;

    @Schema(description = "发货单号", example = "SH-20261018-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "200")
    private Long customerId;

    @Schema(description = "客户公司名称", example = "盛泽纺织")
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

    @Schema(description = "发货明细行列表（带防超退可退量与卷清单）")
    private List<SalesReturnShipmentItemRespVO> items;

}
