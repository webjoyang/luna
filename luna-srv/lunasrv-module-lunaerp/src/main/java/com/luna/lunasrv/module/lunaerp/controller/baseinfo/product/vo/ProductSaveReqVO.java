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

    @Schema(description = "产品类别（字典 lunaerp_product_category 的 value）", requiredMode = Schema.RequiredMode.REQUIRED, example = "FINISHED")
    @NotBlank(message = "产品类别不能为空")
    private String type;

    @Schema(description = "产品类别字典标签（名称冗余）", example = "成品")
    private String typeLabel;

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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "供应商样号", example = "SUP-001")
    private String supplierCode;

    @Schema(description = "工艺类型")
    private String craftType;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "所属胚布品种ID（历史 UUID 字符串，可空）")
    private String greigeTypeId;

    @Schema(description = "标准米长")
    private String standardLength;

    @Schema(description = "布种（字典 lunaerp_fabric_type 的 value）", example = "KNIT")
    private String fabricType;

    @Schema(description = "布种字典标签（名称冗余）", example = "针织")
    private String fabricTypeLabel;

    @Schema(description = "梭织-总经根数", example = "8880")
    private BigDecimal wovenTotalEnds;

    @Schema(description = "梭织-筘号", example = "144.5")
    private BigDecimal wovenReedNo;

    @Schema(description = "梭织-经密（根/CM）", example = "52")
    private BigDecimal wovenWarpDensity;

    @Schema(description = "梭织-纬密（根/CM）", example = "28")
    private BigDecimal wovenWeftDensity;

    @Schema(description = "梭织-上机纬密（根/CM）", example = "30")
    private BigDecimal wovenLoomWeftDensity;

    @Schema(description = "织物结构（KNIT 针织 / WOVEN 梭织）", example = "KNIT")
    private String fabricConstruction;

    @Schema(description = "梭织估算方式（SIMPLE 简易估算 / PROFESSIONAL 经纬精算）", example = "SIMPLE")
    private String wovenCalculationMode;

    @Schema(description = "坯布克重（GSM，梭织简易估算用）", example = "180")
    private BigDecimal greigeWeightGsm;

    @Schema(description = "坯布门幅（CM，梭织简易估算用）", example = "165")
    private BigDecimal greigeWidthCm;

    @Schema(description = "针织加工费（元/KG）", example = "6.5")
    private BigDecimal knitProcessingFeePerKg;

    @Schema(description = "织造费（元/M）", example = "1.25")
    private BigDecimal weavingFeePerMeter;

    @Schema(description = "其他加工费（元/M）", example = "0.8")
    private BigDecimal otherProcessingFeePerMeter;

    @Schema(description = "包装费", example = "0.3")
    private BigDecimal packagingFee;

    @Schema(description = "税率", example = "0.13")
    private BigDecimal taxRate;

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
