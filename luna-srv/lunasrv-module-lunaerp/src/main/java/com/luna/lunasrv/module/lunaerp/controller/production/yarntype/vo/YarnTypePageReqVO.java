package com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 纱线材质分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class YarnTypePageReqVO extends PageParam {

    @Schema(description = "材质名称（支持模糊匹配）", example = "棉")
    private String name;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}
