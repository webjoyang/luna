package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产管理-检验单录检明细 DO
 *
 * 表：lunaerp_production_inspection_order_item
 *
 * 录检明细作为独立子资源处理（ADR-0014）。
 * 单内 (vatNo, rollNo) 唯一性由 service 层校验，不建数据库唯一约束。
 * 本期不做 defects 疵点字段。
 *
 * @author luna
 */
@TableName(value = "lunaerp_production_inspection_order_item", autoResultMap = true)
@KeySequence("lunaerp_production_inspection_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionOrderItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;

    /**
     * 检验单ID（lunaerp_production_inspection_order.id）
     */
    private Long inspectionOrderId;

    /**
     * 卷号（ROLL 模式必填，PIECE 模式可选且允许重复）
     */
    private String rollNo;

    /**
     * 匹号（PIECE 模式必填，ROLL 模式禁填）
     */
    private String pieceNo;

    /**
     * 关联织造布票ID（lunaerp_production_weaving_ticket.id，可选，血缘关联）
     */
    private Long weavingTicketId;

    /**
     * 缸号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String vatNo;

    /**
     * 实称重量 KG
     */
    private BigDecimal measuredWeight;

    /**
     * 米数
     */
    private BigDecimal lengthM;

    /**
     * 机台号
     */
    private String machineNo;

    /**
     * 等级（字典 lunaerp_inspection_grade：A/B/C/DEFECTIVE）
     */
    private String grade;

    /**
     * 颜色名称
     */
    private String colorName;

    /**
     * 产品色号
     */
    private String colorNo;

    /**
     * 款号
     */
    private String styleNo;

    /**
     * 产品编码快照
     */
    private String productCode;

    /**
     * 产品名称快照
     */
    private String productName;

    /**
     * 匹数（缺省 1）
     */
    private Integer pieceCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> photos;

}
