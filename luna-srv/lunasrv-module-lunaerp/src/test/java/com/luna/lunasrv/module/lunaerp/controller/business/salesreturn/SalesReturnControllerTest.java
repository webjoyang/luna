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

        Method receiveMethod = SalesReturnController.class.getMethod("receiveSalesReturn", com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnReceiveReqVO.class);
        PreAuthorize receiveAuth = receiveMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(receiveAuth);
        assertTrue(receiveAuth.value().contains("lunaerp:business:sales-return:receive"));
    }

    @Test
    @DisplayName("receive 接口：正确委托 service 并返回成功")
    void testReceiveSalesReturn() {
        com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnReceiveReqVO reqVO =
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnReceiveReqVO.builder()
                        .id(10L)
                        .rolls(java.util.Collections.emptyList())
                        .build();
        CommonResult<Boolean> result = controller.receiveSalesReturn(reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(salesReturnService).receiveSalesReturn(reqVO);
    }

    @Test
    @DisplayName("post 接口：正确委托 service 并返回成功")
    void testPostSalesReturn() {
        CommonResult<Boolean> result = controller.postSalesReturn(10L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(salesReturnService).postSalesReturn(10L);
    }

    @Test
    @DisplayName("post 权限注解校验：必须配置 @PreAuthorize 包含 lunaerp:business:sales-return:post")
    void testPostPermission() throws NoSuchMethodException {
        Method postMethod = SalesReturnController.class.getMethod("postSalesReturn", Long.class);
        PreAuthorize postAuth = postMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(postAuth);
        assertTrue(postAuth.value().contains("lunaerp:business:sales-return:post"));
    }

    @Test
    @DisplayName("print 接口：正确委托 service 并返回成功")
    void testGetSalesReturnPrintData() {
        com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnPrintRespVO mockPrintVO =
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnPrintRespVO.builder()
                        .id(10L)
                        .returnNo("RT-20261018-001")
                        .build();
        when(salesReturnService.getSalesReturnPrintData(10L)).thenReturn(mockPrintVO);

        CommonResult<com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnPrintRespVO> result =
                controller.getSalesReturnPrintData(10L);
        assertTrue(result.isSuccess());
        assertEquals("RT-20261018-001", result.getData().getReturnNo());
        verify(salesReturnService).getSalesReturnPrintData(10L);
    }

    @Test
    @DisplayName("print 权限注解校验：必须配置 @PreAuthorize 包含 lunaerp:business:sales-return:print")
    void testPrintPermission() throws NoSuchMethodException {
        Method printMethod = SalesReturnController.class.getMethod("getSalesReturnPrintData", Long.class);
        PreAuthorize printAuth = printMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(printAuth);
        assertTrue(printAuth.value().contains("lunaerp:business:sales-return:print"));
    }

    @Test
    @DisplayName("所有端点权限注解全覆盖：增删改查、审批、到货、过账、作废、打印")
    void testAllEndpointsPermissionsComplete() throws NoSuchMethodException {
        // 1. candidates
        Method candidatesMethod = SalesReturnController.class.getMethod("getShipmentCandidates", Long.class, String.class);
        PreAuthorize candidatesAuth = candidatesMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(candidatesAuth);
        assertTrue(candidatesAuth.value().contains("lunaerp:business:sales-return:query"));
        assertTrue(candidatesAuth.value().contains("lunaerp:business:sales-return:create"));

        // 2. detail
        Method detailMethod = SalesReturnController.class.getMethod("getShipmentDetail", Long.class);
        PreAuthorize detailAuth = detailMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(detailAuth);
        assertTrue(detailAuth.value().contains("lunaerp:business:sales-return:query"));
        assertTrue(detailAuth.value().contains("lunaerp:business:sales-return:create"));

        // 3. create
        Method createMethod = SalesReturnController.class.getMethod("createSalesReturn",
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnCreateReqVO.class);
        PreAuthorize createAuth = createMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(createAuth);
        assertTrue(createAuth.value().contains("lunaerp:business:sales-return:create"));

        // 4. update
        Method updateMethod = SalesReturnController.class.getMethod("updateSalesReturn",
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnUpdateReqVO.class);
        PreAuthorize updateAuth = updateMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(updateAuth);
        assertTrue(updateAuth.value().contains("lunaerp:business:sales-return:update"));

        // 5. delete
        Method deleteMethod = SalesReturnController.class.getMethod("deleteSalesReturn", Long.class);
        PreAuthorize deleteAuth = deleteMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(deleteAuth);
        assertTrue(deleteAuth.value().contains("lunaerp:business:sales-return:delete"));

        // 6. get
        Method getMethod = SalesReturnController.class.getMethod("getSalesReturn", Long.class);
        PreAuthorize getAuth = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(getAuth);
        assertTrue(getAuth.value().contains("lunaerp:business:sales-return:query"));

        // 7. page
        Method pageMethod = SalesReturnController.class.getMethod("getSalesReturnPage",
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnPageReqVO.class);
        PreAuthorize pageAuth = pageMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(pageAuth);
        assertTrue(pageAuth.value().contains("lunaerp:business:sales-return:query"));
    }

    @Test
    @DisplayName("纵向越权防御：断言 receive 与 post 等关键写操作必须受到 PreAuthorize 保护，杜绝无权调用")
    void testVerticalPrivilegeEscalationGuards() throws NoSuchMethodException {
        // 无 :receive 权限的用户不能调用 receive 接口
        Method receiveMethod = SalesReturnController.class.getMethod("receiveSalesReturn",
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnReceiveReqVO.class);
        PreAuthorize receiveAuth = receiveMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(receiveAuth, "receiveSalesReturn 必须标注 @PreAuthorize 防纵向越权");
        assertEquals("@ss.hasPermission('lunaerp:business:sales-return:receive')", receiveAuth.value());

        // 无 :post 权限的用户不能调用 post 接口
        Method postMethod = SalesReturnController.class.getMethod("postSalesReturn", Long.class);
        PreAuthorize postAuth = postMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(postAuth, "postSalesReturn 必须标注 @PreAuthorize 防纵向越权");
        assertEquals("@ss.hasPermission('lunaerp:business:sales-return:post')", postAuth.value());
    }

}

