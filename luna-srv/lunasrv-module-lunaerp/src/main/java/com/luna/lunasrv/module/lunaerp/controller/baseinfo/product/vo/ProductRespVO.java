package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 基本信息-产品信息 Response VO")
@Data
public class ProductRespVO {

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "产品编号（租户级唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "P0001")
    private String code;

    @Schema(description = "产品编号规范值")
    private String normalizedCode;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "莫代尔汗布")
    private String name;

    @Schema(description = "产品类别（字典 lunaerp_product_category 的 value）", requiredMode = Schema.RequiredMode.REQUIRED, example = "FINISHED")
    private String type;

    @Schema(description = "产品类别字典标签（名称冗余，供前端直接展示）", example = "成品")
    private String typeLabel;

    @Schema(description = "成分", requiredMode = Schema.RequiredMode.REQUIRED, example = "NR")
    private String composition;

    @Schema(description = "成品门幅（厘米）", requiredMode = Schema.RequiredMode.REQUIRED, example = "160")
    private BigDecimal widthCm;

    @Schema(description = "成品克重（g/m²）", requiredMode = Schema.RequiredMode.REQUIRED, example = "260")
    private BigDecimal weightGsm;

    @Schema(description = "封面图片URL")
    private String coverImageUrl;

    @Schema(description = "供应商ID（历史 UUID 字符串）")
    private String supplierId;

    // TODO 供应商模块迁移后接入：按 supplierId 查询供应商公司名称
    @Schema(description = "供应商公司名称（供应商模块迁移前恒为 null）")
    private String supplierCompany;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用 / DRAFT 草稿）")
    private String status;

    @Schema(description = "是否官网公开")
    private Boolean isPublic;

    @Schema(description = "是否首页推荐")
    private Boolean isFeatured;

    @Schema(description = "首页推荐排序")
    private Integer featuredOrder;

    @Schema(description = "纱支明细（JSON 原文）")
    private String yarnSpecs;

    @Schema(description = "机型")
    private String machineType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "供应商样号")
    private String supplierCode;

    @Schema(description = "工艺类型")
    private String craftType;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "所属胚布品种ID")
    private String greigeTypeId;

    @Schema(description = "标准米长")
    private String standardLength;

    @Schema(description = "布种（字典 lunaerp_fabric_type 的 value）", example = "KNIT")
    private String fabricType;

    @Schema(description = "布种字典标签（名称冗余，供前端直接展示）", example = "针织")
    private String fabricTypeLabel;

    @Schema(description = "梭织-总经根数")
    private BigDecimal wovenTotalEnds;

    @Schema(description = "梭织-筘号")
    private BigDecimal wovenReedNo;

    @Schema(description = "梭织-经密（根/CM）")
    private BigDecimal wovenWarpDensity;

    @Schema(description = "梭织-纬密（根/CM）")
    private BigDecimal wovenWeftDensity;

    @Schema(description = "梭织-上机纬密（根/CM）")
    private BigDecimal wovenLoomWeftDensity;

    @Schema(description = "织物结构（KNIT 针织 / WOVEN 梭织）")
    private String fabricConstruction;

    @Schema(description = "梭织估算方式（SIMPLE 简易估算 / PROFESSIONAL 经纬精算）")
    private String wovenCalculationMode;

    @Schema(description = "坯布克重（GSM）")
    private BigDecimal greigeWeightGsm;

    @Schema(description = "坯布门幅（CM）")
    private BigDecimal greigeWidthCm;

    @Schema(description = "针织加工费（元/KG）")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private BigDecimal knitProcessingFeePerKg;

    @Schema(description = "织造费（元/M）")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private BigDecimal weavingFeePerMeter;

    @Schema(description = "其他加工费（元/M）")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private BigDecimal otherProcessingFeePerMeter;

    @Schema(description = "包装费")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private BigDecimal packagingFee;

    @Schema(description = "税率")
    private BigDecimal taxRate;

    @Schema(description = "成本项明细（JSON 原文）")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private String costItems;

    @Schema(description = "汇总成本价")
    @Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
    private BigDecimal costPrice;

    @Schema(description = "成本状态（MISSING 缺失 / FILLED 已填写）")
    private String costStatus;

    @Schema(description = "成本最后更新时间")
    private LocalDateTime costUpdatedAt;

    @Schema(description = "成本最后更新人ID")
    private String costUpdatedBy;

    @Schema(description = "AI 设计图URL")
    private String aiDesignUrl;

    @Schema(description = "AI 设计分类")
    private String aiDesignCategory;

    @Schema(description = "AI 设计描述")
    private String aiDesignDescription;

    @Schema(description = "手机拍布建档草稿ID")
    private String captureDraftId;

    @Schema(description = "事业部归属（TRADE 贸易 / FACTORY 工厂）")
    private String businessOwnership;

    @Schema(description = "创建人ID")
    private String createdById;

    @Schema(description = "更新人ID")
    private String updatedById;

    @Schema(description = "产品图片列表（按 sortOrder 升序）")
    private List<ProductImageVO> images;

    @Schema(description = "产品标签列表（含标签 id/name/color 与关联 sortOrder）")
    private List<ProductTagRefVO> tags;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
