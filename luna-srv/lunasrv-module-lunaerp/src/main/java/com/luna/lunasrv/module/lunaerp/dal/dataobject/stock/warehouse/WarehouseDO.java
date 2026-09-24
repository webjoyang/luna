package com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存管理 - 仓库表 DO（ADR-0018）
 *
 * 表：lunaerp_stock_warehouse
 * 仓库编号 WH+年份+4位流水（WH20260001），租户内全量唯一含软删行，仿 ADR-0005；
 * 状态 ACTIVE/INACTIVE，不建字典。
 *
 * @author luna
 */
@TableName("lunaerp_stock_warehouse")
@KeySequence("lunaerp_stock_warehouse_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseDO extends TenantBaseDO {

    /**
     * 仓库ID
     */
    private Long id;
    /**
     * 仓库编号（WH+年份+4位流水，按年取号，租户内全量唯一含软删行）
     */
    private String warehouseNo;
    /**
     * 仓库名称
     */
    private String name;
    /**
     * 仓库地址
     */
    private String address;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

}
