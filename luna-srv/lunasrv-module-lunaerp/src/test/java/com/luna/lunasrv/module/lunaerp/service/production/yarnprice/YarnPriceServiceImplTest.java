package com.luna.lunasrv.module.lunaerp.service.production.yarnprice;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnprice.YarnPriceDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnprice.YarnPriceMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.YARN_PRICE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.YARN_SPEC_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class YarnPriceServiceImplTest {

    @Mock
    private YarnPriceMapper yarnPriceMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private YarnPriceServiceImpl yarnPriceService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("创建纱价：正常成功保存")
    void testCreateYarnPrice_Success() {
        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        spec.setYarnTypeId(1L);
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        when(yarnPriceMapper.selectBySpecTypeAndDate(10L, "PURCHASE", LocalDate.of(2026, 10, 10))).thenReturn(null);

        doAnswer(inv -> {
            YarnPriceDO doObj = inv.getArgument(0);
            doObj.setId(100L);
            return 1;
        }).when(yarnPriceMapper).insert(any(YarnPriceDO.class));

        YarnPriceSaveReqVO req = new YarnPriceSaveReqVO();
        req.setYarnSpecId(10L);
        req.setPriceType("PURCHASE");
        req.setPrice(new BigDecimal("35.5000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 10));

        Long id = yarnPriceService.createYarnPrice(req);
        assertEquals(100L, id);
        verify(yarnPriceMapper).insert(any(YarnPriceDO.class));
    }

    @Test
    @DisplayName("创建纱价：同日撞唯一报错 1_030_008_009")
    void testCreateYarnPrice_DuplicateDate() {
        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        spec.setYarnTypeId(1L);
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        YarnPriceDO existing = new YarnPriceDO();
        existing.setId(99L);
        when(yarnPriceMapper.selectBySpecTypeAndDate(10L, "PURCHASE", LocalDate.of(2026, 10, 10))).thenReturn(existing);

        YarnPriceSaveReqVO req = new YarnPriceSaveReqVO();
        req.setYarnSpecId(10L);
        req.setPriceType("PURCHASE");
        req.setPrice(new BigDecimal("35.5000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 10));

        ServiceException ex = assertThrows(ServiceException.class, () -> yarnPriceService.createYarnPrice(req));
        assertEquals(YARN_PRICE_DUPLICATE_DATE.getCode(), ex.getCode());
        verify(yarnPriceMapper, never()).insert(any(YarnPriceDO.class));
    }

    @Test
    @DisplayName("创建纱价：规格不存在报错 1_030_008_002")
    void testCreateYarnPrice_SpecNotExists() {
        when(yarnSpecMapper.selectById(999L)).thenReturn(null);

        YarnPriceSaveReqVO req = new YarnPriceSaveReqVO();
        req.setYarnSpecId(999L);
        req.setPriceType("PURCHASE");
        req.setPrice(new BigDecimal("35.5000"));
        req.setEffectiveDate(LocalDate.of(2026, 10, 10));

        ServiceException ex = assertThrows(ServiceException.class, () -> yarnPriceService.createYarnPrice(req));
        assertEquals(YARN_SPEC_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("取价规则：取不晚于指定日期的最新一条生效价，来源标为 PRICE_TABLE")
    void testGetEffectivePrice_FromPriceTable() {
        YarnPriceDO price1 = new YarnPriceDO();
        price1.setId(101L);
        price1.setYarnSpecId(10L);
        price1.setPriceType("PURCHASE");
        price1.setPrice(new BigDecimal("36.0000"));
        price1.setEffectiveDate(LocalDate.of(2026, 10, 5));

        when(yarnPriceMapper.selectLatestEffectivePrice(10L, "PURCHASE", LocalDate.of(2026, 10, 10))).thenReturn(price1);

        YarnPriceEffectiveRespVO result = yarnPriceService.getEffectivePrice(10L, "PURCHASE", LocalDate.of(2026, 10, 10));
        assertNotNull(result);
        assertEquals(new BigDecimal("36.0000"), result.getPrice());
        assertEquals("PRICE_TABLE", result.getPriceSource());
        assertFalse(result.getIsMissing());
        assertEquals(LocalDate.of(2026, 10, 5), result.getEffectiveDate());
    }

    @Test
    @DisplayName("取价规则：价类隔离（采购参考价 vs 市场参考价）")
    void testGetEffectivePrice_PriceTypeIsolation() {
        YarnPriceDO marketPrice = new YarnPriceDO();
        marketPrice.setId(102L);
        marketPrice.setYarnSpecId(10L);
        marketPrice.setPriceType("MARKET");
        marketPrice.setPrice(new BigDecimal("38.5000"));
        marketPrice.setEffectiveDate(LocalDate.of(2026, 10, 8));

        when(yarnPriceMapper.selectLatestEffectivePrice(10L, "MARKET", LocalDate.of(2026, 10, 10))).thenReturn(marketPrice);

        YarnPriceEffectiveRespVO result = yarnPriceService.getEffectivePrice(10L, "MARKET", LocalDate.of(2026, 10, 10));
        assertEquals(new BigDecimal("38.5000"), result.getPrice());
        assertEquals("MARKET", result.getPriceType());
    }

    @Test
    @DisplayName("取价规则：无记录回退 spec.ref_price 且标记 SPEC_REF_FALLBACK，不计缺价")
    void testGetEffectivePrice_FallbackToSpecRefPrice() {
        when(yarnPriceMapper.selectLatestEffectivePrice(10L, "PURCHASE", LocalDate.of(2026, 10, 10))).thenReturn(null);

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        spec.setRefPrice(new BigDecimal("28.0000"));
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        YarnPriceEffectiveRespVO result = yarnPriceService.getEffectivePrice(10L, "PURCHASE", LocalDate.of(2026, 10, 10));
        assertEquals(new BigDecimal("28.0000"), result.getPrice());
        assertEquals("SPEC_REF_FALLBACK", result.getPriceSource());
        assertFalse(result.getIsMissing());
        assertNull(result.getEffectiveDate());
    }

    @Test
    @DisplayName("取价规则：连 spec.ref_price 都没有才计缺价 MISSING")
    void testGetEffectivePrice_Missing() {
        when(yarnPriceMapper.selectLatestEffectivePrice(10L, "PURCHASE", LocalDate.of(2026, 10, 10))).thenReturn(null);

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        spec.setRefPrice(null);
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        YarnPriceEffectiveRespVO result = yarnPriceService.getEffectivePrice(10L, "PURCHASE", LocalDate.of(2026, 10, 10));
        assertEquals(BigDecimal.ZERO, result.getPrice());
        assertEquals("MISSING", result.getPriceSource());
        assertTrue(result.getIsMissing());
    }

    @Test
    @DisplayName("删除纱价：记录不存在报错 1_030_008_008")
    void testDeleteYarnPrice_NotExists() {
        when(yarnPriceMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> yarnPriceService.deleteYarnPrice(999L));
        assertEquals(YARN_PRICE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("跨租户不可达：查询他租户记录返回不存在")
    void testCrossTenant_NotAccessible() {
        // 当租户拦截器过滤时，selectById 他租户记录返回 null
        when(yarnPriceMapper.selectById(100L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> yarnPriceService.getYarnPrice(100L));
        assertEquals(YARN_PRICE_NOT_EXISTS.getCode(), ex.getCode());
    }

}
