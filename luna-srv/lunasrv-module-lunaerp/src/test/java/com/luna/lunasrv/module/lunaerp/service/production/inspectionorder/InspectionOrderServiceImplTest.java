package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;

import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link InspectionOrderServiceImpl} 的纯 Mockito 单元测试。
 *
 * 01 票：覆盖列表分页查询条件透传（keyword OR 模糊、type/status/inspectionFactoryId 等值、
 * 单据日期区间、默认 order_date 倒序）。
 * 02 票：覆盖主表新建/编辑、编号取号/查重/重试、关联校验与快照写入、详情与预览编号。
 *
 * @author luna
 */
class InspectionOrderServiceImplTest {

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private GreigeOrderMapper greigeOrderMapper;
    @Mock
    private DyeingOrderMapper dyeingOrderMapper;
    @Mock
    private SalesContractMapper salesContractMapper;
    @Mock
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @InjectMocks

    private InspectionOrderServiceImpl inspectionOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderDO.class);
    }

    // ==================== getInspectionOrderPage：查询条件透传（01 票） ====================

    @Test
    void getInspectionOrderPage_filtersPassedThrough_andOrderedByOrderDateDesc() {
        InspectionOrderPageReqVO reqVO = new InspectionOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("INS-2026");
        reqVO.setType("GREIGE");
        reqVO.setStatus("ACTIVE");
        reqVO.setInspectionFactoryId(2048L);
        reqVO.setPackageMode("PIECE");
        reqVO.setOrderDateStart(LocalDate.of(2026, 9, 1));
        reqVO.setOrderDateEnd(LocalDate.of(2026, 9, 21));

        when(inspectionOrderMapper.selectPage(any(InspectionOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        inspectionOrderService.getInspectionOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionOrderMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<InspectionOrderDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        // keyword OR 模糊命中 code / product_code / product_name / inspection_factory_name
        assertTrue(sqlSegment.contains("LOWER(code) LIKE"), "keyword 应透传到 code 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_code) LIKE"), "keyword 应透传到 product_code 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_name) LIKE"), "keyword 应透传到 product_name 模糊");
        assertTrue(sqlSegment.contains("LOWER(inspection_factory_name) LIKE"), "keyword 应透传到 inspection_factory_name 模糊");
        // type 等值透传
        assertTrue(sqlSegment.contains("type ="), "type 应透传等值过滤");
        // status 等值透传
        assertTrue(sqlSegment.contains("status ="), "status 应透传等值过滤");
        // inspection_factory_id 等值透传
        assertTrue(sqlSegment.contains("inspection_factory_id ="), "inspectionFactoryId 应透传等值过滤");
        // package_mode 等值透传
        assertTrue(sqlSegment.contains("package_mode ="), "packageMode 应透传等值过滤");
        // 日期区间透传
        assertTrue(sqlSegment.contains("order_date BETWEEN") || (sqlSegment.contains("order_date >=") && sqlSegment.contains("order_date <=")),
                "orderDate 区间应透传");
        // 默认单据日期倒序
        assertTrue(sqlSegment.contains("ORDER BY order_date DESC"), "默认应按单据日期倒序");
        // keyword 参数值透传
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%INS-2026%"),
                "keyword 参数值应预编译透传");
    }

    @Test
    void getInspectionOrderPage_noFilters_onlyOrderByOrderDateDesc() {
        InspectionOrderPageReqVO reqVO = new InspectionOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(inspectionOrderMapper.selectPage(any(InspectionOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        inspectionOrderService.getInspectionOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionOrderMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertFalse(sqlSegment.contains("WHERE"), "无过滤条件时不应产生 WHERE");
        assertTrue(sqlSegment.contains("ORDER BY order_date DESC"), "仍应按单据日期倒序");
    }

    @Test
    void getInspectionOrderPage_blankKeyword_doesNotAddLikeCondition() {
        InspectionOrderPageReqVO reqVO = new InspectionOrderPageReqVO();
        reqVO.setKeyword("   ");
        when(inspectionOrderMapper.selectPage(any(InspectionOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        inspectionOrderService.getInspectionOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionOrderMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertFalse(sqlSegment.contains("LIKE"), "空白 keyword 不应追加 LIKE 条件");
    }

    // ==================== Issue 02: 主表新建/编辑/编号/快照/详情 ====================

    @Test
    void createInspectionOrder_success_withGeneratedCode_andDefaultStatusDraft_andSnapshotsFilled() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(101L)).thenReturn(product);

        String prefix = "INS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        when(inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix)).thenReturn(Collections.emptyList());
        when(inspectionOrderMapper.selectByCodeIncludingDeleted(prefix + "001")).thenReturn(null);

        Long resultId = inspectionOrderService.createInspectionOrder(reqVO);

        ArgumentCaptor<InspectionOrderDO> captor = ArgumentCaptor.forClass(InspectionOrderDO.class);
        verify(inspectionOrderMapper).insert(captor.capture());
        InspectionOrderDO saved = captor.getValue();

        assertEquals(prefix + "001", saved.getCode());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(LocalDate.now(), saved.getOrderDate());
        assertEquals("P001", saved.getProductCode());
        assertEquals("全棉汗布", saved.getProductName());
        assertEquals("GREIGE", saved.getType());
        assertEquals("ROLL", saved.getPackageMode(), "未传 packageMode 时应默认 ROLL");
    }

    @Test
    void createInspectionOrder_explicitPackageMode_piece() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setPackageMode("PIECE");

        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(101L)).thenReturn(product);

        String prefix = "INS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        when(inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix)).thenReturn(Collections.emptyList());
        when(inspectionOrderMapper.selectByCodeIncludingDeleted(prefix + "001")).thenReturn(null);

        inspectionOrderService.createInspectionOrder(reqVO);

        ArgumentCaptor<InspectionOrderDO> captor = ArgumentCaptor.forClass(InspectionOrderDO.class);
        verify(inspectionOrderMapper).insert(captor.capture());
        assertEquals("PIECE", captor.getValue().getPackageMode());
    }

    @Test
    void createInspectionOrder_withManualCode_success() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setCode("INS-20260921-999");

        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(101L)).thenReturn(product);
        when(inspectionOrderMapper.selectByCodeIncludingDeleted("INS-20260921-999")).thenReturn(null);

        inspectionOrderService.createInspectionOrder(reqVO);

        ArgumentCaptor<InspectionOrderDO> captor = ArgumentCaptor.forClass(InspectionOrderDO.class);
        verify(inspectionOrderMapper).insert(captor.capture());
        assertEquals("INS-20260921-999", captor.getValue().getCode());
    }

    @Test
    void createInspectionOrder_manualCodeDuplicate_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setCode("INS-20260921-999");

        InspectionOrderDO existing = new InspectionOrderDO();
        existing.setId(999L);
        existing.setCode("INS-20260921-999");
        when(inspectionOrderMapper.selectByCodeIncludingDeleted("INS-20260921-999")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void createInspectionOrder_codeGenerate_incrementsMaxNum() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(101L)).thenReturn(product);

        String prefix = "INS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        InspectionOrderDO c1 = new InspectionOrderDO();
        c1.setCode(prefix + "001");
        InspectionOrderDO c2 = new InspectionOrderDO();
        c2.setCode(prefix + "005");
        when(inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix)).thenReturn(Arrays.asList(c1, c2));
        when(inspectionOrderMapper.selectByCodeIncludingDeleted(prefix + "006")).thenReturn(null);

        inspectionOrderService.createInspectionOrder(reqVO);

        ArgumentCaptor<InspectionOrderDO> captor = ArgumentCaptor.forClass(InspectionOrderDO.class);
        verify(inspectionOrderMapper).insert(captor.capture());
        assertEquals(prefix + "006", captor.getValue().getCode());
    }

    @Test
    void createInspectionOrder_codeGenerateRetry_allFail_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        when(productMapper.selectById(101L)).thenReturn(product);

        String prefix = "INS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        when(inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix)).thenReturn(Collections.emptyList());

        InspectionOrderDO conflict = new InspectionOrderDO();
        conflict.setId(888L);
        conflict.setCode(prefix + "001");
        when(inspectionOrderMapper.selectByCodeIncludingDeleted(prefix + "001")).thenReturn(conflict);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void createInspectionOrder_productNotFound_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);

        when(productMapper.selectById(101L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("产品"));
    }

    @Test
    void createInspectionOrder_supplierNotFound_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setInspectionFactoryId(201L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        when(productMapper.selectById(101L)).thenReturn(product);
        when(supplierMapper.selectById(201L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("承检方"));
    }

    @Test
    void createInspectionOrder_greigeOrderNotFound_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setGreigeOrderId(301L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        when(productMapper.selectById(101L)).thenReturn(product);
        when(greigeOrderMapper.selectById(301L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("胚布单"));
    }

    @Test
    void createInspectionOrder_dyeingOrderNotFound_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("FINISHED");
        reqVO.setProductId(101L);
        reqVO.setDyeingOrderId(401L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        when(productMapper.selectById(101L)).thenReturn(product);
        when(dyeingOrderMapper.selectById(401L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("染色单"));
    }

    @Test
    void createInspectionOrder_salesContractNotFound_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setSalesContractId(501L);

        ProductDO product = new ProductDO();
        product.setId(101L);
        when(productMapper.selectById(101L)).thenReturn(product);
        when(salesContractMapper.selectById(501L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.createInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("销售合同"));
    }

    @Test
    void createInspectionOrder_withAllRefs_snapshotsFilled() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("GREIGE");
        reqVO.setProductId(101L);
        reqVO.setGreigeOrderId(301L);
        reqVO.setInspectionFactoryId(201L);
        reqVO.setSalesContractId(501L);
        reqVO.setTargetWeight(new BigDecimal("1200.50"));
        reqVO.setRemark("测试检验单");
        reqVO.setOrderDate(LocalDate.of(2026, 9, 21));

        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(101L)).thenReturn(product);

        SupplierDO supplier = new SupplierDO();
        supplier.setId(201L);
        supplier.setCompanyName("盛泽染整一厂");
        when(supplierMapper.selectById(201L)).thenReturn(supplier);

        GreigeOrderDO greigeOrder = new GreigeOrderDO();
        greigeOrder.setId(301L);
        when(greigeOrderMapper.selectById(301L)).thenReturn(greigeOrder);

        SalesContractDO contract = new SalesContractDO();
        contract.setId(501L);
        contract.setContractNo("CH20260001");
        when(salesContractMapper.selectById(501L)).thenReturn(contract);

        String prefix = "INS-20260921-";
        when(inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix)).thenReturn(Collections.emptyList());
        when(inspectionOrderMapper.selectByCodeIncludingDeleted("INS-20260921-001")).thenReturn(null);

        inspectionOrderService.createInspectionOrder(reqVO);

        ArgumentCaptor<InspectionOrderDO> captor = ArgumentCaptor.forClass(InspectionOrderDO.class);
        verify(inspectionOrderMapper).insert(captor.capture());
        InspectionOrderDO saved = captor.getValue();

        assertEquals("INS-20260921-001", saved.getCode());
        assertEquals("P001", saved.getProductCode());
        assertEquals("全棉汗布", saved.getProductName());
        assertEquals(201L, saved.getInspectionFactoryId());
        assertEquals("盛泽染整一厂", saved.getInspectionFactoryName());
        assertEquals(301L, saved.getGreigeOrderId());
        assertNull(saved.getDyeingOrderId());
        assertEquals(501L, saved.getSalesContractId());
        assertEquals("CH20260001", saved.getContractNo());
        assertEquals(new BigDecimal("1200.50"), saved.getTargetWeight());
        assertEquals("测试检验单", saved.getRemark());
        assertEquals(LocalDate.of(2026, 9, 21), saved.getOrderDate());
    }

    @Test
    void updateInspectionOrder_notExists_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setId(1L);

        when(inspectionOrderMapper.selectById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.updateInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void updateInspectionOrder_notDraft_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setId(1L);

        InspectionOrderDO existing = new InspectionOrderDO();
        existing.setId(1L);
        existing.setStatus("ACTIVE");
        when(inspectionOrderMapper.selectById(1L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.updateInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void updateInspectionOrder_success_draftStatus_updatesSnapshotsAndFields() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setId(1L);
        reqVO.setType("FINISHED");
        reqVO.setProductId(101L);
        reqVO.setDyeingOrderId(401L);
        reqVO.setInspectionFactoryId(201L);
        reqVO.setSalesContractId(501L);
        reqVO.setTargetWeight(new BigDecimal("800.00"));
        reqVO.setPackageMode("PIECE");
        reqVO.setRemark("修改后的备注");
        reqVO.setOrderDate(LocalDate.of(2026, 9, 22));

        InspectionOrderDO existing = new InspectionOrderDO();
        existing.setId(1L);
        existing.setStatus("DRAFT");
        existing.setCode("INS-20260921-001");
        when(inspectionOrderMapper.selectById(1L)).thenReturn(existing);

        ProductDO product = new ProductDO();
        product.setId(101L);
        product.setCode("P002");
        product.setName("双面布");
        when(productMapper.selectById(101L)).thenReturn(product);

        SupplierDO supplier = new SupplierDO();
        supplier.setId(201L);
        supplier.setCompanyName("盛泽染整二厂");
        when(supplierMapper.selectById(201L)).thenReturn(supplier);

        DyeingOrderDO dyeingOrder = new DyeingOrderDO();
        dyeingOrder.setId(401L);
        when(dyeingOrderMapper.selectById(401L)).thenReturn(dyeingOrder);

        SalesContractDO contract = new SalesContractDO();
        contract.setId(501L);
        contract.setContractNo("CH20260002");
        when(salesContractMapper.selectById(501L)).thenReturn(contract);

        inspectionOrderService.updateInspectionOrder(reqVO);

        ArgumentCaptor<InspectionOrderDO> captor = ArgumentCaptor.forClass(InspectionOrderDO.class);
        verify(inspectionOrderMapper).updateById(captor.capture());
        InspectionOrderDO updated = captor.getValue();

        assertEquals(1L, updated.getId());
        assertNull(updated.getCode(), "未传 code 时置 null 避免覆盖现有编号");
        assertEquals("FINISHED", updated.getType());
        assertEquals("P002", updated.getProductCode());
        assertEquals("双面布", updated.getProductName());
        assertEquals("盛泽染整二厂", updated.getInspectionFactoryName());
        assertEquals("CH20260002", updated.getContractNo());
        assertEquals(401L, updated.getDyeingOrderId());
        assertNull(updated.getGreigeOrderId());
        assertEquals(new BigDecimal("800.00"), updated.getTargetWeight());
        assertEquals("PIECE", updated.getPackageMode());
        assertEquals("修改后的备注", updated.getRemark());
        assertEquals(LocalDate.of(2026, 9, 22), updated.getOrderDate());
    }

    @Test
    void updateInspectionOrder_codeChanged_duplicate_throwsException() {
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setId(1L);
        reqVO.setCode("INS-20260921-002");

        InspectionOrderDO existing = new InspectionOrderDO();
        existing.setId(1L);
        existing.setStatus("DRAFT");
        existing.setCode("INS-20260921-001");
        when(inspectionOrderMapper.selectById(1L)).thenReturn(existing);

        InspectionOrderDO conflict = new InspectionOrderDO();
        conflict.setId(2L);
        conflict.setCode("INS-20260921-002");
        when(inspectionOrderMapper.selectByCodeIncludingDeleted("INS-20260921-002")).thenReturn(conflict);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.updateInspectionOrder(reqVO));
        assertEquals(INSPECTION_ORDER_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void getInspectionOrder_success() {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(1L);
        order.setCode("INS-20260921-001");
        order.setType("GREIGE");
        order.setStatus("DRAFT");
        when(inspectionOrderMapper.selectById(1L)).thenReturn(order);

        InspectionOrderRespVO respVO = inspectionOrderService.getInspectionOrder(1L);
        assertNotNull(respVO);
        assertEquals(1L, respVO.getId());
        assertEquals("INS-20260921-001", respVO.getCode());
    }

    @Test
    void getInspectionOrder_notExists_throwsException() {
        when(inspectionOrderMapper.selectById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderService.getInspectionOrder(1L));
        assertEquals(INSPECTION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void getNextInspectionOrderNo_success() {
        String prefix = "INS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        InspectionOrderDO c1 = new InspectionOrderDO();
        c1.setCode(prefix + "001");
        when(inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix)).thenReturn(Collections.singletonList(c1));

        String nextNo = inspectionOrderService.getNextInspectionOrderNo();
        assertEquals(prefix + "002", nextNo);
    }

    // ==================== 列表与详情：已录卷数（rollCount）回填（03 票） ====================

    @Test
    void getInspectionOrderPage_populatesRollCountViaBatchCount() {
        InspectionOrderPageReqVO reqVO = new InspectionOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        InspectionOrderDO order1 = new InspectionOrderDO();
        order1.setId(101L);
        order1.setCode("INS-20260921-001");
        InspectionOrderDO order2 = new InspectionOrderDO();
        order2.setId(102L);
        order2.setCode("INS-20260921-002");

        PageResult<InspectionOrderDO> pageDO = new PageResult<>(Arrays.asList(order1, order2), 2L);
        when(inspectionOrderMapper.selectPage(eq(reqVO), any())).thenReturn(pageDO);

        Map<Long, Integer> countMap = Map.of(101L, 5, 102L, 8);
        when(inspectionOrderItemMapper.selectCountMapByInspectionOrderIds(Arrays.asList(101L, 102L))).thenReturn(countMap);

        PageResult<InspectionOrderRespVO> result = inspectionOrderService.getInspectionOrderPage(reqVO);

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        assertEquals(5, result.getList().get(0).getRollCount());
        assertEquals(8, result.getList().get(1).getRollCount());
    }

    @Test
    void getInspectionOrder_populatesRollCount() {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(101L);
        order.setCode("INS-20260921-001");
        when(inspectionOrderMapper.selectById(101L)).thenReturn(order);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(101L)).thenReturn(12L);

        InspectionOrderRespVO respVO = inspectionOrderService.getInspectionOrder(101L);

        assertNotNull(respVO);
        assertEquals(101L, respVO.getId());
        assertEquals(12, respVO.getRollCount());
    }

    // ==================== 状态操作：完成/取消（03 票） ====================

    @Test
    void completeInspectionOrder_validStatus_transitionsToCompleted() {
        for (String status : Arrays.asList("DRAFT", "ACTIVE")) {
            reset(inspectionOrderMapper);
            InspectionOrderDO existing = new InspectionOrderDO();
            existing.setId(1001L);
            existing.setStatus(status);
            when(inspectionOrderMapper.selectById(1001L)).thenReturn(existing);

            inspectionOrderService.completeInspectionOrder(1001L);

            ArgumentCaptor<LambdaUpdateWrapper<InspectionOrderDO>> captor =
                    ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
            verify(inspectionOrderMapper).update(isNull(), captor.capture());
            LambdaUpdateWrapper<InspectionOrderDO> wrapper = captor.getValue();
            assertTrue(wrapper.getSqlSegment().contains("id ="), "应按 id 条件更新");
            assertTrue(wrapper.getParamNameValuePairs().containsValue("COMPLETED"), "目标状态应为 COMPLETED");
        }
    }

    @Test
    void completeInspectionOrder_notExists_throwsNotExists() {
        when(inspectionOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> inspectionOrderService.completeInspectionOrder(1001L));
        assertEquals(INSPECTION_ORDER_NOT_EXISTS.getCode(), exception.getCode());
        verify(inspectionOrderMapper, never()).update(any(), any());
    }

    @Test
    void completeInspectionOrder_illegalStatus_throwsStatusNotAllowed() {
        for (String status : Arrays.asList("COMPLETED", "CANCELLED")) {
            InspectionOrderDO existing = new InspectionOrderDO();
            existing.setId(1001L);
            existing.setStatus(status);
            when(inspectionOrderMapper.selectById(1001L)).thenReturn(existing);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> inspectionOrderService.completeInspectionOrder(1001L),
                    "状态为 " + status + " 时完成应抛出状态不允许异常");
            assertEquals(INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), exception.getCode());
        }
        verify(inspectionOrderMapper, never()).update(any(), any());
    }

    @Test
    void cancelInspectionOrder_anyStatus_transitionsToCancelled() {
        for (String status : Arrays.asList("DRAFT", "ACTIVE", "COMPLETED", "CANCELLED")) {
            reset(inspectionOrderMapper);
            InspectionOrderDO existing = new InspectionOrderDO();
            existing.setId(1001L);
            existing.setStatus(status);
            when(inspectionOrderMapper.selectById(1001L)).thenReturn(existing);

            inspectionOrderService.cancelInspectionOrder(1001L);

            ArgumentCaptor<LambdaUpdateWrapper<InspectionOrderDO>> captor =
                    ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
            verify(inspectionOrderMapper).update(isNull(), captor.capture());
            LambdaUpdateWrapper<InspectionOrderDO> wrapper = captor.getValue();
            assertTrue(wrapper.getSqlSegment().contains("id ="), "应按 id 条件更新");
            assertTrue(wrapper.getParamNameValuePairs().containsValue("CANCELLED"), "目标状态应为 CANCELLED");
        }
    }


    @Test
    void cancelInspectionOrder_notExists_throwsNotExists() {
        when(inspectionOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> inspectionOrderService.cancelInspectionOrder(1001L));
        assertEquals(INSPECTION_ORDER_NOT_EXISTS.getCode(), exception.getCode());
        verify(inspectionOrderMapper, never()).update(any(), any());
    }

    // ==================== 删除检验单：仅 DRAFT 可删 + 级联删除明细（03 票） ====================

    @Test
    void deleteInspectionOrder_draft_successAndCascadesItemDelete() {
        InspectionOrderDO existing = new InspectionOrderDO();
        existing.setId(1001L);
        existing.setStatus("DRAFT");
        when(inspectionOrderMapper.selectById(1001L)).thenReturn(existing);

        inspectionOrderService.deleteInspectionOrder(1001L);

        verify(inspectionOrderMapper).deleteById(1001L);
        verify(inspectionOrderItemMapper).deleteByInspectionOrderId(1001L);
    }

    @Test
    void deleteInspectionOrder_notExists_throwsNotExists() {
        when(inspectionOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> inspectionOrderService.deleteInspectionOrder(1001L));
        assertEquals(INSPECTION_ORDER_NOT_EXISTS.getCode(), exception.getCode());
        verify(inspectionOrderMapper, never()).deleteById(any());
        verify(inspectionOrderItemMapper, never()).deleteByInspectionOrderId(any());
    }

    @Test
    void deleteInspectionOrder_illegalStatus_throwsStatusNotAllowed() {
        for (String status : Arrays.asList("ACTIVE", "COMPLETED", "CANCELLED")) {
            InspectionOrderDO existing = new InspectionOrderDO();
            existing.setId(1001L);
            existing.setStatus(status);
            when(inspectionOrderMapper.selectById(1001L)).thenReturn(existing);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> inspectionOrderService.deleteInspectionOrder(1001L),
                    "状态为 " + status + " 时删除应抛出状态不允许异常");
            assertEquals(INSPECTION_ORDER_STATUS_NOT_ALLOWED.getCode(), exception.getCode());
        }
        verify(inspectionOrderMapper, never()).deleteById(any());
        verify(inspectionOrderItemMapper, never()).deleteByInspectionOrderId(any());
    }

}

