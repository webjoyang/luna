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

import java.math.BigDecimal;
import java.time.LocalDate;
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

}
