package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
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
 * {@link ColorInstructionRecycleHandler} 单元测试
 *
 * @author luna
 */
public class ColorInstructionRecycleHandlerTest {

    @Mock
    private ColorInstructionMapper colorInstructionMapper;

    @Mock
    private ColorInstructionColorMapper colorInstructionColorMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private ColorInstructionRecycleHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long ORDER_ID = 5001L;
    private static final Long CUSTOMER_ID = 100L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long SUPPLIER_ID = 300L;

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
    @DisplayName("getBizType 返回 COLOR_INSTRUCTION")
    void testGetBizType() {
        assertEquals("COLOR_INSTRUCTION", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询已软删打色指示单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("COLOR_INSTRUCTION");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("COLOR_INSTRUCTION");
            item.setBizId(ORDER_ID);
            item.setDisplayName("DS20261014001 / 某客户 / 某产品 / 2026-10-14");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(colorInstructionMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("DS20261014001 / 某客户 / 某产品 / 2026-10-14", result.getList().get(0).getDisplayName());
        verify(colorInstructionMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：打色单存在且客户、产品与供应商档案均正常")
    void testValidateRestore_success() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setProductId(PRODUCT_ID);
        order.setLabFactoryId(SUPPLIER_ID);
        order.setStatus("PENDING");

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("优衣库服装");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高密全棉府绸");

        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("科源打色房");

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(supplier);

        assertDoesNotThrow(() -> handler.validateRestore(ORDER_ID));
    }

    @Test
    @DisplayName("恢复校验：本厂自打色（labFactoryId 为空）跳过供应商校验成功")
    void testValidateRestore_selfLab_success() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setProductId(PRODUCT_ID);
        order.setLabFactoryId(null);
        order.setStatus("PENDING");

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("优衣库服装");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高密全棉府绸");

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        assertDoesNotThrow(() -> handler.validateRestore(ORDER_ID));
        verify(supplierMapper, never()).selectDeletedById(any(), any());
    }

    @Test
    @DisplayName("恢复校验：记录不存在或不在回收站抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：非 PENDING 态单据（CONFIRMED/IN_PRODUCTION/VOID）被 selectDeletedById 状态过滤排斥，抛 1_030_031_001")
    void testValidateRestore_nonPendingFilteredOut_throws1030031001() {
        // 由于 XML 中硬编码 WHERE status = 'PENDING'，非 PENDING 记录查出为 null，不在回收站中
        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：客户档案在回收站阻断恢复抛 1_030_031_002")
    void testValidateRestore_customerInRecycle_throws1030031002() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("已删除客户");

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除客户"));
    }

    @Test
    @DisplayName("恢复校验：客户档案已彻底删除抛 1_030_031_002")
    void testValidateRestore_customerMissing_throws1030031002() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("客户档案"));
    }

    @Test
    @DisplayName("恢复校验：产品档案在回收站阻断恢复抛 1_030_031_002")
    void testValidateRestore_productInRecycle_throws1030031002() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setProductId(PRODUCT_ID);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("优衣库服装");

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setId(PRODUCT_ID);
        deletedProduct.setName("已删除产品");

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除产品"));
    }

    @Test
    @DisplayName("恢复校验：供应商在回收站阻断恢复抛 1_030_031_002")
    void testValidateRestore_supplierInRecycle_throws1030031002() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setProductId(PRODUCT_ID);
        order.setLabFactoryId(SUPPLIER_ID);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("优衣库服装");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高密全棉府绸");

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删除打色厂");

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除打色厂"));
    }

    @Test
    @DisplayName("恢复成功：主表复位 PENDING，级联复活全部颜色行（含历史 REJECTED 行，按 instruction_id 批量复活）")
    void testRestore_success_withCascadeAllColorRows() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(colorInstructionMapper.restoreColorInstruction(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(colorInstructionColorMapper.restoreByInstructionId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(3);

        assertDoesNotThrow(() -> handler.restore(ORDER_ID));

        verify(colorInstructionMapper).restoreColorInstruction(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(colorInstructionColorMapper).restoreByInstructionId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("恢复并发失败：更新主表影响行数 0 抛 1_030_031_001")
    void testRestore_concurrentFail_throws1030031001() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(colorInstructionMapper.restoreColorInstruction(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
        verify(colorInstructionColorMapper, never()).restoreByInstructionId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用抛 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(colorInstructionMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(ORDER_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(colorInstructionColorMapper, never()).purgeByInstructionId(any(), any());
        verify(colorInstructionMapper, never()).purgeColorInstruction(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：先物理删除颜色明细，再物理删除打色单主表")
    void testPurge_success_withCascade() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(ORDER_ID);

        when(colorInstructionMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(colorInstructionMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(0L);
        when(colorInstructionColorMapper.purgeByInstructionId(ORDER_ID, TENANT_ID_1)).thenReturn(3);
        when(colorInstructionMapper.purgeColorInstruction(ORDER_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(ORDER_ID));

        verify(colorInstructionColorMapper).purgeByInstructionId(ORDER_ID, TENANT_ID_1);
        verify(colorInstructionMapper).purgeColorInstruction(ORDER_ID, TENANT_ID_1);
    }

}
