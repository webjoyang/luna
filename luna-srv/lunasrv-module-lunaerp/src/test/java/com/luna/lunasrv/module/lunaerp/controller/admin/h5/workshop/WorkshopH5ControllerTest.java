package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.service.h5.workshop.WorkshopH5Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link WorkshopH5Controller} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("车间移动 H5 - 接入层 Controller 单测")
class WorkshopH5ControllerTest {

    @Mock
    private WorkshopH5Service workshopH5Service;

    @InjectMocks
    private WorkshopH5Controller workshopH5Controller;

    @Test
    @DisplayName("ticket/start: 委托 Service 并返回 success(true)")
    void testStartTicket_success() {
        WorkshopTicketStartReqVO reqVO = WorkshopTicketStartReqVO.builder()
                .code("BT-TEST-001")
                .actualMachine("LM01")
                .build();

        CommonResult<Boolean> result = workshopH5Controller.startTicket(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(workshopH5Service).startTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/start: 校验端点注解与权限表达式")
    void testStartTicket_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("startTicket", WorkshopTicketStartReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "startTicket 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/ticket/start"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "startTicket 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:report')", preAuthorize.value());
    }

    @Test
    @DisplayName("ticket/complete: 委托 Service 并返回 success(respVO)")
    void testCompleteTicket_success() {
        WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder()
                .code("BT-TEST-001")
                .build();
        WorkshopTicketCompleteRespVO respVO = WorkshopTicketCompleteRespVO.builder()
                .alreadyCompleted(Boolean.FALSE)
                .build();
        when(workshopH5Service.completeTicket(reqVO)).thenReturn(respVO);

        CommonResult<WorkshopTicketCompleteRespVO> result = workshopH5Controller.completeTicket(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData().getAlreadyCompleted());
        verify(workshopH5Service).completeTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/complete: 幂等完工返回 alreadyCompleted=true")
    void testCompleteTicket_alreadyCompleted() {
        WorkshopTicketCompleteReqVO reqVO = WorkshopTicketCompleteReqVO.builder()
                .code("BT-TEST-001")
                .build();
        WorkshopTicketCompleteRespVO respVO = WorkshopTicketCompleteRespVO.builder()
                .alreadyCompleted(Boolean.TRUE)
                .build();
        when(workshopH5Service.completeTicket(reqVO)).thenReturn(respVO);

        CommonResult<WorkshopTicketCompleteRespVO> result = workshopH5Controller.completeTicket(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getAlreadyCompleted());
        verify(workshopH5Service).completeTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/complete: 校验端点注解与权限表达式")
    void testCompleteTicket_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("completeTicket", WorkshopTicketCompleteReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "completeTicket 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/ticket/complete"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "completeTicket 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:report')", preAuthorize.value());
    }

    @Test
    @DisplayName("ticket/exception: 委托 Service 并返回 success(true)")
    void testExceptionTicket_success() {
        WorkshopTicketExceptionReqVO reqVO = WorkshopTicketExceptionReqVO.builder()
                .code("BT-TEST-001")
                .exceptionType("YARN_BREAK")
                .exceptionNote("断纱")
                .build();

        CommonResult<Boolean> result = workshopH5Controller.exceptionTicket(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(workshopH5Service).exceptionTicket(reqVO);
    }

    @Test
    @DisplayName("ticket/exception: 校验端点注解与权限表达式")
    void testExceptionTicket_annotations() throws NoSuchMethodException {
        Method method = WorkshopH5Controller.class.getMethod("exceptionTicket", WorkshopTicketExceptionReqVO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "exceptionTicket 必须标注 @PostMapping");
        assertArrayEquals(new String[]{"/ticket/exception"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "exceptionTicket 必须标注 @PreAuthorize");
        assertEquals("@ss.hasPermission('lunaerp:workshop:h5:report')", preAuthorize.value());
    }

}
