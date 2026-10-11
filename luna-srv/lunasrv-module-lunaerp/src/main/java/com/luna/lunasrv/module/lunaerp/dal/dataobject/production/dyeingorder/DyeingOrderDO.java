package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 生产管理-染色单主表 DO
 *
 * 表：lunaerp_production_dyeing_order
 *
 * 承染方引用供应商档案并保存公司名快照；销售合同/客户为逻辑关联，不建数据库外键。
 * 无业务编号（ADR-0009），对外标识靠 id + 合同号快照。
 * 状态为四态 DRAFT/ACTIVE/COMPLETED/CANCELLED，缺省 DRAFT（ADR-0008）。
 * 颜色明细子表（DyeingColorDO）随单聚合保存（ADR-0003）。
 *
 * @author luna
 */
@TableName("lunaerp_production_dyeing_order")
@KeySequence("lunaerp_production_dyeing_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DyeingOrderDO extends TenantBaseDO {

    /**
     * 染色单ID
     */
    private Long id;
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
     * 客户ID（lunaerp_baseinfo_customer.id，从合同带出可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long customerId;
    /**
     * 客户名快照（从合同带出）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String clientName;
    /**
     * 承染方供应商ID（lunaerp_baseinfo_supplier.id，必填）
     */
    private Long dyeingFactoryId;
    /**
     * 承染方公司名快照（必填）
     */
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
     * 产品编码
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
     * 幅宽
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String width;
    /**
     * 克重
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gsm;
    /**
     * 缩水率
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String shrinkage;
    /**
     * 损耗率
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String loss;
    /**
     * pH 值
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String ph;
    /**
     * 水洗牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessWash;
    /**
     * 皂洗牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessSoap;
    /**
     * 干摩擦牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessDryRub;
    /**
     * 湿摩擦牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessWetRub;
    /**
     * 日晒牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessLight;
    /**
     * 汗渍牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessPerspiration;
    /**
     * 干洗牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessDryClean;
    /**
     * 六千沾色牢度
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fastnessSixFiber;
    /**
     * 其他要求
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String otherReq;
    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /**
     * 染色单状态（字典 lunaerp_dyeing_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED），创建缺省 DRAFT
     */
    private String status;
    /**
     * 打色指示单ID（lunaerp_production_color_instruction.id，逻辑关联可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long colorInstructionId;
    /**
     * 打色单号快照（可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorInstructionNo;

}
