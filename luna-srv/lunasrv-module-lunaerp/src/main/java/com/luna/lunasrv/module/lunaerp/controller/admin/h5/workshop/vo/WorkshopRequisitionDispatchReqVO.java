package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 车间移动 H5 - 领纱明细发纱 Request VO
 *
 * 规范：
 * 1. 移动端发纱必填 requestId（UUID）防重复提交；
 * 2. warehouseId 必填校验；
 * 3. 仅限现场发纱必要字段，不暴露 PC 端专属字段。
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 领纱明细发纱 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopRequisitionDispatchReqVO {

    @Schema(description = "领纱明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "领纱明细ID不能为空")
    private Long itemId;

    @Schema(description = "发纱仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发纱仓库不能为空")
    private Long warehouseId;

    @Schema(description = "库位ID（可空）", example = "10")
    private Long locationId;

    @Schema(description = "发纱数量(KG)", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    @NotNull(message = "发纱数量不能为空")
    @DecimalMin(value = "0.01", message = "发纱数量必须大于0")
    private BigDecimal dispatchQty;

    @Schema(description = "备注说明", example = "现场发纱")
    private String remark;

    @Schema(description = "幂等请求ID（UUID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

}
