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
public class ColorInstructionRecycleBinHandlerTest {

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
    private static final Long INSTRUCTION_ID = 8001L;
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
    @DisplayName("分页查询已软删打色单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("COLOR_INSTRUCTION");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("COLOR_INSTRUCTION");
            item.setBizId(INSTRUCTION_ID);
            item.setDisplayName("DS20261010001");
            item.setDeletedBy("admin");
            item.setDeletedTime(LocalDateTime.now());
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(colorInstructionMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("DS20261010001", result.getList().get(0).getDisplayName());
        assertEquals("COLOR_INSTRUCTION", result.getList().get(0).getBizType());
        verify(colorInstructionMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：打色单存在且客户、产品、供应商档案均正常")
    void testValidateRestore_success() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);
        instruction.setCustomerId(CUSTOMER_ID);
        instruction.setProductId(PRODUCT_ID);
        instruction.setLabFactoryId(SUPPLIER_ID);
        instruction.setStatus("PENDING");

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高支棉布");

        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("染厂供应商");

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(supplier);

        assertDoesNotThrow(() -> handler.validateRestore(INSTRUCTION_ID));
    }

    @Test
    @DisplayName("恢复校验：打色单本厂自做（labFactoryId为空）时正常通过")
    void testValidateRestore_labFactoryIdNull_success() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);
        instruction.setCustomerId(CUSTOMER_ID);
        instruction.setProductId(PRODUCT_ID);
        instruction.setLabFactoryId(null);
        instruction.setStatus("PENDING");

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高支棉布");

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        assertDoesNotThrow(() -> handler.validateRestore(INSTRUCTION_ID));
        verify(supplierMapper, never()).selectDeletedById(any(), any());
    }

    @Test
    @DisplayName("恢复校验：打色单记录不存在或未软删抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(INSTRUCTION_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：id为null抛 1_030_031_001")
    void testValidateRestore_nullId_throws1030031001() {
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(null));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：客户在回收站抛 1_030_031_002")
    void testValidateRestore_customerInRecycle_throws1030031002() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);
        instruction.setCustomerId(CUSTOMER_ID);
        instruction.setProductId(PRODUCT_ID);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("已删除客户");

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(INSTRUCTION_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除客户"));
    }

    @Test
    @DisplayName("恢复校验：产品在回收站抛 1_030_031_002")
    void testValidateRestore_productInRecycle_throws1030031002() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);
        instruction.setCustomerId(CUSTOMER_ID);
        instruction.setProductId(PRODUCT_ID);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setId(PRODUCT_ID);
        deletedProduct.setName("已删除产品");

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(INSTRUCTION_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除产品"));
    }

    @Test
    @DisplayName("恢复校验：打色承接方供应商在回收站抛 1_030_031_002")
    void testValidateRestore_supplierInRecycle_throws1030031002() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);
        instruction.setCustomerId(CUSTOMER_ID);
        instruction.setProductId(PRODUCT_ID);
        instruction.setLabFactoryId(SUPPLIER_ID);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高支棉布");

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删除打色厂");

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(INSTRUCTION_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除打色厂"));
    }

    @Test
    @DisplayName("恢复成功：主表复位为PENDING并级联复活全部颜色行（含历史REJECTED行）")
    void testRestore_success_withCascade() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);
        instruction.setCustomerId(CUSTOMER_ID);
        instruction.setProductId(PRODUCT_ID);
        instruction.setLabFactoryId(null);
        instruction.setStatus("PENDING");

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户A");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高支棉布");

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        // 主表恢复影响 1 行
        when(colorInstructionMapper.restoreColorInstruction(eq(INSTRUCTION_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        // 子表级联复活影响 3 行（包括APPROVED和历史REJECTED轮次行，不改status只改deleted=0）
        when(colorInstructionColorMapper.restoreByInstructionId(eq(INSTRUCTION_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(3);

        assertDoesNotThrow(() -> handler.restore(INSTRUCTION_ID));

        verify(colorInstructionMapper).restoreColorInstruction(eq(INSTRUCTION_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(colorInstructionColorMapper).restoreByInstructionId(eq(INSTRUCTION_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃下游单据引用抛 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(colorInstructionMapper.selectActiveReferenceCount(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(INSTRUCTION_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(colorInstructionColorMapper, never()).purgeByInstructionId(any(), any());
        verify(colorInstructionMapper, never()).purgeColorInstruction(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：先删除子表颜色明细，再删除主表打色单")
    void testPurge_success_withCascade() {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(INSTRUCTION_ID);

        when(colorInstructionMapper.selectDeletedById(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(instruction);
        when(colorInstructionMapper.selectActiveReferenceCount(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(0L);
        when(colorInstructionColorMapper.purgeByInstructionId(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(3);
        when(colorInstructionMapper.purgeColorInstruction(INSTRUCTION_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(INSTRUCTION_ID));

        verify(colorInstructionColorMapper).purgeByInstructionId(INSTRUCTION_ID, TENANT_ID_1);
        verify(colorInstructionMapper).purgeColorInstruction(INSTRUCTION_ID, TENANT_ID_1);
    }

}
