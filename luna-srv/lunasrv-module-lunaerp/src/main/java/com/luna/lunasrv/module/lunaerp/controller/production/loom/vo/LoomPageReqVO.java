package com.luna.lunasrv.module.lunaerp.controller.production.loom.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理-织机分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoomPageReqVO extends PageParam {

    @Schema(description = "智能搜索词（命中 loomNo/model/remark 之一）", example = "A01")
    private String search;

    @Schema(description = "状态：ACTIVE/INACTIVE", example = "ACTIVE")
    private String status;

}
