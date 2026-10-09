package com.luna.lunasrv.module.lunaerp.common.desensitize;

/**
 * 敏感字段脱敏类型枚举
 *
 * @author luna
 */
public enum SensitiveType {

    /**
     * 手机号（前3后4，中间*）
     */
    MOBILE,

    /**
     * 中文名（前1后0，其余*）
     */
    CHINESE_NAME,

    /**
     * 银行卡号（前6后2，中间*）
     */
    BANK_CARD,

    /**
     * 固定电话（前4后2，中间*）
     */
    FIXED_PHONE,

    /**
     * 邮箱（保留首字符与域名，其余*）
     */
    EMAIL,

    /**
     * 金额（无权限序列化为 "****"，有权限返回原值）
     */
    AMOUNT

}
