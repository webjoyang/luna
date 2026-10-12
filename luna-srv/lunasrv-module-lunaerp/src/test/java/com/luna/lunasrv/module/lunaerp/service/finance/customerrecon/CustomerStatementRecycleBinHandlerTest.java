package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
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
 * {@link CustomerStatementRecycleBinHandler} 单元测试
 */
class CustomerStatementRecycleBinHandlerTest {

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerStatementRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;
    private static final Long STATEMENT_ID = 6001L;
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
    @DisplayName("验证 bizType 为 STATEMENT")
    void testGetBizType() {
        assertEquals("STATEMENT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询回收站已删除草稿对账单")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(STATEMENT_ID);
            item.setDisplayName("CS20260001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(customerStatementMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("CS20260001", result.getList().get(0).getDisplayName());
        verify(customerStatementMapper).selectRecyclePage(any(), eq(TENANT_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_notExists() {
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(STATEMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联客户在回收站抛 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testValidateRestore_customerInRecycle() {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(STATEMENT_ID);
        statement.setCustomerId(CUSTOMER_ID);
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(statement);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("杭州明辉");
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(STATEMENT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联客户彻底不存在抛 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testValidateRestore_customerNotExists() {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(STATEMENT_ID);
        statement.setCustomerId(CUSTOMER_ID);
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(statement);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(STATEMENT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复操作成功：级联复活明细行，恢复不补账")
    void testRestore_success() {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(STATEMENT_ID);
        statement.setCustomerId(CUSTOMER_ID);
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(statement);

        CustomerDO activeCustomer = new CustomerDO();
        activeCustomer.setId(CUSTOMER_ID);
        activeCustomer.setCompanyName("杭州明辉");
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(activeCustomer);

        when(customerStatementMapper.restoreStatement(eq(STATEMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(STATEMENT_ID));
        verify(customerStatementMapper).restoreStatement(eq(STATEMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
        verify(customerStatementItemMapper).restoreByStatementId(eq(STATEMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("恢复操作异常：更新行数为0抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testRestore_updateZeroThrows() {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(STATEMENT_ID);
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(statement);

        when(customerStatementMapper.restoreStatement(eq(STATEMENT_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(STATEMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("引用守卫：草稿对账单无下游活跃引用")
    void testHasActiveReference() {
        assertFalse(handler.hasActiveReference(STATEMENT_ID));
    }

    @Test
    @DisplayName("彻底删除成功：级联物理删除明细行与主表")
    void testPurge_success() {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(STATEMENT_ID);
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(statement);
        when(customerStatementMapper.purgeStatement(STATEMENT_ID, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(STATEMENT_ID));
        verify(customerStatementItemMapper).purgeByStatementId(STATEMENT_ID, TENANT_ID);
        verify(customerStatementMapper).purgeStatement(STATEMENT_ID, TENANT_ID);
    }

    @Test
    @DisplayName("彻底删除异常：单据不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testPurge_notExists() {
        when(customerStatementMapper.selectDeletedById(STATEMENT_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(STATEMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

}
