package com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线配比BOM Response VO")
@Data
public class YarnRecipeBomRespVO {

    @Schema(description = "配比BOM ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "产品档案ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PROD2026001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "备注", example = "常规款配比")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "明细行数", example = "2")
    private Integer itemCount;

    @Schema(description = "配比明细列表")
    private List<YarnRecipeBomItemRespVO> items;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
