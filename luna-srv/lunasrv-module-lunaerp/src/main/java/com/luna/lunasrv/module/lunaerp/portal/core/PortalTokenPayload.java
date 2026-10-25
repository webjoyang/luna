package com.luna.lunasrv.module.lunaerp.portal.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户门户令牌 Redis 主键载荷
 *
 * 对应键：luna_portal:token:{token}
 * 内容：JSON {tenantId, customerId, customerName, generateTime}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalTokenPayload {

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 客户编号
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 生成时间
     */
    private LocalDateTime generateTime;

}
