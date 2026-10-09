package com.luna.lunasrv.module.lunaerp.common.recycle;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecycleBinHandlerRegistryTest {

    @Test
    public void testGetHandler_success() {
        RecycleBinHandler mockHandler = mock(RecycleBinHandler.class);
        when(mockHandler.getBizType()).thenReturn("CUSTOMER");

        RecycleBinHandlerRegistry registry = new RecycleBinHandlerRegistry(List.of(mockHandler));

        RecycleBinHandler result = registry.getHandler("CUSTOMER");
        assertNotNull(result);
        assertEquals("CUSTOMER", result.getBizType());
    }

    @Test
    public void testGetHandler_notSupported_throwsException() {
        RecycleBinHandler mockHandler = mock(RecycleBinHandler.class);
        when(mockHandler.getBizType()).thenReturn("CUSTOMER");

        RecycleBinHandlerRegistry registry = new RecycleBinHandlerRegistry(List.of(mockHandler));

        ServiceException ex = assertThrows(ServiceException.class, () -> registry.getHandler("UNKNOWN_TYPE"));
        assertEquals(RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED.getCode(), ex.getCode());
    }

    @Test
    public void testGetAllHandlers() {
        RecycleBinHandler handler1 = mock(RecycleBinHandler.class);
        when(handler1.getBizType()).thenReturn("CUSTOMER");
        RecycleBinHandler handler2 = mock(RecycleBinHandler.class);
        when(handler2.getBizType()).thenReturn("SUPPLIER");

        RecycleBinHandlerRegistry registry = new RecycleBinHandlerRegistry(List.of(handler1, handler2));

        assertEquals(2, registry.getAllHandlers().size());
    }

    @Test
    public void testNullHandlersList() {
        RecycleBinHandlerRegistry registry = new RecycleBinHandlerRegistry(null);
        assertTrue(registry.getAllHandlers().isEmpty());
    }
}
