package com.luna.lunasrv.module.lunaerp.service.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户门户令牌 DTO（存储于 Redis 主键 JSON 中）
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalTokenDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
