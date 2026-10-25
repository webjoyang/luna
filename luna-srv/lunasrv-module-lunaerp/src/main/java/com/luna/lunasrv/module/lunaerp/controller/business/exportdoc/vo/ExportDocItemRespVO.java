package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 业务管理-出口单证明细行 Response VO")
@Data
public class ExportDocItemRespVO {

    @Schema(description = "明细行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "所属单证套ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long docId;

    @Schema(description = "行序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

    @Schema(description = "中文品名", example = "纯棉斜纹布")
    private String nameCn;

    @Schema(description = "英文品名", example = "100% COTTON TWILL FABRIC")
    private String nameEn;

    @Schema(description = "海关编码 HS CODE", example = "5208.1200")
    private String hsCode;

    @Schema(description = "规格（成分/门幅/颜色）", example = "100%C 150CM 200GSM WHITE")
    private String spec;

    @Schema(description = "数量", example = "1000.00")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "M")
    private String unit;

    @Schema(description = "单价", example = "3.50")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "3500.00")
    private BigDecimal amount;

    @Schema(description = "净重 KG", example = "200.00")
    private BigDecimal netWeight;

    @Schema(description = "毛重 KG", example = "215.00")
    private BigDecimal grossWeight;

    @Schema(description = "箱数/件数", example = "10")
    private Integer packages;

    @Schema(description = "体积 CBM", example = "1.250")
    private BigDecimal volume;

    @Schema(description = "原产地", example = "CHINA")
    private String origin;

    @Schema(description = "行备注", example = "卷装贴唛")
    private String remark;

}
