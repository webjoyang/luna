package com.luna.lunasrv.module.lunaerp.service.business.quotation;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation.QuotationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation.QuotationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.QUOTATION_DRAFT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuotationRecycleBinHandlerTest {

    @Mock
    private QuotationMapper quotationMapper;

    @Mock
    private QuotationItemMapper quotationItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private QuotationRecycleBinHandler handler;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("恢复草稿报价单 - 校验记录不存在")
    public void testValidateRestore_RecordNotExists() {
        when(quotationMapper.selectDeletedById(1L, 1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(1L));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿报价单 - 客户在回收站中拦截")
    public void testValidateRestore_CustomerInRecycle() {
        QuotationDO q = QuotationDO.builder().id(1L).customerId(10L).quotationNo("QT20260001").build();
        when(quotationMapper.selectDeletedById(1L, 1L)).thenReturn(q);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(10L);
        deletedCustomer.setCompanyName("已删除客户");
        when(customerMapper.selectDeletedById(10L, 1L)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(1L));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿报价单 - 已存在同单号草稿拦截")
    public void testValidateRestore_ActiveDraftExists() {
        QuotationDO q = QuotationDO.builder().id(1L).customerId(10L).quotationNo("QT20260001").build();
        when(quotationMapper.selectDeletedById(1L, 1L)).thenReturn(q);
        when(customerMapper.selectDeletedById(10L, 1L)).thenReturn(null);
        when(customerMapper.selectById(10L)).thenReturn(new CustomerDO());
        when(quotationMapper.selectDraftByQuotationNo("QT20260001")).thenReturn(new QuotationDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(1L));
        assertEquals(QUOTATION_DRAFT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿报价单 - 成功恢复主表并级联恢复明细")
    public void testRestore_Success() {
        QuotationDO q = QuotationDO.builder().id(1L).customerId(10L).quotationNo("QT20260001").build();
        when(quotationMapper.selectDeletedById(1L, 1L)).thenReturn(q);
        when(customerMapper.selectDeletedById(10L, 1L)).thenReturn(null);
        when(customerMapper.selectById(10L)).thenReturn(new CustomerDO());
        when(quotationMapper.selectDraftByQuotationNo("QT20260001")).thenReturn(null);

        when(quotationMapper.restoreQuotation(eq(1L), eq(1L), any(), any())).thenReturn(1);

        handler.restore(1L);

        verify(quotationMapper).restoreQuotation(eq(1L), eq(1L), any(), any());
        verify(quotationItemMapper).restoreByQuotationId(eq(1L), eq(1L), any(), any());
    }

    @Test
    @DisplayName("物理删除草稿报价单 - 存在业务引用时拦截")
    public void testPurge_HasActiveReference() {
        QuotationDO q = QuotationDO.builder().id(1L).build();
        when(quotationMapper.selectDeletedById(1L, 1L)).thenReturn(q);
        when(quotationMapper.selectActiveReferenceCount(1L, 1L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(1L));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("物理删除草稿报价单 - 成功物理删除子表与主表")
    public void testPurge_Success() {
        QuotationDO q = QuotationDO.builder().id(1L).build();
        when(quotationMapper.selectDeletedById(1L, 1L)).thenReturn(q);
        when(quotationMapper.selectActiveReferenceCount(1L, 1L)).thenReturn(0L);
        when(quotationMapper.purgeQuotation(1L, 1L)).thenReturn(1);

        handler.purge(1L);

        verify(quotationItemMapper).purgeByQuotationId(1L, 1L);
        verify(quotationMapper).purgeQuotation(1L, 1L);
    }

}
