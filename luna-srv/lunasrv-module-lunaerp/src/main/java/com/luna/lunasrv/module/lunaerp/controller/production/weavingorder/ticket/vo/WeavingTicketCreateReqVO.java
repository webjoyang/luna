package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 布票批量生成 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketCreateReqVO {

    @Schema(description = "机台任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "机台任务ID不能为空")
    private Long machineTaskId;

    @Schema(description = "生成布票数量（匹数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "生成布票数量不能为空")
    @Min(value = 1, message = "生成数量必须为正整数")
    private Integer count;

    @Schema(description = "原料批号", example = "YB20260901-01")
    private String materialBatchNo;

    @Schema(description = "挡车工用户ID（可选，默认继承任务挡车工）", example = "12")
    private Long workerUserId;

    @Schema(description = "挡车工姓名（可选，默认继承任务挡车工）", example = "张三")
    private String workerName;

}
