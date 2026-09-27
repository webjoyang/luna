package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 业务管理 - 发货卷清单表 DO（ADR-0030）
 *
 * 表：lunaerp_business_shipment_roll
 *
 * @author luna
 */
@TableName("lunaerp_business_shipment_roll")
@KeySequence("lunaerp_business_shipment_roll_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ShipmentRollDO extends TenantBaseDO {

    /**
     * 卷记录ID
     */
    private Long id;
    /**
     * 所属发货单ID（lunaerp_business_shipment.id，冗余便于级联删除与跨行平铺）
     */
    private Long shipmentId;
    /**
     * 所属发货明细ID（lunaerp_business_shipment_item.id，必填）
     */
    private Long shipmentItemId;
    /**
     * 卷号
     */
    private String rollNo;
    /**
     * 实称重量 KG
     */
    private BigDecimal weight;
    /**
     * 米数（可空）
     */
    private BigDecimal lengthM;
    /**
     * 卷备注
     */
    private String remark;

}
