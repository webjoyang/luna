package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产管理-胚布单颜色投胚分配子表 DO
 *
 * 表：lunaerp_production_greige_order_color
 *
 * 颜色行是胚布单聚合的一部分（ADR-0003）：无独立 Controller/Service/权限码，
 * 写操作随胚布单保存整体替换（事务内按 order_id 删旧行、插新行并重排 line_no），
 * 零颜色合法。demand_qty（投胚需求）由服务端权威重算：成品量 ÷ (1 − 损耗率)，
 * null 成品量按 0；loss_rate 行损耗为空时取主单预设损耗、再缺省 0。
 * 计划投胚量（主单 planned_qty）与颜色投胚需求合计刻意不强制相等。
 *
 * @author luna
 */
@TableName("lunaerp_production_greige_order_color")
@KeySequence("lunaerp_production_greige_order_color_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeOrderColorDO extends TenantBaseDO {

    /**
     * 颜色行ID
     */
    private Long id;
    /**
     * 胚布单ID（lunaerp_production_greige_order.id，必填）
     */
    private Long orderId;
    /**
     * 行号（服务端按提交顺序重排，从 1 开始）
     */
    private Integer lineNo;
    /**
     * 来源合同行 ID（lunaerp_business_sales_contract_item.id，逻辑关联不建外键）
     */
    private Long contractLineId;
    /**
     * 来源合同行号快照
     */
    private Integer contractLineNo;
    /**
     * 颜色
     */
    private String color;
    /**
     * 成品下单量 KG（null 按 0）
     */
    private BigDecimal finishedQty;
    /**
     * 损耗率（小数，0.05 = 5%；保存时空值取主单预设损耗、再缺省 0）
     */
    private BigDecimal lossRate;
    /**
     * 投胚需求 KG（服务端权威重算：成品量÷(1−损耗率)，前端传值被覆盖）
     */
    private BigDecimal demandQty;
    /**
     * 行备注
     */
    private String remark;

}
