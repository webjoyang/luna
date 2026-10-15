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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SupplierReconRecycleBinHandler} 纯 Mockito 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class SupplierReconRecycleBinHandlerTest {

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierReconItemMapper supplierReconItemMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierReconRecycleBinHandler handler;

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
    @DisplayName("bizType 校验：必须为 VENDOR_RECON")
    void testBizType() {
        assertEquals("VENDOR_RECON", handler.getBizType());
    }

    @Test
    @DisplayName("回收站分页查询")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);
        PageResult<RecycleItemRespVO> res = handler.pageDeleted(req);
        assertNotNull(res);
        verify(supplierReconMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID));
    }

    @Test
    @DisplayName("恢复草稿对账单：级联复活明细行")
    void testRestore_Success() {
        Long id = 100L;
        Long supplierId = 10L;
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(id);
        recon.setSupplierId(supplierId);

        when(supplierReconMapper.selectDeletedById(id, TENANT_ID)).thenReturn(recon);
        when(supplierMapper.selectDeletedById(supplierId, TENANT_ID)).thenReturn(null);
        when(supplierMapper.selectById(supplierId)).thenReturn(new SupplierDO());
        when(supplierReconMapper.restoreSupplierRecon(eq(id), eq(TENANT_ID), any(), any())).thenReturn(1);

        handler.restore(id);

        verify(supplierReconMapper).restoreSupplierRecon(eq(id), eq(TENANT_ID), any(), any());
        verify(supplierReconItemMapper).restoreByReconId(eq(id), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("恢复校验：关联供应商在回收站中阻断并报错")
    void testValidateRestore_SupplierInRecycle_Throws() {
        Long id = 100L;
        Long supplierId = 10L;
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(id);
        recon.setSupplierId(supplierId);

        when(supplierReconMapper.selectDeletedById(id, TENANT_ID)).thenReturn(recon);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setCompanyName("已删供应商");
        when(supplierMapper.selectDeletedById(supplierId, TENANT_ID)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(id));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("物理删除：级联物理删除行表并物理删除主表")
    void testPurge_Success() {
        Long id = 100L;
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(id);

        when(supplierReconMapper.selectDeletedById(id, TENANT_ID)).thenReturn(recon);
        when(supplierReconMapper.selectActiveReferenceCount(id, TENANT_ID)).thenReturn(0L);
        when(supplierReconMapper.purgeSupplierRecon(id, TENANT_ID)).thenReturn(1);

        handler.purge(id);

        verify(supplierReconItemMapper).purgeByReconId(id, TENANT_ID);
        verify(supplierReconMapper).purgeSupplierRecon(id, TENANT_ID);
    }

    @Test
    @DisplayName("物理删除校验：存在活跃付款申请引用阻断")
    void testValidatePurge_ActiveReference_Throws() {
        Long id = 100L;
        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(id);

        when(supplierReconMapper.selectDeletedById(id, TENANT_ID)).thenReturn(recon);
        when(supplierReconMapper.selectActiveReferenceCount(id, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validatePurge(id));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

}
