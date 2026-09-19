package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.suppliercontact;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基本信息-供应商联系人表 DO
 *
 * 表：lunaerp_baseinfo_supplier_contact
 *
 * 供应商的独立子资源：supplierId 直接 Long（无历史包袱，不学 product-image 的 String productId）。
 * isPrimary 不建唯一约束，业务允许同供应商多条主联系人，由前端管理。
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_supplier_contact")
@KeySequence("lunaerp_baseinfo_supplier_contact_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierContactDO extends TenantBaseDO {

    /**
     * 联系人ID
     */
    private Long id;
    /**
     * 供应商ID（lunaerp_baseinfo_supplier.id）
     */
    private Long supplierId;
    /**
     * 联系人姓名（必填）
     */
    private String name;
    /**
     * 职位
     */
    private String position;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 备注
     */
    private String remark;
    /**
     * 是否主联系人（1 是 / 0 否）
     */
    private Integer isPrimary;

}
