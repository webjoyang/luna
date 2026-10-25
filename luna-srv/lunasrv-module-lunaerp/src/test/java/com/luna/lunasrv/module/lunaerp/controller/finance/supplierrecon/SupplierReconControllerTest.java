package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon.SupplierReconService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link SupplierReconController} 单元测试（契约与权限校验）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class SupplierReconControllerTest {

    @Mock
    private SupplierReconService supplierReconService;

    @InjectMocks
    private SupplierReconController controller;

    @Test
    @DisplayName("preview 接口：委托 Service 并返回候选列表")
    void testPreview() {
        SupplierReconPreviewReqVO reqVO = new SupplierReconPreviewReqVO();
        List<SupplierReconSourceItemRespVO> list = List.of(new SupplierReconSourceItemRespVO());
        when(supplierReconService.preview(reqVO)).thenReturn(list);

        CommonResult<List<SupplierReconSourceItemRespVO>> result = controller.preview(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(list, result.getData());
        verify(supplierReconService).preview(reqVO);
    }

    @Test
    @DisplayName("createSupplierRecon 接口：委托 Service 并返回主键ID")
    void testCreateSupplierRecon() {
        SupplierReconCreateReqVO reqVO = new SupplierReconCreateReqVO();
        when(supplierReconService.createSupplierRecon(reqVO)).thenReturn(100L);

        CommonResult<Long> result = controller.createSupplierRecon(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(100L, result.getData());
        verify(supplierReconService).createSupplierRecon(reqVO);
    }

    @Test
    @DisplayName("updateSupplierRecon 接口：委托 Service 并返回成功")
    void testUpdateSupplierRecon() {
        SupplierReconUpdateReqVO reqVO = new SupplierReconUpdateReqVO();
        CommonResult<Boolean> result = controller.updateSupplierRecon(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(supplierReconService).updateSupplierRecon(reqVO);
    }

    @Test
    @DisplayName("reFetchSupplierRecon 接口：委托 Service 并返回成功")
    void testReFetchSupplierRecon() {
        CommonResult<Boolean> result = controller.reFetchSupplierRecon(100L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(supplierReconService).reFetchSupplierRecon(100L);
    }

    @Test
    @DisplayName("deleteSupplierRecon 接口：委托 Service 并返回成功")
    void testDeleteSupplierRecon() {
        CommonResult<Boolean> result = controller.deleteSupplierRecon(100L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(supplierReconService).deleteSupplierRecon(100L);
    }

    @Test
    @DisplayName("getSupplierRecon 接口：委托 Service 并返回详情")
    void testGetSupplierRecon() {
        SupplierReconRespVO respVO = new SupplierReconRespVO();
        respVO.setId(100L);
        when(supplierReconService.getSupplierRecon(100L)).thenReturn(respVO);

        CommonResult<SupplierReconRespVO> result = controller.getSupplierRecon(100L);

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(supplierReconService).getSupplierRecon(100L);
    }

    @Test
    @DisplayName("getSupplierReconPage 接口：委托 Service 并返回分页")
    void testGetSupplierReconPage() {
        SupplierReconPageReqVO reqVO = new SupplierReconPageReqVO();
        PageResult<SupplierReconRespVO> page = PageResult.empty();
        when(supplierReconService.getSupplierReconPage(reqVO)).thenReturn(page);

        CommonResult<PageResult<SupplierReconRespVO>> result = controller.getSupplierReconPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(page, result.getData());
        verify(supplierReconService).getSupplierReconPage(reqVO);
    }

    @Test
    @DisplayName("confirmSupplierRecon 接口：委托 Service 并返回成功")
    void testConfirmSupplierRecon() {
        CommonResult<Boolean> result = controller.confirmSupplierRecon(100L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(supplierReconService).confirmSupplierRecon(100L);
    }

    @Test
    @DisplayName("cancelSupplierRecon 接口：委托 Service 并返回成功")
    void testCancelSupplierRecon() {
        CommonResult<Boolean> result = controller.cancelSupplierRecon(100L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(supplierReconService).cancelSupplierRecon(100L);
    }

    @Test
    @DisplayName("getSupplierReconPrintData 接口：委托 Service 并返回打印数据")
    void testGetSupplierReconPrintData() {
        SupplierReconPrintRespVO printVO = new SupplierReconPrintRespVO();
        printVO.setId(100L);
        when(supplierReconService.getSupplierReconPrintData(100L)).thenReturn(printVO);

        CommonResult<SupplierReconPrintRespVO> result = controller.getSupplierReconPrintData(100L);

        assertTrue(result.isSuccess());
        assertEquals(printVO, result.getData());
        verify(supplierReconService).getSupplierReconPrintData(100L);
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("preview", new Class<?>[]{SupplierReconPreviewReqVO.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:create')");
        assertPermission("createSupplierRecon", new Class<?>[]{SupplierReconCreateReqVO.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:create')");
        assertPermission("updateSupplierRecon", new Class<?>[]{SupplierReconUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:update')");
        assertPermission("reFetchSupplierRecon", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:update')");
        assertPermission("deleteSupplierRecon", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:delete')");
        assertPermission("getSupplierRecon", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:query')");
        assertPermission("getSupplierReconPage", new Class<?>[]{SupplierReconPageReqVO.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:query')");
        assertPermission("confirmSupplierRecon", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:confirm')");
        assertPermission("cancelSupplierRecon", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:confirm')");
        assertPermission("getSupplierReconPrintData", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:supplierrecon:print')");
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = SupplierReconController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

}
