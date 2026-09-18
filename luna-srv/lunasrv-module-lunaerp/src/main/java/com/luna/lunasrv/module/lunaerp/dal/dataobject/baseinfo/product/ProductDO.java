package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基本信息-产品表 DO
 *
 * 表：lunaerp_baseinfo_product
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_product")
@KeySequence("lunaerp_baseinfo_product_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDO extends TenantBaseDO {

    /**
     * 产品ID
     */
    private Long id;
    /**
     * 产品编号（租户级唯一）
     */
    private String code;
    /**
     * 产品编号规范值（全角转半角/去噪/大写，供拍照识别精确匹配）
     */
    private String normalizedCode;
    /**
     * 产品名称
     */
    private String name;
    /**
     * 产品类型
     */
    private String type;
    /**
     * 成分（如 NR / TR / CVC）
     */
    private String composition;
    /**
     * 成品门幅（厘米）
     */
    private BigDecimal widthCm;
    /**
     * 成品克重（g/m²）
     */
    private BigDecimal weightGsm;
    /**
     * 封面图片URL
     */
    private String coverImageUrl;
    /**
     * 供应商ID（历史 UUID 字符串）
     */
    private String supplierId;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用 / DRAFT 草稿 / DELETED 已删除 等）
     */
    private String status;
    /**
     * 是否官网公开
     */
    private Boolean isPublic;
    /**
     * 是否首页推荐
     */
    private Boolean isFeatured;
    /**
     * 首页推荐排序
     */
    private Integer featuredOrder;
    /**
     * 纱支明细（JSON 字符串数组：纱支/纺法/比例/备注/单价）
     */
    private String yarnSpecs;
    /**
     * 机型
     */
    private String machineType;
    /**
     * 产量
     *
     * OUTPUT 是 JSQLParser 保留字：租户拦截器（TenantLineInnerInterceptor）会用
     * JSQLParser 解析每条 SQL，未加引号的 output 会被当作关键字导致解析失败（ParseException，
     * 产品分页等所有经过租户拦截器的查询 500）。此处显式加双引号转义为列标识符；
     * PostgreSQL 双引号标识符区分大小写，"output" 与实际列名一致。
     */
    @TableField("\"output\"")
    private String output;
    /**
     * 备注
     */
    private String remark;
    /**
     * 原厂编号
     */
    private String supplierCode;
    /**
     * 临时编号
     */
    private String tempCode;
    /**
     * 工艺类型
     */
    private String craftType;
    /**
     * 颜色
     */
    private String color;
    /**
     * 所属胚布品种ID（历史 UUID 字符串，可空）
     */
    private String greigeTypeId;
    /**
     * 花型号
     */
    private String patternNo;
    /**
     * 标准米长
     */
    private String standardLength;
    /**
     * 算长系数（1KG = ?米/码）
     */
    private BigDecimal conversionRate;
    /**
     * 成本项明细（JSON 字符串数组：供应商/工序/单价/损耗率）
     */
    private String costItems;
    /**
     * 汇总成本价（由成本项计算，冗余存储）
     */
    private BigDecimal costPrice;
    /**
     * 成本状态（MISSING 缺失 / FILLED 已填写）
     */
    private String costStatus;
    /**
     * 成本最后更新时间
     */
    private LocalDateTime costUpdatedAt;
    /**
     * 成本最后更新人ID
     */
    private String costUpdatedBy;
    /**
     * AI 设计图URL
     */
    private String aiDesignUrl;
    /**
     * AI 设计分类（如 CASUAL BLAZER）
     */
    private String aiDesignCategory;
    /**
     * AI 设计描述（推理说明/风格关键词）
     */
    private String aiDesignDescription;
    /**
     * 手机拍布建档草稿ID（幂等键，手工建档为空）
     */
    private String captureDraftId;
    /**
     * 事业部归属（TRADE 贸易 / FACTORY 工厂）
     */
    private String businessOwnership;
    /**
     * 创建人ID
     */
    private String createdById;
    /**
     * 更新人ID
     */
    private String updatedById;

}
