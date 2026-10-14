package com.luna.lunasrv.module.lunaerp.controller.production.scheduling;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.PendingPoolRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.scheduling.SchedulingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerTest {

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private SchedulingController schedulingController;

    @Test
    @DisplayName("Controller获得甘特图数据：委托SchedulingService并返回成功结果")
    void testGetGantt() {
        GanttReqVO reqVO = new GanttReqVO();
        GanttRespVO respVO = new GanttRespVO(Collections.emptyList(), Collections.emptyList());
        when(schedulingService.getGantt(reqVO)).thenReturn(respVO);

        CommonResult<GanttRespVO> result = schedulingController.getGantt(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(schedulingService).getGantt(reqVO);
    }

    @Test
    @DisplayName("Controller获得待排池数据：委托SchedulingService并返回成功结果")
    void testGetPendingPool() {
        PendingPoolRespVO respVO = new PendingPoolRespVO(Collections.emptyList(), Collections.emptyList());
        when(schedulingService.getPendingPool()).thenReturn(respVO);

        CommonResult<PendingPoolRespVO> result = schedulingController.getPendingPool();

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(schedulingService).getPendingPool();
    }

}
