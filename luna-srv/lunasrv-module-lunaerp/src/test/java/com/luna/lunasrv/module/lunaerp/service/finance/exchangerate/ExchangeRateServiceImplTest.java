package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 即期汇率服务单元测试（接缝 1: Service 领域层）
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
        when(exchangeRateMapper.selectLatestEffectiveRate("HKD", bizDate)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exchangeRateService.getRate("HKD", bizDate));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
        verify(exchangeRateMapper).selectLatestEffectiveRate("HKD", bizDate);
    }

    @Test
    @DisplayName("createExchangeRate: 对本位币 RMB/CNY 配置抛出 1_030_020_202 EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN")
    void testCreateExchangeRate_rmbForbidden() {
        ExchangeRateSaveReqVO reqRmb = new ExchangeRateSaveReqVO();
        reqRmb.setCurrency("RMB");
        reqRmb.setRate(new BigDecimal("1.00000000"));
        reqRmb.setEffectiveDate(LocalDate.of(2026, 10, 16));

        ServiceException exRmb = assertThrows(ServiceException.class,
                () -> exchangeRateService.createExchangeRate(reqRmb));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN.getCode(), exRmb.getCode());

        ExchangeRateSaveReqVO reqCny = new ExchangeRateSaveReqVO();
        reqCny.setCurrency("cny");
        reqCny.setRate(new BigDecimal("1.00000000"));
        reqCny.setEffectiveDate(LocalDate.of(2026, 10, 16));

        ServiceException exCny = assertThrows(ServiceException.class,
                () -> exchangeRateService.createExchangeRate(reqCny));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN.getCode(), exCny.getCode());

        verify(exchangeRateMapper, never()).insert(any(ExchangeRateDO.class));
    }

    @Test
    @DisplayName("createExchangeRate: 同币种同生效日重复抛出 1_030_020_201 EXCHANGE_RATE_NO_DUPLICATE")
    void testCreateExchangeRate_duplicateCurrencyAndDate() {
        ExchangeRateSaveReqVO req = new ExchangeRateSaveReqVO();
        req.setCurrency("USD");
        req.setRate(new BigDecimal("7.15000000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 16));

        ExchangeRateDO existing = new ExchangeRateDO();
        existing.setId(99L);
        existing.setCurrency("USD");
        existing.setEffectiveDate(LocalDate.of(2026, 10, 16));

        when(exchangeRateMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exchangeRateService.createExchangeRate(req));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NO_DUPLICATE.getCode(), ex.getCode());
        verify(exchangeRateMapper, never()).insert(any(ExchangeRateDO.class));
    }

    @Test
    @DisplayName("createExchangeRate: 成功创建汇率记录并返回主键 ID，币种转为大写，默认状态 ENABLE")
    void testCreateExchangeRate_success() {
        ExchangeRateSaveReqVO req = new ExchangeRateSaveReqVO();
        req.setCurrency("usd");
        req.setRate(new BigDecimal("7.12345678"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 16));
        req.setRemark("测试汇率");

        when(exchangeRateMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            ExchangeRateDO entity = invocation.getArgument(0);
            entity.setId(1024L);
            return 1;
        }).when(exchangeRateMapper).insert(any(ExchangeRateDO.class));

        Long id = exchangeRateService.createExchangeRate(req);
        assertEquals(1024L, id);

        ArgumentCaptor<ExchangeRateDO> captor = ArgumentCaptor.forClass(ExchangeRateDO.class);
        verify(exchangeRateMapper).insert(captor.capture());
        ExchangeRateDO captured = captor.getValue();
        assertEquals("USD", captured.getCurrency());
        assertEquals(0, new BigDecimal("7.12345678").compareTo(captured.getRate()));
        assertEquals(LocalDate.of(2026, 10, 16), captured.getEffectiveDate());
        assertEquals("ENABLE", captured.getStatus());
        assertEquals("测试汇率", captured.getRemark());
    }

    @Test
    @DisplayName("updateExchangeRate: 记录不存在时抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND")
    void testUpdateExchangeRate_notFound() {
        ExchangeRateUpdateReqVO req = new ExchangeRateUpdateReqVO();
        req.setId(888L);
        req.setStatus("DISABLE");

        when(exchangeRateMapper.selectById(888L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exchangeRateService.updateExchangeRate(req));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
        verify(exchangeRateMapper, never()).updateById(any(ExchangeRateDO.class));
    }

    @Test
    @DisplayName("updateExchangeRate: 成功停用并修改备注")
    void testUpdateExchangeRate_success() {
        ExchangeRateUpdateReqVO req = new ExchangeRateUpdateReqVO();
        req.setId(10L);
        req.setStatus("DISABLE");
        req.setRemark("业务停用此汇率");

        ExchangeRateDO existing = new ExchangeRateDO();
        existing.setId(10L);
        existing.setCurrency("EUR");
        existing.setEffectiveDate(LocalDate.of(2026, 10, 10));
        existing.setStatus("ENABLE");
        existing.setRemark("原备注");

        when(exchangeRateMapper.selectById(10L)).thenReturn(existing);

        exchangeRateService.updateExchangeRate(req);

        ArgumentCaptor<ExchangeRateDO> captor = ArgumentCaptor.forClass(ExchangeRateDO.class);
        verify(exchangeRateMapper).updateById(captor.capture());
        ExchangeRateDO updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("DISABLE", updated.getStatus());
        assertEquals("业务停用此汇率", updated.getRemark());
    }

    @Test
    @DisplayName("updateExchangeRate: 备注允许置空更新（FieldStrategy.ALWAYS, coding-standards.md §15）")
    void testUpdateExchangeRate_clearRemark_success() {
        ExchangeRateUpdateReqVO req = new ExchangeRateUpdateReqVO();
        req.setId(10L);
        req.setStatus("DISABLE");
        req.setRemark("");

        ExchangeRateDO existing = new ExchangeRateDO();
        existing.setId(10L);
        existing.setCurrency("EUR");
        existing.setEffectiveDate(LocalDate.of(2026, 10, 10));
        existing.setStatus("ENABLE");
        existing.setRemark("旧备注");

        when(exchangeRateMapper.selectById(10L)).thenReturn(existing);

        exchangeRateService.updateExchangeRate(req);

        ArgumentCaptor<ExchangeRateDO> captor = ArgumentCaptor.forClass(ExchangeRateDO.class);
        verify(exchangeRateMapper).updateById(captor.capture());
        ExchangeRateDO updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("DISABLE", updated.getStatus());
        assertEquals("", updated.getRemark());
    }

    @Test
    @DisplayName("updateExchangeRate: 纯停用未传备注时保留原备注，防静默清空")
    void testUpdateExchangeRate_pureDisable_preservesExistingRemark() {
        ExchangeRateUpdateReqVO req = new ExchangeRateUpdateReqVO();
        req.setId(10L);
        req.setStatus("DISABLE");
        req.setRemark(null);

        ExchangeRateDO existing = new ExchangeRateDO();
        existing.setId(10L);
        existing.setCurrency("EUR");
        existing.setEffectiveDate(LocalDate.of(2026, 10, 10));
        existing.setStatus("ENABLE");
        existing.setRemark("保留的原备注");

        when(exchangeRateMapper.selectById(10L)).thenReturn(existing);

        exchangeRateService.updateExchangeRate(req);

        ArgumentCaptor<ExchangeRateDO> captor = ArgumentCaptor.forClass(ExchangeRateDO.class);
        verify(exchangeRateMapper).updateById(captor.capture());
        ExchangeRateDO updated = captor.getValue();
        assertEquals("DISABLE", updated.getStatus());
        assertEquals("保留的原备注", updated.getRemark());
    }

    @Test
    @DisplayName("getExchangeRate: 获得详情成功转换为 VO")
    void testGetExchangeRate_success() {
        ExchangeRateDO existing = new ExchangeRateDO();
        existing.setId(10L);
        existing.setCurrency("USD");
        existing.setRate(new BigDecimal("7.12345678"));
        existing.setEffectiveDate(LocalDate.of(2026, 10, 16));
        existing.setStatus("ENABLE");
        existing.setRemark("测试");

        when(exchangeRateMapper.selectById(10L)).thenReturn(existing);

        ExchangeRateRespVO resp = exchangeRateService.getExchangeRate(10L);
        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals("USD", resp.getCurrency());
        assertEquals(0, new BigDecimal("7.12345678").compareTo(resp.getRate()));
        assertEquals(LocalDate.of(2026, 10, 16), resp.getEffectiveDate());
        assertEquals("ENABLE", resp.getStatus());
        assertEquals("测试", resp.getRemark());
    }

    @Test
    @DisplayName("getExchangeRate: 记录不存在时抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND")
    void testGetExchangeRate_notFound() {
        when(exchangeRateMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exchangeRateService.getExchangeRate(999L));
        assertEquals(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getExchangeRatePage: 成功返回分页结果")
    void testGetExchangeRatePage_success() {
        ExchangeRatePageReqVO req = new ExchangeRatePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);
        req.setCurrency("USD");
        req.setStatus("ENABLE");

        ExchangeRateDO item = new ExchangeRateDO();
        item.setId(1L);
        item.setCurrency("USD");
        item.setRate(new BigDecimal("7.12345678"));
        item.setStatus("ENABLE");
        item.setEffectiveDate(LocalDate.of(2026, 10, 16));

        PageResult<ExchangeRateDO> pageDO = new PageResult<>(List.of(item), 1L);
        when(exchangeRateMapper.selectPage(eq(req), any(Wrapper.class))).thenReturn(pageDO);

        PageResult<ExchangeRateRespVO> result = exchangeRateService.getExchangeRatePage(req);
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("USD", result.getList().get(0).getCurrency());
    }

}
