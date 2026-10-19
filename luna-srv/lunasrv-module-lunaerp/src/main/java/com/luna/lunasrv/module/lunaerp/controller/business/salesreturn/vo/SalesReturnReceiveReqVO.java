package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货到货质检 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnReceiveReqVO {

    @Schema(description = "退货单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "退货单ID不能为空")
    private Long id;

    @Schema(description = "退货卷质检判定清单", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "退货卷质检列表不能为空")
    @Valid
    private List<SalesReturnReceiveRollReqVO> rolls;

}
