package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReceiptType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentServiceImpl} 收款单（IN 侧）业务单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PaymentReceiptServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    private static final Long TENANT_ID = 1L;
    private static final Long LOGIN_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentRefDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AccountDO.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
        TenantContextHolder.clear();
    }

    // ==================== 1. 草稿创建 (createReceipt) ====================

    @Test
    @DisplayName("收款单草稿创建：校验账户与客户可用，生成 RC 编号，保存头与核销明细（PENDING 态）")
    void testCreateReceipt_Success() {
        CustomerDO customer = new CustomerDO();
        customer.setId(2001L);
        customer.setTenantId(TENANT_ID);
        customer.setCompanyName("测试客户");
        when(customerMapper.selectById(2001L)).thenReturn(customer);

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setTenantId(TENANT_ID);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountMapper.selectById(3001L)).thenReturn(account);

        // 模拟待核销 AR 分录（未核销额 3000）
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(101L);
        entry.setTenantId(TENANT_ID);
        entry.setPartyId(2001L);
        entry.setCurrency("RMB");
        entry.setCategory("AR_INC");
        entry.setOriginalAmount(new BigDecimal("3000.00"));
        entry.setBizDate(LocalDate.of(2026, 10, 10));
        entry.setRefId(801L);
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry));

        ReceiptSaveReqVO saveReq = new ReceiptSaveReqVO();
        saveReq.setCustomerId(2001L);
        saveReq.setAccountId(3001L);
        saveReq.setCurrency("RMB");
        saveReq.setReceiptType(ReceiptType.RECEIPT.getValue());
        saveReq.setPayAmount(new BigDecimal("2000.00"));
        saveReq.setReceiptDate(LocalDate.of(2026, 10, 16));
        saveReq.setHandlerName("张会计");

        ReceiptItemReqVO item = new ReceiptItemReqVO();
        item.setEntryId(101L);
        item.setAllocateAmount(new BigDecimal("2000.00"));
        saveReq.setItems(List.of(item));

        doAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            p.setId(901L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        Long receiptId = paymentService.createReceipt(saveReq);

        assertNotNull(receiptId);
        assertEquals(901L, receiptId);

        ArgumentCaptor<PaymentDO> paymentCaptor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        PaymentDO savedPayment = paymentCaptor.getValue();
        assertEquals("IN", savedPayment.getDirection());
        assertEquals(ReceiptType.RECEIPT.getValue(), savedPayment.getReceiptType());
        assertEquals("PENDING", savedPayment.getApprovalStatus());
        assertTrue(savedPayment.getPaymentNo().startsWith("RC-"));
        assertEquals(new BigDecimal("2000.00"), savedPayment.getPayAmount());

        ArgumentCaptor<PaymentRefDO> refCaptor = ArgumentCaptor.forClass(PaymentRefDO.class);
        verify(paymentRefMapper).insert(refCaptor.capture());
        PaymentRefDO savedRef = refCaptor.getValue();
        assertEquals(901L, savedRef.getPaymentId());
        assertEquals("AR_ENTRY", savedRef.getRefType());
        assertEquals(101L, savedRef.getRefId());
        assertEquals(new BigDecimal("2000.00"), savedRef.getAllocateAmount());
    }

    @Test
    @DisplayName("收款单创建守卫：分配合计超收款额抛出 RECEIPT_ALLOCATE_EXCEED")
    void testCreateReceipt_AllocateExceed() {
        CustomerDO customer = new CustomerDO();
        customer.setId(2001L);
        customer.setTenantId(TENANT_ID);
        when(customerMapper.selectById(2001L)).thenReturn(customer);

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setTenantId(TENANT_ID);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountMapper.selectById(3001L)).thenReturn(account);

        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(101L);
        entry.setTenantId(TENANT_ID);
        entry.setPartyId(2001L);
        entry.setCurrency("RMB");
        entry.setCategory("AR_INC");
        entry.setOriginalAmount(new BigDecimal("3000.00"));
        entry.setBizDate(LocalDate.of(2026, 10, 10));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry));

        ReceiptSaveReqVO saveReq = new ReceiptSaveReqVO();
        saveReq.setCustomerId(2001L);
        saveReq.setAccountId(3001L);
        saveReq.setCurrency("RMB");
        saveReq.setReceiptType(ReceiptType.RECEIPT.getValue());
        saveReq.setPayAmount(new BigDecimal("1000.00"));
        saveReq.setReceiptDate(LocalDate.of(2026, 10, 16));

        ReceiptItemReqVO item = new ReceiptItemReqVO();
        item.setEntryId(101L);
        item.setAllocateAmount(new BigDecimal("1500.00")); // 超出 1000.00
        saveReq.setItems(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.createReceipt(saveReq));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_ALLOCATE_EXCEED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("收款单创建守卫：币种不一致抛出 RECEIPT_CURRENCY_MISMATCH")
    void testCreateReceipt_CurrencyMismatch() {
        CustomerDO customer = new CustomerDO();
        customer.setId(2001L);
        customer.setTenantId(TENANT_ID);
        when(customerMapper.selectById(2001L)).thenReturn(customer);

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setTenantId(TENANT_ID);
        account.setStatus("ENABLE");
        account.setCurrency("USD"); // 账户币种为 USD
        when(accountMapper.selectById(3001L)).thenReturn(account);

        ReceiptSaveReqVO saveReq = new ReceiptSaveReqVO();
        saveReq.setCustomerId(2001L);
        saveReq.setAccountId(3001L);
        saveReq.setCurrency("RMB"); // 收款币种为 RMB
        saveReq.setReceiptType(ReceiptType.RECEIPT.getValue());
        saveReq.setPayAmount(new BigDecimal("1000.00"));
        saveReq.setReceiptDate(LocalDate.of(2026, 10, 16));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentService.createReceipt(saveReq));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_CURRENCY_MISMATCH.getCode(), ex.getCode());
    }

    // ==================== 2. 草稿修改与删除状态守卫 ====================

    @Test
    @DisplayName("草稿修改与删除状态守卫：仅 approval_status=PENDING 可改删，已审批或审批中抛 RECEIPT_STATUS_NOT_ALLOW")
    void testUpdateAndDeleteReceipt_StatusGuard() {
        PaymentDO payment = new PaymentDO();
        payment.setId(888L);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setApprovalStatus(ApplyStatus.APPROVING.getValue()); // 审批中
        when(paymentMapper.selectById(888L)).thenReturn(payment);

        ReceiptUpdateReqVO updateReq = new ReceiptUpdateReqVO();
        updateReq.setId(888L);
        updateReq.setPayAmount(new BigDecimal("3000.00"));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> paymentService.updateReceipt(updateReq));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW.getCode(), ex1.getCode());

        // REJECTED 亦不可改
        payment.setApprovalStatus(ApplyStatus.REJECTED.getValue());
        ServiceException exRejected = assertThrows(ServiceException.class, () -> paymentService.updateReceipt(updateReq));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW.getCode(), exRejected.getCode());

        ServiceException ex2 = assertThrows(ServiceException.class, () -> paymentService.deleteReceipt(888L));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("草稿修改可空字段置空守卫：bankRefNo/handlerName/remark 允许置空为 null (coding-standards §15)")
    void testUpdateReceipt_clearNullableFields_success() {
        PaymentDO payment = new PaymentDO();
        payment.setId(888L);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setApprovalStatus(ApplyStatus.PENDING.getValue());
        payment.setBankRefNo("OLD_REF");
        payment.setHandlerName("OLD_HANDLER");
        payment.setRemark("OLD_REMARK");
        when(paymentMapper.selectById(888L)).thenReturn(payment);

        ReceiptUpdateReqVO updateReq = new ReceiptUpdateReqVO();
        updateReq.setId(888L);
        updateReq.setBankRefNo(null);
        updateReq.setHandlerName(null);
        updateReq.setRemark(null);

        paymentService.updateReceipt(updateReq);

        ArgumentCaptor<PaymentDO> captor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).updateById(captor.capture());
        PaymentDO updated = captor.getValue();
        assertNull(updated.getBankRefNo(), "bankRefNo 必须允许置空为 null");
        assertNull(updated.getHandlerName(), "handlerName 必须允许置空为 null");
        assertNull(updated.getRemark(), "remark 必须允许置空为 null");
    }

    // ==================== 3. 提交审批校验 (submitReceipt) ====================

    @Test
    @DisplayName("提交审批校验：同币种、分配合计≤收款额、逐行≤未核销余额，成功调用 approvalInstanceService 发起审批")
    void testSubmitReceipt_Success() {
        PaymentDO payment = new PaymentDO();
        payment.setId(901L);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("2000.00"));
        payment.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(paymentMapper.selectById(901L)).thenReturn(payment);

        PaymentRefDO ref = new PaymentRefDO();
        ref.setId(501L);
        ref.setPaymentId(901L);
        ref.setRefType("AR_ENTRY");
        ref.setRefId(101L);
        ref.setAllocateAmount(new BigDecimal("2000.00"));
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(ref));

        // 模拟该分录未核销额为 3000
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(101L);
        entry.setTenantId(TENANT_ID);
        entry.setPartyId(2001L);
        entry.setCurrency("RMB");
        entry.setCategory("AR_INC");
        entry.setOriginalAmount(new BigDecimal("3000.00"));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry));

        paymentService.submitReceipt(901L);

        ArgumentCaptor<ApprovalInstanceSubmitReqVO> captor = ArgumentCaptor.forClass(ApprovalInstanceSubmitReqVO.class);
        verify(approvalInstanceService).submitInstance(captor.capture());
        ApprovalInstanceSubmitReqVO submitReq = captor.getValue();
        assertEquals("CUSTOMER_RECEIPT", submitReq.getBizType());
        assertEquals(901L, submitReq.getBizId());
    }

    // ==================== 4. 自动分配试算 (allocatePreview FIFO) ====================

    @Test
    @DisplayName("自动分配试算：按发货业务日升序（FIFO）依次凑平分录未核销额")
    void testAllocatePreview_FifoOrdering() {
        // 模拟 3 笔未核销 AR 分录（发货日由早到晚）：
        // 1) 10月1日 1000 元
        // 2) 10月5日 2000 元
        // 3) 10月10日 3000 元
        LedgerEntryDO e1 = createArEntry(101L, LocalDate.of(2026, 10, 1), new BigDecimal("1000.00"), 801L);
        LedgerEntryDO e2 = createArEntry(102L, LocalDate.of(2026, 10, 5), new BigDecimal("2000.00"), 802L);
        LedgerEntryDO e3 = createArEntry(103L, LocalDate.of(2026, 10, 10), new BigDecimal("3000.00"), 803L);
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(e3, e1, e2)); // 乱序返回

        AllocatePreviewReqVO req = new AllocatePreviewReqVO();
        req.setCustomerId(2001L);
        req.setCurrency("RMB");
        req.setPayAmount(new BigDecimal("2500.00")); // 实收 2500，应分配 e1 全额 1000，e2 部分 1500，e3 为 0

        List<AllocatePreviewRespVO> result = paymentService.allocatePreview(req);

        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证 FIFO 排序为 101 -> 102 -> 103
        assertEquals(101L, result.get(0).getEntryId());
        assertEquals(new BigDecimal("1000.00"), result.get(0).getAllocateAmount());

        assertEquals(102L, result.get(1).getEntryId());
        assertEquals(new BigDecimal("1500.00"), result.get(1).getAllocateAmount());

        assertEquals(103L, result.get(2).getEntryId());
        assertEquals(BigDecimal.ZERO, result.get(2).getAllocateAmount());
    }

    private LedgerEntryDO createArEntry(Long id, LocalDate bizDate, BigDecimal amount, Long refId) {
        LedgerEntryDO e = new LedgerEntryDO();
        e.setId(id);
        e.setTenantId(TENANT_ID);
        e.setPartyId(2001L);
        e.setCurrency("RMB");
        e.setCategory("AR_INC");
        e.setOriginalAmount(amount);
        e.setBizDate(bizDate);
        e.setRefType("SHIPMENT_ITEM");
        e.setRefId(refId);
        return e;
    }

}
