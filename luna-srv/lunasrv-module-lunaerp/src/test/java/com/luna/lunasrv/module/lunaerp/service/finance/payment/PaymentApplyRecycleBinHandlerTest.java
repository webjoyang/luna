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
 * {@link PaymentApplyRecycleBinHandler} 纯 Mockito 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PaymentApplyRecycleBinHandlerTest {

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private PaymentApplyRecycleBinHandler handler;

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
    @DisplayName("bizType 校验：必须为 PAYMENT_APPLY")
    void testBizType() {
        assertEquals("PAYMENT_APPLY", handler.getBizType());
    }

    @Test
    @DisplayName("回收站分页查询")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);
        PageResult<RecycleItemRespVO> res = handler.pageDeleted(req);
        assertNotNull(res);
        verify(paymentApplyMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID));
    }

    @Test
    @DisplayName("恢复草稿付款申请成功：条件更新为 PENDING")
    void testRestore_success() {
        Long id = 1001L;
        Long supplierId = 3001L;
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(id);
        apply.setSupplierId(supplierId);

        when(paymentApplyMapper.selectDeletedById(id, TENANT_ID)).thenReturn(apply);
        when(supplierMapper.selectDeletedById(supplierId, TENANT_ID)).thenReturn(null);
        when(supplierMapper.selectById(supplierId)).thenReturn(new SupplierDO());
        when(paymentApplyMapper.restorePaymentApply(eq(id), eq(TENANT_ID), any(), any())).thenReturn(1);

        handler.restore(id);

        verify(paymentApplyMapper).restorePaymentApply(eq(id), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("恢复校验：关联供应商在回收站中阻断并报错")
    void testRestore_supplierInRecycle_throws() {
        Long id = 1001L;
        Long supplierId = 3001L;
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(id);
        apply.setSupplierId(supplierId);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(supplierId);
        deletedSupplier.setCompanyName("某供应商");

        when(paymentApplyMapper.selectDeletedById(id, TENANT_ID)).thenReturn(apply);
        when(supplierMapper.selectDeletedById(supplierId, TENANT_ID)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(id));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联对账单在回收站中阻断并报错")
    void testRestore_reconInRecycle_throws() {
        Long id = 1001L;
        Long supplierId = 3001L;
        Long reconId = 2001L;
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(id);
        apply.setSupplierId(supplierId);
        apply.setReconId(reconId);

        when(paymentApplyMapper.selectDeletedById(id, TENANT_ID)).thenReturn(apply);
        when(supplierMapper.selectDeletedById(supplierId, TENANT_ID)).thenReturn(null);
        when(supplierMapper.selectById(supplierId)).thenReturn(new SupplierDO());

        SupplierReconDO deletedRecon = new SupplierReconDO();
        deletedRecon.setId(reconId);
        deletedRecon.setReconNo("SR20261015001");
        when(supplierReconMapper.selectDeletedById(reconId, TENANT_ID)).thenReturn(deletedRecon);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(id));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底物理删除：有活跃引用时禁止删除并报 RECYCLE_BIN_HAS_ACTIVE_REFERENCE")
    void testPurge_hasActiveReference_throws() {
        Long id = 1001L;
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(id);

        when(paymentApplyMapper.selectDeletedById(id, TENANT_ID)).thenReturn(apply);
        when(paymentApplyMapper.selectActiveReferenceCount(id, TENANT_ID)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(id));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底物理删除成功")
    void testPurge_success() {
        Long id = 1001L;
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(id);

        when(paymentApplyMapper.selectDeletedById(id, TENANT_ID)).thenReturn(apply);
        when(paymentApplyMapper.selectActiveReferenceCount(id, TENANT_ID)).thenReturn(0L);
        when(paymentApplyMapper.purgePaymentApply(id, TENANT_ID)).thenReturn(1);

        handler.purge(id);

        verify(paymentApplyMapper).purgePaymentApply(id, TENANT_ID);
    }

}
