package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Test
    @DisplayName("getRate：RMB/CNY 内置 rate=1，不查数据库")
    void testGetRate_Rmb_ReturnsOne() {
        BigDecimal rate = exchangeRateService.getRate("CNY", LocalDate.of(2026, 10, 12));
        assertEquals(new BigDecimal("1.00000000"), rate);
        verifyNoInteractions(exchangeRateMapper);

        BigDecimal rateRmb = exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 12));
        assertEquals(new BigDecimal("1.00000000"), rateRmb);
    }

    @Test
    @DisplayName("getRate：外币查找最新生效汇率成功")
    void testGetRate_ForeignCurrency_Success() {
        LocalDate bizDate = LocalDate.of(2026, 10, 12);
        ExchangeRateDO record = ExchangeRateDO.builder()
                .id(1L)
                .currency("USD")
                .rate(new BigDecimal("7.23450000"))
                .effectiveDate(LocalDate.of(2026, 10, 1))
                .status("ENABLE")
                .build();
        when(exchangeRateMapper.selectLatestEffectiveRate("USD", bizDate)).thenReturn(record);

        BigDecimal rate = exchangeRateService.getRate("USD", bizDate);
        assertEquals(new BigDecimal("7.23450000"), rate);
    }

    @Test
    @DisplayName("getRate：未配置汇率抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND")
    void testGetRate_NotFound_ThrowsException() {
        LocalDate bizDate = LocalDate.of(2026, 10, 12);
        when(exchangeRateMapper.selectLatestEffectiveRate("EUR", bizDate)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> exchangeRateService.getRate("EUR", bizDate));
        assertEquals(EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createExchangeRate：RMB/CNY 禁止配置汇率抛 202")
    void testCreateExchangeRate_Rmb_ThrowsForbidden() {
        ExchangeRateSaveReqVO req = new ExchangeRateSaveReqVO();
        req.setCurrency("CNY");
        req.setRate(new BigDecimal("1.00000000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 12));

        ServiceException ex = assertThrows(ServiceException.class, () -> exchangeRateService.createExchangeRate(req));
        assertEquals(EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createExchangeRate：同币种同生效日已存在抛 201 EXCHANGE_RATE_NO_DUPLICATE")
    void testCreateExchangeRate_Duplicate_ThrowsException() {
        ExchangeRateSaveReqVO req = new ExchangeRateSaveReqVO();
        req.setCurrency("USD");
        req.setRate(new BigDecimal("7.20000000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 12));

        when(exchangeRateMapper.selectByCurrencyAndEffectiveDateIncludingDeleted("USD", req.getEffectiveDate()))
                .thenReturn(ExchangeRateDO.builder().id(99L).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> exchangeRateService.createExchangeRate(req));
        assertEquals(EXCHANGE_RATE_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createExchangeRate：正常创建成功")
    void testCreateExchangeRate_Success() {
        ExchangeRateSaveReqVO req = new ExchangeRateSaveReqVO();
        req.setCurrency("USD");
        req.setRate(new BigDecimal("7.25000000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 12));
        req.setStatus("ENABLE");

        when(exchangeRateMapper.selectByCurrencyAndEffectiveDateIncludingDeleted("USD", req.getEffectiveDate()))
                .thenReturn(null);
        doAnswer(inv -> {
            ExchangeRateDO obj = inv.getArgument(0);
            obj.setId(88L);
            return 1;
        }).when(exchangeRateMapper).insert(any(ExchangeRateDO.class));

        Long id = exchangeRateService.createExchangeRate(req);
        assertEquals(88L, id);
    }
}
