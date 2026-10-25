package com.luna.lunasrv.module.lunaerp.service.production.yarnorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link YarnOrderRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class YarnOrderRecycleBinHandlerTest {

    @Mock
    private YarnOrderMapper yarnOrderMapper;

    @Mock
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @InjectMocks
    private YarnOrderRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long ORDER_ID = 5001L;
    private static final Long SUPPLIER_ID = 701L;
    private static final Long YARN_TYPE_ID = 702L;
    private static final Long YARN_SPEC_ID = 703L;

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
    @DisplayName("getBizType 返回 YARN_PURCHASE_ORDER")
    void testGetBizType() {
        assertEquals("YARN_PURCHASE_ORDER", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿纱线采购单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("YARN_PURCHASE_ORDER");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("YARN_PURCHASE_ORDER");
            item.setBizId(ORDER_ID);
            item.setDisplayName("YP20261001-001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(yarnOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("YP20261001-001", result.getList().get(0).getDisplayName());
        verify(yarnOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且明细中的供应商/材质/规格正常")
    void testValidateRestore_success() {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(ORDER_ID);

        YarnOrderItemDO item = new YarnOrderItemDO();
        item.setOrderId(ORDER_ID);
        item.setSupplierId(SUPPLIER_ID);
        item.setYarnTypeId(YARN_TYPE_ID);
        item.setYarnSpecId(YARN_SPEC_ID);

        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("纱线供应商A");

        YarnTypeDO yarnType = new YarnTypeDO();
        yarnType.setId(YARN_TYPE_ID);
        yarnType.setName("纯棉");

        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setSpecName("32S 精梳棉");

        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(yarnOrderItemMapper.selectDeletedListByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(List.of(item));

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(supplier);

        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnTypeMapper.selectById(YARN_TYPE_ID)).thenReturn(yarnType);

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnSpecMapper.selectById(YARN_SPEC_ID)).thenReturn(yarnSpec);

        assertDoesNotThrow(() -> handler.validateRestore(ORDER_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：供应商在回收站抛 1_030_031_002")
    void testValidateRestore_supplierInRecycle_throws1030031002() {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(ORDER_ID);

        YarnOrderItemDO item = new YarnOrderItemDO();
        item.setSupplierId(SUPPLIER_ID);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删除纱线供应商");

        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(yarnOrderItemMapper.selectDeletedListByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(List.of(item));
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除纱线供应商"));
    }

    @Test
    @DisplayName("恢复校验：纱线材质在回收站抛 1_030_031_002")
    void testValidateRestore_yarnTypeInRecycle_throws1030031002() {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(ORDER_ID);

        YarnOrderItemDO item = new YarnOrderItemDO();
        item.setYarnTypeId(YARN_TYPE_ID);

        YarnTypeDO deletedType = new YarnTypeDO();
        deletedType.setId(YARN_TYPE_ID);
        deletedType.setName("已删除材质");

        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(yarnOrderItemMapper.selectDeletedListByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(List.of(item));
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(deletedType);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除材质"));
    }

    @Test
    @DisplayName("恢复校验：纱线规格在回收站抛 1_030_031_002")
    void testValidateRestore_yarnSpecInRecycle_throws1030031002() {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(ORDER_ID);

        YarnOrderItemDO item = new YarnOrderItemDO();
        item.setYarnSpecId(YARN_SPEC_ID);

        YarnSpecDO deletedSpec = new YarnSpecDO();
        deletedSpec.setId(YARN_SPEC_ID);
        deletedSpec.setSpecName("已删除规格");

        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(yarnOrderItemMapper.selectDeletedListByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(List.of(item));
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(deletedSpec);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除规格"));
    }

    @Test
    @DisplayName("恢复成功：级联复活采购明细")
    void testRestore_success_withCascade() {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(ORDER_ID);

        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(yarnOrderItemMapper.selectDeletedListByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(Collections.emptyList());
        when(yarnOrderMapper.restoreYarnOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(yarnOrderItemMapper.restoreByOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(3);

        assertDoesNotThrow(() -> handler.restore(ORDER_ID));

        verify(yarnOrderMapper).restoreYarnOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(yarnOrderItemMapper).restoreByOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除成功：先删除采购明细，再删除采购主表")
    void testPurge_success_withCascade() {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(ORDER_ID);

        when(yarnOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(yarnOrderMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(0L);
        when(yarnOrderItemMapper.purgeByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(3);
        when(yarnOrderMapper.purgeYarnOrder(ORDER_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(ORDER_ID));

        verify(yarnOrderItemMapper).purgeByOrderId(ORDER_ID, TENANT_ID_1);
        verify(yarnOrderMapper).purgeYarnOrder(ORDER_ID, TENANT_ID_1);
    }

}
