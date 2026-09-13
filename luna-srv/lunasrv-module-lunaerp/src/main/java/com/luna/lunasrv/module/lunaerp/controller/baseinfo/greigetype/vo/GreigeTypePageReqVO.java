package com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 胚布品种分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeTypePageReqVO extends PageParam {

    @Schema(description = "品种编号，模糊匹配", example = "GT-0001")
    private String code;

    @Schema(description = "显示名称，模糊匹配", example = "罗马布")
    private String name;

    @Schema(description = "布种，模糊匹配", example = "罗马布")
    private String fabricType;

    @Schema(description = "成分，模糊匹配", example = "NR")
    private String composition;

    @Schema(description = "纱支，模糊匹配", example = "30S")
    private String yarnSpec;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），等值匹配", example = "ACTIVE")
    private String status;

}
