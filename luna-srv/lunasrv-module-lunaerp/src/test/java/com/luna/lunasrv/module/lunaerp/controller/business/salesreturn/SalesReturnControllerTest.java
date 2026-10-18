package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnCancelReqVO;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.SalesReturnService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link SalesReturnController} 单元测试（接口分发与权限守卫）
 */
@ExtendWith(MockitoExtension.class)
public class SalesReturnControllerTest {

    @Mock
    private SalesReturnService salesReturnService;

    @InjectMocks
    private SalesReturnController controller;

    @Test
    @DisplayName("submit 接口：正确委托 service 并返回成功")
    void testSubmitSalesReturn() {
        CommonResult<Boolean> result = controller.submitSalesReturn(10L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(salesReturnService).submitSalesReturn(10L);
    }

    @Test
    @DisplayName("withdraw 接口：正确委托 service 并返回成功")
    void testWithdrawSalesReturn() {
        CommonResult<Boolean> result = controller.withdrawSalesReturn(10L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(salesReturnService).withdrawSalesReturn(10L);
    }

    @Test
    @DisplayName("cancel 接口：正确传递 id 与 request body cancelReason 入参")
    void testCancelSalesReturn() {
        SalesReturnCancelReqVO reqVO = SalesReturnCancelReqVO.builder()
                .cancelReason("客户取消")
                .build();
        CommonResult<Boolean> result = controller.cancelSalesReturn(10L, reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(salesReturnService).cancelSalesReturn(10L, "客户取消");
    }

    @Test
    @DisplayName("权限注解校验：submit, withdraw, cancel 正确配置 @PreAuthorize")
    void testPermissions() throws NoSuchMethodException {
        Method submitMethod = SalesReturnController.class.getMethod("submitSalesReturn", Long.class);
        PreAuthorize submitAuth = submitMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(submitAuth);
        assertTrue(submitAuth.value().contains("lunaerp:business:sales-return:submit"));

        Method withdrawMethod = SalesReturnController.class.getMethod("withdrawSalesReturn", Long.class);
        PreAuthorize withdrawAuth = withdrawMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(withdrawAuth);
        assertTrue(withdrawAuth.value().contains("lunaerp:business:sales-return:submit"));

        Method cancelMethod = SalesReturnController.class.getMethod("cancelSalesReturn", Long.class, SalesReturnCancelReqVO.class);
        PreAuthorize cancelAuth = cancelMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(cancelAuth);
        assertTrue(cancelAuth.value().contains("lunaerp:business:sales-return:cancel"));
    }

}
