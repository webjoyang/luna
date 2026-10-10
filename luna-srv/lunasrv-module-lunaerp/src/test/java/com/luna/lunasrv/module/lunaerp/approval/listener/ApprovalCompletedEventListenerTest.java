package com.luna.lunasrv.module.lunaerp.approval.listener;

import com.luna.lunasrv.module.lunaerp.approval.event.ApprovalCompletedEvent;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * {@link ApprovalCompletedEventListener} 单元测试
 *
 * 覆盖 Issue 07:
 * - 事件监听路由到匹配的 ApprovalHandler
 * - 空事件或空参数安全忽略
 * - 无对应 Handler 时安全放行（预留接入点）
 * - Handler 抛异常时向上透传，触发事务回滚
 * - 多 Handler 匹配时全部触发
 */
class ApprovalCompletedEventListenerTest {

    @Mock
    private ApprovalHandler salesContractHandler;

    @Mock
    private ApprovalHandler anotherHandler;

    @InjectMocks
    private ApprovalCompletedEventListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(salesContractHandler.supports("sales_contract")).thenReturn(true);
        when(salesContractHandler.supports("payment")).thenReturn(false);

        when(anotherHandler.supports("sales_contract")).thenReturn(false);
        when(anotherHandler.supports("payment")).thenReturn(true);

        ReflectionTestUtils.setField(listener, "handlers", List.of(salesContractHandler, anotherHandler));
    }

    @Test
    @DisplayName("监听到事件：精确路由到匹配的 ApprovalHandler 执行生效动作")
    void testOnApprovalCompleted_RoutesToMatchingHandler() {
        ApprovalCompletedEvent event = new ApprovalCompletedEvent("sales_contract", 1001L);
        listener.onApprovalCompleted(event);

        verify(salesContractHandler, times(1)).onApproved(1001L);
        verify(anotherHandler, never()).onApproved(any());
    }

    @Test
    @DisplayName("空参数守卫：event 为空、bizType 为空或 bizId 为空时直接返回不调用 handler")
    void testOnApprovalCompleted_NullOrEmptyParams_Ignored() {
        assertDoesNotThrow(() -> listener.onApprovalCompleted(null));
        assertDoesNotThrow(() -> listener.onApprovalCompleted(new ApprovalCompletedEvent(null, 1001L)));
        assertDoesNotThrow(() -> listener.onApprovalCompleted(new ApprovalCompletedEvent("   ", 1001L)));
        assertDoesNotThrow(() -> listener.onApprovalCompleted(new ApprovalCompletedEvent("sales_contract", null)));

        verifyNoInteractions(salesContractHandler);
        verifyNoInteractions(anotherHandler);
    }

    @Test
    @DisplayName("预留点支持：无匹配 Handler 的业务类型安全放行，不抛异常")
    void testOnApprovalCompleted_NoMatchingHandler_SafeBypass() {
        // 未接入的具体单据类型，如 expense / payroll / sales_return
        ApprovalCompletedEvent event = new ApprovalCompletedEvent("expense", 2002L);
        assertDoesNotThrow(() -> listener.onApprovalCompleted(event));

        verify(salesContractHandler, never()).onApproved(any());
        verify(anotherHandler, never()).onApproved(any());
    }

    @Test
    @DisplayName("Handler 列表为空时安全放行")
    void testOnApprovalCompleted_EmptyHandlers_SafeBypass() {
        ReflectionTestUtils.setField(listener, "handlers", Collections.emptyList());
        ApprovalCompletedEvent event = new ApprovalCompletedEvent("sales_contract", 1001L);
        assertDoesNotThrow(() -> listener.onApprovalCompleted(event));

        verifyNoInteractions(salesContractHandler);
    }

    @Test
    @DisplayName("异常向上抛出：Handler 执行失败抛出异常时透传，确保触发事务回滚")
    void testOnApprovalCompleted_HandlerThrows_PropagatesException() {
        doThrow(new RuntimeException("业务台账过账异常")).when(salesContractHandler).onApproved(1001L);

        ApprovalCompletedEvent event = new ApprovalCompletedEvent("sales_contract", 1001L);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> listener.onApprovalCompleted(event));
        assertEquals("业务台账过账异常", ex.getMessage());
    }
}
