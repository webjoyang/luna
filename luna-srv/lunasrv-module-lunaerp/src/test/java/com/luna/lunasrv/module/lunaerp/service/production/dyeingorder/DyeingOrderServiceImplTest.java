package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.DYEING_ORDER_STATUS_NOT_ALLOWED;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_COLOR_HAS_VATS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;

/**
 * {@link DyeingOrderServiceImpl} 的纯 Mockito 单元测试。
 *
 * 01 票：覆盖列表分页查询条件透传（keyword OR 模糊、status/customerId/dyeingFactoryId 等值、
 * 单据日期/交货日期区间、默认 order_date 倒序）。
 * 02 票：覆盖新建染色单（status 缺省 DRAFT、传 ACTIVE 保留、order_date 缺省今天、
 * 销售合同/承染方引用校验、快照写入）。
 * 03 票：覆盖查看与修改染色单（DRAFT 可更新、非 DRAFT 报状态错误、快照刷新）。
 * 04 票：覆盖颜色明细聚合（新建/更新删旧插新、重排 line_no、零颜色合法、详情嵌套返回）。
 *
 * @author luna
 */
class DyeingOrderServiceImplTest {

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @Mock
    private DyeingColorMapper dyeingColorMapper;

    @Mock
    private DyeingColorDeleteGuard dyeingColorDeleteGuard;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private ColorInstructionService colorInstructionService;

    @InjectMocks
    private DyeingOrderServiceImpl dyeingOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingOrderDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingColorDO.class);
    }

    // ==================== getDyeingOrderPage：查询条件透传（01 票） ====================

    @Test
    void getDyeingOrderPage_filtersPassedThrough_andOrderedByOrderDateDesc() {
        DyeingOrderPageReqVO reqVO = new DyeingOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("CH2026");
        reqVO.setStatus("ACTIVE");
        reqVO.setCustomerId(1001L);
        reqVO.setDyeingFactoryId(2002L);
        reqVO.setOrderDateStart(LocalDate.of(2026, 9, 1));
        reqVO.setOrderDateEnd(LocalDate.of(2026, 9, 19));
        reqVO.setDeliveryDateStart(LocalDate.of(2026, 9, 10));
        reqVO.setDeliveryDateEnd(LocalDate.of(2026, 9, 30));

        when(dyeingOrderMapper.selectPage(any(DyeingOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        dyeingOrderService.getDyeingOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(dyeingOrderMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<DyeingOrderDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        // keyword OR 模糊命中 contract_no / client_name / product_code / product_name
        assertTrue(sqlSegment.contains("LOWER(contract_no) LIKE"), "keyword 应透传到 contract_no 模糊");
        assertTrue(sqlSegment.contains("LOWER(client_name) LIKE"), "keyword 应透传到 client_name 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_code) LIKE"), "keyword 应透传到 product_code 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_name) LIKE"), "keyword 应透传到 product_name 模糊");
        // status 等值透传
        assertTrue(sqlSegment.contains("status ="), "status 应透传等值过滤");
        // customer_id 等值透传
        assertTrue(sqlSegment.contains("customer_id ="), "customerId 应透传等值过滤");
        // dyeing_factory_id 等值透传
        assertTrue(sqlSegment.contains("dyeing_factory_id ="), "dyeingFactoryId 应透传等值过滤");
        // 日期区间透传
        assertTrue(sqlSegment.contains("order_date BETWEEN") || (sqlSegment.contains("order_date >=") && sqlSegment.contains("order_date <=")),
                "orderDate 区间应透传");
        assertTrue(sqlSegment.contains("delivery_date BETWEEN") || (sqlSegment.contains("delivery_date >=") && sqlSegment.contains("delivery_date <=")),
                "deliveryDate 区间应透传");
        // 默认单据日期倒序
        assertTrue(sqlSegment.contains("ORDER BY order_date DESC"), "默认应按单据日期倒序");
        // keyword 参数值透传
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%CH2026%"),
                "keyword 参数值应预编译透传");
    }

    @Test
    void getDyeingOrderPage_noFilters_onlyOrderByOrderDateDesc() {
        DyeingOrderPageReqVO reqVO = new DyeingOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(dyeingOrderMapper.selectPage(any(DyeingOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        dyeingOrderService.getDyeingOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(dyeingOrderMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("ORDER BY order_date DESC"));
        assertFalse(sqlSegment.contains("LOWER(contract_no) LIKE"), "无 keyword 时不应拼接模糊条件");
        assertFalse(sqlSegment.contains("status ="), "无 status 时不应拼接等值过滤");
        assertFalse(sqlSegment.contains("customer_id ="), "无 customerId 时不应拼接等值过滤");
        assertFalse(sqlSegment.contains("dyeing_factory_id ="), "无 dyeingFactoryId 时不应拼接等值过滤");
    }

    @Test
    void getDyeingOrderPage_blankKeyword_treatedAsNoKeyword() {
        DyeingOrderPageReqVO reqVO = new DyeingOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("   ");
        when(dyeingOrderMapper.selectPage(any(DyeingOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        dyeingOrderService.getDyeingOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(dyeingOrderMapper).selectPage(eq(reqVO), captor.capture());
        assertFalse(captor.getValue().getSqlSegment().contains("LOWER(contract_no) LIKE"),
                "空白 keyword 视为未输入，不拼接模糊条件");
    }

    // ==================== createDyeingOrder：状态与日期缺省（02 票） ====================

    @Test
    void createDyeingOrder_statusDefaultsToDraft_andOrderDateDefaultsToToday() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setStatus(null);
        reqVO.setOrderDate(null);

        dyeingOrderService.createDyeingOrder(reqVO);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).insert(captor.capture());
        DyeingOrderDO inserted = captor.getValue();
        assertEquals("DRAFT", inserted.getStatus(), "status 为空时应缺省为 DRAFT");
        assertEquals(LocalDate.now(), inserted.getOrderDate(), "orderDate 为空时应缺省为当天");
    }

    @Test
    void createDyeingOrder_passedActive_preservesActive() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setStatus("ACTIVE");

        dyeingOrderService.createDyeingOrder(reqVO);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).insert(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus(), "传 ACTIVE 时应保留 ACTIVE");
    }

    // ==================== createDyeingOrder：引用校验与快照（02 票） ====================

    @Test
    void createDyeingOrder_contractMissing_throwsReferenceNotFound() {
        when(salesContractMapper.selectById(101L)).thenReturn(null);
        stubSupplier(301L, "海宁染整厂");
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.createDyeingOrder(reqVO));
        assertEquals(1_030_007_002, exception.getCode());
        verify(dyeingOrderMapper, never()).insert(any(DyeingOrderDO.class));
    }

    @Test
    void createDyeingOrder_factoryMissing_throwsReferenceNotFound() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        when(supplierMapper.selectById(301L)).thenReturn(null);
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.createDyeingOrder(reqVO));
        assertEquals(1_030_007_002, exception.getCode());
        verify(dyeingOrderMapper, never()).insert(any(DyeingOrderDO.class));
    }

    @Test
    void createDyeingOrder_snapshotsWrittenFromContractAndSupplier() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setContractNo(null);
        reqVO.setCustomerId(null);
        reqVO.setClientName(null);
        reqVO.setDyeingFactoryName(null);

        dyeingOrderService.createDyeingOrder(reqVO);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).insert(captor.capture());
        DyeingOrderDO inserted = captor.getValue();
        assertEquals(101L, inserted.getSalesContractId());
        assertEquals("CH20260001", inserted.getContractNo(), "合同号快照应从销售合同档案自动带出");
        assertEquals(201L, inserted.getCustomerId(), "客户ID应从销售合同档案自动带出");
        assertEquals("测试客户", inserted.getClientName(), "客户名称快照应从销售合同档案自动带出");
        assertEquals(301L, inserted.getDyeingFactoryId());
        assertEquals("海宁染整厂", inserted.getDyeingFactoryName(), "承染方公司名快照应从供应商档案自动带出");
    }

    @Test
    void createDyeingOrder_snapshotsAuthoritativelyOverwrittenFromContract() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setContractNo("CH20260001-CUSTOM");
        reqVO.setCustomerId(999L);
        reqVO.setClientName("定制客户名");
        reqVO.setDyeingFactoryName("定制染厂名");

        dyeingOrderService.createDyeingOrder(reqVO);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).insert(captor.capture());
        DyeingOrderDO inserted = captor.getValue();
        assertEquals("CH20260001", inserted.getContractNo(), "合同号快照以销售合同档案为准");
        assertEquals(201L, inserted.getCustomerId(), "客户ID以销售合同档案为准");
        assertEquals("测试客户", inserted.getClientName(), "客户名快照以销售合同档案为准");
        assertEquals("海宁染整厂", inserted.getDyeingFactoryName(), "染厂名快照以供应商档案为准");
    }

    @Test
    void createDyeingOrder_invalidStatus_throwsStatusNotAllowed() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setStatus("CANCELLED");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.createDyeingOrder(reqVO));
        assertEquals(1_030_007_001, exception.getCode());
        verify(dyeingOrderMapper, never()).insert(any(DyeingOrderDO.class));
    }

    // ==================== getDyeingOrder：详情查询（03 票） ====================

    @Test
    void getDyeingOrder_notExists_throwsNotExists() {
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.getDyeingOrder(1001L));
        assertEquals(1_030_007_000, exception.getCode());
    }

    @Test
    void getDyeingOrder_exists_returnsDetails() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(1001L);
        order.setSalesContractId(101L);
        order.setContractNo("CH20260001");
        order.setCustomerId(201L);
        order.setClientName("测试客户");
        order.setDyeingFactoryId(301L);
        order.setDyeingFactoryName("海宁染整厂");
        order.setStatus("DRAFT");
        order.setOrderDate(LocalDate.of(2026, 9, 19));
        order.setProductCode("P001");
        order.setProductName("全棉汗布");
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(order);

        DyeingOrderRespVO respVO = dyeingOrderService.getDyeingOrder(1001L);

        assertEquals(1001L, respVO.getId());
        assertEquals("CH20260001", respVO.getContractNo());
        assertEquals("测试客户", respVO.getClientName());
        assertEquals("海宁染整厂", respVO.getDyeingFactoryName());
        assertEquals("DRAFT", respVO.getStatus());
        assertEquals("P001", respVO.getProductCode());
    }

    // ==================== updateDyeingOrder：更新单据（03 票） ====================

    @Test
    void updateDyeingOrder_notExists_throwsNotExists() {
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(null);
        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.updateDyeingOrder(reqVO));
        assertEquals(1_030_007_000, exception.getCode());
        verify(dyeingOrderMapper, never()).updateById(any(DyeingOrderDO.class));
    }

    @Test
    void updateDyeingOrder_nonDraft_throwsStatusNotAllowed() {
        for (String status : List.of("ACTIVE", "COMPLETED", "CANCELLED")) {
            stubExistingOrder(1001L, status);

            DyeingOrderSaveReqVO reqVO = validCreateReqVO();
            reqVO.setId(1001L);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> dyeingOrderService.updateDyeingOrder(reqVO),
                    "状态为 " + status + " 时更新应抛出状态不允许异常");
            assertEquals(1_030_007_001, exception.getCode());
        }
        verify(dyeingOrderMapper, never()).updateById(any(DyeingOrderDO.class));
    }

    @Test
    void updateDyeingOrder_draft_updatesSuccessfullyAndRefreshesSnapshots() {
        stubExistingOrder(1001L, "DRAFT");

        stubContract(102L, "CH20260002", 202L, "新客户");
        stubSupplier(302L, "嘉兴新染厂");

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setSalesContractId(102L);
        reqVO.setDyeingFactoryId(302L);
        reqVO.setProductCode("P002");
        reqVO.setProductName("新产品");
        reqVO.setLoss("6%");
        reqVO.setRemark("修改后的备注");

        dyeingOrderService.updateDyeingOrder(reqVO);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).updateById(captor.capture());
        DyeingOrderDO updated = captor.getValue();
        assertEquals(1001L, updated.getId());
        assertEquals("DRAFT", updated.getStatus());
        assertEquals("CH20260002", updated.getContractNo(), "合同号快照应刷新");
        assertEquals(202L, updated.getCustomerId(), "客户ID应刷新");
        assertEquals("新客户", updated.getClientName(), "客户名快照应刷新");
        assertEquals(302L, updated.getDyeingFactoryId());
        assertEquals("嘉兴新染厂", updated.getDyeingFactoryName(), "承染方公司名快照应刷新");
        assertEquals("P002", updated.getProductCode());
        assertEquals("新产品", updated.getProductName());
        assertEquals("6%", updated.getLoss());
        assertEquals("修改后的备注", updated.getRemark());
    }

    @Test
    void updateDyeingOrder_nullOrderDate_preservesExistingOrderDate() {
        LocalDate originalDate = LocalDate.of(2026, 8, 15);
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(1001L);
        existing.setStatus("DRAFT");
        existing.setOrderDate(originalDate);
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(existing);

        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setOrderDate(null);

        dyeingOrderService.updateDyeingOrder(reqVO);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).updateById(captor.capture());
        assertEquals(originalDate, captor.getValue().getOrderDate(), "更新时 orderDate 为空应保留既有单据日期");
    }

    @Test
    void updateDyeingOrder_contractMissing_throwsReferenceNotFound() {
        stubExistingOrder(1001L, "DRAFT");

        when(salesContractMapper.selectById(101L)).thenReturn(null);
        stubSupplier(301L, "海宁染整厂");

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.updateDyeingOrder(reqVO));
        assertEquals(1_030_007_002, exception.getCode());
        verify(dyeingOrderMapper, never()).updateById(any(DyeingOrderDO.class));
    }

    @Test
    void updateDyeingOrder_factoryMissing_throwsReferenceNotFound() {
        stubExistingOrder(1001L, "DRAFT");

        stubContract(101L, "CH20260001", 201L, "测试客户");
        when(supplierMapper.selectById(301L)).thenReturn(null);

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.updateDyeingOrder(reqVO));
        assertEquals(1_030_007_002, exception.getCode());
        verify(dyeingOrderMapper, never()).updateById(any(DyeingOrderDO.class));
    }

    // ==================== 颜色明细聚合保存与查询（04 票） ====================

    @Test
    void createDyeingOrder_withColors_savesColorsAndReordersLineNo() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        DyeingColorSaveReqVO color1 = new DyeingColorSaveReqVO();
        color1.setCustomerColorNo("C-01");
        color1.setColorName("藏青");
        color1.setQuantity(new BigDecimal("500.00"));
        color1.setPieces(20);
        color1.setDyeingInputQty(new BigDecimal("525.00"));
        color1.setDyeingInputUnit("KG");
        color1.setContractQty(new BigDecimal("500.00"));

        DyeingColorSaveReqVO color2 = new DyeingColorSaveReqVO();
        color2.setCustomerColorNo("C-02");
        color2.setColorName("花灰");
        color2.setQuantity(new BigDecimal("300.00"));
        color2.setPieces(12);
        color2.setDyeingInputQty(new BigDecimal("315.00"));
        color2.setDyeingInputUnit("KG");
        color2.setContractQty(new BigDecimal("300.00"));

        reqVO.setColors(List.of(color1, color2));

        when(dyeingOrderMapper.insert(any(DyeingOrderDO.class))).thenAnswer(invocation -> {
            DyeingOrderDO order = invocation.getArgument(0);
            order.setId(1001L);
            return 1;
        });

        Long orderId = dyeingOrderService.createDyeingOrder(reqVO);

        assertEquals(1001L, orderId);
        ArgumentCaptor<List<DyeingColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(dyeingColorMapper).insertBatch(captor.capture());
        List<DyeingColorDO> savedColors = captor.getValue();
        assertEquals(2, savedColors.size());
        assertEquals(1, savedColors.get(0).getLineNo());
        assertEquals("C-01", savedColors.get(0).getCustomerColorNo());
        assertEquals("藏青", savedColors.get(0).getColorName());
        assertEquals(new BigDecimal("500.00"), savedColors.get(0).getQuantity());
        assertEquals(1001L, savedColors.get(0).getDyeingOrderId());

        assertEquals(2, savedColors.get(1).getLineNo());
        assertEquals("C-02", savedColors.get(1).getCustomerColorNo());
        assertEquals("花灰", savedColors.get(1).getColorName());
        assertEquals(new BigDecimal("300.00"), savedColors.get(1).getQuantity());
        assertEquals(1001L, savedColors.get(1).getDyeingOrderId());
    }

    @Test
    void createDyeingOrder_zeroColors_legalAndDoesNotInsertColors() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setColors(Collections.emptyList());

        dyeingOrderService.createDyeingOrder(reqVO);

        verify(dyeingOrderMapper).insert(any(DyeingOrderDO.class));
        verify(dyeingColorMapper, never()).insertBatch(any());
    }

    @Test
    void createDyeingOrder_withColors_clearsIdAndSetsLineNo() {
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        DyeingColorSaveReqVO color1 = buildColorReqVO(999L, "C-01", "藏青", new BigDecimal("100.00"));
        reqVO.setColors(List.of(color1));

        dyeingOrderService.createDyeingOrder(reqVO);

        ArgumentCaptor<List<DyeingColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(dyeingColorMapper).insertBatch(captor.capture());
        List<DyeingColorDO> inserted = captor.getValue();
        assertEquals(1, inserted.size());
        assertEquals(null, inserted.get(0).getId());
        assertEquals(1, inserted.get(0).getLineNo());
    }

    // ==================== 颜色明细差异保存（ADR-0032） ====================

    @Test
    void updateDyeingOrder_allExistingColors_updatesColorsZeroDeleteZeroInsert() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        DyeingColorDO c2 = stubColorDO(502L, 1001L, 2, "C-02", "花灰", new BigDecimal("300.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setColors(List.of(
                buildColorReqVO(501L, "C-01", "藏青", new BigDecimal("500.00")),
                buildColorReqVO(502L, "C-02", "花灰", new BigDecimal("300.00"))
        ));

        dyeingOrderService.updateDyeingOrder(reqVO);

        verify(dyeingOrderMapper).updateById(any(DyeingOrderDO.class));
        verify(dyeingColorMapper, never()).deleteByIds(any());
        verify(dyeingColorMapper, never()).deleteByOrderId(any());
        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
        verify(dyeingColorDeleteGuard, never()).validateCanDeleteColors(any(), any());

        ArgumentCaptor<DyeingColorDO> captor = ArgumentCaptor.forClass(DyeingColorDO.class);
        verify(dyeingColorMapper, times(2)).updateById(captor.capture());
        List<DyeingColorDO> updatedList = captor.getAllValues();
        assertEquals(501L, updatedList.get(0).getId());
        assertEquals(1, updatedList.get(0).getLineNo());
        assertEquals(502L, updatedList.get(1).getId());
        assertEquals(2, updatedList.get(1).getLineNo());
    }

    @Test
    void updateDyeingOrder_modifyColor_updatesTargetColorWithNewValues() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        DyeingColorDO c2 = stubColorDO(502L, 1001L, 2, "C-02", "花灰", new BigDecimal("300.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setColors(List.of(
                buildColorReqVO(501L, "C-01-MOD", "深藏青", new BigDecimal("550.00")),
                buildColorReqVO(502L, "C-02", "花灰", new BigDecimal("300.00"))
        ));

        dyeingOrderService.updateDyeingOrder(reqVO);

        ArgumentCaptor<DyeingColorDO> captor = ArgumentCaptor.forClass(DyeingColorDO.class);
        verify(dyeingColorMapper, times(2)).updateById(captor.capture());
        List<DyeingColorDO> updatedList = captor.getAllValues();
        DyeingColorDO updatedC1 = updatedList.stream().filter(c -> c.getId().equals(501L)).findFirst().orElseThrow();
        assertEquals("C-01-MOD", updatedC1.getCustomerColorNo());
        assertEquals("深藏青", updatedC1.getColorName());
        assertEquals(new BigDecimal("550.00"), updatedC1.getQuantity());

        verify(dyeingColorMapper, never()).deleteByIds(any());
        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
    }

    @Test
    void updateDyeingOrder_removeColor_logicallyDeletesRemovedColorAndCallsGuard() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        DyeingColorDO c2 = stubColorDO(502L, 1001L, 2, "C-02", "花灰", new BigDecimal("300.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        // 仅保留 501L，删除 502L
        reqVO.setColors(List.of(
                buildColorReqVO(501L, "C-01", "藏青", new BigDecimal("500.00"))
        ));

        dyeingOrderService.updateDyeingOrder(reqVO);

        verify(dyeingColorDeleteGuard).validateCanDeleteColors(eq(1001L), eq(Set.of(502L)));
        verify(dyeingColorMapper).deleteByIds(eq(Set.of(502L)));

        ArgumentCaptor<DyeingColorDO> captor = ArgumentCaptor.forClass(DyeingColorDO.class);
        verify(dyeingColorMapper, times(1)).updateById(captor.capture());
        assertEquals(501L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getLineNo());

        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
    }

    @Test
    void updateDyeingOrder_removeColorWithVats_guardThrows004AndAbortsSave() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        DyeingColorDO c2 = stubColorDO(502L, 1001L, 2, "C-02", "花灰", new BigDecimal("300.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        // 尝试移除 502L
        reqVO.setColors(List.of(
                buildColorReqVO(501L, "C-01", "藏青", new BigDecimal("500.00"))
        ));

        // 守卫拦截：待删除颜色行存在反馈缸，抛出 1-030-017-004
        doThrow(exception(PROGRESS_FEEDBACK_COLOR_HAS_VATS, "花灰", 2))
                .when(dyeingColorDeleteGuard).validateCanDeleteColors(eq(1001L), eq(Set.of(502L)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dyeingOrderService.updateDyeingOrder(reqVO));

        assertEquals(PROGRESS_FEEDBACK_COLOR_HAS_VATS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("花灰"));
        assertTrue(ex.getMessage().contains("2"));

        // 验证删除被硬拦截：明细的 deleteByIds / updateById / insert 均未执行
        verify(dyeingColorDeleteGuard).validateCanDeleteColors(eq(1001L), eq(Set.of(502L)));
        verify(dyeingColorMapper, never()).deleteByIds(any());
        verify(dyeingColorMapper, never()).updateById(any(DyeingColorDO.class));
        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
    }

    @Test
    void updateDyeingOrder_addColor_insertsNewColorAndPreservesExisting() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setColors(List.of(
                buildColorReqVO(501L, "C-01", "藏青", new BigDecimal("500.00")),
                buildColorReqVO(null, "C-03", "酒红", new BigDecimal("200.00"))
        ));

        dyeingOrderService.updateDyeingOrder(reqVO);

        verify(dyeingColorMapper, never()).deleteByIds(any());
        verify(dyeingColorMapper, times(1)).updateById(any(DyeingColorDO.class));

        ArgumentCaptor<DyeingColorDO> insertCaptor = ArgumentCaptor.forClass(DyeingColorDO.class);
        verify(dyeingColorMapper).insert(insertCaptor.capture());
        DyeingColorDO inserted = insertCaptor.getValue();
        assertEquals(null, inserted.getId());
        assertEquals(1001L, inserted.getDyeingOrderId());
        assertEquals(2, inserted.getLineNo());
        assertEquals("C-03", inserted.getCustomerColorNo());
        assertEquals("酒红", inserted.getColorName());
    }

    @Test
    void updateDyeingOrder_colorNotBelongingToOrder_throwsInvalidParamException() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        // 传入不属于本单据的 ID 999L
        reqVO.setColors(List.of(
                buildColorReqVO(999L, "C-99", "错误颜色", new BigDecimal("100.00"))
        ));

        assertThrows(ServiceException.class, () -> dyeingOrderService.updateDyeingOrder(reqVO));
        verify(dyeingColorMapper, never()).updateById(any(DyeingColorDO.class));
        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
        verify(dyeingColorMapper, never()).deleteByIds(any());
    }

    @Test
    void updateDyeingOrder_duplicateColorIds_throwsInvalidParamException() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setColors(List.of(
                buildColorReqVO(501L, "C-01", "藏青", new BigDecimal("500.00")),
                buildColorReqVO(501L, "C-01", "藏青重复", new BigDecimal("500.00"))
        ));

        assertThrows(ServiceException.class, () -> dyeingOrderService.updateDyeingOrder(reqVO));
        verify(dyeingColorMapper, never()).updateById(any(DyeingColorDO.class));
        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
        verify(dyeingColorMapper, never()).deleteByIds(any());
    }

    @Test
    void updateDyeingOrder_toEmptyColors_callsGuardAndDeletesAllOldColors() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        DyeingColorDO c2 = stubColorDO(502L, 1001L, 2, "C-02", "花灰", new BigDecimal("300.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        reqVO.setColors(Collections.emptyList());

        dyeingOrderService.updateDyeingOrder(reqVO);

        verify(dyeingOrderMapper).updateById(any(DyeingOrderDO.class));
        verify(dyeingColorDeleteGuard).validateCanDeleteColors(eq(1001L), eq(Set.of(501L, 502L)));
        verify(dyeingColorMapper).deleteByIds(eq(Set.of(501L, 502L)));
        verify(dyeingColorMapper, never()).updateById(any(DyeingColorDO.class));
        verify(dyeingColorMapper, never()).insert(any(DyeingColorDO.class));
    }

    @Test
    void updateDyeingOrder_reorderColors_updatesLineNoAccordingToSubmitOrder() {
        stubExistingOrder(1001L, "DRAFT");
        stubContract(101L, "CH20260001", 201L, "测试客户");
        stubSupplier(301L, "海宁染整厂");

        DyeingColorDO c1 = stubColorDO(501L, 1001L, 1, "C-01", "藏青", new BigDecimal("500.00"));
        DyeingColorDO c2 = stubColorDO(502L, 1001L, 2, "C-02", "花灰", new BigDecimal("300.00"));
        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1001L);
        // 调换顺序：c2 先，c1 后
        reqVO.setColors(List.of(
                buildColorReqVO(502L, "C-02", "花灰", new BigDecimal("300.00")),
                buildColorReqVO(501L, "C-01", "藏青", new BigDecimal("500.00"))
        ));

        dyeingOrderService.updateDyeingOrder(reqVO);

        ArgumentCaptor<DyeingColorDO> captor = ArgumentCaptor.forClass(DyeingColorDO.class);
        verify(dyeingColorMapper, times(2)).updateById(captor.capture());
        List<DyeingColorDO> updatedList = captor.getAllValues();
        assertEquals(502L, updatedList.get(0).getId());
        assertEquals(1, updatedList.get(0).getLineNo());
        assertEquals(501L, updatedList.get(1).getId());
        assertEquals(2, updatedList.get(1).getLineNo());
    }

    @Test
    void getDyeingOrder_nestedColors_orderedByLineNo() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(1001L);
        order.setSalesContractId(101L);
        order.setContractNo("CH20260001");
        order.setCustomerId(201L);
        order.setClientName("测试客户");
        order.setDyeingFactoryId(301L);
        order.setDyeingFactoryName("海宁染整厂");
        order.setStatus("DRAFT");
        order.setOrderDate(LocalDate.of(2026, 9, 19));
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(order);

        DyeingColorDO c1 = new DyeingColorDO();
        c1.setId(501L);
        c1.setDyeingOrderId(1001L);
        c1.setLineNo(1);
        c1.setCustomerColorNo("C-01");
        c1.setColorName("藏青");
        c1.setQuantity(new BigDecimal("500.00"));
        c1.setPieces(20);
        c1.setDyeingInputQty(new BigDecimal("525.00"));
        c1.setDyeingInputUnit("KG");
        c1.setContractQty(new BigDecimal("500.00"));

        DyeingColorDO c2 = new DyeingColorDO();
        c2.setId(502L);
        c2.setDyeingOrderId(1001L);
        c2.setLineNo(2);
        c2.setCustomerColorNo("C-02");
        c2.setColorName("花灰");
        c2.setQuantity(new BigDecimal("300.00"));
        c2.setPieces(12);
        c2.setDyeingInputQty(new BigDecimal("315.00"));
        c2.setDyeingInputUnit("KG");
        c2.setContractQty(new BigDecimal("300.00"));

        when(dyeingColorMapper.selectListByOrderId(1001L)).thenReturn(List.of(c1, c2));

        DyeingOrderRespVO respVO = dyeingOrderService.getDyeingOrder(1001L);

        assertEquals(1001L, respVO.getId());
        assertEquals(2, respVO.getColors().size());
        assertEquals(1, respVO.getColors().get(0).getLineNo());
        assertEquals("C-01", respVO.getColors().get(0).getCustomerColorNo());
        assertEquals("藏青", respVO.getColors().get(0).getColorName());
        assertEquals(new BigDecimal("500.00"), respVO.getColors().get(0).getQuantity());

        assertEquals(2, respVO.getColors().get(1).getLineNo());
        assertEquals("C-02", respVO.getColors().get(1).getCustomerColorNo());
        assertEquals("花灰", respVO.getColors().get(1).getColorName());
        assertEquals(new BigDecimal("300.00"), respVO.getColors().get(1).getQuantity());
    }

    // ==================== 状态操作：下达/完成/取消（05 票） ====================

    @Test
    void effectDyeingOrder_draft_transitionsToActive_andOnlyModifiesStatus() {
        stubExistingOrder(1001L, "DRAFT");

        dyeingOrderService.effectDyeingOrder(1001L);

        ArgumentCaptor<LambdaUpdateWrapper<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(dyeingOrderMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<DyeingOrderDO> wrapper = captor.getValue();
        String sqlSet = wrapper.getSqlSet();
        assertTrue(sqlSet.contains("status"), "应更新 status 字段");
        assertFalse(sqlSet.contains("sales_contract_id"), "不应触碰 sales_contract_id 等业务字段");
        assertFalse(sqlSet.contains("product_code"), "不应触碰 product_code 等业务字段");
        assertTrue(wrapper.getSqlSegment().contains("id ="), "应按 id 条件更新");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("ACTIVE"), "目标状态应为 ACTIVE");
    }

    @Test
    void completeDyeingOrder_active_transitionsToCompleted_andOnlyModifiesStatus() {
        stubExistingOrder(1001L, "ACTIVE");

        dyeingOrderService.completeDyeingOrder(1001L);

        ArgumentCaptor<LambdaUpdateWrapper<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(dyeingOrderMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<DyeingOrderDO> wrapper = captor.getValue();
        String sqlSet = wrapper.getSqlSet();
        assertTrue(sqlSet.contains("status"), "应更新 status 字段");
        assertFalse(sqlSet.contains("sales_contract_id"), "不应触碰 sales_contract_id 等业务字段");
        assertFalse(sqlSet.contains("product_code"), "不应触碰 product_code 等业务字段");
        assertTrue(wrapper.getSqlSegment().contains("id ="), "应按 id 条件更新");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("COMPLETED"), "目标状态应为 COMPLETED");
    }

    @Test
    void cancelDyeingOrder_active_transitionsToCancelled_andOnlyModifiesStatus() {
        stubExistingOrder(1001L, "ACTIVE");

        dyeingOrderService.cancelDyeingOrder(1001L);

        ArgumentCaptor<LambdaUpdateWrapper<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(dyeingOrderMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<DyeingOrderDO> wrapper = captor.getValue();
        String sqlSet = wrapper.getSqlSet();
        assertTrue(sqlSet.contains("status"), "应更新 status 字段");
        assertFalse(sqlSet.contains("sales_contract_id"), "不应触碰 sales_contract_id 等业务字段");
        assertFalse(sqlSet.contains("product_code"), "不应触碰 product_code 等业务字段");
        assertTrue(wrapper.getSqlSegment().contains("id ="), "应按 id 条件更新");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("CANCELLED"), "目标状态应为 CANCELLED");
    }

    @Test
    void effectDyeingOrder_notExists_throwsNotExists() {
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.effectDyeingOrder(1001L));
        assertEquals(1_030_007_000, exception.getCode());
        verify(dyeingOrderMapper, never()).update(any(), any());
    }

    @Test
    void effectDyeingOrder_illegalStatus_throwsStatusNotAllowed() {
        for (String status : List.of("ACTIVE", "COMPLETED", "CANCELLED")) {
            stubExistingOrder(1001L, status);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> dyeingOrderService.effectDyeingOrder(1001L),
                    "状态为 " + status + " 时下达应抛出状态不允许异常");
            assertEquals(1_030_007_001, exception.getCode());
        }
        verify(dyeingOrderMapper, never()).update(any(), any());
    }

    @Test
    void completeDyeingOrder_notExists_throwsNotExists() {
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.completeDyeingOrder(1001L));
        assertEquals(1_030_007_000, exception.getCode());
        verify(dyeingOrderMapper, never()).update(any(), any());
    }

    @Test
    void completeDyeingOrder_illegalStatus_throwsStatusNotAllowed() {
        for (String status : List.of("DRAFT", "COMPLETED", "CANCELLED")) {
            stubExistingOrder(1001L, status);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> dyeingOrderService.completeDyeingOrder(1001L),
                    "状态为 " + status + " 时完成应抛出状态不允许异常");
            assertEquals(1_030_007_001, exception.getCode());
        }
        verify(dyeingOrderMapper, never()).update(any(), any());
    }

    @Test
    void cancelDyeingOrder_notExists_throwsNotExists() {
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.cancelDyeingOrder(1001L));
        assertEquals(1_030_007_000, exception.getCode());
        verify(dyeingOrderMapper, never()).update(any(), any());
    }

    @Test
    void cancelDyeingOrder_illegalStatus_throwsStatusNotAllowed() {
        for (String status : List.of("DRAFT", "COMPLETED", "CANCELLED")) {
            stubExistingOrder(1001L, status);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> dyeingOrderService.cancelDyeingOrder(1001L),
                    "状态为 " + status + " 时取消应抛出状态不允许异常");
            assertEquals(1_030_007_001, exception.getCode());
        }
        verify(dyeingOrderMapper, never()).update(any(), any());
    }

    // ==================== 删除染色单（06 票） ====================

    @Test
    void deleteDyeingOrder_draft_successAndCascadesColorDelete() {
        stubExistingOrder(1001L, "DRAFT");

        dyeingOrderService.deleteDyeingOrder(1001L);

        verify(dyeingOrderMapper).deleteById(1001L);
        verify(dyeingColorMapper).deleteByOrderId(1001L);
    }

    @Test
    void deleteDyeingOrder_notExists_throwsNotExists() {
        when(dyeingOrderMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> dyeingOrderService.deleteDyeingOrder(1001L));
        assertEquals(1_030_007_000, exception.getCode());
        verify(dyeingOrderMapper, never()).deleteById(any());
        verify(dyeingColorMapper, never()).deleteByOrderId(any());
    }

    @Test
    void deleteDyeingOrder_nonDraft_throwsStatusNotAllowed() {
        for (String status : List.of("ACTIVE", "COMPLETED", "CANCELLED")) {
            stubExistingOrder(1001L, status);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> dyeingOrderService.deleteDyeingOrder(1001L),
                    "状态为 " + status + " 时删除应抛出状态不允许异常");
            assertEquals(1_030_007_001, exception.getCode());
        }
        verify(dyeingOrderMapper, never()).deleteById(any());
        verify(dyeingColorMapper, never()).deleteByOrderId(any());
    }

    // ==================== 辅助方法 ====================

    private void stubExistingOrder(Long id, String status) {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(id);
        existing.setStatus(status);
        existing.setOrderDate(LocalDate.of(2026, 9, 19));
        when(dyeingOrderMapper.selectById(id)).thenReturn(existing);
    }

    private DyeingOrderSaveReqVO validCreateReqVO() {
        DyeingOrderSaveReqVO reqVO = new DyeingOrderSaveReqVO();
        reqVO.setSalesContractId(101L);
        reqVO.setDyeingFactoryId(301L);
        reqVO.setProductCode("P001");
        reqVO.setProductName("全棉汗布");
        reqVO.setComposition("95棉5氨");
        reqVO.setWidth("160cm");
        reqVO.setGsm("180g");
        reqVO.setShrinkage("≤5%");
        reqVO.setLoss("5%");
        reqVO.setPh("6.5");
        reqVO.setRemark("新建备注");
        return reqVO;
    }

    private void stubContract(Long id, String contractNo, Long customerId, String clientName) {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(id);
        contract.setContractNo(contractNo);
        contract.setCustomerId(customerId);
        contract.setClientName(clientName);
        when(salesContractMapper.selectById(id)).thenReturn(contract);
    }

    private void stubSupplier(Long id, String companyName) {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(id);
        supplier.setCompanyName(companyName);
        when(supplierMapper.selectById(id)).thenReturn(supplier);
    }

    private DyeingColorDO stubColorDO(Long id, Long dyeingOrderId, Integer lineNo, String customerColorNo, String colorName, BigDecimal qty) {
        DyeingColorDO color = new DyeingColorDO();
        color.setId(id);
        color.setDyeingOrderId(dyeingOrderId);
        color.setLineNo(lineNo);
        color.setCustomerColorNo(customerColorNo);
        color.setColorName(colorName);
        color.setQuantity(qty);
        color.setPieces(10);
        color.setDyeingInputQty(qty);
        color.setDyeingInputUnit("KG");
        color.setContractQty(qty);
        return color;
    }

    private DyeingColorSaveReqVO buildColorReqVO(Long id, String customerColorNo, String colorName, BigDecimal qty) {
        DyeingColorSaveReqVO vo = new DyeingColorSaveReqVO();
        vo.setId(id);
        vo.setCustomerColorNo(customerColorNo);
        vo.setColorName(colorName);
        vo.setQuantity(qty);
        vo.setPieces(10);
        vo.setDyeingInputQty(qty);
        vo.setDyeingInputUnit("KG");
        vo.setContractQty(qty);
        return vo;
    }

    // ==================== Issue 06: 下游引用打色指示单校验与快照 ====================

    private ColorInstructionDO stubColorInstruction(Long id, String no, Long customerId, Long productId, String productCode, String status) {
        ColorInstructionDO ci = new ColorInstructionDO();
        ci.setId(id);
        ci.setInstructionNo(no);
        ci.setCustomerId(customerId);
        ci.setProductId(productId);
        ci.setProductCode(productCode);
        ci.setStatus(status);
        ci.setDeleted(false);
        ci.setTenantId(TenantContextHolder.getTenantId() == null ? 1L : TenantContextHolder.getTenantId());
        return ci;
    }

    @Test
    void createDyeingOrder_colorInstructionNotExists_throwsReferenceNotFound() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setColorInstructionId(999L);
        when(colorInstructionService.validateColorInstructionForDownstream(eq(999L), any(), any(), any(), any()))
                .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.createDyeingOrder(req));
        assertEquals(1_030_007_002, ex.getCode(), "打色单不存在应抛 DYEING_ORDER_REFERENCE_NOT_FOUND");
    }

    @Test
    void createDyeingOrder_colorInstructionOtherTenant_throwsReferenceNotFound() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setColorInstructionId(501L);

        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any()))
                .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.createDyeingOrder(req));
        assertEquals(1_030_007_002, ex.getCode(), "他租户打色单应抛 DYEING_ORDER_REFERENCE_NOT_FOUND");
    }

    @Test
    void createDyeingOrder_colorInstructionDeleted_throwsReferenceNotFound() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setColorInstructionId(501L);

        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any()))
                .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.createDyeingOrder(req));
        assertEquals(1_030_007_002, ex.getCode(), "已软删打色单应抛 DYEING_ORDER_REFERENCE_NOT_FOUND");
    }

    @Test
    void createDyeingOrder_colorInstructionStatusNotAllowed_throwsReferenceNotFound() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        for (String invalidStatus : List.of("PENDING", "IN_PROGRESS", "VOID")) {
            DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
            req.setSalesContractId(1L);
            req.setDyeingFactoryId(2L);
            req.setColorInstructionId(501L);

            when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any()))
                    .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

            ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.createDyeingOrder(req));
            assertEquals(1_030_007_002, ex.getCode(), "非 CONFIRMED/IN_PRODUCTION 打色单应抛 DYEING_ORDER_REFERENCE_NOT_FOUND: " + invalidStatus);
        }
    }

    @Test
    void createDyeingOrder_colorInstructionCustomerMismatch_throwsReferenceNotFound() {
        // 合同客户 ID 为 100
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setColorInstructionId(501L);

        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), eq(100L), any(), any(), any()))
                .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.createDyeingOrder(req));
        assertEquals(1_030_007_002, ex.getCode(), "客户口径不一致应抛 DYEING_ORDER_REFERENCE_NOT_FOUND");
    }

    @Test
    void createDyeingOrder_colorInstructionProductMismatch_throwsReferenceNotFound() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setProductCode("P001");
        req.setColorInstructionId(501L);

        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), eq("P001"), any()))
                .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.createDyeingOrder(req));
        assertEquals(1_030_007_002, ex.getCode(), "产品口径不一致应抛 DYEING_ORDER_REFERENCE_NOT_FOUND");
    }

    @Test
    void createDyeingOrder_colorInstructionValid_successAndSnapshots() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        for (String validStatus : List.of("CONFIRMED", "IN_PRODUCTION")) {
            DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
            req.setSalesContractId(1L);
            req.setDyeingFactoryId(2L);
            req.setProductCode("P001");
            req.setColorInstructionId(501L);

            ColorInstructionDO ci = stubColorInstruction(501L, "DS20261007001", 100L, 10L, "P001", validStatus);
            when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), eq("P001"), any())).thenReturn(ci);

            dyeingOrderService.createDyeingOrder(req);

            ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
            verify(dyeingOrderMapper, times(validStatus.equals("CONFIRMED") ? 1 : 2)).insert(captor.capture());
            DyeingOrderDO inserted = captor.getValue();
            assertEquals(501L, inserted.getColorInstructionId());
            assertEquals("DS20261007001", inserted.getColorInstructionNo(), "有效打色单应正确带出单号快照");
        }
    }

    @Test
    void updateDyeingOrder_clearColorInstruction_clearsSnapshot() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setColorInstructionId(501L);
        existing.setColorInstructionNo("DS20261007001");
        existing.setOrderDate(LocalDate.now());
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        DyeingOrderSaveReqVO updateReq = new DyeingOrderSaveReqVO();
        updateReq.setId(10L);
        updateReq.setSalesContractId(1L);
        updateReq.setDyeingFactoryId(2L);
        updateReq.setColorInstructionId(null); // 清空打色单引用

        dyeingOrderService.updateDyeingOrder(updateReq);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).updateById(captor.capture());
        DyeingOrderDO updated = captor.getValue();
        assertNull(updated.getColorInstructionId(), "清空后 colorInstructionId 应为 null");
        assertNull(updated.getColorInstructionNo(), "清空后 colorInstructionNo 快照应为 null");
    }

    @Test
    void updateDyeingOrder_draftStatusStillValidatesColorInstruction() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setOrderDate(LocalDate.now());
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        DyeingOrderSaveReqVO updateReq = new DyeingOrderSaveReqVO();
        updateReq.setId(10L);
        updateReq.setSalesContractId(1L);
        updateReq.setDyeingFactoryId(2L);
        updateReq.setColorInstructionId(999L);
        when(colorInstructionService.validateColorInstructionForDownstream(eq(999L), any(), any(), any(), any()))
                .thenThrow(new ServiceException(1_030_007_002, "打色指示单"));

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.updateDyeingOrder(updateReq));
        assertEquals(1_030_007_002, ex.getCode(), "DRAFT 态更新也必须校验打色单引用合法性");
    }

    @Test
    void createDyeingOrder_singleColorInstructionConstraint_atMostOneColorInstruction() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        ColorInstructionDO ci1 = stubColorInstruction(501L, "DS20261007001", 100L, 10L, "P001", "CONFIRMED");
        ColorInstructionDO ci2 = stubColorInstruction(502L, "DS20261007002", 100L, 10L, "P001", "CONFIRMED");
        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any())).thenReturn(ci1);
        when(colorInstructionService.validateColorInstructionForDownstream(eq(502L), any(), any(), any(), any())).thenReturn(ci2);

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setColorInstructionId(501L);

        dyeingOrderService.createDyeingOrder(req);

        ArgumentCaptor<DyeingOrderDO> captor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).insert(captor.capture());
        assertEquals(501L, captor.getValue().getColorInstructionId());
        assertEquals("DS20261007001", captor.getValue().getColorInstructionNo());

        // 再次修改单据并关联另一张打色单，验证单次保存只保留唯一打色单
        req.setId(10L);
        req.setColorInstructionId(502L);
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setColorInstructionId(501L);
        existing.setColorInstructionNo("DS20261007001");
        existing.setOrderDate(LocalDate.now());
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        dyeingOrderService.updateDyeingOrder(req);
        ArgumentCaptor<DyeingOrderDO> updateCaptor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).updateById(updateCaptor.capture());
        assertEquals(502L, updateCaptor.getValue().getColorInstructionId());
        assertEquals("DS20261007002", updateCaptor.getValue().getColorInstructionNo());
    }

    // ==================== Issue 07: 投产自动触发 (染色单路径) ====================

    @Test
    void createDyeingOrder_withColorInstruction_doesNotTriggerMarkInProduction() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        ColorInstructionDO ci = stubColorInstruction(501L, "DS20261007001", 100L, 10L, "P001", "CONFIRMED");
        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any())).thenReturn(ci);

        DyeingOrderSaveReqVO req = new DyeingOrderSaveReqVO();
        req.setSalesContractId(1L);
        req.setDyeingFactoryId(2L);
        req.setColorInstructionId(501L);

        dyeingOrderService.createDyeingOrder(req);

        // DRAFT 态创建保存，严格不触发打色单投产
        verify(colorInstructionService, never()).markInProduction(any());
    }

    @Test
    void updateDyeingOrder_draftStatus_withColorInstruction_doesNotTriggerMarkInProduction() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setOrderDate(LocalDate.now());
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        ColorInstructionDO ci = stubColorInstruction(501L, "DS20261007001", 100L, 10L, "P001", "CONFIRMED");
        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any())).thenReturn(ci);

        DyeingOrderSaveReqVO updateReq = new DyeingOrderSaveReqVO();
        updateReq.setId(10L);
        updateReq.setSalesContractId(1L);
        updateReq.setDyeingFactoryId(2L);
        updateReq.setColorInstructionId(501L);

        dyeingOrderService.updateDyeingOrder(updateReq);

        // DRAFT 态更新保存，同样不触发打色单投产
        verify(colorInstructionService, never()).markInProduction(any());
    }

    @Test
    void effectDyeingOrder_withColorInstruction_triggersMarkInProduction() {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setColorInstructionId(501L);
        existing.setColorInstructionNo("DS20261007001");
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        dyeingOrderService.effectDyeingOrder(10L);

        // 下达时 status 变为 ACTIVE 并触发投产
        verify(colorInstructionService).markInProduction(501L);
    }

    @Test
    void effectDyeingOrder_withoutColorInstruction_doesNotTriggerMarkInProduction() {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setColorInstructionId(null);
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        dyeingOrderService.effectDyeingOrder(10L);

        verify(colorInstructionService, never()).markInProduction(any());
    }

    @Test
    void updateDyeingOrder_activeStatus_backfillColorInstruction_successAndTriggersMarkInProduction() {
        stubContract(1L, "CH20260001", 100L, "优衣纺织");
        stubSupplier(2L, "海宁染厂");

        // 原单据为 ACTIVE 且未引用打色单
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("ACTIVE");
        existing.setColorInstructionId(null);
        existing.setOrderDate(LocalDate.now());
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        ColorInstructionDO ci = stubColorInstruction(501L, "DS20261007001", 100L, 10L, "P001", "CONFIRMED");
        when(colorInstructionService.validateColorInstructionForDownstream(eq(501L), any(), any(), any(), any())).thenReturn(ci);

        DyeingOrderSaveReqVO updateReq = new DyeingOrderSaveReqVO();
        updateReq.setId(10L);
        updateReq.setSalesContractId(1L);
        updateReq.setDyeingFactoryId(2L);
        updateReq.setColorInstructionId(501L);

        dyeingOrderService.updateDyeingOrder(updateReq);

        // 验证保持 ACTIVE 状态落库并立即触发投产
        ArgumentCaptor<DyeingOrderDO> updateCaptor = ArgumentCaptor.forClass(DyeingOrderDO.class);
        verify(dyeingOrderMapper).updateById(updateCaptor.capture());
        assertEquals("ACTIVE", updateCaptor.getValue().getStatus(), "已 ACTIVE 单补填打色单后必须保持 ACTIVE 态");
        assertEquals(501L, updateCaptor.getValue().getColorInstructionId());
        assertEquals("DS20261007001", updateCaptor.getValue().getColorInstructionNo());

        verify(colorInstructionService).markInProduction(501L);
    }

    @Test
    void updateDyeingOrder_activeStatus_alreadyHasColorInstruction_throwsStatusNotAllowed() {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("ACTIVE");
        existing.setColorInstructionId(501L);
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        DyeingOrderSaveReqVO updateReq = new DyeingOrderSaveReqVO();
        updateReq.setId(10L);
        updateReq.setColorInstructionId(502L);

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.updateDyeingOrder(updateReq));
        assertEquals(DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(colorInstructionService, never()).markInProduction(any());
    }

    @Test
    void updateDyeingOrder_activeStatus_colorInstructionRemainsNull_throwsStatusNotAllowed() {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("ACTIVE");
        existing.setColorInstructionId(null);
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        DyeingOrderSaveReqVO updateReq = new DyeingOrderSaveReqVO();
        updateReq.setId(10L);
        updateReq.setColorInstructionId(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> dyeingOrderService.updateDyeingOrder(updateReq));
        assertEquals(DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(colorInstructionService, never()).markInProduction(any());
    }

    @Test
    void deleteDyeingOrder_doesNotRollbackColorInstruction() {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setColorInstructionId(501L);
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        dyeingOrderService.deleteDyeingOrder(10L);

        verify(dyeingOrderMapper).deleteById(10L);
        verify(colorInstructionService, never()).markInProduction(any());
    }

    @Test
    void cancelDyeingOrder_doesNotRollbackColorInstruction() {
        DyeingOrderDO existing = new DyeingOrderDO();
        existing.setId(10L);
        existing.setStatus("ACTIVE");
        existing.setColorInstructionId(501L);
        when(dyeingOrderMapper.selectById(10L)).thenReturn(existing);

        dyeingOrderService.cancelDyeingOrder(10L);

        verify(colorInstructionService, never()).markInProduction(any());
    }

}


