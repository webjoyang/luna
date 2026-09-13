package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 基本信息-产品创建/修改 Request VO")
@Data
public class ProductSaveReqVO {

    @Schema(description = "产品ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "产品编号（租户级唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "P0001")
    @NotBlank(message = "产品编号不能为空")
    private String code;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "莫代尔汗布")
    @NotBlank(message = "产品名称不能为空")
    private String name;

    @Schema(description = "产品类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "汗布")
    @NotBlank(message = "产品类型不能为空")
    private String type;

    @Schema(description = "成分（如 NR / TR / CVC）", requiredMode = Schema.RequiredMode.REQUIRED, example = "NR")
    @NotBlank(message = "成分不能为空")
    private String composition;

    @Schema(description = "成品门幅（厘米）", requiredMode = Schema.RequiredMode.REQUIRED, example = "160")
    @NotNull(message = "成品门幅不能为空")
    private BigDecimal widthCm;

    @Schema(description = "成品克重（g/m²）", requiredMode = Schema.RequiredMode.REQUIRED, example = "260")
    @NotNull(message = "成品克重不能为空")
    private BigDecimal weightGsm;

    @Schema(description = "封面图片URL", example = "https://www.abc.com/cover.png")
    private String coverImageUrl;

    @Schema(description = "供应商ID（历史 UUID 字符串）", example = "999")
    private String supplierId;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用 / DRAFT 草稿），创建缺省 ACTIVE", example = "ACTIVE")
    private String status;

    @Schema(description = "是否官网公开", example = "false")
    private Boolean isPublic;

    @Schema(description = "是否首页推荐", example = "false")
    private Boolean isFeatured;

    @Schema(description = "首页推荐排序", example = "0")
    private Integer featuredOrder;

    @Schema(description = "纱支明细（JSON 原文，不解析）")
    private String yarnSpecs;

    @Schema(description = "机型", example = "大圆机")
    private String machineType;

    @Schema(description = "产量", example = "200kg/天")
    private String output;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "原厂编号", example = "SUP-001")
    private String supplierCode;

    @Schema(description = "临时编号")
    private String tempCode;

    @Schema(description = "工艺类型")
    private String craftType;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "所属胚布品种ID（历史 UUID 字符串，可空）")
    private String greigeTypeId;

    @Schema(description = "花型号")
    private String patternNo;

    @Schema(description = "标准米长")
    private String standardLength;

    @Schema(description = "算长系数（1KG = ?米/码）", example = "3.25")
    private BigDecimal conversionRate;

    @Schema(description = "成本项明细（JSON 原文，不解析）")
    private String costItems;

    @Schema(description = "AI 设计图URL")
    private String aiDesignUrl;

    @Schema(description = "AI 设计分类（如 CASUAL BLAZER）")
    private String aiDesignCategory;

    @Schema(description = "AI 设计描述（推理说明/风格关键词）")
    private String aiDesignDescription;

    @Schema(description = "事业部归属（TRADE 贸易 / FACTORY 工厂）")
    private String businessOwnership;

    @Schema(description = "产品图片列表（整体替换，按 sortOrder 升序展示）")
    @Valid
    private List<ProductImageVO> images;

    @Schema(description = "产品标签列表（整体替换；同租户同名标签复用并保留原色）")
    @Valid
    private List<ProductTagRefVO> tags;

}
