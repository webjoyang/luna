package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理-质检确认责任认定 Request VO")
@Data
public class InspectionConfirmationConfirmReqVO {

    @Schema(description = "质检确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "质检确认记录ID不能为空")
    private Long id;

    @Schema(description = "责任类型（EQUIPMENT 设备故障 / OPERATOR 操作失误 / WAIVED 免扣）", requiredMode = Schema.RequiredMode.REQUIRED, example = "OPERATOR")
    @NotBlank(message = "责任类型不能为空")
    private String responsibility;

    @Schema(description = "挡车工用户ID（责任类型为 OPERATOR 时必填）", example = "2048")
    private Long workerUserId;

    @Schema(description = "原因备注", example = "氨纶灯不亮导致断氨纶")
    @Size(max = 1000, message = "原因备注长度不能超过1000个字符")
    private String note;

}
