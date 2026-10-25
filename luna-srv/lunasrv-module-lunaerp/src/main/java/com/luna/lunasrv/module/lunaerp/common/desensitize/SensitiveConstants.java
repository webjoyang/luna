package com.luna.lunasrv.module.lunaerp.common.desensitize;

/**
 * 敏感字段权限常量
 *
 * @author luna
 */
public interface SensitiveConstants {

    /**
     * 金额类敏感字段查看权限
     */
    String PERMISSION_AMOUNT = "lunaerp:sensitive:amount";

    /**
     * 联系人/联系方式类敏感字段查看权限
     */
    String PERMISSION_CONTACT = "lunaerp:sensitive:contact";

}
