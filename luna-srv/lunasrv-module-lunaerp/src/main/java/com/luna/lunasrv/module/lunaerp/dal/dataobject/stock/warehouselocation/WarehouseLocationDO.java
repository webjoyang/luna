package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存管理 - 库位表 DO（ADR-0018）
 *
 * 表：lunaerp_stock_warehouse_location
 * 库位编码人工维护（如 A-01-02），同一仓库内唯一含软删行；
 * 状态 ACTIVE/INACTIVE，不建字典。
 *
 * @author luna
 */
@TableName("lunaerp_stock_warehouse_location")
@KeySequence("lunaerp_stock_warehouse_location_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseLocationDO extends TenantBaseDO {

    /**
     * 库位ID
     */
    private Long id;
    /**
     * 所属仓库ID
     */
    private Long warehouseId;
    /**
     * 库位编码（同一仓库内唯一含软删行，如 A-01-02）
     */
    private String code;
    /**
     * 库位名称
     */
    private String name;
    /**
     * 排序（缺省 0）
     */
    private Integer sort;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

}
