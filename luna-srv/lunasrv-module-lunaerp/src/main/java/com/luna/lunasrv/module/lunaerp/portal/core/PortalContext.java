package com.luna.lunasrv.module.lunaerp.portal.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户门户当前请求上下文（ThreadLocal）
 */
public class PortalContext {

    private static final ThreadLocal<PortalContextPayload> CONTEXT = new ThreadLocal<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortalContextPayload {
        private Long tenantId;
        private Long customerId;
        private String customerName;
    }

    public static void set(Long tenantId, Long customerId, String customerName) {
        CONTEXT.set(new PortalContextPayload(tenantId, customerId, customerName));
    }

    public static void set(PortalTokenPayload payload) {
        if (payload != null) {
            set(payload.getTenantId(), payload.getCustomerId(), payload.getCustomerName());
        }
    }

    public static Long getTenantId() {
        PortalContextPayload payload = CONTEXT.get();
        return payload != null ? payload.getTenantId() : null;
    }

    public static Long getCustomerId() {
        PortalContextPayload payload = CONTEXT.get();
        return payload != null ? payload.getCustomerId() : null;
    }

    public static String getCustomerName() {
        PortalContextPayload payload = CONTEXT.get();
        return payload != null ? payload.getCustomerName() : null;
    }

    public static PortalContextPayload get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
