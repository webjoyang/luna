package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产管理 - 织机档案 DO
 *
 * 表：lunaerp_production_loom
 * 机台号 loomNo 租户内唯一含软删行，服务端取号/可手填；
 * 状态 ACTIVE/INACTIVE，不建字典，缺省 ACTIVE。
 *
 * @author luna
 */
@TableName("lunaerp_production_loom")
@KeySequence("lunaerp_production_loom_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoomDO extends TenantBaseDO {

    /**
     * 织机ID
     */
    private Long id;

    /**
     * 机台号（租户内唯一含软删行，服务端取号/可手填）
     */
    private String loomNo;

    /**
     * 型号
     */
    private String model;

    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
