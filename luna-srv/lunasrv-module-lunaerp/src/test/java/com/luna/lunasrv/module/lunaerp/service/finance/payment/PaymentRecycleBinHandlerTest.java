package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentRecycleBinHandler} 纯 Mockito 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PaymentRecycleBinHandlerTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private PaymentRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private PaymentDO createDeletedPayment(Long id, Long customerId, Long supplierId) {
        PaymentDO payment = new PaymentDO();
        payment.setId(id);
        payment.setCustomerId(customerId);
        payment.setSupplierId(supplierId);
        return payment;
    }

    private CustomerDO createCustomer(Long id, String name) {
        CustomerDO customer = new CustomerDO();
        customer.setId(id);
        customer.setCompanyName(name);
        return customer;
    }

    private SupplierDO createSupplier(Long id, String name) {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(id);
        supplier.setCompanyName(name);
        return supplier;
    }

    @Test
    @DisplayName("验证业务类型标识为 PAYMENT")
    void testGetBizType() {
        assertEquals("PAYMENT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询回收站已软删草稿收付款单（仅 approval_status=PENDING）")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(1024L);
            item.setDisplayName("RC-20261018-001");
            page.setRecords(List.of(item));
            page.setTotal(1);
            return page;
        }).when(paymentMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("RC-20261018-001", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("恢复草稿收款单：非 PENDING 态（或记录不存在）被过滤返回 null 抛出 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testRestore_nonPendingOrNotExists_throwsException() {
        Long paymentId = 999L;
        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(paymentId));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿收款单：关联客户档案不存在时抛出 CUSTOMER_NOT_EXISTS")
    void testRestore_customerNotFound_throwsException() {
        Long paymentId = 501L;
        PaymentDO payment = createDeletedPayment(paymentId, 301L, null);
        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(301L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(paymentId));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿收款单：关联客户在回收站中抛出 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testRestore_customerInRecycle_throwsException() {
        Long paymentId = 501L;
        PaymentDO payment = createDeletedPayment(paymentId, 301L, null);

        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);

        CustomerDO deletedCustomer = createCustomer(301L, "测试客户B");
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(paymentId));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿付款单：关联供应商档案不存在时抛出 SUPPLIER_NOT_EXISTS")
    void testRestore_supplierNotFound_throwsException() {
        Long paymentId = 502L;
        PaymentDO payment = createDeletedPayment(paymentId, null, 401L);
        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);
        when(supplierMapper.selectDeletedById(401L, TENANT_ID)).thenReturn(null);
        when(supplierMapper.selectById(401L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(paymentId));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿付款单：关联供应商在回收站中抛出 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testRestore_supplierInRecycle_throwsException() {
        Long paymentId = 502L;
        PaymentDO payment = createDeletedPayment(paymentId, null, 401L);

        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);

        SupplierDO deletedSupplier = createSupplier(401L, "测试供应商C");
        when(supplierMapper.selectDeletedById(401L, TENANT_ID)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(paymentId));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿收款单：正常恢复主表为 PENDING 态并级联复活分配明细（不补账）")
    void testRestore_success() {
        Long paymentId = 501L;
        PaymentDO payment = createDeletedPayment(paymentId, 301L, null);

        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(null);
        CustomerDO activeCustomer = createCustomer(301L, "活跃客户B");
        when(customerMapper.selectById(301L)).thenReturn(activeCustomer);

        when(paymentMapper.restorePayment(eq(paymentId), eq(TENANT_ID), any(), any())).thenReturn(1);
        when(paymentRefMapper.restoreByPaymentId(eq(paymentId), eq(TENANT_ID), any(), any())).thenReturn(2);

        assertDoesNotThrow(() -> handler.restore(paymentId));

        verify(paymentMapper).restorePayment(eq(paymentId), eq(TENANT_ID), any(), any());
        verify(paymentRefMapper).restoreByPaymentId(eq(paymentId), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("彻底删除草稿收付款单：有活跃业务引用抛出 RECYCLE_BIN_HAS_ACTIVE_REFERENCE")
    void testPurge_hasActiveReference_throwsException() {
        Long paymentId = 501L;
        PaymentDO payment = createDeletedPayment(paymentId, 301L, null);

        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);
        when(paymentMapper.selectActiveReferenceCount(paymentId, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(paymentId));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底删除草稿收付款单：物理删除子表与主表")
    void testPurge_success() {
        Long paymentId = 501L;
        PaymentDO payment = createDeletedPayment(paymentId, 301L, null);

        when(paymentMapper.selectDeletedById(paymentId, TENANT_ID)).thenReturn(payment);
        when(paymentMapper.selectActiveReferenceCount(paymentId, TENANT_ID)).thenReturn(0L);
        when(paymentRefMapper.purgeByPaymentId(paymentId, TENANT_ID)).thenReturn(2);
        when(paymentMapper.purgePayment(paymentId, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(paymentId));

        verify(paymentRefMapper).purgeByPaymentId(paymentId, TENANT_ID);
        verify(paymentMapper).purgePayment(paymentId, TENANT_ID);
    }
}
