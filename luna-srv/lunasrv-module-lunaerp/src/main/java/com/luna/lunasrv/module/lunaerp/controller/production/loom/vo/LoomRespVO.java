package com.luna.lunasrv.module.lunaerp.controller.production.loom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-织机 Response VO")
@Data
public class LoomRespVO {

    @Schema(description = "织机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "机台号", requiredMode = Schema.RequiredMode.REQUIRED, example = "LM20260001")
    private String loomNo;

    @Schema(description = "型号", example = "34″·28G")
    private String model;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
