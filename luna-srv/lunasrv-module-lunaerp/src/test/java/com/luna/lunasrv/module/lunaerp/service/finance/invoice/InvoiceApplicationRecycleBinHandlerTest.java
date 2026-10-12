package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link InvoiceApplicationRecycleBinHandler} 单元测试
 */
class InvoiceApplicationRecycleBinHandlerTest {

    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private InvoiceApplicationRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;
    private static final Long APP_ID = 5001L;
    private static final Long CUSTOMER_ID = 1001L;

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
    @DisplayName("验证 bizType 为 INVOICE_APPLICATION")
    void testGetBizType() {
        assertEquals("INVOICE_APPLICATION", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询回收站已删除草稿")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            return page;
        }).when(invoiceApplicationMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        verify(invoiceApplicationMapper).selectRecyclePage(any(), eq(TENANT_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_notExists() {
        when(invoiceApplicationMapper.selectDeletedById(APP_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(APP_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联客户在回收站抛 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testValidateRestore_customerInRecycle() {
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(APP_ID);
        app.setCustomerId(CUSTOMER_ID);
        when(invoiceApplicationMapper.selectDeletedById(APP_ID, TENANT_ID)).thenReturn(app);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("某某客户");
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(APP_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复操作成功")
    void testRestore_success() {
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(APP_ID);
        app.setCustomerId(CUSTOMER_ID);
        when(invoiceApplicationMapper.selectDeletedById(APP_ID, TENANT_ID)).thenReturn(app);

        CustomerDO activeCustomer = new CustomerDO();
        activeCustomer.setId(CUSTOMER_ID);
        activeCustomer.setCompanyName("某某客户");
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(activeCustomer);

        when(invoiceApplicationMapper.restoreInvoiceApplication(eq(APP_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(APP_ID));
        verify(invoiceApplicationMapper).restoreInvoiceApplication(eq(APP_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除成功")
    void testPurge_success() {
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(APP_ID);
        when(invoiceApplicationMapper.selectDeletedById(APP_ID, TENANT_ID)).thenReturn(app);
        when(invoiceApplicationMapper.purgeInvoiceApplication(APP_ID, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(APP_ID));
        verify(invoiceApplicationMapper).purgeInvoiceApplication(APP_ID, TENANT_ID);
    }

}
