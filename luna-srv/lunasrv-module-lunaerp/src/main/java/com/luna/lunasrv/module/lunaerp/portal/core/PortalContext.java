package com.luna.lunasrv.module.lunaerp.portal.core;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 客户门户上下文 Holder
 *
 * <p>基于 TransmittableThreadLocal 存放当前请求的客户门户身份，包括租户编号、客户编号、客户名称。</p>
 *
 * @author luna
 */
public class PortalContext {

    private static final ThreadLocal<PortalCustomerContext> CONTEXT = new TransmittableThreadLocal<>();

    /**
     * 获取当前门户客户上下文
     *
     * @return 门户客户上下文，若未设置则返回 null
     */
    public static PortalCustomerContext get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前门户租户编号
     *
     * @return 租户编号
     */
    public static Long getTenantId() {
        PortalCustomerContext ctx = get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    /**
     * 获取当前门户客户编号
     *
     * @return 客户编号
     */
    public static Long getCustomerId() {
        PortalCustomerContext ctx = get();
        return ctx != null ? ctx.getCustomerId() : null;
    }

    /**
     * 获取当前门户客户名称
     *
     * @return 客户名称
     */
    public static String getCustomerName() {
        PortalCustomerContext ctx = get();
        return ctx != null ? ctx.getCustomerName() : null;
    }

    /**
     * 设置当前门户客户上下文对象
     *
     * @param context 门户客户上下文
     */
    public static void set(PortalCustomerContext context) {
        CONTEXT.set(context);
    }

    /**
     * 便捷设置门户客户上下文
     *
     * @param tenantId 租户编号
     * @param customerId 客户编号
     * @param customerName 客户名称
     */
    public static void set(Long tenantId, Long customerId, String customerName) {
        CONTEXT.set(PortalCustomerContext.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .customerName(customerName)
                .build());
    }

    /**
     * 清理当前线程的门户上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

}
