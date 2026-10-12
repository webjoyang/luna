package com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.*;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalStatementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalStatementController 门户对账单控制器测试")
public class PortalStatementControllerTest {

    @InjectMocks
    private PortalStatementController controller;

    @Mock
    private PortalStatementService portalStatementService;

    @Test
    @DisplayName("列表端点正常委托 Service 并返回 CommonResult")
    void testGetStatementList() {
        PortalStatementRespVO item = PortalStatementRespVO.builder()
                .id(101L)
                .statementNo("CS-202610110001")
                .customerId(1001L)
                .customerName("测试客户")
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 10, 1))
                .periodEnd(LocalDate.of(2026, 10, 31))
                .openingBalance(BigDecimal.ZERO)
                .periodDebit(BigDecimal.TEN)
                .periodCredit(BigDecimal.ONE)
                .closingBalance(new BigDecimal("9.00"))
                .status("SENT")
                .build();

        when(portalStatementService.getStatementList(1001L, "SENT")).thenReturn(List.of(item));

        CommonResult<List<PortalStatementRespVO>> result = controller.getStatementList(1001L, "SENT");

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("CS-202610110001", result.getData().get(0).getStatementNo());
        verify(portalStatementService).getStatementList(1001L, "SENT");
    }

    @Test
    @DisplayName("详情端点正常委托 Service 并返回 CommonResult（支持 id 与 no 双形态）")
    void testGetStatementDetail() {
        Long id = 101L;
        String no = "CS-202610110001";
        PortalStatementDetailRespVO detail = PortalStatementDetailRespVO.builder()
                .id(id)
                .statementNo(no)
                .status("SENT")
                .items(Collections.emptyList())
                .build();

        when(portalStatementService.getStatementDetail(id, no, null)).thenReturn(detail);

        CommonResult<PortalStatementDetailRespVO> result = controller.getStatementDetail(id, no, null);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(id, result.getData().getId());
        assertEquals(no, result.getData().getStatementNo());
        verify(portalStatementService).getStatementDetail(id, no, null);
    }

    @Test
    @DisplayName("确认端点正常委托 Service 并返回 CommonResult")
    void testConfirmStatement() {
        PortalStatementConfirmReqVO reqVO = PortalStatementConfirmReqVO.builder()
                .id(101L)
                .confirmByName("张三")
                .build();

        doNothing().when(portalStatementService).confirmStatement(reqVO);

        CommonResult<Boolean> result = controller.confirmStatement(reqVO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(portalStatementService).confirmStatement(reqVO);
    }

    @Test
    @DisplayName("驳高端点正常委托 Service 并返回 CommonResult")
    void testRejectStatement() {
        PortalStatementRejectReqVO reqVO = PortalStatementRejectReqVO.builder()
                .id(101L)
                .rejectReason("金额有误")
                .build();

        doNothing().when(portalStatementService).rejectStatement(reqVO);

        CommonResult<Boolean> result = controller.rejectStatement(reqVO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(portalStatementService).rejectStatement(reqVO);
    }

}
