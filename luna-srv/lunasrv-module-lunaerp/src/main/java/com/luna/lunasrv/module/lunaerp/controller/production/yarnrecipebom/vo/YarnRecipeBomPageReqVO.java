package com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 生产管理-纱线配比BOM分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class YarnRecipeBomPageReqVO extends PageParam {

    @Schema(description = "产品档案ID", example = "2048")
    private Long productId;

    @Schema(description = "产品编码（模糊匹配）", example = "PROD2026")
    private String productCode;

    @Schema(description = "产品名称（模糊匹配）", example = "全棉汗布")
    private String productName;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

}
