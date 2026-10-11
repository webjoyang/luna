package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatusEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.seam.FinanceAccountSeam;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.seam.FinanceLedgerSeam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private PaymentApplyService paymentApplyService;

    @Mock
    private FinanceAccountSeam financeAccountSeam;

    @Mock
    private FinanceLedgerSeam financeLedgerSeam;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private SupplierDO createSupplier(Long id, String name) {
        SupplierDO s = new SupplierDO();
        s.setId(id);
        s.setCompanyName(name);
        return s;
    }

    private PaymentApplyDO createApply(Long id, String applyNo, Long supplierId, BigDecimal amount, String status, LocalDateTime createTime) {
        PaymentApplyDO apply = PaymentApplyDO.builder()
                .id(id)
                .applyNo(applyNo)
                .supplierId(supplierId)
                .amount(amount)
                .approvalStatus(status)
                .build();
        apply.setCreateTime(createTime);
        return apply;
    }

    @Test
    @DisplayName("多申请顺序分配与预付抵扣+账户实付登记成功")
    void testRegisterPayment_Success_MultipleApplies_OrderByCreateTime() {
        Long supplierId = 100L;
        Long accountId = 200L;

        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        // 申请 1：创建时间较晚（10:00），金额 5000，已核销 1000，剩余 4000
        PaymentApplyDO apply1 = createApply(1L, "FK20261011001", supplierId, new BigDecimal("5000.00"),
                "APPROVED", LocalDateTime.of(2026, 10, 11, 10, 0));

        // 申请 2：创建时间较早（09:00），金额 3000，已核销 0，剩余 3000
        PaymentApplyDO apply2 = createApply(2L, "FK20261011002", supplierId, new BigDecimal("3000.00"),
                "APPROVED", LocalDateTime.of(2026, 10, 11, 9, 0));

        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Arrays.asList(apply1, apply2));

        // 申请 1 已核销 1000
        PaymentRefDO existingRef1 = PaymentRefDO.builder()
                .paymentId(99L)
                .refType(PaymentRefTypeEnum.PAY_APPLY.getValue())
                .refId(1L)
                .allocateAmount(new BigDecimal("1000.00"))
                .build();
        when(paymentRefMapper.selectListByRefTypeAndRefId(PaymentRefTypeEnum.PAY_APPLY.getValue(), 1L))
                .thenReturn(Collections.singletonList(existingRef1));
        when(paymentRefMapper.selectListByRefTypeAndRefId(PaymentRefTypeEnum.PAY_APPLY.getValue(), 2L))
                .thenReturn(Collections.emptyList());

        // 预付余额 2500，本次动用 2000
        when(paymentApplyService.getPrepayBalance(supplierId)).thenReturn(new BigDecimal("2500.00"));

        // 本次核销：申请 1 核销 3000，申请 2 核销 3000，合计 6000
        // 实付 4000 + 预付抵扣 2000 = 6000
        PaymentRegisterItemReqVO item1 = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("3000.00")).build();
        PaymentRegisterItemReqVO item2 = PaymentRegisterItemReqVO.builder().applyId(2L).allocateAmount(new BigDecimal("3000.00")).build();

        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(accountId)
                .payAmount(new BigDecimal("4000.00"))
                .prepayUsedAmount(new BigDecimal("2000.00"))
                .items(Arrays.asList(item1, item2))
                .remark("登记两笔申请")
                .build();

        doAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            p.setId(888L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        Long paymentId = paymentService.registerPayment(reqVO);
        assertEquals(888L, paymentId);

        // 校验账户扣减
        verify(financeAccountSeam).deductAccountBalance(accountId, new BigDecimal("4000.00"));
        verify(financeAccountSeam).recordAccountOutflow(eq(accountId), eq(new BigDecimal("4000.00")), eq(888L), anyString());

        // 校验分录账写入
        verify(financeLedgerSeam).appendSupplierPaymentEntry(eq(888L), anyString(), eq(supplierId),
                eq(new BigDecimal("6000.00")), eq(new BigDecimal("4000.00")), eq(new BigDecimal("2000.00")));

        // 校验核销行插入顺序（apply2 创建时间早，排在前面插入）
        ArgumentCaptor<PaymentRefDO> refCaptor = ArgumentCaptor.forClass(PaymentRefDO.class);
        verify(paymentRefMapper, times(2)).insert(refCaptor.capture());
        List<PaymentRefDO> insertedRefs = refCaptor.getAllValues();
        assertEquals(2L, insertedRefs.get(0).getRefId());
        assertEquals("FK20261011002", insertedRefs.get(0).getApplyNo());
        assertEquals(new BigDecimal("3000.00"), insertedRefs.get(0).getAllocateAmount());

        assertEquals(1L, insertedRefs.get(1).getRefId());
        assertEquals("FK20261011001", insertedRefs.get(1).getApplyNo());
        assertEquals(new BigDecimal("3000.00"), insertedRefs.get(1).getAllocateAmount());
    }

    @Test
    @DisplayName("单笔申请部分核销成功")
    void testRegisterPayment_Success_PartialAllocation() {
        Long supplierId = 100L;
        Long accountId = 200L;

        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("5000.00"),
                "APPROVED", LocalDateTime.now());
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.emptyList());

        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("2000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(accountId)
                .payAmount(new BigDecimal("2000.00"))
                .items(Collections.singletonList(item))
                .build();

        doAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            p.setId(777L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        Long paymentId = paymentService.registerPayment(reqVO);
        assertEquals(777L, paymentId);
        verify(financeAccountSeam).deductAccountBalance(accountId, new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("核销分配金额超过付款申请剩余额抛出 023_003")
    void testRegisterPayment_AllocateExceedsRemaining_Throws023_003() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("5000.00"),
                "APPROVED", LocalDateTime.now());
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));

        // 已核销 4500，剩余 500
        PaymentRefDO ref = PaymentRefDO.builder().allocateAmount(new BigDecimal("4500.00")).build();
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.singletonList(ref));

        // 分配 600 > 500
        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("600.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(new BigDecimal("600.00"))
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_ALLOCATE_EXCEED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("预付抵扣金额超过供应商预付余额抛出 023_005")
    void testRegisterPayment_PrepayExceedsBalance_Throws023_005() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("5000.00"),
                "APPROVED", LocalDateTime.now());
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.emptyList());

        // 预付余额只有 500
        when(paymentApplyService.getPrepayBalance(supplierId)).thenReturn(new BigDecimal("500.00"));

        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("1000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(BigDecimal.ZERO)
                .prepayUsedAmount(new BigDecimal("1000.00")) // 1000 > 500
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_PREPAY_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("账户停用抛出 023_006")
    void testRegisterPayment_AccountNotAvailable_Throws023_006() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("1000.00"),
                "APPROVED", LocalDateTime.now());
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.emptyList());

        doThrow(exception(PAYMENT_ACCOUNT_NOT_AVAILABLE))
                .when(financeAccountSeam).deductAccountBalance(eq(200L), any(BigDecimal.class));

        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("1000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(new BigDecimal("1000.00"))
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_ACCOUNT_NOT_AVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("账户余额不足扣减失败抛出 023_004")
    void testRegisterPayment_AccountBalanceNotEnough_Throws023_004() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("1000.00"),
                "APPROVED", LocalDateTime.now());
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.emptyList());

        doThrow(exception(PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH))
                .when(financeAccountSeam).deductAccountBalance(eq(200L), any(BigDecimal.class));

        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("1000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(new BigDecimal("1000.00"))
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("分配总额大于实付+预付（钱不够分）抛出 023_007")
    void testRegisterPayment_AllocateExceedsFunds_Throws023_007() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("5000.00"),
                "APPROVED", LocalDateTime.now());
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.emptyList());

        // 分配 3000，但实付 1000 + 预付 1000 = 2000
        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("3000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(new BigDecimal("1000.00"))
                .prepayUsedAmount(new BigDecimal("1000.00"))
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非 APPROVED 状态申请被拒绝抛出 023_001")
    void testRegisterPayment_NotApproved_Throws023_001() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", supplierId, new BigDecimal("1000.00"),
                "APPROVING", LocalDateTime.now()); // 审批中非 APPROVED
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));

        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("1000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(new BigDecimal("1000.00"))
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("申请属于其他供应商抛出 023_007")
    void testRegisterPayment_SupplierMismatch_Throws023_007() {
        Long supplierId = 100L;
        when(supplierMapper.selectById(supplierId)).thenReturn(createSupplier(supplierId, "测试供应商"));

        PaymentApplyDO apply = createApply(1L, "FK20261011001", 999L, new BigDecimal("1000.00"),
                "APPROVED", LocalDateTime.now()); // 其他供应商
        when(paymentApplyMapper.selectBatchIds(anySet())).thenReturn(Collections.singletonList(apply));

        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder().applyId(1L).allocateAmount(new BigDecimal("1000.00")).build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(supplierId)
                .accountId(200L)
                .payAmount(new BigDecimal("1000.00"))
                .items(Collections.singletonList(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("查询付款单详情成功内嵌核销行")
    void testGetPayment_Success() {
        PaymentDO payment = PaymentDO.builder()
                .id(10L)
                .paymentNo("PAY20261011001")
                .direction(PaymentDirectionEnum.OUT.getValue())
                .supplierId(100L)
                .accountId(200L)
                .payAmount(new BigDecimal("5000.00"))
                .prepayUsedAmount(BigDecimal.ZERO)
                .status(PaymentStatusEnum.COMPLETED.getValue())
                .build();
        payment.setCreateTime(LocalDateTime.now());
        when(paymentMapper.selectById(10L)).thenReturn(payment);
        when(supplierMapper.selectById(100L)).thenReturn(createSupplier(100L, "测试供应商"));

        PaymentRefDO ref = PaymentRefDO.builder()
                .id(1L)
                .paymentId(10L)
                .refType(PaymentRefTypeEnum.PAY_APPLY.getValue())
                .refId(1001L)
                .applyNo("FK20261011001")
                .allocateAmount(new BigDecimal("5000.00"))
                .build();
        when(paymentRefMapper.selectListByPaymentId(10L)).thenReturn(Collections.singletonList(ref));

        PaymentRespVO respVO = paymentService.getPayment(10L);
        assertNotNull(respVO);
        assertEquals("PAY20261011001", respVO.getPaymentNo());
        assertEquals("测试供应商", respVO.getSupplierName());
        assertEquals(1, respVO.getItems().size());
        assertEquals("FK20261011001", respVO.getItems().get(0).getApplyNo());
    }

    @Test
    @DisplayName("查询不存在的付款单抛出 023_008")
    void testGetPayment_NotExists() {
        when(paymentMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.getPayment(999L));
        assertEquals(PAYMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

}
