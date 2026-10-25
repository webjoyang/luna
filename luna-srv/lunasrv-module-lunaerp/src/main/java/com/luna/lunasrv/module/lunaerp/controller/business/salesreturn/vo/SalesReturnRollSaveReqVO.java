package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 业务管理 - 销售退货卷明细 Save Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnRollSaveReqVO {

    @Schema(description = "退货卷ID（修改已有卷时传递）", example = "1001")
    private Long id;

    @Schema(description = "关联原发货卷ID（勾选原卷带回，散退手填为空）", example = "5001")
    private Long shipmentRollId;

    @Schema(description = "卷号（勾选带回可改，手填必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ROLL-001")
    @NotBlank(message = "卷号不能为空")
    private String rollNo;

    @Schema(description = "实称重量（KG）", example = "25.50")
    private BigDecimal weight;

    @Schema(description = "米数（M）", example = "50.00")
    private BigDecimal lengthM;

    @Schema(description = "备注", example = "A等品")
    private String remark;

}
