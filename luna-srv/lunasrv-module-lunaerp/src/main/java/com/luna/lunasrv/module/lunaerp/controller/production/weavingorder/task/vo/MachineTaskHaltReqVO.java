package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理 - 机台任务停机 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineTaskHaltReqVO {

    @Schema(description = "机台任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "机台任务ID不能为空")
    private Long id;

    @Schema(description = "停机原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "机械故障检修")
    @NotBlank(message = "停机原因不能为空")
    @Size(max = 255, message = "停机原因长度不能超过 255 位")
    private String haltReason;

}
