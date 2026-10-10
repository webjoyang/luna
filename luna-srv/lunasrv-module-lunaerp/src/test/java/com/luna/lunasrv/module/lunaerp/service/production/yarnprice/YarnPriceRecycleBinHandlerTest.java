package com.luna.lunasrv.module.lunaerp.service.production.yarnprice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnprice.YarnPriceDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnprice.YarnPriceMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.*;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link YarnPriceRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class YarnPriceRecycleBinHandlerTest {

    @Mock
    private YarnPriceMapper yarnPriceMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @InjectMocks
    private YarnPriceRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long YARN_PRICE_ID = 6001L;
    private static final Long YARN_SPEC_ID = 5001L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(TENANT_ID_1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType 返回 YARN_PRICE")
    void testGetBizType() {
        assertEquals("YARN_PRICE", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询已软删纱价：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("YARN_PRICE");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("YARN_PRICE");
            item.setBizId(YARN_PRICE_ID);
            item.setDisplayName("32S 棉 (采购参考价: 25.50)");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(yarnPriceMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    @Test
    @DisplayName("恢复校验：规格仍在回收站时阻断")
    void testValidateRestore_specInRecycle() {
        YarnPriceDO priceDO = YarnPriceDO.builder()
                .id(YARN_PRICE_ID)
                .yarnSpecId(YARN_SPEC_ID)
                .priceType("PURCHASE")
                .price(new BigDecimal("25.00"))
                .effectiveDate(LocalDate.now())
                .build();
        when(yarnPriceMapper.selectDeletedById(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(priceDO);

        YarnSpecDO deletedSpec = new YarnSpecDO();
        deletedSpec.setId(YARN_SPEC_ID);
        deletedSpec.setSpecName("32S 棉");
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(deletedSpec);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(YARN_PRICE_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：恢复发生同日价格冲突阻断")
    void testValidateRestore_duplicateDateConflict() {
        LocalDate date = LocalDate.now();
        YarnPriceDO priceDO = YarnPriceDO.builder()
                .id(YARN_PRICE_ID)
                .yarnSpecId(YARN_SPEC_ID)
                .priceType("PURCHASE")
                .price(new BigDecimal("25.00"))
                .effectiveDate(date)
                .build();
        when(yarnPriceMapper.selectDeletedById(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(priceDO);
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnSpecMapper.selectById(YARN_SPEC_ID)).thenReturn(new YarnSpecDO());

        YarnPriceDO conflictDO = YarnPriceDO.builder()
                .id(9999L)
                .yarnSpecId(YARN_SPEC_ID)
                .priceType("PURCHASE")
                .price(new BigDecimal("26.00"))
                .effectiveDate(date)
                .build();
        when(yarnPriceMapper.selectBySpecTypeAndDate(YARN_SPEC_ID, "PURCHASE", date)).thenReturn(conflictDO);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(YARN_PRICE_ID));
        assertEquals(YARN_PRICE_DUPLICATE_DATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复成功：正常恢复")
    void testRestore_success() {
        LocalDate date = LocalDate.now();
        YarnPriceDO priceDO = YarnPriceDO.builder()
                .id(YARN_PRICE_ID)
                .yarnSpecId(YARN_SPEC_ID)
                .priceType("PURCHASE")
                .price(new BigDecimal("25.00"))
                .effectiveDate(date)
                .build();
        when(yarnPriceMapper.selectDeletedById(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(priceDO);
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnSpecMapper.selectById(YARN_SPEC_ID)).thenReturn(new YarnSpecDO());
        when(yarnPriceMapper.selectBySpecTypeAndDate(YARN_SPEC_ID, "PURCHASE", date)).thenReturn(null);

        when(yarnPriceMapper.restoreYarnPrice(eq(YARN_PRICE_ID), eq(TENANT_ID_1), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(YARN_PRICE_ID));
        verify(yarnPriceMapper, times(1)).restoreYarnPrice(eq(YARN_PRICE_ID), eq(TENANT_ID_1), any(), any());
    }

    @Test
    @DisplayName("彻底删除：存在引用阻断")
    void testPurge_hasActiveReference() {
        YarnPriceDO priceDO = YarnPriceDO.builder().id(YARN_PRICE_ID).build();
        when(yarnPriceMapper.selectDeletedById(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(priceDO);
        when(yarnPriceMapper.selectActiveReferenceCount(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(YARN_PRICE_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底删除：无引用物理删除成功")
    void testPurge_success() {
        YarnPriceDO priceDO = YarnPriceDO.builder().id(YARN_PRICE_ID).build();
        when(yarnPriceMapper.selectDeletedById(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(priceDO);
        when(yarnPriceMapper.selectActiveReferenceCount(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(0L);
        when(yarnPriceMapper.purgeYarnPrice(YARN_PRICE_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(YARN_PRICE_ID));
        verify(yarnPriceMapper, times(1)).purgeYarnPrice(YARN_PRICE_ID, TENANT_ID_1);
    }
}
