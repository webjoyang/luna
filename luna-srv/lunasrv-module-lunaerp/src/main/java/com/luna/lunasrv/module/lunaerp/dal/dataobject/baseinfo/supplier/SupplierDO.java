package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基本信息-供应商表 DO
 *
 * 表：lunaerp_baseinfo_supplier
 *
 * 字段范围：A 组基础档案（supplierNo/companyName/shortName/phone/address/
 * supplierType/machineType/remark/status）。
 * 不引入 code、businessOwnership，亦无业务唯一约束（公司名允许重名）；
 * supplier_no 租户内全量唯一（含软删行，见 ADR-0005）。
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_supplier")
@KeySequence("lunaerp_baseinfo_supplier_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierDO extends TenantBaseDO {

    /**
     * 供应商ID
     */
    private Long id;
    /**
     * 供应商编号（SUP+年份+4位序号，租户内全量唯一含软删行）
     */
    private String supplierNo;
    /**
     * 公司/档口名称（必填）
     */
    private String companyName;
    /**
     * 简称（为空时后端用 companyName 回填）
     */
    private String shortName;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 公司地址
     */
    private String address;
    /**
     * 供应商类型（字典 lunaerp_supplier_type，可空）
     */
    private String supplierType;
    /**
     * 主营机型/设备说明（自由文本）
     */
    private String machineType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE
     */
    private String status;

}
