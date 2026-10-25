package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementMapper;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerStatementRecycleBinHandler} 纯 Mockito 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerStatementRecycleBinHandlerTest {

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerStatementRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private CustomerStatementDO createDeletedStatement(Long id, Long customerId) {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(id);
        statement.setCustomerId(customerId);
        return statement;
    }

    private CustomerDO createCustomer(Long id, String name) {
        CustomerDO customer = new CustomerDO();
        customer.setId(id);
        customer.setCompanyName(name);
        return customer;
    }

    @Test
    @DisplayName("验证业务类型标识为 STATEMENT")
    void testGetBizType() {
        assertEquals("STATEMENT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询回收站已软删草稿客户对账单")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(101L);
            item.setDisplayName("CS20260001");
            page.setRecords(List.of(item));
            page.setTotal(1);
            return page;
        }).when(customerStatementMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("CS20260001", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("恢复草稿对账单：非草稿态（或记录不存在）被过滤返回 null 抛出 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testRestore_nonDraftOrNotExists_throwsException() {
        Long statementId = 999L;
        when(customerStatementMapper.selectDeletedById(statementId, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(statementId));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿对账单：关联客户档案不存在时抛出 CUSTOMER_NOT_EXISTS")
    void testRestore_customerNotFound_throwsException() {
        Long statementId = 201L;
        CustomerStatementDO statement = createDeletedStatement(statementId, 301L);
        when(customerStatementMapper.selectDeletedById(statementId, TENANT_ID)).thenReturn(statement);
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(null);
        when(customerMapper.selectById(301L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(statementId));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿对账单：关联客户在回收站中抛出 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testRestore_customerInRecycle_throwsException() {
        Long statementId = 201L;
        CustomerStatementDO statement = createDeletedStatement(statementId, 301L);
        when(customerStatementMapper.selectDeletedById(statementId, TENANT_ID)).thenReturn(statement);

        CustomerDO deletedCustomer = createCustomer(301L, "测试客户A");
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(statementId));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复草稿对账单：正常恢复主表为 DRAFT 态并级联复活明细行（不补账）")
    void testRestore_success() {
        Long statementId = 201L;
        CustomerStatementDO statement = createDeletedStatement(statementId, 301L);

        when(customerStatementMapper.selectDeletedById(statementId, TENANT_ID)).thenReturn(statement);
        when(customerMapper.selectDeletedById(301L, TENANT_ID)).thenReturn(null);
        CustomerDO activeCustomer = createCustomer(301L, "活跃客户A");
        when(customerMapper.selectById(301L)).thenReturn(activeCustomer);

        when(customerStatementMapper.restoreCustomerStatement(eq(statementId), eq(TENANT_ID), any(), any())).thenReturn(1);
        when(customerStatementItemMapper.restoreByStatementId(eq(statementId), eq(TENANT_ID), any(), any())).thenReturn(3);

        assertDoesNotThrow(() -> handler.restore(statementId));

        verify(customerStatementMapper).restoreCustomerStatement(eq(statementId), eq(TENANT_ID), any(), any());
        verify(customerStatementItemMapper).restoreByStatementId(eq(statementId), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("彻底删除草稿对账单：有活跃业务引用抛出 RECYCLE_BIN_HAS_ACTIVE_REFERENCE")
    void testPurge_hasActiveReference_throwsException() {
        Long statementId = 201L;
        CustomerStatementDO statement = createDeletedStatement(statementId, 301L);

        when(customerStatementMapper.selectDeletedById(statementId, TENANT_ID)).thenReturn(statement);
        when(customerStatementMapper.selectActiveReferenceCount(statementId, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(statementId));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底删除草稿对账单：物理删除子表与主表")
    void testPurge_success() {
        Long statementId = 201L;
        CustomerStatementDO statement = createDeletedStatement(statementId, 301L);

        when(customerStatementMapper.selectDeletedById(statementId, TENANT_ID)).thenReturn(statement);
        when(customerStatementMapper.selectActiveReferenceCount(statementId, TENANT_ID)).thenReturn(0L);
        when(customerStatementItemMapper.purgeByStatementId(statementId, TENANT_ID)).thenReturn(3);
        when(customerStatementMapper.purgeCustomerStatement(statementId, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(statementId));

        verify(customerStatementItemMapper).purgeByStatementId(statementId, TENANT_ID);
        verify(customerStatementMapper).purgeCustomerStatement(statementId, TENANT_ID);
    }
}
