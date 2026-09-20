package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理-胚布单主表 DO
 *
 * 表：lunaerp_production_greige_order
 *
 * 坯布厂/染厂引用供应商档案并保存公司名快照，供应商改名不回写；
 * 引用不做供应商类型强过滤（ADR-0007）。胚布单号按天取号、全量唯一、
 * 软删不释放（ADR-0006）。颜色投胚分配子表（GreigeOrderColorDO）随单聚合保存（ADR-0003）。
 *
 * @author luna
 */
@TableName("lunaerp_production_greige_order")
@KeySequence("lunaerp_production_greige_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeOrderDO extends TenantBaseDO {

    /**
     * 胚布单ID
     */
    private Long id;
    /**
     * 胚布单号（PB+月日+3位顺序号，按天取号，可手填，租户内全量唯一含已删除行，见 ADR-0006）
     */
    private String orderNo;
    /**
     * 销售合同ID（lunaerp_business_sales_contract.id，逻辑关联可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salesContractId;
    /**
     * 合同号快照（选中合同时保存）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contractNo;
    /**
     * 承织坯布厂供应商ID（lunaerp_baseinfo_supplier.id，必填，见 ADR-0007）
     */
    private Long greigeFactoryId;
    /**
     * 坯布厂公司名快照（选中供应商时保存，必填）
     */
    private String greigeFactoryName;
    /**
     * 指定染厂供应商ID（lunaerp_baseinfo_supplier.id，可空，见 ADR-0007）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dyeingFactoryId;
    /**
     * 染厂公司名快照（选中供应商时保存）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dyeingFactoryName;
    /**
     * 单据日期（必填，创建缺省当天）
     */
    private LocalDate orderDate;
    /**
     * 交货日期（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate deliveryDate;
    /**
     * 产品档案ID（强关联，用于纱线测算时查配比BOM，见 ADR-0011）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long productId;
    /**
     * 产品编号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productCode;
    /**
     * 产品名称
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productName;
    /**
     * 成分
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String composition;
    /**
     * 幅宽（保留文本，兼容"160cm 有效"等写法）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String width;
    /**
     * 克重（保留文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gsm;
    /**
     * 缩水率（描述性文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String shrinkage;
    /**
     * 匹重 KG（缺省 25）
     */
    private BigDecimal pieceWeight;
    /**
     * 匹数（前端按 ceil(投胚量÷匹重) 自动算，可手改；服务端不重算）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer pieceCount;
    /**
     * 计划投胚量 KG（工厂经验口径，与颜色折算投胚需求之和不强制相等）
     */
    private BigDecimal plannedQty;
    /**
     * 下单成品数量 KG
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal finishedQty;
    /**
     * 预设损耗（小数，0.05 = 5%，缺省 0）
     */
    private BigDecimal suggestedLoss;
    /**
     * 加工费
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal processingFee;
    /**
     * 所属胚布品种（胚布品种档案 id 为字符串，手选不自动识别）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String greigeTypeId;
    /**
     * 生产工艺备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productionRemark;
    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /**
     * 胚布单状态（字典 lunaerp_greige_order_status：ACTIVE 进行中 / COMPLETED 已完成 / CANCELLED 已取消），创建缺省 ACTIVE
     */
    private String status;

}
