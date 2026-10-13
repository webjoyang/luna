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

}

