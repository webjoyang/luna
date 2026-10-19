package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 业务管理 - 销售退货卷到货质检明细 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnReceiveRollReqVO {

    @Schema(description = "退货卷ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "退货卷ID不能为空")
    private Long id;

    @Schema(description = "质检结果判定（RESTOCK 可二次入库 / SCRAP 废品）", requiredMode = Schema.RequiredMode.REQUIRED, example = "RESTOCK")
    @NotBlank(message = "质检结果不能为空")
    private String qcResult;

    @Schema(description = "目标库位ID（仅 RESTOCK 卷，可空）", example = "6001")
    private Long locationId;

    @Schema(description = "质检处置备注", example = "外观完好可二次入库")
    private String qcRemark;

}
