package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyType;
import com.luna.lunasrv.module.lunaerp.enums.finance.Direction;
import com.luna.lunasrv.module.lunaerp.service.finance.account.FinanceAccountService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

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
    private FinanceAccountService financeAccountService;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    private static final Long TENANT_ID = 1L;
    private static final Long LOGIN_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentRefDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentApplyDO.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
        TenantContextHolder.clear();
    }

    // ==================== 1. 多单顺序分配 ====================

    @Test
    @DisplayName("多申请顺序分配：按申请创建顺序分配实付与预付抵扣，生成付款单与核销明细")
    void testRegisterPayment_MultipleApplications_SequentialAllocation() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        // 申请单1：创建时间早，金额 1000，未核销
        PaymentApplyDO apply1 = new PaymentApplyDO();
        apply1.setId(101L);
        apply1.setApplyNo("FK20261015001");
        apply1.setSupplierId(supplierId);
        apply1.setAmount(new BigDecimal("1000.00"));
        apply1.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply1.setTenantId(TENANT_ID);
        apply1.setCreateTime(LocalDateTime.of(2026, 10, 15, 9, 0, 0));

        // 申请单2：创建时间晚，金额 1000，未核销
        PaymentApplyDO apply2 = new PaymentApplyDO();
        apply2.setId(102L);
        apply2.setApplyNo("FK20261015002");
        apply2.setSupplierId(supplierId);
        apply2.setAmount(new BigDecimal("1000.00"));
        apply2.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply2.setTenantId(TENANT_ID);
        apply2.setCreateTime(LocalDateTime.of(2026, 10, 15, 10, 0, 0));

        when(paymentApplyMapper.selectById(101L)).thenReturn(apply1);
        when(paymentApplyMapper.selectById(102L)).thenReturn(apply2);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(101L)).thenReturn(BigDecimal.ZERO);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(102L)).thenReturn(BigDecimal.ZERO);

        // 供应商预付余额充足
        when(paymentApplyService.getPrepayBalance(supplierId)).thenReturn(new BigDecimal("800.00"));
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            PaymentDO payment = invocation.getArgument(0);
            payment.setId(888L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("1000.00"));
        reqVO.setPrepayUsedAmount(new BigDecimal("500.00"));
        reqVO.setRemark("合并付款1500");

        // 传入两个申请项（不指定具体分配额，由系统按顺序分配）
        PaymentRegisterItemReqVO item1 = new PaymentRegisterItemReqVO();
        item1.setApplyId(101L);
        PaymentRegisterItemReqVO item2 = new PaymentRegisterItemReqVO();
        item2.setApplyId(102L);
        reqVO.setItems(List.of(item1, item2));

        Long paymentId = paymentService.registerPayment(reqVO);
        assertEquals(888L, paymentId);

        // 验证主表写入
        ArgumentCaptor<PaymentDO> paymentCaptor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        PaymentDO insertedPayment = paymentCaptor.getValue();
        assertEquals("OUT", insertedPayment.getDirection());
        assertEquals(supplierId, insertedPayment.getSupplierId());
        assertEquals(accountId, insertedPayment.getAccountId());
        assertEquals(new BigDecimal("1000.00"), insertedPayment.getPayAmount());
        assertEquals(new BigDecimal("500.00"), insertedPayment.getPrepayUsedAmount());
        assertEquals("COMPLETED", insertedPayment.getStatus());
        assertTrue(insertedPayment.getPaymentNo().startsWith("PAY"));

        // 验证核销明细写入：apply1 分到 1000（全额），apply2 分到 500（部分）
        ArgumentCaptor<PaymentRefDO> refCaptor = ArgumentCaptor.forClass(PaymentRefDO.class);
        verify(paymentRefMapper, times(2)).insert(refCaptor.capture());
        List<PaymentRefDO> insertedRefs = refCaptor.getAllValues();
        assertEquals(2, insertedRefs.size());

        PaymentRefDO ref1 = insertedRefs.get(0);
        assertEquals(101L, ref1.getRefId());
        assertEquals("FK20261015001", ref1.getApplyNo());
        assertEquals(new BigDecimal("1000.00"), ref1.getAllocateAmount());

        PaymentRefDO ref2 = insertedRefs.get(1);
        assertEquals(102L, ref2.getRefId());
        assertEquals("FK20261015002", ref2.getApplyNo());
        assertEquals(new BigDecimal("500.00"), ref2.getAllocateAmount());

        // 验证同事务四步调用
        verify(financeAccountService).validateAccountAvailable(accountId);
        verify(financeAccountService).debitAccount(accountId, new BigDecimal("1000.00"));
        verify(financeAccountService).recordAccountOutflow(eq(accountId), eq(new BigDecimal("1000.00")), eq(888L), any());
        verify(financeAccountService).appendOutSupplierLedgerEntry(eq(888L), any(), eq(supplierId), eq(new BigDecimal("1500.00")), eq(new BigDecimal("500.00")), eq(accountId), any());
    }

    // ==================== 2. 单笔部分核销 ====================

    @Test
    @DisplayName("单笔部分核销：申请1000本次实付400，剩余600继续挂应付")
    void testRegisterPayment_SingleApplication_PartialWriteOff() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(201L);
        apply.setApplyNo("FK20261015010");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(TENANT_ID);
        apply.setCreateTime(LocalDateTime.now());

        when(paymentApplyMapper.selectById(201L)).thenReturn(apply);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(201L)).thenReturn(BigDecimal.ZERO);
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            PaymentDO payment = invocation.getArgument(0);
            payment.setId(999L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("400.00"));
        reqVO.setPrepayUsedAmount(BigDecimal.ZERO);

        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(201L);
        item.setAllocateAmount(new BigDecimal("400.00"));
        reqVO.setItems(List.of(item));

        Long paymentId = paymentService.registerPayment(reqVO);
        assertEquals(999L, paymentId);

        ArgumentCaptor<PaymentRefDO> refCaptor = ArgumentCaptor.forClass(PaymentRefDO.class);
        verify(paymentRefMapper).insert(refCaptor.capture());
        PaymentRefDO ref = refCaptor.getValue();
        assertEquals(201L, ref.getRefId());
        assertEquals(new BigDecimal("400.00"), ref.getAllocateAmount());

        verify(financeAccountService).validateAccountAvailable(accountId);
        verify(financeAccountService).debitAccount(accountId, new BigDecimal("400.00"));
        verify(financeAccountService).recordAccountOutflow(eq(accountId), eq(new BigDecimal("400.00")), eq(999L), any());
        verify(financeAccountService).appendOutSupplierLedgerEntry(eq(999L), any(), eq(supplierId), eq(new BigDecimal("400.00")), eq(BigDecimal.ZERO), eq(accountId), any());
    }

    // ==================== 3. 分配超剩余 003 ====================

    @Test
    @DisplayName("分配超剩余报 023_003：申请剩余400但本次分配500")
    void testRegisterPayment_AllocateExceed_Throws023_003() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(301L);
        apply.setApplyNo("FK20261015020");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(TENANT_ID);

        when(paymentApplyMapper.selectById(301L)).thenReturn(apply);
        // 已核销 600，剩余 400
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(301L)).thenReturn(new BigDecimal("600.00"));

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        reqVO.setPrepayUsedAmount(BigDecimal.ZERO);

        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(301L);
        item.setAllocateAmount(new BigDecimal("500.00"));
        reqVO.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_ALLOCATE_EXCEED.getCode(), ex.getCode());
    }

    // ==================== 4. 预付超额 005 ====================

    @Test
    @DisplayName("预付超额报 023_005：供应商预付余额不足本次抵扣额")
    void testRegisterPayment_PrepayBalanceNotEnough_Throws023_005() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        // 预付余额仅 200
        when(paymentApplyService.getPrepayBalance(supplierId)).thenReturn(new BigDecimal("200.00"));

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        reqVO.setPrepayUsedAmount(new BigDecimal("300.00")); // 300 > 200

        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(401L);
        reqVO.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_PREPAY_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    // ==================== 5. 账户停用 006 ====================

    @Test
    @DisplayName("账户停用报 023_006：资金账户不存在或停用")
    void testRegisterPayment_AccountNotAvailable_Throws023_006() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        doThrow(exception(PAYMENT_ACCOUNT_NOT_AVAILABLE))
                .when(financeAccountService).validateAccountAvailable(accountId);

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        reqVO.setPrepayUsedAmount(BigDecimal.ZERO);

        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(501L);
        reqVO.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_ACCOUNT_NOT_AVAILABLE.getCode(), ex.getCode());
    }

    // ==================== 6. 账户条件更新失败 004 ====================

    @Test
    @DisplayName("账户条件更新失败报 023_004：资金账户余额不足扣减")
    void testRegisterPayment_AccountDebitFailed_Throws023_004() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(601L);
        apply.setApplyNo("FK20261015060");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(TENANT_ID);

        when(paymentApplyMapper.selectById(601L)).thenReturn(apply);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(601L)).thenReturn(BigDecimal.ZERO);

        doThrow(exception(PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH))
                .when(financeAccountService).debitAccount(accountId, new BigDecimal("1000.00"));

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("1000.00"));
        reqVO.setPrepayUsedAmount(BigDecimal.ZERO);

        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(601L);
        item.setAllocateAmount(new BigDecimal("1000.00"));
        reqVO.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    // ==================== 7. 金额非法 007 ====================

    @Test
    @DisplayName("金额非法报 023_007：负数金额、总额为零、钱不够分、跨供应商")
    void testRegisterPayment_InvalidAmount_Throws023_007() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        // 7.1 总金额为 0
        PaymentRegisterReqVO reqVOZero = new PaymentRegisterReqVO();
        reqVOZero.setSupplierId(supplierId);
        reqVOZero.setAccountId(accountId);
        reqVOZero.setPayAmount(BigDecimal.ZERO);
        reqVOZero.setPrepayUsedAmount(BigDecimal.ZERO);
        PaymentRegisterItemReqVO itemZero = new PaymentRegisterItemReqVO();
        itemZero.setApplyId(701L);
        reqVOZero.setItems(List.of(itemZero));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVOZero));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex1.getCode());

        // 7.2 负数金额
        PaymentRegisterReqVO reqVONeg = new PaymentRegisterReqVO();
        reqVONeg.setSupplierId(supplierId);
        reqVONeg.setAccountId(accountId);
        reqVONeg.setPayAmount(new BigDecimal("-100.00"));
        reqVONeg.setItems(List.of(itemZero));

        ServiceException ex2 = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVONeg));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex2.getCode());

        // 7.3 分配总额超过实付加抵扣（钱不够分）
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(702L);
        apply.setApplyNo("FK20261015072");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(TENANT_ID);
        when(paymentApplyMapper.selectById(702L)).thenReturn(apply);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(702L)).thenReturn(BigDecimal.ZERO);

        PaymentRegisterReqVO reqVOExceed = new PaymentRegisterReqVO();
        reqVOExceed.setSupplierId(supplierId);
        reqVOExceed.setAccountId(accountId);
        reqVOExceed.setPayAmount(new BigDecimal("500.00")); // 池子 500
        PaymentRegisterItemReqVO itemExceed = new PaymentRegisterItemReqVO();
        itemExceed.setApplyId(702L);
        itemExceed.setAllocateAmount(new BigDecimal("600.00")); // 分配 600 > 500
        reqVOExceed.setItems(List.of(itemExceed));

        ServiceException ex3 = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVOExceed));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex3.getCode());

        // 7.4 跨供应商申请
        PaymentApplyDO otherSupplierApply = new PaymentApplyDO();
        otherSupplierApply.setId(703L);
        otherSupplierApply.setApplyNo("FK20261015073");
        otherSupplierApply.setSupplierId(9999L); // 属于其他供应商
        otherSupplierApply.setAmount(new BigDecimal("1000.00"));
        otherSupplierApply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        otherSupplierApply.setTenantId(TENANT_ID);
        when(paymentApplyMapper.selectById(703L)).thenReturn(otherSupplierApply);

        PaymentRegisterReqVO reqVOCross = new PaymentRegisterReqVO();
        reqVOCross.setSupplierId(supplierId);
        reqVOCross.setAccountId(accountId);
        reqVOCross.setPayAmount(new BigDecimal("500.00"));
        PaymentRegisterItemReqVO itemCross = new PaymentRegisterItemReqVO();
        itemCross.setApplyId(703L);
        itemCross.setAllocateAmount(new BigDecimal("500.00"));
        reqVOCross.setItems(List.of(itemCross));

        ServiceException ex4 = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVOCross));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex4.getCode());
    }

    // ==================== 8. 申请未审批 001 ====================

    @Test
    @DisplayName("申请状态非法报 023_001：申请非 APPROVED 状态禁止付款登记")
    void testRegisterPayment_ApplicationNotApproved_Throws023_001() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(801L);
        apply.setApplyNo("FK20261015081");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVING.getValue()); // 审批中，非 APPROVED
        apply.setTenantId(TENANT_ID);
        when(paymentApplyMapper.selectById(801L)).thenReturn(apply);

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(801L);
        item.setAllocateAmount(new BigDecimal("500.00"));
        reqVO.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== 9. 四步原子回滚 ====================

    @Test
    @DisplayName("四步原子异常回滚：第四步写分录失败整体抛出异常")
    void testRegisterPayment_FourStepsAtomicityRollback() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(901L);
        apply.setApplyNo("FK20261015091");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(TENANT_ID);

        when(paymentApplyMapper.selectById(901L)).thenReturn(apply);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(901L)).thenReturn(BigDecimal.ZERO);
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());

        // 第四步追加分录模拟异常
        doThrow(new RuntimeException("分录账服务异常"))
                .when(financeAccountService).appendOutSupplierLedgerEntry(any(), any(), any(), any(), any(), any(), any());

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(901L);
        item.setAllocateAmount(new BigDecimal("500.00"));
        reqVO.setItems(List.of(item));

        assertThrows(RuntimeException.class, () -> paymentService.registerPayment(reqVO));
    }

    // ==================== 10. 详情与分页查询 ====================

    @Test
    @DisplayName("获得付款单详情：包含内嵌 ref 核销行与供应商名称")
    void testGetPayment_Success() {
        PaymentDO payment = new PaymentDO();
        payment.setId(1001L);
        payment.setPaymentNo("PAY20261015001");
        payment.setDirection("OUT");
        payment.setSupplierId(2001L);
        payment.setAccountId(3001L);
        payment.setPayAmount(new BigDecimal("1500.00"));
        payment.setPrepayUsedAmount(new BigDecimal("500.00"));
        payment.setStatus("COMPLETED");
        payment.setRemark("结算明细");
        payment.setTenantId(TENANT_ID);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);

        SupplierDO supplier = new SupplierDO();
        supplier.setId(2001L);
        supplier.setCompanyName("华丰纺织厂");
        when(supplierMapper.selectById(2001L)).thenReturn(supplier);

        PaymentRefDO ref = new PaymentRefDO();
        ref.setId(5001L);
        ref.setPaymentId(1001L);
        ref.setRefType("PAY_APPLY");
        ref.setRefId(101L);
        ref.setApplyNo("FK20261015001");
        ref.setAllocateAmount(new BigDecimal("2000.00"));
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(ref));

        PaymentRespVO vo = paymentService.getPayment(1001L);
        assertNotNull(vo);
        assertEquals("PAY20261015001", vo.getPaymentNo());
        assertEquals("华丰纺织厂", vo.getSupplierName());
        assertEquals(new BigDecimal("1500.00"), vo.getPayAmount());
        assertEquals(new BigDecimal("500.00"), vo.getPrepayUsedAmount());
        assertEquals(1, vo.getItems().size());
        assertEquals("FK20261015001", vo.getItems().get(0).getApplyNo());
        assertEquals(new BigDecimal("2000.00"), vo.getItems().get(0).getAllocateAmount());
    }

    @Test
    @DisplayName("获得付款单详情不存在时抛 023_009")
    void testGetPayment_NotFound_Throws023_009() {
        when(paymentMapper.selectById(9999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.getPayment(9999L));
        assertEquals(PAYMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("获得付款单分页列表：direction=OUT 固定筛选")
    void testGetPaymentPage_Success() {
        PaymentPageReqVO pageReqVO = new PaymentPageReqVO();
        pageReqVO.setSupplierId(2001L);
        pageReqVO.setAccountId(3001L);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);

        PaymentDO payment = new PaymentDO();
        payment.setId(1001L);
        payment.setPaymentNo("PAY20261015001");
        payment.setDirection("OUT");
        payment.setSupplierId(2001L);
        payment.setAccountId(3001L);
        payment.setPayAmount(new BigDecimal("1000.00"));
        payment.setPrepayUsedAmount(BigDecimal.ZERO);
        payment.setStatus("COMPLETED");

        PageResult<PaymentDO> mockPage = new PageResult<>(List.of(payment), 1L);
        when(paymentMapper.selectPage(eq(pageReqVO), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        SupplierDO supplier = new SupplierDO();
        supplier.setId(2001L);
        supplier.setCompanyName("华丰纺织厂");
        when(supplierMapper.selectBatchIds(any())).thenReturn(List.of(supplier));

        PageResult<PaymentRespVO> pageResult = paymentService.getPaymentPage(pageReqVO);
        assertEquals(1L, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        PaymentRespVO item = pageResult.getList().get(0);
        assertEquals("PAY20261015001", item.getPaymentNo());
        assertEquals("华丰纺织厂", item.getSupplierName());
    }

    // ==================== 11. 核销后剩余额与预付余额聚合 ====================

    @Test
    @DisplayName("核销后聚合口径：核销后申请剩余额与供应商预付余额经既有聚合口径实时反映")
    void testAggregate_PrepayBalanceAndApplyAllocatedAmount() {
        Long supplierId = 5001L;
        Long accountId = 6001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(501L);
        apply.setApplyNo("FK20261015501");
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("2000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(TENANT_ID);
        apply.setCreateTime(LocalDateTime.now());

        when(paymentApplyMapper.selectById(501L)).thenReturn(apply);
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(501L)).thenReturn(BigDecimal.ZERO);
        when(paymentApplyService.getPrepayBalance(supplierId)).thenReturn(new BigDecimal("1000.00"));
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        reqVO.setPrepayUsedAmount(new BigDecimal("300.00"));

        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(501L);
        item.setAllocateAmount(new BigDecimal("800.00"));
        reqVO.setItems(List.of(item));

        paymentService.registerPayment(reqVO);

        // 验证写入的核销额
        ArgumentCaptor<PaymentRefDO> refCaptor = ArgumentCaptor.forClass(PaymentRefDO.class);
        verify(paymentRefMapper).insert(refCaptor.capture());
        BigDecimal thisAllocated = refCaptor.getValue().getAllocateAmount();
        assertEquals(new BigDecimal("800.00"), thisAllocated);

        // 验证写入的预付使用额
        ArgumentCaptor<PaymentDO> paymentCaptor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        BigDecimal thisPrepayUsed = paymentCaptor.getValue().getPrepayUsedAmount();
        assertEquals(new BigDecimal("300.00"), thisPrepayUsed);

        // 核销后申请剩余额聚合口径：amount - Σ(allocatedAmount)
        BigDecimal newRemainAmount = apply.getAmount().subtract(thisAllocated);
        assertEquals(new BigDecimal("1200.00"), newRemainAmount);

        // 核销后供应商预付余额聚合口径：前次余额 - 本次 prepay_used_amount
        BigDecimal newPrepayBalance = new BigDecimal("1000.00").subtract(thisPrepayUsed);
        assertEquals(new BigDecimal("700.00"), newPrepayBalance);
    }

    // ==================== 12. 多租户横向越权与参数守卫 ====================

    @Test
    @DisplayName("横向越权守卫：他人租户的申请单禁止登记，抛出 023_000")
    void testRegisterPayment_CrossTenant_ThrowsPaymentApplyNotExists() {
        Long supplierId = 1001L;
        Long accountId = 2001L;

        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(101L);
        apply.setSupplierId(supplierId);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        apply.setTenantId(999L); // 他人租户

        when(paymentApplyMapper.selectById(101L)).thenReturn(apply);

        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccountId(accountId);
        reqVO.setPayAmount(new BigDecimal("500.00"));
        PaymentRegisterItemReqVO item = new PaymentRegisterItemReqVO();
        item.setApplyId(101L);
        item.setAllocateAmount(new BigDecimal("500.00"));
        reqVO.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_APPLY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("横向越权守卫：他人租户的付款单查询详情抛出 023_009")
    void testGetPayment_CrossTenant_ThrowsPaymentNotExists() {
        PaymentDO payment = new PaymentDO();
        payment.setId(888L);
        payment.setTenantId(999L); // 他人租户
        when(paymentMapper.selectById(888L)).thenReturn(payment);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.getPayment(888L));
        assertEquals(PAYMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重复申请守卫：入参明细包含重复 applyId 时抛出 023_007 防止突破限额")
    void testRegisterPayment_DuplicateApplyId_Throws023_007() {
        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(1001L);
        reqVO.setAccountId(2001L);
        reqVO.setPayAmount(new BigDecimal("500.00"));

        PaymentRegisterItemReqVO item1 = new PaymentRegisterItemReqVO();
        item1.setApplyId(101L);
        item1.setAllocateAmount(new BigDecimal("250.00"));

        PaymentRegisterItemReqVO item2 = new PaymentRegisterItemReqVO();
        item2.setApplyId(101L); // 重复 applyId
        item2.setAllocateAmount(new BigDecimal("250.00"));

        reqVO.setItems(List.of(item1, item2));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("混合传参守卫：明细部分项传分配额、部分项为 null 时抛出 023_007")
    void testRegisterPayment_MixedAllocateAmount_Throws023_007() {
        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        reqVO.setSupplierId(1001L);
        reqVO.setAccountId(2001L);
        reqVO.setPayAmount(new BigDecimal("500.00"));

        PaymentRegisterItemReqVO item1 = new PaymentRegisterItemReqVO();
        item1.setApplyId(101L);
        item1.setAllocateAmount(new BigDecimal("300.00"));

        PaymentRegisterItemReqVO item2 = new PaymentRegisterItemReqVO();
        item2.setApplyId(102L);
        item2.setAllocateAmount(null); // 混合传参

        reqVO.setItems(List.of(item1, item2));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.registerPayment(reqVO));
        assertEquals(PAYMENT_AMOUNT_INVALID.getCode(), ex.getCode());
    }

}
