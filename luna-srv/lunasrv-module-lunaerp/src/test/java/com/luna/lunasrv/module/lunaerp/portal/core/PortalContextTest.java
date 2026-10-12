package com.luna.lunasrv.module.lunaerp.portal.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PortalContext 上下文与线程隔离测试")
class PortalContextTest {

    @AfterEach
    void tearDown() {
        PortalContext.clear();
    }

    @Test
    @DisplayName("基本操作：set, get, 字段访问与 clear")
    void testBasicOperations() {
        assertNull(PortalContext.get());
        assertNull(PortalContext.getTenantId());
        assertNull(PortalContext.getCustomerId());
        assertNull(PortalContext.getCustomerName());

        PortalContext.set(100L, 200L, "测试客户有限公司");

        PortalCustomerContext ctx = PortalContext.get();
        assertNotNull(ctx);
        assertEquals(100L, ctx.getTenantId());
        assertEquals(200L, ctx.getCustomerId());
        assertEquals("测试客户有限公司", ctx.getCustomerName());

        assertEquals(100L, PortalContext.getTenantId());
        assertEquals(200L, PortalContext.getCustomerId());
        assertEquals("测试客户有限公司", PortalContext.getCustomerName());

        PortalContext.clear();
        assertNull(PortalContext.get());
        assertNull(PortalContext.getTenantId());
        assertNull(PortalContext.getCustomerId());
        assertNull(PortalContext.getCustomerName());
    }

    @Test
    @DisplayName("直接设置 PortalCustomerContext 对象")
    void testSetObject() {
        PortalCustomerContext custom = PortalCustomerContext.builder()
                .tenantId(1L)
                .customerId(2L)
                .customerName("客户A")
                .build();
        PortalContext.set(custom);

        assertSame(custom, PortalContext.get());
        assertEquals(1L, PortalContext.getTenantId());
        assertEquals(2L, PortalContext.getCustomerId());
        assertEquals("客户A", PortalContext.getCustomerName());
    }

    @Test
    @DisplayName("并发线程隔离：不同线程独立维护上下文，互不干扰且清理彻底")
    void testThreadIsolation() throws InterruptedException {
        PortalContext.set(1L, 10L, "主线程客户");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Long> subThreadCustomerId = new AtomicReference<>();
        AtomicReference<String> subThreadCustomerName = new AtomicReference<>();

        Thread subThread = new Thread(() -> {
            PortalContext.set(2L, 20L, "子线程客户");
            subThreadCustomerId.set(PortalContext.getCustomerId());
            subThreadCustomerName.set(PortalContext.getCustomerName());
            PortalContext.clear();
            latch.countDown();
        });

        subThread.start();
        latch.await();

        // 子线程拿到自己的上下文
        assertEquals(20L, subThreadCustomerId.get());
        assertEquals("子线程客户", subThreadCustomerName.get());

        // 主线程上下文未被篡改
        assertEquals(10L, PortalContext.getCustomerId());
        assertEquals("主线程客户", PortalContext.getCustomerName());

        PortalContext.clear();
        assertNull(PortalContext.get());
    }
}
