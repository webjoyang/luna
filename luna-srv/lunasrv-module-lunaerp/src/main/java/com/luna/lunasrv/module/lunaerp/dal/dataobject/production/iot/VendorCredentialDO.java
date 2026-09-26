package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 生产管理 - IoT 厂商凭证 DO
 *
 * 表：lunaerp_production_vendor_credential
 * vendorAccount 租户内唯一
 * vendorPasswordEnc 加密存储（禁止明文回显）
 * scope 设备范围说明
 * status ACTIVE/INACTIVE
 * lastSyncAt 最后同步时间
 *
 * @author luna
 */
@TableName("lunaerp_production_vendor_credential")
@KeySequence("lunaerp_production_vendor_credential_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorCredentialDO extends TenantBaseDO {

    /**
     * 凭证ID
     */
    private Long id;

    /**
     * 厂商账号（租户内唯一）
     */
    private String vendorAccount;

    /**
     * 厂商密码密文（AES加密存储，禁止明文回显）
     */
    private String vendorPasswordEnc;

    /**
     * 设备范围说明
     */
    private String scope;

    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE
     */
    private String status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncAt;

}
