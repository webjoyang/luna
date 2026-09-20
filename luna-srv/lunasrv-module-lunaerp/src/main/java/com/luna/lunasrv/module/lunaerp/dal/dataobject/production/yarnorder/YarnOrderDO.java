package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理-纱线采购单主表 DO
 *
 * 表：lunaerp_production_yarn_order
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_order")
@KeySequence("lunaerp_production_yarn_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnOrderDO extends TenantBaseDO {

    /**
     * 采购单ID
     */
    @TableId
    private Long id;

    /**
     * 采购单号（YP+年月日+3位日流水，按天取号，租户内全量唯一含已删除行，见 ADR-0006 模式）
     */
    private String orderNo;

    /**
     * 关联胚布指示单 ID 数组（JSON 字符串，如 "[123,456]"）
     */
    private String sourceInstructions;

    /**
     * 单据日期
     */
    private LocalDate orderDate;

    /**
     * 交货日期
     */
    private LocalDate deliveryDate;

    /**
     * 损耗率百分比数字（5 = 5%），缺省 0
     */
    private BigDecimal lossRate;

    /**
     * 状态（字典 lunaerp_yarn_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
