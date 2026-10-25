package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ColorInstructionControllerTest {

    @Mock
    private ColorInstructionService colorInstructionService;

    @InjectMocks
    private ColorInstructionController colorInstructionController;

    @Test
    @DisplayName("Controller获得打色指示单分页：委托Service并返回分页结果")
    void testGetColorInstructionPage() {
        ColorInstructionPageReqVO pageReqVO = new ColorInstructionPageReqVO();
        PageResult<ColorInstructionRespVO> pageResult = PageResult.empty();
        when(colorInstructionService.getColorInstructionPage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<ColorInstructionRespVO>> result = colorInstructionController.getColorInstructionPage(pageReqVO);

        assertTrue(result.isSuccess());
        assertEquals(pageResult, result.getData());
        verify(colorInstructionService).getColorInstructionPage(pageReqVO);
    }

    @Test
    @DisplayName("Controller获得打色指示单详情：委托Service并返回详情")
    void testGetColorInstruction() {
        ColorInstructionRespVO respVO = new ColorInstructionRespVO();
        respVO.setId(1024L);
        when(colorInstructionService.getColorInstruction(1024L)).thenReturn(respVO);

        CommonResult<ColorInstructionRespVO> result = colorInstructionController.getColorInstruction(1024L);

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(colorInstructionService).getColorInstruction(1024L);
    }

    @Test
    @DisplayName("Controller创建打色指示单：委托Service并返回ID")
    void testCreateColorInstruction() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        when(colorInstructionService.createColorInstruction(reqVO)).thenReturn(999L);

        CommonResult<Long> result = colorInstructionController.createColorInstruction(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(999L, result.getData());
        verify(colorInstructionService).createColorInstruction(reqVO);
    }

    @Test
    @DisplayName("Controller预览下一个打色单号：委托Service并返回编号")
    void testGetNextInstructionNo() {
        when(colorInstructionService.getNextInstructionNo()).thenReturn("DS20261013001");

        CommonResult<String> result = colorInstructionController.getNextInstructionNo();

        assertTrue(result.isSuccess());
        assertEquals("DS20261013001", result.getData());
        verify(colorInstructionService).getNextInstructionNo();
    }

    @Test
    @DisplayName("Controller软删除打色指示单：委托Service并返回成功")
    void testDeleteColorInstruction() {
        doNothing().when(colorInstructionService).deleteColorInstruction(1024L);

        CommonResult<Boolean> result = colorInstructionController.deleteColorInstruction(1024L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).deleteColorInstruction(1024L);
    }

    @Test
    @DisplayName("Controller更新打色指示单：委托Service并返回成功")
    void testUpdateColorInstruction() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1024L);
        doNothing().when(colorInstructionService).updateColorInstruction(reqVO);

        CommonResult<Boolean> result = colorInstructionController.updateColorInstruction(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).updateColorInstruction(reqVO);
    }

    @Test
    @DisplayName("Controller开始打色：委托Service并返回成功")
    void testStartColorInstruction() {
        ColorInstructionStartReqVO reqVO = new ColorInstructionStartReqVO();
        reqVO.setId(1024L);
        doNothing().when(colorInstructionService).startColorInstruction(1024L);

        CommonResult<Boolean> result = colorInstructionController.startColorInstruction(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).startColorInstruction(1024L);
    }

    @Test
    @DisplayName("Controller通过打色颜色行：委托Service并返回成功")
    void testApproveColorLine() {
        ColorInstructionColorApproveReqVO reqVO = new ColorInstructionColorApproveReqVO();
        reqVO.setId(11L);
        doNothing().when(colorInstructionService).approveColorLine(11L);

        CommonResult<Boolean> result = colorInstructionController.approveColorLine(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).approveColorLine(11L);
    }

    @Test
    @DisplayName("Controller驳回重打颜色行：委托Service并返回成功")
    void testRejectRestrikeColorLine() {
        ColorInstructionColorRejectReqVO reqVO = new ColorInstructionColorRejectReqVO();
        reqVO.setId(11L);
        reqVO.setRejectReason("颜色偏浅，需调深半度");
        doNothing().when(colorInstructionService).rejectRestrikeColorLine(11L, "颜色偏浅，需调深半度");

        CommonResult<Boolean> result = colorInstructionController.rejectRestrikeColorLine(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).rejectRestrikeColorLine(11L, "颜色偏浅，需调深半度");
    }

    @Test
    @DisplayName("Controller确认打色单：委托Service并返回成功，具备:confirm权限注解")
    void testConfirmColorInstruction() throws NoSuchMethodException {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(1024L);
        reqVO.setConfirmType("CUSTOMER");
        reqVO.setCustomerFeedback("客户确认通过");

        doNothing().when(colorInstructionService).confirmColorInstruction(reqVO);

        CommonResult<Boolean> result = colorInstructionController.confirmColorInstruction(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).confirmColorInstruction(reqVO);

        // 校验注解
        var method = ColorInstructionController.class.getMethod("confirmColorInstruction", ColorInstructionConfirmReqVO.class);
        var putMapping = method.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class);
        org.junit.jupiter.api.Assertions.assertNotNull(putMapping);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"/confirm"}, putMapping.value());

        var preAuth = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuth);
        org.junit.jupiter.api.Assertions.assertEquals("@ss.hasPermission('lunaerp:production:color-instruction:confirm')", preAuth.value());
    }

    @Test
    @DisplayName("Controller回退打色单：委托Service并返回成功，具备:confirm权限注解")
    void testRollbackColorInstruction() throws NoSuchMethodException {
        ColorInstructionRollbackReqVO reqVO = new ColorInstructionRollbackReqVO();
        reqVO.setId(1024L);

        doNothing().when(colorInstructionService).rollbackColorInstruction(1024L);

        CommonResult<Boolean> result = colorInstructionController.rollbackColorInstruction(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).rollbackColorInstruction(1024L);

        // 校验注解
        var method = ColorInstructionController.class.getMethod("rollbackColorInstruction", ColorInstructionRollbackReqVO.class);
        var putMapping = method.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class);
        org.junit.jupiter.api.Assertions.assertNotNull(putMapping);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"/rollback"}, putMapping.value());

        var preAuth = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuth);
        org.junit.jupiter.api.Assertions.assertEquals("@ss.hasPermission('lunaerp:production:color-instruction:confirm')", preAuth.value());
    }

    @Test
    @DisplayName("Controller作废打色单：委托Service并返回成功，具备:confirm权限注解")
    void testVoidColorInstruction() throws NoSuchMethodException {
        ColorInstructionVoidReqVO reqVO = new ColorInstructionVoidReqVO();
        reqVO.setId(1024L);
        reqVO.setVoidReason("客户取消");

        doNothing().when(colorInstructionService).voidColorInstruction(reqVO);

        CommonResult<Boolean> result = colorInstructionController.voidColorInstruction(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(colorInstructionService).voidColorInstruction(reqVO);

        // 校验注解
        var method = ColorInstructionController.class.getMethod("voidColorInstruction", ColorInstructionVoidReqVO.class);
        var putMapping = method.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class);
        org.junit.jupiter.api.Assertions.assertNotNull(putMapping);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"/void"}, putMapping.value());

        var preAuth = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuth);
        org.junit.jupiter.api.Assertions.assertEquals("@ss.hasPermission('lunaerp:production:color-instruction:confirm')", preAuth.value());
    }

    @Test
    @DisplayName("Controller获得打色指示单打印数据：通过 PathVariable 传入 ID，委托 Service 并返回打印 VO")
    void testGetColorInstructionPrintData() {
        ColorInstructionPrintRespVO printRespVO = new ColorInstructionPrintRespVO();
        printRespVO.setId(1024L);
        printRespVO.setInstructionNo("DS20261014001");
        when(colorInstructionService.getColorInstructionPrintData(1024L)).thenReturn(printRespVO);

        CommonResult<ColorInstructionPrintRespVO> result = colorInstructionController.getColorInstructionPrintData(1024L);

        assertTrue(result.isSuccess());
        assertEquals(printRespVO, result.getData());
        verify(colorInstructionService).getColorInstructionPrintData(1024L);
    }

    @Test
    @DisplayName("Controller打印数据路由与权限注解：严格映射 /{id}/print-data，具备:print权限")
    void testPrintDataAnnotations() throws NoSuchMethodException {
        var method = ColorInstructionController.class.getMethod("getColorInstructionPrintData", Long.class);
        var getMapping = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        org.junit.jupiter.api.Assertions.assertNotNull(getMapping);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"/{id}/print-data"}, getMapping.value());

        var preAuth = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuth);
        org.junit.jupiter.api.Assertions.assertEquals("@ss.hasPermission('lunaerp:production:color-instruction:print')", preAuth.value());
    }

}

