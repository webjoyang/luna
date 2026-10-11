package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
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
 * {@link PaymentApplyRecycleBinHandler} 单元测试
 */
class PaymentApplyRecycleBinHandlerTest {

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @InjectMocks
    private PaymentApplyRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;
    private static final Long APPLY_ID = 7001L;
    private static final Long SUPPLIER_ID = 801L;
    private static final Long RECON_ID = 901L;

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
    @DisplayName("getBizType 校验：返回 PAYMENT_APPLY")
    void testGetBizType() {
        assertEquals("PAYMENT_APPLY", handler.getBizType());
    }

    @Test
    @DisplayName("pageDeleted：返回回收站分页")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        verify(paymentApplyMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID));
    }

    @Test
    @DisplayName("validateRestore：不存在于回收站抛异常")
    void testValidateRestore_NotFound_Throws() {
        when(paymentApplyMapper.selectDeletedById(APPLY_ID, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(APPLY_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore：供应商在回收站中，阻断恢复并提示")
    void testValidateRestore_SupplierInRecycle_Throws() {
        PaymentApplyDO apply = PaymentApplyDO.builder().id(APPLY_ID).supplierId(SUPPLIER_ID).build();
        when(paymentApplyMapper.selectDeletedById(APPLY_ID, TENANT_ID)).thenReturn(apply);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删除供应商");
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(APPLY_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("restore 成功：更新主数据 deleted=0, approval_status=PENDING")
    void testRestore_Success() {
        PaymentApplyDO apply = PaymentApplyDO.builder().id(APPLY_ID).supplierId(SUPPLIER_ID).reconId(RECON_ID).build();
        when(paymentApplyMapper.selectDeletedById(APPLY_ID, TENANT_ID)).thenReturn(apply);

        SupplierDO activeSupplier = new SupplierDO();
        activeSupplier.setId(SUPPLIER_ID);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID)).thenReturn(null);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(activeSupplier);

        SupplierReconDO recon = SupplierReconDO.builder().id(RECON_ID).build();
        when(supplierReconMapper.selectById(RECON_ID)).thenReturn(recon);

        when(paymentApplyMapper.restorePaymentApply(eq(APPLY_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(APPLY_ID));
        verify(paymentApplyMapper).restorePaymentApply(eq(APPLY_ID), eq(TENANT_ID), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("hasActiveReference：存在活跃引用返回 true")
    void testHasActiveReference() {
        when(paymentApplyMapper.selectActiveReferenceCount(APPLY_ID, TENANT_ID)).thenReturn(2L);
        assertTrue(handler.hasActiveReference(APPLY_ID));

        when(paymentApplyMapper.selectActiveReferenceCount(APPLY_ID, TENANT_ID)).thenReturn(0L);
        assertFalse(handler.hasActiveReference(APPLY_ID));
    }

    @Test
    @DisplayName("purge 存在活跃引用时抛 1_030_001_004")
    void testPurge_HasActiveReference_Throws() {
        PaymentApplyDO apply = PaymentApplyDO.builder().id(APPLY_ID).build();
        when(paymentApplyMapper.selectDeletedById(APPLY_ID, TENANT_ID)).thenReturn(apply);
        when(paymentApplyMapper.selectActiveReferenceCount(APPLY_ID, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(APPLY_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("purge 成功：物理删除")
    void testPurge_Success() {
        PaymentApplyDO apply = PaymentApplyDO.builder().id(APPLY_ID).build();
        when(paymentApplyMapper.selectDeletedById(APPLY_ID, TENANT_ID)).thenReturn(apply);
        when(paymentApplyMapper.selectActiveReferenceCount(APPLY_ID, TENANT_ID)).thenReturn(0L);
        when(paymentApplyMapper.purgePaymentApply(APPLY_ID, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(APPLY_ID));
        verify(paymentApplyMapper).purgePaymentApply(APPLY_ID, TENANT_ID);
    }

}
