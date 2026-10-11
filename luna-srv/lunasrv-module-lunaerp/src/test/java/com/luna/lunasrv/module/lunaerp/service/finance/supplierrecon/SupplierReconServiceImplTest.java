package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.PricingBasisEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconLineTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconSourceRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link SupplierReconServiceImpl} 纯 Mockito 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SupplierReconServiceImplTest {

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierReconItemMapper supplierReconItemMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SupplierReconSourceService supplierReconSourceService;

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private SupplierReconServiceImpl supplierReconService;

    private static final Long SUPPLIER_ID = 1001L;
    private static final String SUPPLIER_NAME = "浙江织造有限公司";
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);

    private SupplierDO mockSupplier;

    @BeforeEach
    void setUp() {
        mockSupplier = new SupplierDO();
        mockSupplier.setId(SUPPLIER_ID);
        mockSupplier.setCompanyName(SUPPLIER_NAME);
    }

    /**
     * 1. 004 冲突检查：源明细已被其他有效对账单引用
     */
    @Test
    void createRecon_sourceItemReferencedByActiveRecon_throws004WithConflictReconNo() {
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(mockSupplier);

        // 候选池中为空（已被占用排除）
        when(supplierReconSourceService.getCandidateItems(SUPPLIER_ID, START, END))
                .thenReturn(new SupplierReconCandidatesRespVO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));

        // 请求提交了一条源明细
        SupplierReconItemSaveReqVO itemReq = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                .sourceRefId(500L)
                .inspectionOrderId(500L)
                .build();

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .items(Collections.singletonList(itemReq))
                .build();

        // 模拟已存在引用此指纹的对账单行
        SupplierReconItemDO conflictItem = SupplierReconItemDO.builder()
                .reconId(88L)
                .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                .sourceRefId(500L)
                .build();
        when(supplierReconItemMapper.selectListBySourceRef(SupplierReconSourceRefTypeEnum.INSPECTION.getValue(), 500L, null))
                .thenReturn(Collections.singletonList(conflictItem));

        // 冲突主表处于 DRAFT
        SupplierReconDO conflictRecon = SupplierReconDO.builder()
                .id(88L)
                .reconNo("SR20261011001")
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .build();
        when(supplierReconMapper.selectById(88L)).thenReturn(conflictRecon);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createRecon(req));
        assertEquals(1_030_022_004, ex.getCode());
        assertTrue(ex.getMessage().contains("SR20261011001"));
    }

    /**
     * 2. 服务端金额权威重算覆盖前端入参
     */
    @Test
    void createRecon_serverAuthoritativeRecalculation_overridesInputLineAmount() {
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(mockSupplier);

        // 构造 PROCESS 候选
        SupplierReconCandidateItemRespVO candProcess = SupplierReconCandidateItemRespVO.builder()
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                .sourceRefId(101L)
                .qtyKg(new BigDecimal("50.00"))
                .qtyM(new BigDecimal("100.00"))
                .pricingBasis(PricingBasisEnum.METER.getValue())
                .billQty(new BigDecimal("100.00"))
                .build();

        // 构造 PURCHASE 候选
        SupplierReconCandidateItemRespVO candPurchase = SupplierReconCandidateItemRespVO.builder()
                .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                .sourceRefId(201L)
                .sourceRefItemId(2011L)
                .billQty(new BigDecimal("20.00"))
                .build();

        when(supplierReconSourceService.getCandidateItems(SUPPLIER_ID, START, END))
                .thenReturn(new SupplierReconCandidatesRespVO(
                        Arrays.asList(candProcess, candPurchase),
                        Collections.singletonList(candProcess),
                        Collections.singletonList(candPurchase)));

        // 用户提交的行携带恶意/错误的前端金额
        SupplierReconItemSaveReqVO item1 = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                .sourceRefId(101L)
                .pricingBasis(PricingBasisEnum.METER.getValue())
                .unitPrice(new BigDecimal("2.50"))
                .lineAmount(new BigDecimal("99999.00")) // 前端乱传
                .build();

        SupplierReconItemSaveReqVO item2 = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                .sourceRefId(201L)
                .sourceRefItemId(2011L)
                .unitPrice(new BigDecimal("10.00"))
                .lineAmount(new BigDecimal("88888.00")) // 前端乱传
                .build();

        SupplierReconItemSaveReqVO item3 = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                .lineAmount(new BigDecimal("-50.00"))
                .adjustReason("质量赔付扣款")
                .build();

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .items(Arrays.asList(item1, item2, item3))
                .build();

        when(supplierReconMapper.selectListByReconNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(supplierReconMapper.selectByReconNoIncludingDeleted(anyString())).thenReturn(null);

        doAnswer(invocation -> {
            SupplierReconDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
            return 1;
        }).when(supplierReconMapper).insert(any(SupplierReconDO.class));

        Long id = supplierReconService.createRecon(req);
        assertNotNull(id);

        ArgumentCaptor<SupplierReconDO> reconCaptor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).insert(reconCaptor.capture());
        SupplierReconDO savedRecon = reconCaptor.getValue();

        // 100 * 2.50 = 250.00; 20 * 10 = 200.00; -50.00 -> total = 400.00
        assertEquals(new BigDecimal("400.00"), savedRecon.getTotalAmount(), "总金额应为服务端重算值 400.00");

        ArgumentCaptor<SupplierReconItemDO> itemCaptor = ArgumentCaptor.forClass(SupplierReconItemDO.class);
        verify(supplierReconItemMapper, times(3)).insert(itemCaptor.capture());
        List<SupplierReconItemDO> savedItems = itemCaptor.getAllValues();

        assertEquals(new BigDecimal("250.00"), savedItems.get(0).getLineAmount());
        assertEquals(new BigDecimal("200.00"), savedItems.get(1).getLineAmount());
        assertEquals(new BigDecimal("-50.00"), savedItems.get(2).getLineAmount());
    }

    /**
     * 3. 005 调整项行不合法：金额为 0 或缺备注
     */
    @Test
    void createRecon_adjustLineAmountZero_throws005() {
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(mockSupplier);

        SupplierReconItemSaveReqVO item = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                .lineAmount(BigDecimal.ZERO)
                .adjustReason("原因")
                .build();

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createRecon(req));
        assertEquals(1_030_022_005, ex.getCode());
    }

    @Test
    void createRecon_adjustLineMissingReason_throws005() {
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(mockSupplier);

        SupplierReconItemSaveReqVO item = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                .lineAmount(new BigDecimal("100.00"))
                .adjustReason("   ") // 空白备注
                .build();

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createRecon(req));
        assertEquals(1_030_022_005, ex.getCode());
    }

    /**
     * 4. 003 对账区间内无可对账明细
     */
    @Test
    void createRecon_emptyItems_throws003() {
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(mockSupplier);

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .items(Collections.emptyList())
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.createRecon(req));
        assertEquals(1_030_022_003, ex.getCode());
    }

    /**
     * 5. recon_no 按天取号与撞号重试
     */
    @Test
    void createRecon_reconNoDailySequenceAndRetry_generatesNextSequence() {
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(mockSupplier);

        SupplierReconCandidateItemRespVO cand = SupplierReconCandidateItemRespVO.builder()
                .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                .sourceRefId(301L)
                .sourceRefItemId(3011L)
                .billQty(new BigDecimal("10.00"))
                .build();
        when(supplierReconSourceService.getCandidateItems(SUPPLIER_ID, START, END))
                .thenReturn(new SupplierReconCandidatesRespVO(
                        Collections.singletonList(cand), Collections.emptyList(), Collections.singletonList(cand)));

        SupplierReconItemSaveReqVO item = SupplierReconItemSaveReqVO.builder()
                .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                .sourceRefId(301L)
                .sourceRefItemId(3011L)
                .unitPrice(new BigDecimal("5.00"))
                .build();

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .items(Collections.singletonList(item))
                .build();

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String existing1 = "SR" + dateStr + "001";
        String existing2 = "SR" + dateStr + "002";
        String expected = "SR" + dateStr + "003";

        // 模拟当天已有 001、002
        when(supplierReconMapper.selectListByReconNoPrefixIncludingDeleted("SR" + dateStr))
                .thenReturn(Arrays.asList(
                        SupplierReconDO.builder().reconNo(existing1).build(),
                        SupplierReconDO.builder().reconNo(existing2).build()
                ));
        when(supplierReconMapper.selectByReconNoIncludingDeleted(expected)).thenReturn(null);

        supplierReconService.createRecon(req);

        ArgumentCaptor<SupplierReconDO> captor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).insert(captor.capture());
        assertEquals(expected, captor.getValue().getReconNo(), "单号应顺延为 003");
    }

    /**
     * 6. 001 非草稿状态更新与删除被拒绝
     */
    @Test
    void updateRecon_statusNotDraft_throws001() {
        SupplierReconDO confirmedRecon = SupplierReconDO.builder()
                .id(200L)
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .build();
        when(supplierReconMapper.selectById(200L)).thenReturn(confirmedRecon);

        SupplierReconSaveReqVO req = SupplierReconSaveReqVO.builder()
                .id(200L)
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.updateRecon(req));
        assertEquals(1_030_022_001, ex.getCode());
    }

    @Test
    void deleteRecon_statusNotDraft_throws001() {
        SupplierReconDO cancelledRecon = SupplierReconDO.builder()
                .id(201L)
                .status(SupplierReconStatusEnum.CANCELLED.getValue())
                .build();
        when(supplierReconMapper.selectById(201L)).thenReturn(cancelledRecon);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.deleteRecon(201L));
        assertEquals(1_030_022_001, ex.getCode());
    }

    @Test
    void reFetchRecon_statusNotDraft_throws001() {
        SupplierReconDO confirmedRecon = SupplierReconDO.builder()
                .id(202L)
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .build();
        when(supplierReconMapper.selectById(202L)).thenReturn(confirmedRecon);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.reFetchRecon(202L));
        assertEquals(1_030_022_001, ex.getCode());
    }

    /**
     * 7. 横向越权（不同租户返回不存在）
     */
    @Test
    void getRecon_differentTenantNotExists_throws000() {
        when(supplierReconMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.getRecon(9999L));
        assertEquals(1_030_022_000, ex.getCode());
    }

    /**
     * 8. 已申请金额实时聚合口径（排除 REJECTED 和 CANCELLED）
     */
    @Test
    void getRecon_appliedAmountAggregation_excludesRejectedAndCancelled() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(300L)
                .reconNo("SR20261011005")
                .supplierId(SUPPLIER_ID)
                .supplierName(SUPPLIER_NAME)
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .totalAmount(new BigDecimal("10000.00"))
                .build();
        when(supplierReconMapper.selectById(300L)).thenReturn(recon);
        when(supplierReconItemMapper.selectListByReconId(300L)).thenReturn(Collections.emptyList());

        // 模拟付款申请：通过 5000，审批中 3000
        PaymentApplyDO apply1 = PaymentApplyDO.builder().reconId(300L).amount(new BigDecimal("5000.00")).approvalStatus("APPROVED").build();
        PaymentApplyDO apply2 = PaymentApplyDO.builder().reconId(300L).amount(new BigDecimal("3000.00")).approvalStatus("APPROVING").build();

        when(paymentApplyMapper.selectList(any())).thenReturn(Arrays.asList(apply1, apply2));

        SupplierReconRespVO respVO = supplierReconService.getRecon(300L);
        assertNotNull(respVO);
        assertEquals(new BigDecimal("8000.00"), respVO.getAppliedAmount(), "已申请金额应为 5000 + 3000 = 8000.00");
    }

    /**
     * 9. 草稿重取：保留现有 ADJUST 项，替换 PROCESS/PURCHASE 行
     */
    @Test
    void reFetchRecon_keepsAdjustItemsAndReplacesSourceItems() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(400L)
                .reconNo("SR20261011009")
                .supplierId(SUPPLIER_ID)
                .periodStart(START)
                .periodEnd(END)
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(new BigDecimal("500.00"))
                .build();
        when(supplierReconMapper.selectById(400L)).thenReturn(recon);

        SupplierReconItemDO oldProcess = SupplierReconItemDO.builder()
                .id(1L)
                .reconId(400L)
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .lineAmount(new BigDecimal("300.00"))
                .build();
        SupplierReconItemDO oldAdjust = SupplierReconItemDO.builder()
                .id(2L)
                .reconId(400L)
                .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                .lineAmount(new BigDecimal("-50.00"))
                .adjustReason("扣款")
                .build();
        when(supplierReconItemMapper.selectListByReconId(400L)).thenReturn(Arrays.asList(oldProcess, oldAdjust));

        // 新候选
        SupplierReconCandidateItemRespVO newCand = SupplierReconCandidateItemRespVO.builder()
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                .sourceRefId(600L)
                .billQty(new BigDecimal("200.00"))
                .unitPrice(new BigDecimal("2.00"))
                .lineAmount(new BigDecimal("400.00"))
                .build();
        when(supplierReconSourceService.getCandidateItems(SUPPLIER_ID, START, END, 400L))
                .thenReturn(new SupplierReconCandidatesRespVO(Collections.singletonList(newCand), Collections.singletonList(newCand), Collections.emptyList()));

        supplierReconService.reFetchRecon(400L);

        // 验证旧 PROCESS 行被删除
        verify(supplierReconItemMapper).deleteById(1L);
        // 验证 ADJUST 行未被删除
        verify(supplierReconItemMapper, never()).deleteById(2L);

        // 验证新行插入
        verify(supplierReconItemMapper).insert(any(SupplierReconItemDO.class));

        // 验证更新主表总额：-50.00 + 400.00 = 350.00
        ArgumentCaptor<SupplierReconDO> captor = ArgumentCaptor.forClass(SupplierReconDO.class);
        verify(supplierReconMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("350.00"), captor.getValue().getTotalAmount());
    }

    // ==================== Issue 04: 对账单确认/作废守卫 + 打印数据接口 ====================

    /**
     * 10. 并发双确认：第一次成功，第二次条件更新影响 0 行报 001
     */
    @Test
    void confirmRecon_concurrentDoubleConfirm_secondFailsWith001() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(100L)
                .reconNo("SR20261011100")
                .supplierId(SUPPLIER_ID)
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(new BigDecimal("1250.00"))
                .build();
        when(supplierReconMapper.selectById(100L)).thenReturn(recon);

        SupplierReconItemDO item = SupplierReconItemDO.builder()
                .id(1L)
                .reconId(100L)
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .lineAmount(new BigDecimal("1250.00"))
                .build();
        when(supplierReconItemMapper.selectListByReconId(100L)).thenReturn(Collections.singletonList(item));

        // 第一次成功：影响 1 行
        when(supplierReconMapper.updateToConfirmed(eq(100L), any(), any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> supplierReconService.confirmRecon(100L));

        // 第二次（并发冲突，已被确认导致影响 0 行）：报 001
        when(supplierReconMapper.updateToConfirmed(eq(100L), any(), any(), any())).thenReturn(0);
        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.confirmRecon(100L));
        assertEquals(1_030_022_001, ex.getCode());
    }

    /**
     * 11. 零金额或空明细对账单拒绝确认报 001
     */
    @Test
    void confirmRecon_zeroAmountOrEmptyItems_rejectedWith001() {
        // 金额为 0
        SupplierReconDO zeroRecon = SupplierReconDO.builder()
                .id(101L)
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(BigDecimal.ZERO)
                .build();
        when(supplierReconMapper.selectById(101L)).thenReturn(zeroRecon);
        when(supplierReconItemMapper.selectListByReconId(101L))
                .thenReturn(Collections.singletonList(SupplierReconItemDO.builder().id(1L).reconId(101L).build()));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierReconService.confirmRecon(101L));
        assertEquals(1_030_022_001, ex1.getCode());

        // 明细为空
        SupplierReconDO emptyItemsRecon = SupplierReconDO.builder()
                .id(102L)
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(supplierReconMapper.selectById(102L)).thenReturn(emptyItemsRecon);
        when(supplierReconItemMapper.selectListByReconId(102L)).thenReturn(Collections.emptyList());

        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierReconService.confirmRecon(102L));
        assertEquals(1_030_022_001, ex2.getCode());

        // 金额为 null
        SupplierReconDO nullAmountRecon = SupplierReconDO.builder()
                .id(103L)
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(null)
                .build();
        when(supplierReconMapper.selectById(103L)).thenReturn(nullAmountRecon);
        when(supplierReconItemMapper.selectListByReconId(103L))
                .thenReturn(Collections.singletonList(SupplierReconItemDO.builder().id(2L).reconId(103L).build()));

        ServiceException ex3 = assertThrows(ServiceException.class, () -> supplierReconService.confirmRecon(103L));
        assertEquals(1_030_022_001, ex3.getCode());
    }

    /**
     * 12. 确认后冻结语义：已确认单据 update/reFetch/delete 以及再次 confirm 全部报 001
     */
    @Test
    void confirmRecon_freezesRecon_subsequentOperationsBlockedWith001() {
        SupplierReconDO confirmedRecon = SupplierReconDO.builder()
                .id(104L)
                .reconNo("SR20261011104")
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .totalAmount(new BigDecimal("5000.00"))
                .build();
        when(supplierReconMapper.selectById(104L)).thenReturn(confirmedRecon);

        // 再次确认报 001
        ServiceException exConfirm = assertThrows(ServiceException.class, () -> supplierReconService.confirmRecon(104L));
        assertEquals(1_030_022_001, exConfirm.getCode());

        // update 报 001
        SupplierReconSaveReqVO updateReq = SupplierReconSaveReqVO.builder().id(104L).build();
        ServiceException exUpdate = assertThrows(ServiceException.class, () -> supplierReconService.updateRecon(updateReq));
        assertEquals(1_030_022_001, exUpdate.getCode());

        // re-fetch 报 001
        ServiceException exReFetch = assertThrows(ServiceException.class, () -> supplierReconService.reFetchRecon(104L));
        assertEquals(1_030_022_001, exReFetch.getCode());

        // delete 报 001
        ServiceException exDelete = assertThrows(ServiceException.class, () -> supplierReconService.deleteRecon(104L));
        assertEquals(1_030_022_001, exDelete.getCode());
    }

    /**
     * 13. 作废守卫状态矩阵：
     * - PENDING -> 报 006
     * - APPROVING -> 报 006
     * - APPROVED -> 报 006
     * - REJECTED -> 成功
     * - CANCELLED -> 成功
     * - 无挂靠申请 -> 成功
     */
    @Test
    void cancelRecon_paymentApplyStatusMatrix_guardsCorrectly() {
        SupplierReconDO confirmedRecon = SupplierReconDO.builder()
                .id(200L)
                .reconNo("SR20261011200")
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .totalAmount(new BigDecimal("6000.00"))
                .build();
        when(supplierReconMapper.selectById(200L)).thenReturn(confirmedRecon);

        // 1. PENDING -> 报 006
        PaymentApplyDO applyPending = PaymentApplyDO.builder().id(1L).reconId(200L).approvalStatus("PENDING").build();
        when(paymentApplyMapper.selectListByReconId(200L)).thenReturn(Collections.singletonList(applyPending));
        ServiceException exPending = assertThrows(ServiceException.class, () -> supplierReconService.cancelRecon(200L));
        assertEquals(1_030_022_006, exPending.getCode());

        // 2. APPROVING -> 报 006
        PaymentApplyDO applyApproving = PaymentApplyDO.builder().id(2L).reconId(200L).approvalStatus("APPROVING").build();
        when(paymentApplyMapper.selectListByReconId(200L)).thenReturn(Collections.singletonList(applyApproving));
        ServiceException exApproving = assertThrows(ServiceException.class, () -> supplierReconService.cancelRecon(200L));
        assertEquals(1_030_022_006, exApproving.getCode());

        // 3. APPROVED -> 报 006
        PaymentApplyDO applyApproved = PaymentApplyDO.builder().id(3L).reconId(200L).approvalStatus("APPROVED").build();
        when(paymentApplyMapper.selectListByReconId(200L)).thenReturn(Collections.singletonList(applyApproved));
        ServiceException exApproved = assertThrows(ServiceException.class, () -> supplierReconService.cancelRecon(200L));
        assertEquals(1_030_022_006, exApproved.getCode());

        // 4. REJECTED -> 成功作废
        PaymentApplyDO applyRejected = PaymentApplyDO.builder().id(4L).reconId(200L).approvalStatus("REJECTED").build();
        when(paymentApplyMapper.selectListByReconId(200L)).thenReturn(Collections.singletonList(applyRejected));
        when(supplierReconMapper.updateToCancelled(eq(200L), any(), any(), any())).thenReturn(1);
        assertDoesNotThrow(() -> supplierReconService.cancelRecon(200L));

        // 5. CANCELLED -> 成功作废
        PaymentApplyDO applyCancelled = PaymentApplyDO.builder().id(5L).reconId(200L).approvalStatus("CANCELLED").build();
        when(paymentApplyMapper.selectListByReconId(200L)).thenReturn(Collections.singletonList(applyCancelled));
        assertDoesNotThrow(() -> supplierReconService.cancelRecon(200L));

        // 6. 无挂靠申请 -> 成功作废
        when(paymentApplyMapper.selectListByReconId(200L)).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> supplierReconService.cancelRecon(200L));
    }

    /**
     * 14. 作废单据并发或行数为0报 001
     */
    @Test
    void cancelRecon_concurrencyZeroRowsAffected_throws001() {
        SupplierReconDO confirmedRecon = SupplierReconDO.builder()
                .id(205L)
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .build();
        when(supplierReconMapper.selectById(205L)).thenReturn(confirmedRecon);
        when(paymentApplyMapper.selectListByReconId(205L)).thenReturn(Collections.emptyList());
        when(supplierReconMapper.updateToCancelled(eq(205L), any(), any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.cancelRecon(205L));
        assertEquals(1_030_022_001, ex.getCode());
    }

    /**
     * 15. 终态 CANCELLED 单据不可再次确认或作废（报 001）
     */
    @Test
    void cancelledRecon_cannotBeConfirmedOrCancelledAgain_throws001() {
        SupplierReconDO cancelledRecon = SupplierReconDO.builder()
                .id(206L)
                .status(SupplierReconStatusEnum.CANCELLED.getValue())
                .build();
        when(supplierReconMapper.selectById(206L)).thenReturn(cancelledRecon);

        ServiceException exConfirm = assertThrows(ServiceException.class, () -> supplierReconService.confirmRecon(206L));
        assertEquals(1_030_022_001, exConfirm.getCode());

        ServiceException exCancel = assertThrows(ServiceException.class, () -> supplierReconService.cancelRecon(206L));
        assertEquals(1_030_022_001, exCancel.getCode());
    }

    /**
     * 16. 作废对账单释放指纹：验证 updateToCancelled 触发
     */
    @Test
    void cancelRecon_releasesFingerprints_verified() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(207L)
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .build();
        recon.setTenantId(1L);
        when(supplierReconMapper.selectById(207L)).thenReturn(recon);
        when(paymentApplyMapper.selectListByReconId(207L)).thenReturn(Collections.emptyList());
        when(supplierReconMapper.updateToCancelled(eq(207L), eq(1L), any(), any())).thenReturn(1);

        supplierReconService.cancelRecon(207L);

        verify(supplierReconMapper).updateToCancelled(eq(207L), eq(1L), any(), any());
    }

    /**
     * 17. 打印数据组装：抬头、供应商、区间、三类行、合计、确认人与时间、打印时间（豁免脱敏）
     */
    @Test
    void getPrintData_success_assemblesAllFieldsAndExemptFromSensitive() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(301L)
                .reconNo("SR20261011301")
                .supplierId(SUPPLIER_ID)
                .supplierName(SUPPLIER_NAME)
                .periodStart(START)
                .periodEnd(END)
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .totalAmount(new BigDecimal("14800.00"))
                .confirmedBy(88L)
                .confirmedAt(LocalDateTime.of(2026, 10, 11, 10, 0, 0))
                .remark("9月对账确认套打")
                .build();
        when(supplierReconMapper.selectById(301L)).thenReturn(recon);

        // Mock 用户 API 获取确认人姓名
        AdminUserRespDTO adminUser = new AdminUserRespDTO();
        adminUser.setId(88L);
        adminUser.setNickname("张主管");
        when(adminUserApi.getUser(88L)).thenReturn(adminUser);

        // 构造三类行：PROCESS、PURCHASE、ADJUST
        SupplierReconItemDO processItem = SupplierReconItemDO.builder()
                .id(1L)
                .reconId(301L)
                .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                .inspectionOrderId(101L)
                .inspectionOrderNo("INS-20260901")
                .inspectionType("GREIGE")
                .productCode("FAB-001")
                .productName("胚布A")
                .qtyKg(new BigDecimal("100.00"))
                .qtyM(new BigDecimal("500.00"))
                .pricingBasis(PricingBasisEnum.METER.getValue())
                .billQty(new BigDecimal("500.00"))
                .unitPrice(new BigDecimal("10.0000"))
                .lineAmount(new BigDecimal("5000.00"))
                .build();

        SupplierReconItemDO purchaseItem = SupplierReconItemDO.builder()
                .id(2L)
                .reconId(301L)
                .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                .stockInId(201L)
                .stockInNo("IN-20260902")
                .yarnOrderNo("YO-20260901")
                .productCode("YARN-32")
                .productName("32支全棉纱")
                .batchNo("B202609")
                .qtyKg(new BigDecimal("1000.00"))
                .pricingBasis(PricingBasisEnum.KG.getValue())
                .billQty(new BigDecimal("1000.00"))
                .unitPrice(new BigDecimal("10.0000"))
                .lineAmount(new BigDecimal("10000.00"))
                .build();

        SupplierReconItemDO adjustItem = SupplierReconItemDO.builder()
                .id(3L)
                .reconId(301L)
                .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                .lineAmount(new BigDecimal("-200.00"))
                .adjustReason("色差质量扣款")
                .remark("扣减200元")
                .build();

        when(supplierReconItemMapper.selectListByReconId(301L))
                .thenReturn(Arrays.asList(processItem, purchaseItem, adjustItem));

        SupplierReconPrintRespVO printData = supplierReconService.getPrintData(301L);

        assertNotNull(printData);
        assertEquals("供应商对账单", printData.getCompanyTitle());
        assertEquals(301L, printData.getId());
        assertEquals("SR20261011301", printData.getReconNo());
        assertEquals(SUPPLIER_ID, printData.getSupplierId());
        assertEquals(SUPPLIER_NAME, printData.getSupplierName());
        assertEquals(START, printData.getPeriodStart());
        assertEquals(END, printData.getPeriodEnd());
        assertEquals(SupplierReconStatusEnum.CONFIRMED.getValue(), printData.getStatus());
        assertEquals(new BigDecimal("14800.00"), printData.getTotalAmount());
        assertEquals(88L, printData.getConfirmedBy());
        assertEquals("张主管", printData.getConfirmedByName());
        assertEquals(LocalDateTime.of(2026, 10, 11, 10, 0, 0), printData.getConfirmedAt());
        assertEquals("9月对账确认套打", printData.getRemark());
        assertNotNull(printData.getPrintTime());

        assertEquals(3, printData.getItems().size());

        SupplierReconPrintItemRespVO pItem = printData.getItems().get(0);
        assertEquals(SupplierReconLineTypeEnum.PROCESS.getValue(), pItem.getLineType());
        assertEquals("INS-20260901", pItem.getInspectionOrderNo());
        assertEquals("M", pItem.getQtyUnit());
        assertEquals(new BigDecimal("5000.00"), pItem.getLineAmount());

        SupplierReconPrintItemRespVO puItem = printData.getItems().get(1);
        assertEquals(SupplierReconLineTypeEnum.PURCHASE.getValue(), puItem.getLineType());
        assertEquals("IN-20260902", puItem.getStockInNo());
        assertEquals("KG", puItem.getQtyUnit());
        assertEquals(new BigDecimal("10000.00"), puItem.getLineAmount());

        SupplierReconPrintItemRespVO aItem = printData.getItems().get(2);
        assertEquals(SupplierReconLineTypeEnum.ADJUST.getValue(), aItem.getLineType());
        assertEquals(new BigDecimal("-200.00"), aItem.getLineAmount());
        assertEquals("色差质量扣款", aItem.getAdjustReason());
    }

    /**
     * 18. 打印数据获取不存在的对账单报 000
     */
    @Test
    void getPrintData_reconNotExists_throws000() {
        when(supplierReconMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> supplierReconService.getPrintData(999L));
        assertEquals(1_030_022_000, ex.getCode());
    }

}
