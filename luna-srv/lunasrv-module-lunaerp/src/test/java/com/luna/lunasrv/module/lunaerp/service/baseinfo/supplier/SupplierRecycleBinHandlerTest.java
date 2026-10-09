package com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.suppliercontact.SupplierContactMapper;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SupplierRecycleBinHandler} 单元测试
 *
 * 覆盖供应商回收站关键业务语义：
 *  1. 回收站分页查询带 tenant_id 隔离
 *  2. 恢复校验（存在且 deleted=1 通过，不存在抛 1_030_031_001）
 *  3. 恢复成功并级联复活联系人（父->子批量 deleted=0）
 *  4. 并发条件更新防重入：影响行数 0 即报错（抛 1_030_031_001）
 *  5. 横向越权防范（他租户 bizId 查不到或更新为 0 报错）
 *  6. 彻底删除引用守卫（存在有效引用抛 1_030_031_003 阻断）
 *  7. 彻底删除成功并级联物理删除联系人
 *
 * @author luna
 */
public class SupplierRecycleBinHandlerTest {

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SupplierContactMapper supplierContactMapper;

    @InjectMocks
    private SupplierRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long TENANT_ID_2 = 2L;
    private static final Long SUPPLIER_ID = 2048L;

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
    @DisplayName("getBizType 返回 SUPPLIER")
    void testGetBizType() {
        assertEquals("SUPPLIER", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询已软删供应商：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("SUPPLIER");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("SUPPLIER");
            item.setBizId(SUPPLIER_ID);
            item.setDisplayName("盛泽纺织原料厂");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(supplierMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("盛泽纺织原料厂", result.getList().get(0).getDisplayName());
        verify(supplierMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：记录存在且已删除通过")
    void testValidateRestore_success() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("盛泽纺织原料厂");

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(supplier);

        assertDoesNotThrow(() -> handler.validateRestore(SUPPLIER_ID));
        verify(supplierMapper).selectDeletedById(SUPPLIER_ID, TENANT_ID_1);
    }

    @Test
    @DisplayName("恢复校验：记录不存在或未删除抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(SUPPLIER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：ID 为空抛 1_030_031_001")
    void testValidateRestore_nullId_throws1030031001() {
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(null));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复供应商：主数据恢复为 INACTIVE 且级联复活联系人")
    void testRestore_success_withCascade() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("盛泽纺织原料厂");

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(supplier);
        when(supplierMapper.restoreSupplier(eq(SUPPLIER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(supplierContactMapper.restoreContactsBySupplierId(eq(SUPPLIER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);

        assertDoesNotThrow(() -> handler.restore(SUPPLIER_ID));

        verify(supplierMapper).restoreSupplier(eq(SUPPLIER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(supplierContactMapper).restoreContactsBySupplierId(eq(SUPPLIER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("恢复供应商：并发条件更新影响行数 0 报错（防重入与并发冲突）")
    void testRestore_concurrentUpdate_affectedRowsZero_throws1030031001() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("盛泽纺织原料厂");

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(supplier);
        when(supplierMapper.restoreSupplier(eq(SUPPLIER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(SUPPLIER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(supplierContactMapper, never()).restoreContactsBySupplierId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("横向越权防御：租户 A 尝试恢复租户 B 的供应商档案，查不到报错")
    void testRestore_crossTenant_throws1030031001() {
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(SUPPLIER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(supplierMapper, never()).restoreSupplier(any(), any(), any(), any());
        verify(supplierContactMapper, never()).restoreContactsBySupplierId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用（deleted=0）时阻断并报 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("盛泽纺织原料厂");

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(supplier);
        when(supplierMapper.selectActiveReferenceCount(SUPPLIER_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(SUPPLIER_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(supplierMapper, never()).purgeSupplier(any(), any());
        verify(supplierContactMapper, never()).purgeContactsBySupplierId(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：无有效引用时物理删除供应商并级联物理删除联系人")
    void testPurge_success_withCascade() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("盛泽纺织原料厂");

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(supplier);
        when(supplierMapper.selectActiveReferenceCount(SUPPLIER_ID, TENANT_ID_1)).thenReturn(0L);
        when(supplierMapper.purgeSupplier(SUPPLIER_ID, TENANT_ID_1)).thenReturn(1);
        when(supplierContactMapper.purgeContactsBySupplierId(SUPPLIER_ID, TENANT_ID_1)).thenReturn(2);

        assertDoesNotThrow(() -> handler.purge(SUPPLIER_ID));

        verify(supplierMapper).purgeSupplier(SUPPLIER_ID, TENANT_ID_1);
        verify(supplierContactMapper).purgeContactsBySupplierId(SUPPLIER_ID, TENANT_ID_1);
    }

    @Test
    @DisplayName("彻底删除并发或未找到：影响行数 0 报错")
    void testPurge_concurrentOrMissing_affectedRowsZero_throws1030031001() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("盛泽纺织原料厂");

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(supplier);
        when(supplierMapper.selectActiveReferenceCount(SUPPLIER_ID, TENANT_ID_1)).thenReturn(0L);
        when(supplierMapper.purgeSupplier(SUPPLIER_ID, TENANT_ID_1)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(SUPPLIER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(supplierContactMapper, never()).purgeContactsBySupplierId(any(), any());
    }

    @Test
    @DisplayName("横向越权防御：租户 A 尝试彻底删除租户 B 的供应商档案，查不到报错")
    void testPurge_crossTenant_throws1030031001() {
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(SUPPLIER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(supplierMapper, never()).purgeSupplier(any(), any());
        verify(supplierContactMapper, never()).purgeContactsBySupplierId(any(), any());
    }

}
