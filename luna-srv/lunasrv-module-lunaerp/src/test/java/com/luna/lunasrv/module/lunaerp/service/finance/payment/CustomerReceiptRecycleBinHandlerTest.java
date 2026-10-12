package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerReceiptRecycleBinHandler} 单元测试
 */
class CustomerReceiptRecycleBinHandlerTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private CustomerReceiptRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;
    private static final Long PAYMENT_ID = 8001L;
    private static final Long CUSTOMER_ID = 1001L;
    private static final Long ACCOUNT_ID = 2001L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("验证 bizType 为 PAYMENT")
    void testGetBizType() {
        assertEquals("PAYMENT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询回收站已删除草稿客户收款单")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(PAYMENT_ID);
            item.setDisplayName("RC20261012001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(paymentMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("RC20261012001", result.getList().get(0).getDisplayName());
        verify(paymentMapper).selectRecyclePage(any(), eq(TENANT_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_notExists() {
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(PAYMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联客户在回收站抛 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testValidateRestore_customerInRecycle() {
        PaymentDO payment = new PaymentDO();
        payment.setId(PAYMENT_ID);
        payment.setCustomerId(CUSTOMER_ID);
        payment.setAccountId(ACCOUNT_ID);
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(payment);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("广州盛世");
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(PAYMENT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联资金账户不存在或禁用抛 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testValidateRestore_accountDisabled() {
        PaymentDO payment = new PaymentDO();
        payment.setId(PAYMENT_ID);
        payment.setCustomerId(CUSTOMER_ID);
        payment.setAccountId(ACCOUNT_ID);
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(payment);

        CustomerDO activeCustomer = new CustomerDO();
        activeCustomer.setId(CUSTOMER_ID);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(activeCustomer);

        AccountDO disabledAccount = AccountDO.builder().id(ACCOUNT_ID).status("DISABLE").build();
        when(accountService.getAccount(ACCOUNT_ID)).thenReturn(disabledAccount);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(PAYMENT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复操作成功：级联复活核销子表，恢复不补账")
    void testRestore_success() {
        PaymentDO payment = new PaymentDO();
        payment.setId(PAYMENT_ID);
        payment.setCustomerId(CUSTOMER_ID);
        payment.setAccountId(ACCOUNT_ID);
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(payment);

        CustomerDO activeCustomer = new CustomerDO();
        activeCustomer.setId(CUSTOMER_ID);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(activeCustomer);

        AccountDO activeAccount = AccountDO.builder().id(ACCOUNT_ID).status("ENABLE").build();
        when(accountService.getAccount(ACCOUNT_ID)).thenReturn(activeAccount);

        when(paymentMapper.restorePayment(eq(PAYMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(PAYMENT_ID));
        verify(paymentMapper).restorePayment(eq(PAYMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
        verify(paymentRefMapper).restoreByPaymentId(eq(PAYMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("恢复操作异常：更新行数为0抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testRestore_updateZeroThrows() {
        PaymentDO payment = new PaymentDO();
        payment.setId(PAYMENT_ID);
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(payment);

        when(paymentMapper.restorePayment(eq(PAYMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(PAYMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("引用守卫：草稿收款单无下游活跃引用")
    void testHasActiveReference() {
        assertFalse(handler.hasActiveReference(PAYMENT_ID));
    }

    @Test
    @DisplayName("彻底删除成功：级联物理删除核销行与主表")
    void testPurge_success() {
        PaymentDO payment = new PaymentDO();
        payment.setId(PAYMENT_ID);
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(payment);
        when(paymentMapper.purgePayment(PAYMENT_ID, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(PAYMENT_ID));
        verify(paymentRefMapper).purgeByPaymentId(PAYMENT_ID, TENANT_ID);
        verify(paymentMapper).purgePayment(PAYMENT_ID, TENANT_ID);
    }

    @Test
    @DisplayName("彻底删除异常：单据不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testPurge_notExists() {
        when(paymentMapper.selectDeletedById(PAYMENT_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(PAYMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

}
