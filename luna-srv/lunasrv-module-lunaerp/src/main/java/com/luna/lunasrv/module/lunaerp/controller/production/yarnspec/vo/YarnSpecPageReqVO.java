package com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 纱线规格分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class YarnSpecPageReqVO extends PageParam {

    @Schema(description = "所属材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "规格名称（模糊匹配）", example = "32S")
    private String specName;

    @Schema(description = "标准名称（模糊匹配）", example = "COTTON")
    private String standardName;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}
