package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link VendorReconRecycleBinHandler} 单元测试
 */
class VendorReconRecycleBinHandlerTest {

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierReconItemMapper supplierReconItemMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private VendorReconRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;
    private static final Long RECON_ID = 7001L;
    private static final Long SUPPLIER_ID = 801L;

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
    @DisplayName("getBizType 返回 VENDOR_RECON")
    void testGetBizType() {
        assertEquals("VENDOR_RECON", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿对账单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("VENDOR_RECON");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(RECON_ID);
            item.setDisplayName("SR20261011001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(supplierReconMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("SR20261011001", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("恢复校验：记录不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_notExists() {
        when(supplierReconMapper.selectDeletedById(RECON_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RECON_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：上游供应商在回收站中抛 RECYCLE_BIN_REFERENCE_IN_RECYCLE")
    void testValidateRestore_supplierInRecycle() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(RECON_ID)
                .supplierId(SUPPLIER_ID)
                .status("DRAFT")
                .build();
        when(supplierReconMapper.selectDeletedById(RECON_ID, TENANT_ID)).thenReturn(recon);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删供应商");
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RECON_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复成功：级联复活对账单与明细")
    void testRestore_success() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(RECON_ID)
                .supplierId(SUPPLIER_ID)
                .status("DRAFT")
                .build();
        when(supplierReconMapper.selectDeletedById(RECON_ID, TENANT_ID)).thenReturn(recon);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID)).thenReturn(null);

        SupplierDO activeSupplier = new SupplierDO();
        activeSupplier.setId(SUPPLIER_ID);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(activeSupplier);

        when(supplierReconMapper.restoreSupplierRecon(eq(RECON_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(1);

        handler.restore(RECON_ID);

        verify(supplierReconMapper).restoreSupplierRecon(eq(RECON_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
        verify(supplierReconItemMapper).restoreByReconId(eq(RECON_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除：存在活跃引用抛 RECYCLE_BIN_HAS_ACTIVE_REFERENCE")
    void testPurge_hasActiveReference() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(RECON_ID)
                .status("DRAFT")
                .build();
        when(supplierReconMapper.selectDeletedById(RECON_ID, TENANT_ID)).thenReturn(recon);
        when(supplierReconMapper.selectActiveReferenceCount(RECON_ID, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(RECON_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底删除成功：级联物理删除明细与主表")
    void testPurge_success() {
        SupplierReconDO recon = SupplierReconDO.builder()
                .id(RECON_ID)
                .status("DRAFT")
                .build();
        when(supplierReconMapper.selectDeletedById(RECON_ID, TENANT_ID)).thenReturn(recon);
        when(supplierReconMapper.selectActiveReferenceCount(RECON_ID, TENANT_ID)).thenReturn(0L);
        when(supplierReconMapper.purgeSupplierRecon(RECON_ID, TENANT_ID)).thenReturn(1);

        handler.purge(RECON_ID);

        verify(supplierReconItemMapper).purgeByReconId(RECON_ID, TENANT_ID);
        verify(supplierReconMapper).purgeSupplierRecon(RECON_ID, TENANT_ID);
    }

}
