package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务管理-卖方出口配置表 DO
 *
 * 表：lunaerp_business_export_seller_config
 * 一租户一行
 *
 * @author luna
 */
@TableName("lunaerp_business_export_seller_config")
@KeySequence("lunaerp_business_export_seller_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportSellerConfigDO extends TenantBaseDO {

    /**
     * 配置ID
     */
    private Long id;
    /**
     * 卖方英文名称（抬头/签章）
     */
    private String sellerName;
    /**
     * 卖方英文地址
     */
    private String sellerAddress;
    /**
     * 收款银行名称
     */
    private String bankName;
    /**
     * 收款银行账号 A/C
     */
    private String bankAccount;
    /**
     * 收款银行 SWIFT 码
     */
    private String bankSwift;
    /**
     * 收款银行地址
     */
    private String bankAddress;
    /**
     * 备注
     */
    private String remark;

}
