package com.luna.lunasrv.module.lunaerp.portal.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PortalContextTest {

    @AfterEach
    void tearDown() {
        PortalContext.clear();
    }

    @Test
    @DisplayName("上下文设置、读取与清理正常")
    void testSetGetClear() {
        assertNull(PortalContext.get());
        assertNull(PortalContext.getTenantId());
        assertNull(PortalContext.getCustomerId());
        assertNull(PortalContext.getCustomerName());

        PortalContext.set(1L, 1001L, "测试客户");

        assertEquals(1L, PortalContext.getTenantId());
        assertEquals(1001L, PortalContext.getCustomerId());
        assertEquals("测试客户", PortalContext.getCustomerName());
        assertNotNull(PortalContext.get());

        PortalContext.clear();

        assertNull(PortalContext.get());
        assertNull(PortalContext.getTenantId());
        assertNull(PortalContext.getCustomerId());
        assertNull(PortalContext.getCustomerName());
    }

    @Test
    @DisplayName("通过 PortalTokenPayload 设置上下文")
    void testSetFromPayload() {
        PortalTokenPayload payload = PortalTokenPayload.builder()
                .tenantId(2L)
                .customerId(2002L)
                .customerName("载荷客户")
                .build();

        PortalContext.set(payload);

        assertEquals(2L, PortalContext.getTenantId());
        assertEquals(2002L, PortalContext.getCustomerId());
        assertEquals("载荷客户", PortalContext.getCustomerName());
    }
}
