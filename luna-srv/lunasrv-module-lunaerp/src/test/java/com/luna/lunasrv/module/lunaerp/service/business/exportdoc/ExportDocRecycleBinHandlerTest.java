package com.luna.lunasrv.module.lunaerp.service.business.exportdoc;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportDocItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportDocMapper;
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
 * {@link ExportDocRecycleBinHandler} 单元测试
 */
class ExportDocRecycleBinHandlerTest {

    @Mock
    private ExportDocMapper exportDocMapper;

    @Mock
    private ExportDocItemMapper exportDocItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private ExportDocRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long DOC_ID = 5001L;
    private static final Long CUSTOMER_ID = 110L;

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
    @DisplayName("getBizType 返回 EXPORT_DOC")
    void testGetBizType() {
        assertEquals("EXPORT_DOC", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿单证套：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("EXPORT_DOC");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("EXPORT_DOC");
            item.setBizId(DOC_ID);
            item.setDisplayName("ED20260001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(exportDocMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("ED20260001", result.getList().get(0).getDisplayName());
        verify(exportDocMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且客户正常")
    void testValidateRestore_success() {
        ExportDocDO doc = new ExportDocDO();
        doc.setId(DOC_ID);
        doc.setCustomerId(CUSTOMER_ID);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户C");

        when(exportDocMapper.selectDeletedById(DOC_ID, TENANT_ID_1)).thenReturn(doc);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);

        assertDoesNotThrow(() -> handler.validateRestore(DOC_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists() {
        when(exportDocMapper.selectDeletedById(DOC_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(DOC_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：客户在回收站抛 1_030_031_002")
    void testValidateRestore_customerInRecycle() {
        ExportDocDO doc = new ExportDocDO();
        doc.setId(DOC_ID);
        doc.setCustomerId(CUSTOMER_ID);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("已删除客户C");

        when(exportDocMapper.selectDeletedById(DOC_ID, TENANT_ID_1)).thenReturn(doc);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(DOC_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除客户C"));
    }

    @Test
    @DisplayName("恢复成功：级联复活明细")
    void testRestore_success_withCascade() {
        ExportDocDO doc = new ExportDocDO();
        doc.setId(DOC_ID);

        when(exportDocMapper.selectDeletedById(DOC_ID, TENANT_ID_1)).thenReturn(doc);
        when(exportDocMapper.restoreExportDoc(eq(DOC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(exportDocItemMapper.restoreByDocId(eq(DOC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);

        assertDoesNotThrow(() -> handler.restore(DOC_ID));

        verify(exportDocMapper).restoreExportDoc(eq(DOC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(exportDocItemMapper).restoreByDocId(eq(DOC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除成功：先删明细，再删单证套主表")
    void testPurge_success_withCascade() {
        ExportDocDO doc = new ExportDocDO();
        doc.setId(DOC_ID);

        when(exportDocMapper.selectDeletedById(DOC_ID, TENANT_ID_1)).thenReturn(doc);
        when(exportDocItemMapper.purgeByDocId(DOC_ID, TENANT_ID_1)).thenReturn(2);
        when(exportDocMapper.purgeExportDoc(DOC_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(DOC_ID));

        verify(exportDocItemMapper).purgeByDocId(DOC_ID, TENANT_ID_1);
        verify(exportDocMapper).purgeExportDoc(DOC_ID, TENANT_ID_1);
    }
}
