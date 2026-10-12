package com.luna.lunasrv.module.lunaerp.portal.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 客户门户上下文对象
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalCustomerContext implements Serializable {

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
     * 客户名称（公司名称）
     */
    private String customerName;

}
