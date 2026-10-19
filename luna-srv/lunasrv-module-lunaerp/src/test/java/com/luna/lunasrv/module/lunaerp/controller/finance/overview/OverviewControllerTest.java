package com.luna.lunasrv.module.lunaerp.controller.finance.overview;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.overview.FinanceOverviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OverviewController} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class OverviewControllerTest {

    @InjectMocks
    private OverviewController overviewController;

    @Mock
    private FinanceOverviewService overviewService;

    @Test
    @DisplayName("getOverviewSummary: 委托 Service 获得收支总览汇总")
    void testGetOverviewSummary() {
        OverviewSummaryReqVO reqVO = new OverviewSummaryReqVO();
        reqVO.setPeriodStart(LocalDate.of(2026, 10, 1));
        reqVO.setPeriodEnd(LocalDate.of(2026, 10, 31));

        OverviewSummaryRespVO resp = new OverviewSummaryRespVO();
        resp.setPeriodStart(reqVO.getPeriodStart());
        resp.setPeriodEnd(reqVO.getPeriodEnd());
        when(overviewService.getOverviewSummary(reqVO)).thenReturn(resp);

        CommonResult<OverviewSummaryRespVO> result = overviewController.getOverviewSummary(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(reqVO.getPeriodStart(), result.getData().getPeriodStart());
        verify(overviewService).getOverviewSummary(reqVO);
    }

    @Test
    @DisplayName("getGrossMargin: 委托 Service 获得经营毛利参考")
    void testGetGrossMargin() {
        GrossMarginReqVO reqVO = new GrossMarginReqVO();
        reqVO.setPeriodStart(LocalDate.of(2026, 10, 1));
        reqVO.setPeriodEnd(LocalDate.of(2026, 10, 31));

        GrossMarginRespVO resp = new GrossMarginRespVO();
        resp.setRevenue(new BigDecimal("100000.00"));
        resp.setCost(new BigDecimal("60000.00"));
        resp.setGrossProfit(new BigDecimal("40000.00"));
        when(overviewService.getGrossMargin(reqVO)).thenReturn(resp);

        CommonResult<GrossMarginRespVO> result = overviewController.getGrossMargin(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(new BigDecimal("100000.00"), result.getData().getRevenue());
        verify(overviewService).getGrossMargin(reqVO);
    }

}
