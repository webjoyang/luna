package com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线配比BOM保存 Request VO")
@Data
public class YarnRecipeBomSaveReqVO {

    @Schema(description = "配比BOM ID", example = "1024")
    private Long id;

    @Schema(description = "产品档案ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "产品档案ID不能为空")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PROD2026001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "备注", example = "常规款配比")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", example = "ACTIVE")
    private String status;

    @Schema(description = "配比明细列表")
    @Valid
    private List<YarnRecipeBomItemSaveReqVO> items;

}
