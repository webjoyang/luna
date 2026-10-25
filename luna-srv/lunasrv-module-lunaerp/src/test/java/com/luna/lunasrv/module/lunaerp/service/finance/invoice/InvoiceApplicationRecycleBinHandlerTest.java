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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link InvoiceApplicationRecycleBinHandler} 纯 Mockito 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class InvoiceApplicationRecycleBinHandlerTest {

    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private InvoiceApplicationRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("验证业务类型标识为 INVOICE_APPLICATION")
    void testGetBizType() {
        assertEquals("INVOICE_APPLICATION", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询回收站已软删草稿发票申请")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(101L);
            item.setDisplayName("IA20260001");
            page.setRecords(List.of(item));
            page.setTotal(1);
            return page;
        }).when(invoiceApplicationMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("IA20260001", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("恢复草稿发票申请：客户在回收站中抛出 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testRestore_customerInRecycle_throwsException() {
        Long appId = 201L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setCustomerId(301L);

        when(invoiceApplicationMapper.selectDeletedById(appId, TENANT_ID)).thenReturn(app);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(301L);
        deletedCustomer.setCompanyName("测试客户A");
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(appId));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿发票申请：正常恢复并级联复活明细")
    void testRestore_success() {
        Long appId = 201L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setCustomerId(301L);

        when(invoiceApplicationMapper.selectDeletedById(appId, TENANT_ID)).thenReturn(app);
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(null);
        CustomerDO activeCustomer = new CustomerDO();
        activeCustomer.setId(301L);
        when(customerMapper.selectById(301L)).thenReturn(activeCustomer);

        when(invoiceApplicationMapper.restoreInvoiceApplication(eq(appId), eq(TENANT_ID), any(), any())).thenReturn(1);
        when(invoiceApplicationItemMapper.restoreByApplicationId(eq(appId), eq(TENANT_ID), any(), any())).thenReturn(2);

        assertDoesNotThrow(() -> handler.restore(appId));

        verify(invoiceApplicationMapper).restoreInvoiceApplication(eq(appId), eq(TENANT_ID), any(), any());
        verify(invoiceApplicationItemMapper).restoreByApplicationId(eq(appId), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("彻底删除草稿发票申请：有活跃业务引用抛出 RECYCLE_BIN_HAS_ACTIVE_REFERENCE")
    void testPurge_hasActiveReference_throwsException() {
        Long appId = 201L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);

        when(invoiceApplicationMapper.selectDeletedById(appId, TENANT_ID)).thenReturn(app);
        when(invoiceApplicationMapper.selectActiveReferenceCount(appId, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(appId));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底删除草稿发票申请：物理删除子表与主表")
    void testPurge_success() {
        Long appId = 201L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);

        when(invoiceApplicationMapper.selectDeletedById(appId, TENANT_ID)).thenReturn(app);
        when(invoiceApplicationMapper.selectActiveReferenceCount(appId, TENANT_ID)).thenReturn(0L);
        when(invoiceApplicationItemMapper.purgeByApplicationId(appId, TENANT_ID)).thenReturn(2);
        when(invoiceApplicationMapper.purgeInvoiceApplication(appId, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(appId));

        verify(invoiceApplicationItemMapper).purgeByApplicationId(appId, TENANT_ID);
        verify(invoiceApplicationMapper).purgeInvoiceApplication(appId, TENANT_ID);
    }
}
