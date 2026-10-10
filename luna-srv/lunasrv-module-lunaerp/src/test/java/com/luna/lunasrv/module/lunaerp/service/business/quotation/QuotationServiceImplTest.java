package com.luna.lunasrv.module.lunaerp.service.business.quotation;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation.QuotationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation.QuotationMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuotationServiceImplTest {

    @Mock
    private QuotationMapper quotationMapper;

    @Mock
    private QuotationItemMapper quotationItemMapper;

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private ConfigApi configApi;

    @InjectMocks
    private QuotationServiceImpl quotationService;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    private QuotationSaveReqVO buildValidReqVO() {
        QuotationItemSaveReqVO item = QuotationItemSaveReqVO.builder()
                .productName("测试面料")
                .quantity(new BigDecimal("100.00"))
                .costSource("CARD")
                .costUnitPriceRmb(new BigDecimal("80.0000"))
                .targetMarginRate(new BigDecimal("0.200000"))
                .build();

        return QuotationSaveReqVO.builder()
                .customerId(10L)
                .clientName("测试客户")
                .quotationDate(LocalDate.now())
                .validFrom(LocalDate.now())
                .currency("RMB")
                .exchangeRate(BigDecimal.ONE)
                .items(Collections.singletonList(item))
                .build();
    }

    @Test
    @DisplayName("创建报价单 - 自动取号与毛利反算单价及整单汇总")
    public void testCreateQuotation_AutoNoAndMarginReverse() {
        QuotationSaveReqVO req = buildValidReqVO();
        when(securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT)).thenReturn(true);
        when(quotationMapper.selectListByQuotationNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(quotationMapper.selectDraftByQuotationNo(any())).thenReturn(null);

        Long id = quotationService.createQuotation(req);

        ArgumentCaptor<QuotationDO> captor = ArgumentCaptor.forClass(QuotationDO.class);
        verify(quotationMapper).insert(captor.capture());
        QuotationDO saved = captor.getValue();

        assertNotNull(saved.getQuotationNo());
        assertTrue(saved.getQuotationNo().startsWith("QT"));
        assertEquals(1, saved.getVersionNo());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(new BigDecimal("100.00"), saved.getTotalQuantity());
        // cost = 80, margin = 0.2 => unitPrice = 80 / (1 - 0.2) = 100.0000
        // totalAmount = 100 * 100 = 10000.0000
        // totalCost = 100 * 80 = 8000.0000
        // avgMarginRate = (10000 - 8000) / 10000 = 0.200000
        assertEquals(new BigDecimal("10000.0000"), saved.getTotalAmount());
        assertEquals(new BigDecimal("8000.0000"), saved.getTotalCostAmount());
        assertEquals(new BigDecimal("0.200000"), saved.getAvgMarginRate());

        ArgumentCaptor<QuotationItemDO> itemCaptor = ArgumentCaptor.forClass(QuotationItemDO.class);
        verify(quotationItemMapper).insert(itemCaptor.capture());
        QuotationItemDO itemDO = itemCaptor.getValue();
        assertEquals(1, itemDO.getLineNo());
        assertEquals(new BigDecimal("100.0000"), itemDO.getUnitPrice());
        assertEquals(new BigDecimal("10000.0000"), itemDO.getAmount());
        assertEquals(new BigDecimal("8000.0000"), itemDO.getCostAmount());
        assertEquals(new BigDecimal("0.200000"), itemDO.getMarginRate());
    }

    @Test
    @DisplayName("创建报价单 - 手填单号查重覆盖软删行")
    public void testCreateQuotation_ManualNoDuplicate() {
        QuotationSaveReqVO req = buildValidReqVO();
        req.setQuotationNo("QT20268888");

        QuotationDO existing = QuotationDO.builder().id(99L).quotationNo("QT20268888").versionNo(1).build();
        when(quotationMapper.selectByNoAndVersionIncludingDeleted("QT20268888", 1)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.createQuotation(req));
        assertEquals(QUOTATION_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建报价单 - 同号至多一草稿冲突拦截")
    public void testCreateQuotation_DraftExists() {
        QuotationSaveReqVO req = buildValidReqVO();
        req.setQuotationNo("QT20268888");

        when(quotationMapper.selectByNoAndVersionIncludingDeleted("QT20268888", 1)).thenReturn(null);
        when(quotationMapper.selectDraftByQuotationNo("QT20268888")).thenReturn(new QuotationDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.createQuotation(req));
        assertEquals(QUOTATION_DRAFT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建报价单 - 外币报价未填汇率拦截")
    public void testCreateQuotation_FxRateRequired() {
        QuotationSaveReqVO req = buildValidReqVO();
        req.setCurrency("USD");
        req.setExchangeRate(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.createQuotation(req));
        assertEquals(QUOTATION_FX_RATE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建报价单 - 外币报价折币与汇率快照")
    public void testCreateQuotation_ForeignCurrencyConversion() {
        QuotationSaveReqVO req = buildValidReqVO();
        req.setCurrency("USD");
        req.setExchangeRate(new BigDecimal("8.000000")); // 1 USD = 8 RMB
        // costRmb = 80 => costUsd = 80 / 8 = 10.0000
        // margin = 0.2 => unitPrice = 10 / 0.8 = 12.5000 USD
        when(securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT)).thenReturn(true);
        when(quotationMapper.selectListByQuotationNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(quotationMapper.selectDraftByQuotationNo(any())).thenReturn(null);

        quotationService.createQuotation(req);

        ArgumentCaptor<QuotationDO> captor = ArgumentCaptor.forClass(QuotationDO.class);
        verify(quotationMapper).insert(captor.capture());
        QuotationDO saved = captor.getValue();
        assertEquals("USD", saved.getCurrency());
        assertEquals(new BigDecimal("8.000000"), saved.getExchangeRate());
        assertEquals(new BigDecimal("1250.0000"), saved.getTotalAmount());
        assertEquals(new BigDecimal("1000.0000"), saved.getTotalCostAmount());
        assertEquals(new BigDecimal("0.200000"), saved.getAvgMarginRate());

        ArgumentCaptor<QuotationItemDO> itemCaptor = ArgumentCaptor.forClass(QuotationItemDO.class);
        verify(quotationItemMapper).insert(itemCaptor.capture());
        QuotationItemDO itemDO = itemCaptor.getValue();
        assertEquals(new BigDecimal("10.0000"), itemDO.getCostUnitPrice());
        assertEquals(new BigDecimal("12.5000"), itemDO.getUnitPrice());
    }

    @Test
    @DisplayName("创建报价单 - 目标毛利率等于 100% 拦截")
    public void testCreateQuotation_MarginRate100PercentInvalid() {
        QuotationSaveReqVO req = buildValidReqVO();
        req.getItems().get(0).setTargetMarginRate(BigDecimal.ONE);

        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.createQuotation(req));
        assertEquals(QUOTATION_MARGIN_RATE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建报价单 - 无 amount 权限位时 MANUAL 手填成本被忽略")
    public void testCreateQuotation_NoAmountPermissionManualCostIgnored() {
        QuotationSaveReqVO req = buildValidReqVO();
        req.getItems().get(0).setCostSource("MANUAL");
        req.getItems().get(0).setCostUnitPriceRmb(new BigDecimal("50.0000"));
        req.getItems().get(0).setUnitPrice(new BigDecimal("60.0000"));
        req.getItems().get(0).setTargetMarginRate(null);

        when(securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT)).thenReturn(false);
        when(quotationMapper.selectListByQuotationNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(quotationMapper.selectDraftByQuotationNo(any())).thenReturn(null);

        quotationService.createQuotation(req);

        ArgumentCaptor<QuotationItemDO> itemCaptor = ArgumentCaptor.forClass(QuotationItemDO.class);
        verify(quotationItemMapper).insert(itemCaptor.capture());
        QuotationItemDO itemDO = itemCaptor.getValue();
        // 成本应被清零
        assertEquals(BigDecimal.ZERO, itemDO.getCostUnitPriceRmb());
        assertEquals(new BigDecimal("0.0000"), itemDO.getCostUnitPrice());
        assertEquals(new BigDecimal("0.0000"), itemDO.getCostAmount());
    }

    @Test
    @DisplayName("创建报价单 - 低毛利预警判定落标")
    public void testCreateQuotation_LowMarginWarning() {
        QuotationSaveReqVO req = buildValidReqVO();
        // 设置毛利率 5%
        req.getItems().get(0).setTargetMarginRate(new BigDecimal("0.050000"));
        when(configApi.getConfigValueByKey("quotation.min-margin-rate")).thenReturn("0.10"); // 最低 10%
        when(securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT)).thenReturn(true);
        when(quotationMapper.selectListByQuotationNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(quotationMapper.selectDraftByQuotationNo(any())).thenReturn(null);

        quotationService.createQuotation(req);

        ArgumentCaptor<QuotationItemDO> itemCaptor = ArgumentCaptor.forClass(QuotationItemDO.class);
        verify(quotationItemMapper).insert(itemCaptor.capture());
        QuotationItemDO itemDO = itemCaptor.getValue();
        assertTrue(itemDO.getLowMarginWarning());
    }

    @Test
    @DisplayName("确认报价单 - DRAFT 成功转 CONFIRMED，非草稿守卫拦截")
    public void testConfirmQuotation() {
        QuotationDO draft = QuotationDO.builder().id(1L).status("DRAFT").build();
        when(quotationMapper.selectById(1L)).thenReturn(draft);
        when(quotationMapper.confirmQuotation(eq(1L), any(), any())).thenReturn(1);

        quotationService.confirmQuotation(1L);
        verify(quotationMapper).confirmQuotation(eq(1L), any(), any());

        // 非草稿拦截
        QuotationDO confirmed = QuotationDO.builder().id(2L).status("CONFIRMED").build();
        when(quotationMapper.selectById(2L)).thenReturn(confirmed);
        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.confirmQuotation(2L));
        assertEquals(QUOTATION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("取消报价单 - CONFIRMED 成功转 CANCELLED，非已确认守卫拦截")
    public void testCancelQuotation() {
        QuotationDO confirmed = QuotationDO.builder().id(1L).status("CONFIRMED").build();
        when(quotationMapper.selectById(1L)).thenReturn(confirmed);
        when(quotationMapper.cancelQuotation(eq(1L), any(), any())).thenReturn(1);

        quotationService.cancelQuotation(1L);
        verify(quotationMapper).cancelQuotation(eq(1L), any(), any());

        // 草稿状态不可取消
        QuotationDO draft = QuotationDO.builder().id(2L).status("DRAFT").build();
        when(quotationMapper.selectById(2L)).thenReturn(draft);
        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.cancelQuotation(2L));
        assertEquals(QUOTATION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除报价单 - 仅草稿可删并级联删除明细")
    public void testDeleteQuotation() {
        QuotationDO draft = QuotationDO.builder().id(1L).status("DRAFT").build();
        when(quotationMapper.selectById(1L)).thenReturn(draft);

        quotationService.deleteQuotation(1L);
        verify(quotationMapper).deleteById(1L);
        verify(quotationItemMapper).deleteByQuotationId(1L);

        // 已确认不可删除
        QuotationDO confirmed = QuotationDO.builder().id(2L).status("CONFIRMED").build();
        when(quotationMapper.selectById(2L)).thenReturn(confirmed);
        ServiceException ex = assertThrows(ServiceException.class, () -> quotationService.deleteQuotation(2L));
        assertEquals(QUOTATION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新报价单 - 整体替换明细并重算")
    public void testUpdateQuotation() {
        QuotationDO existing = QuotationDO.builder()
                .id(1L)
                .quotationNo("QT20260001")
                .versionNo(1)
                .status("DRAFT")
                .salespersonUserId(5L)
                .deptId(10L)
                .currency("RMB")
                .exchangeRate(BigDecimal.ONE)
                .build();
        when(quotationMapper.selectById(1L)).thenReturn(existing);
        when(securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT)).thenReturn(true);

        QuotationSaveReqVO updateReq = buildValidReqVO();
        updateReq.setId(1L);
        quotationService.updateQuotation(updateReq);

        verify(quotationMapper).updateById(any(QuotationDO.class));
        verify(quotationItemMapper).deleteByQuotationId(1L);
        verify(quotationItemMapper).insert(any(QuotationItemDO.class));
    }

    @Test
    @DisplayName("获取详情 - 无 amount 权限时抹除敏感字段")
    public void testGetQuotation_NoAmountPermissionMasked() {
        QuotationDO q = QuotationDO.builder()
                .id(1L)
                .quotationNo("QT20260001")
                .versionNo(1)
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("10000.0000"))
                .totalCostAmount(new BigDecimal("8000.0000"))
                .avgMarginRate(new BigDecimal("0.200000"))
                .build();
        when(quotationMapper.selectById(1L)).thenReturn(q);

        QuotationItemDO item = QuotationItemDO.builder()
                .id(10L)
                .quotationId(1L)
                .amount(new BigDecimal("10000.0000"))
                .costAmount(new BigDecimal("8000.0000"))
                .marginRate(new BigDecimal("0.200000"))
                .costUnitPrice(new BigDecimal("80.0000"))
                .build();
        when(quotationItemMapper.selectListByQuotationId(1L)).thenReturn(Collections.singletonList(item));
        when(securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT)).thenReturn(false);

        QuotationRespVO resp = quotationService.getQuotation(1L);
        assertNotNull(resp);
        assertEquals(new BigDecimal("10000.0000"), resp.getTotalAmount());
        assertNull(resp.getTotalCostAmount());
        assertNull(resp.getAvgMarginRate());

        QuotationItemRespVO itemResp = resp.getItems().get(0);
        assertEquals(new BigDecimal("10000.0000"), itemResp.getAmount());
        assertNull(itemResp.getCostAmount());
        assertNull(itemResp.getMarginRate());
        assertNull(itemResp.getCostUnitPrice());
    }

    @Test
    @DisplayName("修订报价单 - 成功将最新已确认版 SUPERSEDED 并创建新版本草稿")
    public void testReviseQuotation() {
        QuotationDO confirmed = QuotationDO.builder()
                .id(1L)
                .quotationNo("QT20260001")
                .versionNo(1)
                .status("CONFIRMED")
                .customerId(10L)
                .clientName("测试客户")
                .currency("RMB")
                .exchangeRate(BigDecimal.ONE)
                .build();
        when(quotationMapper.selectById(1L)).thenReturn(confirmed);
        when(quotationMapper.selectMaxVersionNoByQuotationNo("QT20260001")).thenReturn(1);
        when(quotationMapper.selectDraftByQuotationNo("QT20260001")).thenReturn(null);
        when(quotationMapper.supersedeQuotation(eq(1L), any(), any())).thenReturn(1);

        QuotationItemDO item = QuotationItemDO.builder()
                .id(100L)
                .quotationId(1L)
                .productName("测试布")
                .quantity(new BigDecimal("50.00"))
                .build();
        when(quotationItemMapper.selectListByQuotationId(1L)).thenReturn(Collections.singletonList(item));

        Long newId = quotationService.reviseQuotation(1L);

        verify(quotationMapper).supersedeQuotation(eq(1L), any(), any());
        ArgumentCaptor<QuotationDO> captor = ArgumentCaptor.forClass(QuotationDO.class);
        verify(quotationMapper).insert(captor.capture());
        QuotationDO newDraft = captor.getValue();
        assertEquals(2, newDraft.getVersionNo());
        assertEquals("DRAFT", newDraft.getStatus());
        assertEquals("QT20260001", newDraft.getQuotationNo());

        verify(quotationItemMapper).insert(any(QuotationItemDO.class));
    }

}
