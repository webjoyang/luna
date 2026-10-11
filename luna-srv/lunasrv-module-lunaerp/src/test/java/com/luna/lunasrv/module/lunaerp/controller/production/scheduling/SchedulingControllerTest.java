package com.luna.lunasrv.module.lunaerp.controller.production.scheduling;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttQueryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.PendingPoolRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.scheduling.SchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulingControllerTest {

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private SchedulingController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("getGanttData: 成功调用 Service 并返回结果")
    void testGetGanttData() {
        GanttQueryReqVO reqVO = new GanttQueryReqVO();
        reqVO.setStart(LocalDate.of(2026, 10, 1));
        reqVO.setEnd(LocalDate.of(2026, 10, 31));

        GanttRespVO respVO = new GanttRespVO(Collections.emptyList(), Collections.emptyList());
        when(schedulingService.getGanttData(reqVO)).thenReturn(respVO);

        CommonResult<GanttRespVO> result = controller.getGanttData(reqVO);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertSame(respVO, result.getData());
        verify(schedulingService).getGanttData(reqVO);
    }

    @Test
    @DisplayName("getPendingPool: 成功调用 Service 并返回结果")
    void testGetPendingPool() {
        PendingPoolRespVO respVO = new PendingPoolRespVO(Collections.emptyList(), Collections.emptyList());
        when(schedulingService.getPendingPool()).thenReturn(respVO);

        CommonResult<PendingPoolRespVO> result = controller.getPendingPool();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertSame(respVO, result.getData());
        verify(schedulingService).getPendingPool();
    }

}
