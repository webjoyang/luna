package com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-纱线配比BOM明细 Response VO")
@Data
public class YarnRecipeBomItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "所属配比BOM ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long recipeBomId;

    @Schema(description = "纱线材质ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线材质名称", example = "棉")
    private String yarnTypeName;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "配比百分比数字（49 = 49%）", requiredMode = Schema.RequiredMode.REQUIRED, example = "49.00")
    private BigDecimal ratio;

    @Schema(description = "纺纱工艺（可空）", example = "环锭纺")
    private String spinMethod;

    @Schema(description = "备注", example = "经纱使用")
    private String remark;

    @Schema(description = "参考单价（可空，测算兜底取规格 ref_price）", example = "28.5000")
    private BigDecimal refPrice;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
