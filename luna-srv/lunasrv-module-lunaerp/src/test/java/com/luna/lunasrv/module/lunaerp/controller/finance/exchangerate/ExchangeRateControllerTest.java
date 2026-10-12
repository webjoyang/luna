package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ExchangeRateController} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateControllerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateController exchangeRateController;

    @Test
    @DisplayName("分页查询端点转发验证")
    void testGetExchangeRatePage() {
        ExchangeRatePageReqVO pageReqVO = new ExchangeRatePageReqVO();
        pageReqVO.setCurrency("USD");

        ExchangeRateDO rateDO = ExchangeRateDO.builder()
                .id(1L)
                .currency("USD")
                .rate(new BigDecimal("7.25000000"))
                .effectiveDate(LocalDate.of(2026, 10, 1))
                .status("ENABLE")
                .build();
        when(exchangeRateService.getExchangeRatePage(any(ExchangeRatePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(rateDO), 1L));

        CommonResult<PageResult<ExchangeRateRespVO>> result = exchangeRateController.getExchangeRatePage(pageReqVO);
        assertNotNull(result);
        assertEquals(1L, result.getData().getTotal());
        assertEquals("USD", result.getData().getList().get(0).getCurrency());
        verify(exchangeRateService).getExchangeRatePage(pageReqVO);
    }

    @Test
    @DisplayName("详情查询端点转发验证")
    void testGetExchangeRate() {
        ExchangeRateDO rateDO = ExchangeRateDO.builder()
                .id(1L)
                .currency("USD")
                .rate(new BigDecimal("7.25000000"))
                .effectiveDate(LocalDate.of(2026, 10, 1))
                .status("ENABLE")
                .build();
        when(exchangeRateService.getExchangeRate(1L)).thenReturn(rateDO);

        CommonResult<ExchangeRateRespVO> result = exchangeRateController.getExchangeRate(1L);
        assertNotNull(result);
        assertEquals(1L, result.getData().getId());
        assertEquals("USD", result.getData().getCurrency());
        verify(exchangeRateService).getExchangeRate(1L);
    }

    @Test
    @DisplayName("新增汇率端点转发验证")
    void testCreateExchangeRate() {
        ExchangeRateSaveReqVO reqVO = new ExchangeRateSaveReqVO();
        reqVO.setCurrency("EUR");
        reqVO.setRate(new BigDecimal("7.80000000"));
        reqVO.setEffectiveDate(LocalDate.of(2026, 10, 1));
        when(exchangeRateService.createExchangeRate(any(ExchangeRateSaveReqVO.class))).thenReturn(100L);

        CommonResult<Long> result = exchangeRateController.createExchangeRate(reqVO);
        assertNotNull(result);
        assertEquals(100L, result.getData());
        verify(exchangeRateService).createExchangeRate(reqVO);
    }

    @Test
    @DisplayName("更新汇率端点转发验证")
    void testUpdateExchangeRate() {
        ExchangeRateSaveReqVO reqVO = new ExchangeRateSaveReqVO();
        reqVO.setId(100L);
        reqVO.setStatus("DISABLE");
        reqVO.setRemark("停用");

        CommonResult<Boolean> result = exchangeRateController.updateExchangeRate(reqVO);
        assertNotNull(result);
        assertTrue(result.getData());
        verify(exchangeRateService).updateExchangeRate(reqVO);
    }

}
