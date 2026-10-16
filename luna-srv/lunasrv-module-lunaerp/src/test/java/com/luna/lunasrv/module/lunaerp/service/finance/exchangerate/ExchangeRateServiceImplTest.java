package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 即期汇率服务单元测试（接缝 3）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceImplTest {

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExchangeRateDO.class);
    }

    @Test
    @DisplayName("getRate: RMB/CNY 内置 rate=1.00000000，不查表直接返回")
    void testGetRate_rmb_returnsOne() {
        BigDecimal rateRmb = exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 16));
        assertEquals(0, new BigDecimal("1.00000000").compareTo(rateRmb));

        BigDecimal rateCny = exchangeRateService.getRate("cny", LocalDate.of(2026, 10, 16));
        assertEquals(0, new BigDecimal("1.00000000").compareTo(rateCny));

        BigDecimal rateNull = exchangeRateService.getRate(null, LocalDate.of(2026, 10, 16));
        assertEquals(0, new BigDecimal("1.00000000").compareTo(rateNull));

        verify(exchangeRateMapper, never()).selectLatestEffectiveRate(anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("getRate: 外币查询生效日<=业务日的最新启用汇率并返回")
    void testGetRate_foreignCurrency_returnsLatestEffective() {
        LocalDate bizDate = LocalDate.of(2026, 10, 16);
        ExchangeRateDO rateDO = new ExchangeRateDO();
        rateDO.setId(10L);
        rateDO.setCurrency("USD");
        rateDO.setRate(new BigDecimal("7.12345678"));
        rateDO.setEffectiveDate(LocalDate.of(2026, 10, 1));
        rateDO.setStatus("ENABLE");

        when(exchangeRateMapper.selectLatestEffectiveRate("USD", bizDate)).thenReturn(rateDO);

        BigDecimal rate = exchangeRateService.getRate("USD", bizDate);
        assertNotNull(rate);
        assertEquals(0, new BigDecimal("7.12345678").compareTo(rate));
        verify(exchangeRateMapper, times(1)).selectLatestEffectiveRate("USD", bizDate);
    }

    @Test
    @DisplayName("getRate: 外币无可用生效汇率时抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND")
    void testGetRate_notFound_throwsException() {
        LocalDate bizDate = LocalDate.of(2026, 10, 16);
        when(exchangeRateMapper.selectLatestEffectiveRate("EUR", bizDate)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exchangeRateService.getRate("EUR", bizDate));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getRate: 停用（DISABLE）汇率被剔除，无可用启用汇率时抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND")
    void testGetRate_disabledRate_excluded() {
        LocalDate bizDate = LocalDate.of(2026, 10, 16);
        // mapper.selectLatestEffectiveRate 查询条件包含 status = 'ENABLE'，停用记录不会被查出
        when(exchangeRateMapper.selectLatestEffectiveRate("HKD", bizDate)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exchangeRateService.getRate("HKD", bizDate));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
        verify(exchangeRateMapper).selectLatestEffectiveRate("HKD", bizDate);
    }

}
