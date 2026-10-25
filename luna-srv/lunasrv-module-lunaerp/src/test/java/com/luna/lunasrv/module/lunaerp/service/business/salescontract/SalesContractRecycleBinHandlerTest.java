package com.luna.lunasrv.module.lunaerp.service.business.salescontract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SalesContractRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class SalesContractRecycleBinHandlerTest {

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private SalesContractItemMapper salesContractItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private SalesContractRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long CONTRACT_ID = 2048L;
    private static final Long CUSTOMER_ID = 100L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(TENANT_ID_1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType 返回 SALES_CONTRACT")
    void testGetBizType() {
        assertEquals("SALES_CONTRACT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿销售合同：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("SALES_CONTRACT");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("SALES_CONTRACT");
            item.setBizId(CONTRACT_ID);
            item.setDisplayName("HT20261001-001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(salesContractMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("HT20261001-001", result.getList().get(0).getDisplayName());
        verify(salesContractMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且客户正常")
    void testValidateRestore_success() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(CONTRACT_ID);
        contract.setCustomerId(CUSTOMER_ID);
        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        when(salesContractMapper.selectDeletedById(CONTRACT_ID, TENANT_ID_1)).thenReturn(contract);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);

        assertDoesNotThrow(() -> handler.validateRestore(CONTRACT_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(salesContractMapper.selectDeletedById(CONTRACT_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(CONTRACT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联客户在回收站抛 1_030_031_002 提示客户名")
    void testValidateRestore_customerInRecycle_throws1030031002() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(CONTRACT_ID);
        contract.setCustomerId(CUSTOMER_ID);
        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("待恢复客户A");

        when(salesContractMapper.selectDeletedById(CONTRACT_ID, TENANT_ID_1)).thenReturn(contract);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(CONTRACT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("待恢复客户A"));
    }

    @Test
    @DisplayName("恢复成功：级联复活合同明细")
    void testRestore_success_withCascade() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(CONTRACT_ID);
        contract.setCustomerId(CUSTOMER_ID);
        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        when(salesContractMapper.selectDeletedById(CONTRACT_ID, TENANT_ID_1)).thenReturn(contract);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(salesContractMapper.restoreSalesContract(eq(CONTRACT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(salesContractItemMapper.restoreByContractId(eq(CONTRACT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(3);

        assertDoesNotThrow(() -> handler.restore(CONTRACT_ID));

        verify(salesContractMapper).restoreSalesContract(eq(CONTRACT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(salesContractItemMapper).restoreByContractId(eq(CONTRACT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用抛 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(CONTRACT_ID);

        when(salesContractMapper.selectDeletedById(CONTRACT_ID, TENANT_ID_1)).thenReturn(contract);
        when(salesContractMapper.selectActiveReferenceCount(CONTRACT_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(CONTRACT_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(salesContractItemMapper, never()).purgeByContractId(any(), any());
        verify(salesContractMapper, never()).purgeSalesContract(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：先删除子表明细，再删除合同主表")
    void testPurge_success_withCascade() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(CONTRACT_ID);

        when(salesContractMapper.selectDeletedById(CONTRACT_ID, TENANT_ID_1)).thenReturn(contract);
        when(salesContractMapper.selectActiveReferenceCount(CONTRACT_ID, TENANT_ID_1)).thenReturn(0L);
        when(salesContractItemMapper.purgeByContractId(CONTRACT_ID, TENANT_ID_1)).thenReturn(3);
        when(salesContractMapper.purgeSalesContract(CONTRACT_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(CONTRACT_ID));

        verify(salesContractItemMapper).purgeByContractId(CONTRACT_ID, TENANT_ID_1);
        verify(salesContractMapper).purgeSalesContract(CONTRACT_ID, TENANT_ID_1);
    }

}
