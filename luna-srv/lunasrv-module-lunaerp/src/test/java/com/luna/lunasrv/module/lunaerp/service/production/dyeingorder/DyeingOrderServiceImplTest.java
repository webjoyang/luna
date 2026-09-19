package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;

/**
 * {@link DyeingOrderServiceImpl} 的纯 Mockito 单元测试。
 *
 * 01 票：覆盖列表分页查询条件透传（keyword OR 模糊、status/customerId/dyeingFactoryId 等值、
 * 单据日期/交货日期区间、默认 order_date 倒序）。
 * 02 票：覆盖新建染色单（status 缺省 DRAFT、传 ACTIVE 保留、order_date 缺省今天、
 * 销售合同/承染方引用校验、快照写入）。
 *
 * @author luna
 */
class DyeingOrderServiceImplTest {

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

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

}
