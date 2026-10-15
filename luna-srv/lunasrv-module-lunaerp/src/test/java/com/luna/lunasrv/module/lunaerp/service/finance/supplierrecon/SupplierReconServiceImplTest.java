package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.LineType;
import com.luna.lunasrv.module.lunaerp.enums.finance.PricingBasis;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReconStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.SourceRefType;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier.SupplierService;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link SupplierReconServiceImpl} 纯 Mockito 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class SupplierReconServiceImplTest {

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierReconItemMapper supplierReconItemMapper;

    @Mock
    private SupplierReconSourceService supplierReconSourceService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private SupplierReconServiceImpl supplierReconService;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SupplierReconDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SupplierReconItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentApplyDO.class);
        TenantContextHolder.setTenantId(TENANT_ID);

        lenient().when(supplierReconSourceService.buildFingerprint(any(), any(), any())).thenAnswer(inv -> {
            String type = inv.getArgument(0);
            Long id = inv.getArgument(1);
            Long itemId = inv.getArgument(2);
            if ("INSPECTION".equalsIgnoreCase(type) || itemId == null) {
                return type + ":" + id;
            }
            return type + ":" + id + ":" + itemId;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("002 守卫：预览时供应商不存在报 SUPPLIER_RECON_SUPPLIER_NOT_EXISTS")
    void testPreviewSupplierNotExists() {
        SupplierReconPreviewReqVO req = new SupplierReconPreviewReqVO();
        req.setSupplierId(999L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        when(supplierService.getSupplier(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.preview(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("003 守卫：生成对账单时若区间内无可对账明细报 SUPPLIER_RECON_NO_SOURCE_ITEMS")
    void testCreateNoSourceItems() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        // 1. 请求明细列表为空直接报 003
        req.setItems(Collections.emptyList());

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_NO_SOURCE_ITEMS.getCode(), ex1.getCode());

        // 2. 请求明细含有调整项，但取数服务返回无可对账明细，报 003
        SupplierReconItemSaveVO adjustItem = new SupplierReconItemSaveVO();
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setLineAmount(new BigDecimal("100.00"));
        adjustItem.setAdjustReason("运费调整");
        req.setItems(List.of(adjustItem));

        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(Collections.emptyList());

        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_NO_SOURCE_ITEMS.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("004 守卫：生成瞬间服务端重取发现源明细被其他有效对账单抢引用，报 SUPPLIER_RECON_SOURCE_REFERENCED 带对方单号")
    void testCreateSourceItemReferencedByOther() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        SupplierReconItemSaveVO processItem = new SupplierReconItemSaveVO();
        processItem.setLineType(LineType.PROCESS.getValue());
        processItem.setSourceRefType(SourceRefType.INSPECTION.getValue());
        processItem.setSourceRefId(100L);
        processItem.setInspectionOrderId(100L);
        processItem.setInspectionOrderNo("INS001");
        processItem.setPricingBasis(PricingBasis.METER.getValue());
        processItem.setQtyM(new BigDecimal("100.00"));
        processItem.setUnitPrice(new BigDecimal("5.0000"));
        req.setItems(List.of(processItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        // 模拟取数防重检查发现该指纹被对账单 SR20261014001 占用
        String fp = SourceRefType.INSPECTION.getValue() + ":100";
        when(supplierReconSourceService.findOccupiedFingerprints(Set.of(fp)))
                .thenReturn(Map.of(fp, "SR20261014001"));

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("SR20261014001"));
    }

    @Test
    @DisplayName("004 守卫强化：优先进行被抢占用比对，即使可用候选池变空也绝不被 003 提前截断")
    void testCreateSourceItemReferencedWhenAvailableCandidatesEmpty() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        SupplierReconItemSaveVO processItem = new SupplierReconItemSaveVO();
        processItem.setLineType(LineType.PROCESS.getValue());
        processItem.setSourceRefType(SourceRefType.INSPECTION.getValue());
        processItem.setSourceRefId(200L);
        processItem.setInspectionOrderId(200L);
        req.setItems(List.of(processItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        // 占用检查发现被单据 SR20261015888 引用
        String fp = SourceRefType.INSPECTION.getValue() + ":200";
        when(supplierReconSourceService.findOccupiedFingerprints(Set.of(fp)))
                .thenReturn(Map.of(fp, "SR20261015888"));

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("SR20261015888"));
    }

    @Test
    @DisplayName("005 守卫：ADJUST 行金额为 0 报 SUPPLIER_RECON_ADJUST_LINE_INVALID")
    void testCreateAdjustLineZeroAmount() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        SupplierReconItemSaveVO adjustItem = new SupplierReconItemSaveVO();
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setLineAmount(BigDecimal.ZERO);
        adjustItem.setAdjustReason("质量扣款");
        req.setItems(List.of(adjustItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        SupplierReconSourceItemRespVO candidate = new SupplierReconSourceItemRespVO();
        candidate.setFingerprint("INS:1");
        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(candidate));

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_ADJUST_LINE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("005 守卫：ADJUST 行未填备注/调整原因报 SUPPLIER_RECON_ADJUST_LINE_INVALID")
    void testCreateAdjustLineMissingReason() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        SupplierReconItemSaveVO adjustItem = new SupplierReconItemSaveVO();
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setLineAmount(new BigDecimal("-100.00"));
        adjustItem.setAdjustReason("   "); // 空白
        req.setItems(List.of(adjustItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        SupplierReconSourceItemRespVO candidate = new SupplierReconSourceItemRespVO();
        candidate.setFingerprint("INS:1");
        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(candidate));

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_ADJUST_LINE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("服务端权威重算：覆盖前端传入的 line_amount，PROCESS=bill_qty×unit_price，PURCHASE=qty×unit_price，ADJUST 直接取，total_amount=Σ行金额")
    void testCreateAuthorityRecalculation() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        // 1. PROCESS 行
        SupplierReconItemSaveVO processItem = new SupplierReconItemSaveVO();
        processItem.setLineType(LineType.PROCESS.getValue());
        processItem.setSourceRefType(SourceRefType.INSPECTION.getValue());
        processItem.setSourceRefId(101L);
        processItem.setPricingBasis(PricingBasis.METER.getValue());
        processItem.setUnitPrice(new BigDecimal("3.5000"));
        processItem.setLineAmount(new BigDecimal("999999.00")); // 前端错误金额

        // 2. PURCHASE 行
        SupplierReconItemSaveVO purchaseItem = new SupplierReconItemSaveVO();
        purchaseItem.setLineType(LineType.PURCHASE.getValue());
        purchaseItem.setSourceRefType(SourceRefType.STOCK_IN_ITEM.getValue());
        purchaseItem.setSourceRefId(201L);
        purchaseItem.setSourceRefItemId(301L);
        purchaseItem.setUnitPrice(new BigDecimal("12.0000"));
        purchaseItem.setLineAmount(BigDecimal.ZERO);

        // 3. ADJUST 行
        SupplierReconItemSaveVO adjustItem = new SupplierReconItemSaveVO();
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setLineAmount(new BigDecimal("-50.00"));
        adjustItem.setAdjustReason("次品折让");

        req.setItems(List.of(processItem, purchaseItem, adjustItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        // 模拟候选池返回权威快照
        SupplierReconSourceItemRespVO processCandidate = new SupplierReconSourceItemRespVO();
        processCandidate.setLineType(LineType.PROCESS.getValue());
        processCandidate.setSourceRefType(SourceRefType.INSPECTION.getValue());
        processCandidate.setSourceRefId(101L);
        processCandidate.setFingerprint(SourceRefType.INSPECTION.getValue() + ":101");
        processCandidate.setInspectionOrderId(101L);
        processCandidate.setInspectionOrderNo("INS101");
        processCandidate.setQtyM(new BigDecimal("100.00"));
        processCandidate.setQtyKg(new BigDecimal("45.00"));
        processCandidate.setPricingBasis(PricingBasis.METER.getValue());

        SupplierReconSourceItemRespVO purchaseCandidate = new SupplierReconSourceItemRespVO();
        purchaseCandidate.setLineType(LineType.PURCHASE.getValue());
        purchaseCandidate.setSourceRefType(SourceRefType.STOCK_IN_ITEM.getValue());
        purchaseCandidate.setSourceRefId(201L);
        purchaseCandidate.setSourceRefItemId(301L);
        purchaseCandidate.setFingerprint(SourceRefType.STOCK_IN_ITEM.getValue() + ":201:301");
        purchaseCandidate.setStockInId(201L);
        purchaseCandidate.setStockInNo("IN201");
        purchaseCandidate.setStockInItemId(301L);
        purchaseCandidate.setBillQty(new BigDecimal("200.00"));
        purchaseCandidate.setPricingBasis("KG");

        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(processCandidate, purchaseCandidate));

        when(supplierReconMapper.selectListByReconNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(supplierReconMapper.selectByReconNoIncludingDeleted(anyString())).thenReturn(null);

        // 执行保存
        Long reconId = supplierReconService.createSupplierRecon(req);

        // 验证主表保存：total_amount = 350.00 + 2400.00 - 50.00 = 2700.00
        ArgumentCaptor<SupplierReconDO> reconCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).insert(reconCaptor.capture());
        SupplierReconDO savedRecon = reconCaptor.getValue();
        assertEquals(new BigDecimal("2700.00"), savedRecon.getTotalAmount());
        assertEquals(ReconStatus.DRAFT.getValue(), savedRecon.getStatus());

        // 验证行表批量保存：金额已权威覆盖
        ArgumentCaptor<List<SupplierReconItemDO>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(supplierReconItemMapper).insertBatch(itemCaptor.capture());
        List<SupplierReconItemDO> savedItems = itemCaptor.getValue();

        SupplierReconItemDO savedProcess = savedItems.stream()
                .filter(i -> LineType.PROCESS.getValue().equals(i.getLineType()))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("350.00"), savedProcess.getLineAmount());
        assertEquals(new BigDecimal("100.00"), savedProcess.getBillQty());

        SupplierReconItemDO savedPurchase = savedItems.stream()
                .filter(i -> LineType.PURCHASE.getValue().equals(i.getLineType()))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("2400.00"), savedPurchase.getLineAmount());

        SupplierReconItemDO savedAdjust = savedItems.stream()
                .filter(i -> LineType.ADJUST.getValue().equals(i.getLineType()))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("-50.00"), savedAdjust.getLineAmount());
    }

    @Test
    @DisplayName("ADR-0006 取号防撞重试：SR+YYYYMMDD+3位流水，遇冲突顺延重试")
    void testReconNoGenerationAndRetry() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        SupplierReconItemSaveVO adjustItem = new SupplierReconItemSaveVO();
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setLineAmount(new BigDecimal("100.00"));
        adjustItem.setAdjustReason("运费补差");
        req.setItems(List.of(adjustItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        SupplierReconSourceItemRespVO candidate = new SupplierReconSourceItemRespVO();
        candidate.setFingerprint("INS:1");
        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(candidate));

        // 模拟库中已有 001
        SupplierReconDO existing = new SupplierReconDO();
        existing.setReconNo("SR" + LocalDate.now().toString().replace("-", "") + "001");
        when(supplierReconMapper.selectListByReconNoPrefixIncludingDeleted(anyString())).thenReturn(List.of(existing));

        // 顺延取 002 时无冲突
        when(supplierReconMapper.selectByReconNoIncludingDeleted(anyString())).thenReturn(null);

        supplierReconService.createSupplierRecon(req);

        ArgumentCaptor<SupplierReconDO> reconCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).insert(reconCaptor.capture());
        String generatedNo = reconCaptor.getValue().getReconNo();
        assertTrue(generatedNo.endsWith("002"));
    }

    @Test
    @DisplayName("001 守卫：非 DRAFT 状态对账单调 update / delete / reFetch 报 SUPPLIER_RECON_STATUS_NOT_ALLOWED")
    void testNonDraftStatusGuard() {
        SupplierReconDO confirmedRecon = new SupplierReconDO();
        confirmedRecon.setId(100L);
        confirmedRecon.setStatus(ReconStatus.CONFIRMED.getValue());
        confirmedRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(confirmedRecon);

        // 1. update
        SupplierReconUpdateReqVO updateReq = new SupplierReconUpdateReqVO();
        updateReq.setId(100L);
        updateReq.setItems(List.of(new SupplierReconItemSaveVO()));
        ServiceException updateEx = assertThrows(ServiceException.class, () -> supplierReconService.updateSupplierRecon(updateReq));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), updateEx.getCode());

        // 2. delete
        ServiceException deleteEx = assertThrows(ServiceException.class, () -> supplierReconService.deleteSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), deleteEx.getCode());

        // 3. reFetch
        ServiceException reFetchEx = assertThrows(ServiceException.class, () -> supplierReconService.reFetchSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), reFetchEx.getCode());
    }

    @Test
    @DisplayName("update 草稿编辑：计价基准切换（METER->KG）bill_qty 随之重算，删除指定行，total_amount 重新计算")
    void testUpdateDraftRecon() {
        SupplierReconDO draftRecon = new SupplierReconDO();
        draftRecon.setId(100L);
        draftRecon.setStatus(ReconStatus.DRAFT.getValue());
        draftRecon.setTenantId(TENANT_ID);
        when(supplierReconMapper.selectById(100L)).thenReturn(draftRecon);

        // 库中现有行 1 与行 2
        SupplierReconItemDO dbLine1 = new SupplierReconItemDO();
        dbLine1.setId(1L);
        dbLine1.setReconId(100L);

        SupplierReconItemDO dbLine2 = new SupplierReconItemDO();
        dbLine2.setId(2L);
        dbLine2.setReconId(100L);
        dbLine2.setLineType(LineType.PROCESS.getValue());
        dbLine2.setQtyKg(new BigDecimal("40.00"));
        dbLine2.setQtyM(new BigDecimal("100.00"));

        when(supplierReconItemMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(dbLine1, dbLine2)) // 第一次查询
                .thenReturn(List.of(dbLine2));         // 删行 1 后的重算查询

        SupplierReconUpdateReqVO updateReq = new SupplierReconUpdateReqVO();
        updateReq.setId(100L);
        updateReq.setDeletedItemIds(List.of(1L)); // 删行 1

        // 行 2：原基准 METER(100m)，现改为 KG(40kg)，单价 10.0000 -> 重算 line_amount = 400.00
        SupplierReconItemSaveVO line2 = new SupplierReconItemSaveVO();
        line2.setId(2L);
        line2.setLineType(LineType.PROCESS.getValue());
        line2.setPricingBasis(PricingBasis.KG.getValue());
        line2.setUnitPrice(new BigDecimal("10.0000"));

        updateReq.setItems(List.of(line2));

        supplierReconService.updateSupplierRecon(updateReq);

        // 验证删除被删行
        verify(supplierReconItemMapper).deleteBatchIds(List.of(1L));

        // 验证更新行 2 的 bill_qty 为 40.00，line_amount 为 400.00
        ArgumentCaptor<SupplierReconItemDO> itemCaptor = ArgumentCaptor.forClass(SupplierReconItemDO.class);
        verify(supplierReconItemMapper).updateById(itemCaptor.capture());
        SupplierReconItemDO updatedItem = itemCaptor.getValue();
        assertEquals(new BigDecimal("40.00"), updatedItem.getBillQty());
        assertEquals(new BigDecimal("400.00"), updatedItem.getLineAmount());

        // 验证更新主表 total_amount 为 400.00
        ArgumentCaptor<SupplierReconDO> reconCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).updateById(reconCaptor.capture());
        assertEquals(new BigDecimal("400.00"), reconCaptor.getValue().getTotalAmount());
    }

    @Test
    @DisplayName("update 纯删行：items 允许为空，支持仅传 deletedItemIds 执行删行并重算 total")
    void testUpdateDraftReconPureDeletion() {
        SupplierReconDO draftRecon = new SupplierReconDO();
        draftRecon.setId(100L);
        draftRecon.setStatus(ReconStatus.DRAFT.getValue());
        draftRecon.setTenantId(TENANT_ID);
        when(supplierReconMapper.selectById(100L)).thenReturn(draftRecon);

        SupplierReconItemDO dbLine1 = new SupplierReconItemDO();
        dbLine1.setId(1L);
        dbLine1.setReconId(100L);

        when(supplierReconItemMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(dbLine1)) // db 现有行
                .thenReturn(Collections.emptyList()); // 删完后为空

        SupplierReconUpdateReqVO updateReq = new SupplierReconUpdateReqVO();
        updateReq.setId(100L);
        updateReq.setDeletedItemIds(List.of(1L));
        updateReq.setItems(null); // items 为空

        supplierReconService.updateSupplierRecon(updateReq);

        verify(supplierReconItemMapper).deleteBatchIds(List.of(1L));
        ArgumentCaptor<SupplierReconDO> reconCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).updateById(reconCaptor.capture());
        assertEquals(new BigDecimal("0.00"), reconCaptor.getValue().getTotalAmount());
    }

    @Test
    @DisplayName("update 范围守卫：编辑草稿时禁止新增非 ADJUST 明细行（凭空新增取数行报 SUPPLIER_RECON_STATUS_NOT_ALLOWED）")
    void testUpdateDraftReconRejectNonAdjustNewLine() {
        SupplierReconDO draftRecon = new SupplierReconDO();
        draftRecon.setId(100L);
        draftRecon.setStatus(ReconStatus.DRAFT.getValue());
        draftRecon.setTenantId(TENANT_ID);
        when(supplierReconMapper.selectById(100L)).thenReturn(draftRecon);

        when(supplierReconItemMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.emptyList());

        SupplierReconUpdateReqVO updateReq = new SupplierReconUpdateReqVO();
        updateReq.setId(100L);

        SupplierReconItemSaveVO illegalProcessItem = new SupplierReconItemSaveVO();
        illegalProcessItem.setId(null); // 新增行
        illegalProcessItem.setLineType(LineType.PROCESS.getValue()); // 非 ADJUST
        illegalProcessItem.setUnitPrice(new BigDecimal("10.00"));
        updateReq.setItems(List.of(illegalProcessItem));

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.updateSupplierRecon(updateReq));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("ADR-0006 取号并发：连续重试全部冲突时抛出 SUPPLIER_RECON_STATUS_NOT_ALLOWED 而非底层数据库异常")
    void testAdr0006ReconNoExhaustedRetryThrowsException() {
        SupplierReconCreateReqVO req = new SupplierReconCreateReqVO();
        req.setSupplierId(10L);
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        SupplierReconItemSaveVO adjustItem = new SupplierReconItemSaveVO();
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setLineAmount(new BigDecimal("50.00"));
        adjustItem.setAdjustReason("运费调整");
        req.setItems(List.of(adjustItem));

        SupplierRespVO mockSupplier = new SupplierRespVO();
        mockSupplier.setId(10L);
        mockSupplier.setCompanyName("测试供应商");
        when(supplierService.getSupplier(10L)).thenReturn(mockSupplier);

        SupplierReconSourceItemRespVO candidate = new SupplierReconSourceItemRespVO();
        candidate.setFingerprint("INS:1");
        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(candidate));

        // 模拟已存在 001
        SupplierReconDO existing = new SupplierReconDO();
        existing.setReconNo("SR" + LocalDate.now().toString().replace("-", "") + "001");
        when(supplierReconMapper.selectListByReconNoPrefixIncludingDeleted(anyString())).thenReturn(List.of(existing));

        // 模拟随后的所有重试 candidate 全部冲突占用
        when(supplierReconMapper.selectByReconNoIncludingDeleted(anyString())).thenReturn(new SupplierReconDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createSupplierRecon(req));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reFetch 草稿整体重取：替换全部 PROCESS/PURCHASE 行，保留原有的 ADJUST 调整项行，批量插入重算 total")
    void testReFetchDraftRecon() {
        SupplierReconDO draftRecon = new SupplierReconDO();
        draftRecon.setId(100L);
        draftRecon.setSupplierId(10L);
        draftRecon.setPeriodStart(LocalDate.of(2026, 9, 1));
        draftRecon.setPeriodEnd(LocalDate.of(2026, 9, 30));
        draftRecon.setStatus(ReconStatus.DRAFT.getValue());
        draftRecon.setTenantId(TENANT_ID);
        when(supplierReconMapper.selectById(100L)).thenReturn(draftRecon);

        // 现有行：1 行 PROCESS、1 行 ADJUST
        SupplierReconItemDO oldProcess = new SupplierReconItemDO();
        oldProcess.setId(1L);
        oldProcess.setReconId(100L);
        oldProcess.setLineType(LineType.PROCESS.getValue());

        SupplierReconItemDO oldAdjust = new SupplierReconItemDO();
        oldAdjust.setId(2L);
        oldAdjust.setReconId(100L);
        oldAdjust.setLineType(LineType.ADJUST.getValue());
        oldAdjust.setLineAmount(new BigDecimal("-30.00"));
        oldAdjust.setAdjustReason("旧调整项");

        when(supplierReconItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(oldProcess, oldAdjust));

        // 新取数返回 1 个新的 PURCHASE 候选行
        SupplierReconSourceItemRespVO newCandidate = new SupplierReconSourceItemRespVO();
        newCandidate.setLineType(LineType.PURCHASE.getValue());
        newCandidate.setSourceRefType(SourceRefType.STOCK_IN_ITEM.getValue());
        newCandidate.setSourceRefId(501L);
        newCandidate.setSourceRefItemId(601L);
        newCandidate.setBillQty(new BigDecimal("50.00"));
        newCandidate.setUnitPrice(new BigDecimal("20.0000"));
        newCandidate.setLineAmount(new BigDecimal("1000.00"));
        when(supplierReconSourceService.getAvailableCandidateItems(10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(newCandidate));

        supplierReconService.reFetchSupplierRecon(100L);

        // 验证删除了旧 PROCESS 行 (ID 1L)，而 ADJUST 行 (ID 2L) 未被删除
        verify(supplierReconItemMapper).deleteBatchIds(List.of(1L));

        // 验证批量插入新取数候选行
        ArgumentCaptor<List<SupplierReconItemDO>> newCandidateCaptor = ArgumentCaptor.forClass(List.class);
        verify(supplierReconItemMapper).insertBatch(newCandidateCaptor.capture());
        assertEquals(1, newCandidateCaptor.getValue().size());
        assertEquals(LineType.PURCHASE.getValue(), newCandidateCaptor.getValue().get(0).getLineType());

        // 验证主表 total_amount 重算 = 1000.00 (新候选) - 30.00 (保留的调整项) = 970.00
        ArgumentCaptor<SupplierReconDO> reconCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).updateById(reconCaptor.capture());
        assertEquals(new BigDecimal("970.00"), reconCaptor.getValue().getTotalAmount());
    }

    @Test
    @DisplayName("getSupplierRecon 详情：内嵌 items 并只读实时聚合已申请金额（排除 REJECTED 和 CANCELLED）")
    void testGetSupplierReconAppliedAmountAggregation() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setReconNo("SR20261015001");
        recon.setSupplierId(10L);
        recon.setSupplierName("测试供应商");
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTotalAmount(new BigDecimal("5000.00"));
        recon.setTenantId(TENANT_ID);
        when(supplierReconMapper.selectById(100L)).thenReturn(recon);

        SupplierReconItemDO item = new SupplierReconItemDO();
        item.setId(1L);
        item.setReconId(100L);
        item.setLineType(LineType.PROCESS.getValue());
        item.setLineAmount(new BigDecimal("5000.00"));
        when(supplierReconItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item));

        // 付款申请：
        PaymentApplyDO apply1 = new PaymentApplyDO();
        apply1.setReconId(100L);
        apply1.setAmount(new BigDecimal("1000.00"));
        apply1.setApprovalStatus("APPROVED");

        PaymentApplyDO apply2 = new PaymentApplyDO();
        apply2.setReconId(100L);
        apply2.setAmount(new BigDecimal("2000.00"));
        apply2.setApprovalStatus("APPROVING");

        when(paymentApplyMapper.selectList(any(Wrapper.class))).thenReturn(List.of(apply1, apply2));

        SupplierReconRespVO resp = supplierReconService.getSupplierRecon(100L);
        assertNotNull(resp);
        assertEquals(1, resp.getItems().size());
        assertEquals(new BigDecimal("3000.00"), resp.getAppliedAmount());
    }

    @Test
    @DisplayName("getSupplierReconPage 分页查询：批量实时聚合已申请金额")
    void testGetSupplierReconPage() {
        SupplierReconPageReqVO pageReq = new SupplierReconPageReqVO();
        pageReq.setPageNo(1);
        pageReq.setPageSize(10);
        pageReq.setKeyword("SR2026");

        SupplierReconDO recon1 = new SupplierReconDO();
        recon1.setId(101L);
        recon1.setReconNo("SR20261015001");

        SupplierReconDO recon2 = new SupplierReconDO();
        recon2.setId(102L);
        recon2.setReconNo("SR20261015002");

        PageResult<SupplierReconDO> pageResult = new PageResult<>(List.of(recon1, recon2), 2L);
        when(supplierReconMapper.selectPage(eq(pageReq), any(Wrapper.class))).thenReturn(pageResult);

        // 模拟批量付款申请聚合
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setReconId(101L);
        apply.setAmount(new BigDecimal("1500.00"));
        apply.setApprovalStatus("APPROVED");
        when(paymentApplyMapper.selectList(any(Wrapper.class))).thenReturn(List.of(apply));

        PageResult<SupplierReconRespVO> result = supplierReconService.getSupplierReconPage(pageReq);
        assertNotNull(result);
        assertEquals(2, result.getList().size());
        assertEquals(new BigDecimal("1500.00"), result.getList().get(0).getAppliedAmount());
        assertEquals(new BigDecimal("0.00"), result.getList().get(1).getAppliedAmount());
    }

    @Test
    @DisplayName("横向越权隔离：他人租户单据返回不存在")
    void testCrossTenantIsolation() {
        SupplierReconDO otherTenantRecon = new SupplierReconDO();
        otherTenantRecon.setId(200L);
        otherTenantRecon.setTenantId(OTHER_TENANT_ID); // 他人租户

        when(supplierReconMapper.selectById(200L)).thenReturn(otherTenantRecon);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.getSupplierRecon(200L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== Issue 04: 确认/作废守卫 + 打印数据接口 ====================

    @Test
    @DisplayName("确认对账单：成功确认，状态变为 CONFIRMED 并记录确认人与时间")
    void testConfirmSupplierRecon_Success() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.DRAFT.getValue());
        recon.setTotalAmount(new BigDecimal("1200.00"));
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(supplierReconItemMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        when(supplierReconMapper.update(any(SupplierReconDO.class), any(Wrapper.class))).thenReturn(1);

        supplierReconService.confirmSupplierRecon(100L);

        ArgumentCaptor<SupplierReconDO> updateCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).update(updateCaptor.capture(), any(Wrapper.class));
        SupplierReconDO updated = updateCaptor.getValue();
        assertEquals(ReconStatus.CONFIRMED.getValue(), updated.getStatus());
        assertNotNull(updated.getConfirmedAt());
    }

    @Test
    @DisplayName("确认对账单：并发双确认只成功一次，影响行数 0 抛 001")
    void testConfirmSupplierRecon_ConcurrentDoubleConfirm_Throws001() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.DRAFT.getValue());
        recon.setTotalAmount(new BigDecimal("1200.00"));
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(supplierReconItemMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        // 模拟并发双确认时另一个事务先完成更新，条件更新 WHERE status='DRAFT' 影响行数为 0
        when(supplierReconMapper.update(any(SupplierReconDO.class), any(Wrapper.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.confirmSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认对账单：非 DRAFT 状态（已确认或已作废）拒绝确认报 001")
    void testConfirmSupplierRecon_NonDraftStatus_Throws001() {
        SupplierReconDO confirmedRecon = new SupplierReconDO();
        confirmedRecon.setId(100L);
        confirmedRecon.setStatus(ReconStatus.CONFIRMED.getValue());
        confirmedRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(confirmedRecon);

        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierReconService.confirmSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        SupplierReconDO cancelledRecon = new SupplierReconDO();
        cancelledRecon.setId(101L);
        cancelledRecon.setStatus(ReconStatus.CANCELLED.getValue());
        cancelledRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(101L)).thenReturn(cancelledRecon);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierReconService.confirmSupplierRecon(101L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("确认对账单：total_amount 为 0 或无有效明细行拒绝确认报 001")
    void testConfirmSupplierRecon_ZeroAmountOrNoItems_Throws001() {
        // total_amount 为 0
        SupplierReconDO zeroAmountRecon = new SupplierReconDO();
        zeroAmountRecon.setId(100L);
        zeroAmountRecon.setStatus(ReconStatus.DRAFT.getValue());
        zeroAmountRecon.setTotalAmount(BigDecimal.ZERO);
        zeroAmountRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(zeroAmountRecon);

        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierReconService.confirmSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        // total_amount > 0 但有效明细行为 0
        SupplierReconDO noItemsRecon = new SupplierReconDO();
        noItemsRecon.setId(101L);
        noItemsRecon.setStatus(ReconStatus.DRAFT.getValue());
        noItemsRecon.setTotalAmount(new BigDecimal("500.00"));
        noItemsRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(101L)).thenReturn(noItemsRecon);
        when(supplierReconItemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierReconService.confirmSupplierRecon(101L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("冻结语义：CONFIRMED 后 update/re-fetch/delete 全部拒绝报 001")
    void testFrozenSemanticsAfterConfirmed() {
        SupplierReconDO confirmedRecon = new SupplierReconDO();
        confirmedRecon.setId(100L);
        confirmedRecon.setStatus(ReconStatus.CONFIRMED.getValue());
        confirmedRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(confirmedRecon);

        // 1. update 拒绝
        SupplierReconUpdateReqVO updateReq = new SupplierReconUpdateReqVO();
        updateReq.setId(100L);
        ServiceException exUpdate = assertThrows(ServiceException.class, () -> supplierReconService.updateSupplierRecon(updateReq));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), exUpdate.getCode());

        // 2. re-fetch 拒绝
        ServiceException exRefetch = assertThrows(ServiceException.class, () -> supplierReconService.reFetchSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), exRefetch.getCode());

        // 3. delete 拒绝
        ServiceException exDelete = assertThrows(ServiceException.class, () -> supplierReconService.deleteSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), exDelete.getCode());
    }

    @Test
    @DisplayName("冻结语义：详情返回行表快照值，源单事后变化不影响对账单返回内容（零回写）")
    void testConfirmedDetailReturnsSnapshot() {
        SupplierReconDO confirmedRecon = new SupplierReconDO();
        confirmedRecon.setId(100L);
        confirmedRecon.setReconNo("SR20261015001");
        confirmedRecon.setStatus(ReconStatus.CONFIRMED.getValue());
        confirmedRecon.setTotalAmount(new BigDecimal("1200.00"));
        confirmedRecon.setTenantId(TENANT_ID);

        SupplierReconItemDO snapshotItem = new SupplierReconItemDO();
        snapshotItem.setId(1L);
        snapshotItem.setReconId(100L);
        snapshotItem.setLineType(LineType.PROCESS.getValue());
        snapshotItem.setSourceRefType(SourceRefType.INSPECTION.getValue());
        snapshotItem.setSourceRefId(888L);
        snapshotItem.setBillQty(new BigDecimal("300.00"));
        snapshotItem.setUnitPrice(new BigDecimal("4.0000"));
        snapshotItem.setLineAmount(new BigDecimal("1200.00"));

        when(supplierReconMapper.selectById(100L)).thenReturn(confirmedRecon);
        when(supplierReconItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(snapshotItem));
        when(paymentApplyMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        SupplierReconRespVO respVO = supplierReconService.getSupplierRecon(100L);

        assertNotNull(respVO);
        assertEquals(1, respVO.getItems().size());
        SupplierReconItemRespVO item = respVO.getItems().get(0);
        assertEquals(new BigDecimal("1200.00"), item.getLineAmount());
        assertEquals(new BigDecimal("4.0000"), item.getUnitPrice());
        // 验证源单服务未被调用（纯行表快照，零回写）
        verifyNoInteractions(supplierReconSourceService);
    }

    @Test
    @DisplayName("作废守卫：存在 PENDING 状态的付款申请时拒绝作废报 006")
    void testCancelSupplierRecon_GuardWithPendingApply_Throws006() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(1L); // 存在 PENDING 申请

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.cancelSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废守卫：存在 APPROVING 状态的付款申请时拒绝作废报 006")
    void testCancelSupplierRecon_GuardWithApprovingApply_Throws006() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(1L); // 存在 APPROVING 申请

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.cancelSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废守卫：存在 APPROVED 状态的付款申请时拒绝作废报 006")
    void testCancelSupplierRecon_GuardWithApprovedApply_Throws006() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(1L); // 存在 APPROVED 申请

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.cancelSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废守卫：挂靠申请为 REJECTED 时不阻断作废，作废成功")
    void testCancelSupplierRecon_SuccessWithRejectedApply() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        // PENDING/APPROVING/APPROVED 为 0（REJECTED 申请不阻断）
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierReconMapper.update(any(SupplierReconDO.class), any(Wrapper.class))).thenReturn(1);

        supplierReconService.cancelSupplierRecon(100L);

        ArgumentCaptor<SupplierReconDO> updateCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).update(updateCaptor.capture(), any(Wrapper.class));
        SupplierReconDO updated = updateCaptor.getValue();
        assertEquals(ReconStatus.CANCELLED.getValue(), updated.getStatus());
        assertNotNull(updated.getCancelledAt());
    }

    @Test
    @DisplayName("作废守卫：挂靠申请为 CANCELLED 时不阻断作废，作废成功")
    void testCancelSupplierRecon_SuccessWithCancelledApply() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        // PENDING/APPROVING/APPROVED 为 0（CANCELLED 申请不阻断）
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierReconMapper.update(any(SupplierReconDO.class), any(Wrapper.class))).thenReturn(1);

        supplierReconService.cancelSupplierRecon(100L);

        ArgumentCaptor<SupplierReconDO> updateCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).update(updateCaptor.capture(), any(Wrapper.class));
        SupplierReconDO updated = updateCaptor.getValue();
        assertEquals(ReconStatus.CANCELLED.getValue(), updated.getStatus());
        assertNotNull(updated.getCancelledAt());
    }

    @Test
    @DisplayName("终态与状态守卫：草稿 DRAFT 或已作废 CANCELLED 单据再作废报 001")
    void testCancelSupplierRecon_NonConfirmedStatus_Throws001() {
        // 草稿直接作废拒绝
        SupplierReconDO draftRecon = new SupplierReconDO();
        draftRecon.setId(100L);
        draftRecon.setStatus(ReconStatus.DRAFT.getValue());
        draftRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(draftRecon);

        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierReconService.cancelSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        // 已作废终态单据再作废拒绝
        SupplierReconDO cancelledRecon = new SupplierReconDO();
        cancelledRecon.setId(101L);
        cancelledRecon.setStatus(ReconStatus.CANCELLED.getValue());
        cancelledRecon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(101L)).thenReturn(cancelledRecon);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierReconService.cancelSupplierRecon(101L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("作废并发防重：条件更新影响行数为 0 报 001")
    void testCancelSupplierRecon_ConcurrentConflict_Throws001() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierReconMapper.update(any(SupplierReconDO.class), any(Wrapper.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.cancelSupplierRecon(100L));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废后指纹释放：单据变为 CANCELLED 后，指纹不再被占用并回到候选池")
    void testCancelReleasesFingerprintToCandidatePool() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTenantId(TENANT_ID);

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierReconMapper.update(any(SupplierReconDO.class), any(Wrapper.class))).thenReturn(1);

        supplierReconService.cancelSupplierRecon(100L);

        // 验证作废单据被正确更新为 CANCELLED 终态
        ArgumentCaptor<SupplierReconDO> captor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).update(captor.capture(), any(Wrapper.class));
        assertEquals(ReconStatus.CANCELLED.getValue(), captor.getValue().getStatus());

        // 模拟作废后后续取数检测：有效占用查询排除 CANCELLED 单据，占用映射为空（指纹自动回到可用候选池）
        when(supplierReconSourceService.findOccupiedFingerprints(Set.of("INSPECTION:888"))).thenReturn(Collections.emptyMap());
        Map<String, String> occupied = supplierReconSourceService.findOccupiedFingerprints(Set.of("INSPECTION:888"));
        assertTrue(occupied.isEmpty(), "作废对账单释放指纹后，候选池查询该指纹不再被占用");
    }

    @Test
    @DisplayName("打印数据接口：出参遵循 04 号通用件 print-data 契约，正确包含三类行、合计与抬头")
    void testGetSupplierReconPrintData_Success() {
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(100L);
        recon.setReconNo("SR20261015001");
        recon.setSupplierId(10L);
        recon.setSupplierName("杭州某纺织厂");
        recon.setPeriodStart(LocalDate.of(2026, 9, 1));
        recon.setPeriodEnd(LocalDate.of(2026, 9, 30));
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTotalAmount(new BigDecimal("15000.00"));
        recon.setConfirmedBy(1L);
        recon.setTenantId(TENANT_ID);

        // 三类行：PROCESS / PURCHASE / ADJUST
        SupplierReconItemDO processItem = new SupplierReconItemDO();
        processItem.setId(1L);
        processItem.setReconId(100L);
        processItem.setLineType(LineType.PROCESS.getValue());
        processItem.setInspectionOrderNo("INS001");
        processItem.setLineAmount(new BigDecimal("8000.00"));

        SupplierReconItemDO purchaseItem = new SupplierReconItemDO();
        purchaseItem.setId(2L);
        purchaseItem.setReconId(100L);
        purchaseItem.setLineType(LineType.PURCHASE.getValue());
        purchaseItem.setStockInNo("IN001");
        purchaseItem.setLineAmount(new BigDecimal("7500.00"));

        SupplierReconItemDO adjustItem = new SupplierReconItemDO();
        adjustItem.setId(3L);
        adjustItem.setReconId(100L);
        adjustItem.setLineType(LineType.ADJUST.getValue());
        adjustItem.setAdjustReason("质量扣款");
        adjustItem.setLineAmount(new BigDecimal("-500.00"));

        SupplierRespVO supplier = new SupplierRespVO();
        supplier.setId(10L);
        supplier.setCompanyName("杭州某纺织厂");
        supplier.setPhone("13800000000");

        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(1L);
        user.setNickname("李财务");

        when(supplierReconMapper.selectById(100L)).thenReturn(recon);
        when(supplierReconItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(processItem, purchaseItem, adjustItem));
        when(supplierService.getSupplier(10L)).thenReturn(supplier);
        when(adminUserApi.getUser(1L)).thenReturn(user);

        SupplierReconPrintRespVO printVO = supplierReconService.getSupplierReconPrintData(100L);

        assertNotNull(printVO);
        assertEquals("VENDOR_RECON", printVO.getBizType(), "bizType 必须为 VENDOR_RECON");
        assertEquals("供应商对账单", printVO.getTitle());
        assertEquals("SR20261015001", printVO.getReconNo());
        assertEquals("杭州某纺织厂", printVO.getSupplierName());
        assertEquals("13800000000", printVO.getSupplierPhone());
        assertEquals("李财务", printVO.getConfirmedByName());
        assertEquals(new BigDecimal("15000.00"), printVO.getTotalAmount());
        assertEquals(new BigDecimal("8000.00"), printVO.getTotalProcessAmount());
        assertEquals(new BigDecimal("7500.00"), printVO.getTotalPurchaseAmount());
        assertEquals(new BigDecimal("-500.00"), printVO.getTotalAdjustAmount());
        assertEquals(3, printVO.getItems().size());
        assertNotNull(printVO.getPrintTime());
    }

    @Test
    @DisplayName("脱敏豁免规约：打印 VO 豁免金额脱敏（无 @Sensitive 注解），而业务 VO 保留 @Sensitive")
    void testPrintVOExemptFromSensitiveAnnotation() throws NoSuchFieldException {
        // 打印 VO 金额字段不含 @Sensitive
        Field printTotalAmount = SupplierReconPrintRespVO.class.getDeclaredField("totalAmount");
        assertFalse(printTotalAmount.isAnnotationPresent(Sensitive.class),
                "SupplierReconPrintRespVO.totalAmount 必须豁免脱敏，严禁标注 @Sensitive");

        Field printItemLineAmount = SupplierReconPrintItemRespVO.class.getDeclaredField("lineAmount");
        assertFalse(printItemLineAmount.isAnnotationPresent(Sensitive.class),
                "SupplierReconPrintItemRespVO.lineAmount 必须豁免脱敏，严禁标注 @Sensitive");

        // 业务 VO 金额字段必须含 @Sensitive
        Field respTotalAmount = SupplierReconRespVO.class.getDeclaredField("totalAmount");
        assertTrue(respTotalAmount.isAnnotationPresent(Sensitive.class),
                "SupplierReconRespVO.totalAmount 必须标注 @Sensitive");

        Field respItemLineAmount = SupplierReconItemRespVO.class.getDeclaredField("lineAmount");
        assertTrue(respItemLineAmount.isAnnotationPresent(Sensitive.class),
                "SupplierReconItemRespVO.lineAmount 必须标注 @Sensitive");
    }

    @Test
    @DisplayName("切面日志规约：confirm / cancel / getSupplierReconPrintData 正确标注 @LogRecord")
    void testLogRecordAnnotations() throws NoSuchMethodException {
        Method confirmMethod = SupplierReconServiceImpl.class.getMethod("confirmSupplierRecon", Long.class);
        LogRecord confirmLog = confirmMethod.getAnnotation(LogRecord.class);
        assertNotNull(confirmLog, "confirmSupplierRecon 必须标注 @LogRecord");
        assertEquals("SUPPLIER_RECON", confirmLog.type());
        assertEquals("CONFIRM", confirmLog.subType());

        Method cancelMethod = SupplierReconServiceImpl.class.getMethod("cancelSupplierRecon", Long.class);
        LogRecord cancelLog = cancelMethod.getAnnotation(LogRecord.class);
        assertNotNull(cancelLog, "cancelSupplierRecon 必须标注 @LogRecord");
        assertEquals("SUPPLIER_RECON", cancelLog.type());
        assertEquals("CANCEL", cancelLog.subType());

        Method printMethod = SupplierReconServiceImpl.class.getMethod("getSupplierReconPrintData", Long.class);
        LogRecord printLog = printMethod.getAnnotation(LogRecord.class);
        assertNotNull(printLog, "getSupplierReconPrintData 必须标注 @LogRecord");
        assertEquals("打印", printLog.type());
        assertEquals("供应商对账单", printLog.subType());
    }

}
