package com.luna.lunasrv.module.lunaerp.controller.production.loom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理-织机创建/修改 Request VO")
@Data
public class LoomSaveReqVO {

    @Schema(description = "织机ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "机台号（留空自动生成 LM+年份+4位序号；手填按原值保存）", example = "LM20260001")
    private String loomNo;

    @Schema(description = "型号", example = "34″·28G")
    private String model;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE", example = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;

}
