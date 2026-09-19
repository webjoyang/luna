package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GreigeOrderServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖行为：
 *  - 01 票：列表分页查询条件（keyword OR 模糊、status 等值、默认 order_date 倒序）
 *  - 02 票：创建/取号（ADR-0006 按天取号含已删除行、手填撞号含软删报错、
 *    四类引用校验、快照回填、缺省值 单据日期/状态/匹重/损耗）
 *  - 03 票：查看与修改（get/update 目标不存在校验、更新整体保存路径语义、
 *    编号查重排除自身、匹数人工值保留）
 *
 * 说明：分页查询条件在 Service 构造（对齐 sales-contract 模块），测试通过捕获 Wrapper
 * 断言 SQL 片段与参数；Lambda 列名解析需先初始化 MyBatis-Plus TableInfo 缓存（见 setUp）。
 *
 * @author luna
 */
class GreigeOrderServiceImplTest {

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private GreigeTypeMapper greigeTypeMapper;

    @InjectMocks
    private GreigeOrderServiceImpl greigeOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 MyBatis 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeOrderDO.class);
    }

    // ==================== getGreigeOrderPage：查询条件透传（01 票） ====================

    @Test
    void getGreigeOrderPage_filtersPassedThrough_andOrderedByOrderDateDesc() {
        GreigeOrderPageReqVO reqVO = new GreigeOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("PB0919");
        reqVO.setStatus("ACTIVE");
        when(greigeOrderMapper.selectPage(any(GreigeOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        greigeOrderService.getGreigeOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<GreigeOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(greigeOrderMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<GreigeOrderDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        // keyword OR 模糊命中 order_no / contract_no / product_name / product_code
        assertTrue(sqlSegment.contains("LOWER(order_no) LIKE"), "keyword 应透传到 order_no 模糊");
        assertTrue(sqlSegment.contains("LOWER(contract_no) LIKE"), "keyword 应透传到 contract_no 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_name) LIKE"), "keyword 应透传到 product_name 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_code) LIKE"), "keyword 应透传到 product_code 模糊");
        // status 等值透传
        assertTrue(sqlSegment.contains("status ="), "status 应透传等值过滤");
        // 默认单据日期倒序
        assertTrue(sqlSegment.contains("ORDER BY order_date DESC"), "默认应按单据日期倒序");
        // keyword 参数值透传
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%PB0919%"),
                "keyword 参数值应预编译透传");
    }

    @Test
    void getGreigeOrderPage_noFilters_onlyOrderByOrderDateDesc() {
        GreigeOrderPageReqVO reqVO = new GreigeOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(greigeOrderMapper.selectPage(any(GreigeOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        greigeOrderService.getGreigeOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<GreigeOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(greigeOrderMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("ORDER BY order_date DESC"));
        assertFalse(sqlSegment.contains("LOWER(order_no) LIKE"), "无 keyword 时不应拼接模糊条件");
        assertFalse(sqlSegment.contains("status ="), "无 status 时不应拼接等值过滤");
    }

    @Test
    void getGreigeOrderPage_blankKeyword_treatedAsNoKeyword() {
        // 空白关键词视为未输入，不拼接模糊条件
        GreigeOrderPageReqVO reqVO = new GreigeOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("   ");
        when(greigeOrderMapper.selectPage(any(GreigeOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        greigeOrderService.getGreigeOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<GreigeOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(greigeOrderMapper).selectPage(eq(reqVO), captor.capture());
        assertFalse(captor.getValue().getSqlSegment().contains("LOWER(order_no) LIKE"),
                "空白 keyword 不应拼接模糊条件");
    }

    // ==================== createGreigeOrder：自动取号（ADR-0006） ====================

    @Test
    void createGreigeOrder_blankOrderNo_generatesDailySequenceMaxPlusOne() {
        // 当天前缀 PB0919：已有 PB0919002 与一条已删除的 PB0919001（软删行也参与扫描）
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted("PB0919")).thenReturn(List.of(
                greigeOrder(1L, "PB0919002"),
                greigeOrder(2L, "PB0919001")
        ));
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919003")).thenReturn(null);
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo(null);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals("PB0919003", captor.getValue().getOrderNo(),
                "自动号应取当天前缀最大数字后缀 +1（扫描含已删除行）");
    }

    @Test
    void createGreigeOrder_blankOrderNo_noExistingToday_startsFrom001() {
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted("PB0919")).thenReturn(List.of());
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919001")).thenReturn(null);
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo("");

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals("PB0919001", captor.getValue().getOrderNo(), "当天无单据时应从 001 起");
    }

    // ==================== createGreigeOrder：手填编号查重（ADR-0006） ====================

    @Test
    void createGreigeOrder_manualOrderNo_hitsDeletedRow_throwsDuplicate() {
        // 撞号命中已删除单据（软删不释放编号）：报 GREIGE_ORDER_NO_DUPLICATE，不顺延
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919001"))
                .thenReturn(greigeOrder(9L, "PB0919001"));
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo("PB0919001");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_001, exception.getCode());
        verify(greigeOrderMapper, never()).insert(any(GreigeOrderDO.class));
    }

    @Test
    void createGreigeOrder_manualOrderNo_unique_keptAsEntered() {
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919777")).thenReturn(null);
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo("PB0919777");

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals("PB0919777", captor.getValue().getOrderNo(), "手填编号合法时应按原值保存");
    }

    // ==================== createGreigeOrder：引用校验（ADR-0007） ====================

    @Test
    void createGreigeOrder_greigeFactoryMissing_throwsReferenceNotFound() {
        when(supplierMapper.selectById(1L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    @Test
    void createGreigeOrder_dyeingFactoryMissing_throwsReferenceNotFound() {
        stubSupplier(1L, "海宁坯布厂");
        when(supplierMapper.selectById(2L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setDyeingFactoryId(2L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    @Test
    void createGreigeOrder_salesContractMissing_throwsReferenceNotFound() {
        stubSupplier(1L, "海宁坯布厂");
        when(salesContractMapper.selectById(100L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setSalesContractId(100L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    @Test
    void createGreigeOrder_greigeTypeMissing_throwsReferenceNotFound() {
        stubSupplier(1L, "海宁坯布厂");
        when(greigeTypeMapper.selectById(5L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setGreigeTypeId("5");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    @Test
    void createGreigeOrder_greigeTypeIdNotNumeric_throwsReferenceNotFound() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setGreigeTypeId("not-a-number");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    // ==================== createGreigeOrder：快照回填与缺省值 ====================

    @Test
    void createGreigeOrder_snapshotsBackfilled_andDefaultsApplied() {
        stubSupplier(1L, "海宁坯布厂");
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        when(salesContractMapper.selectById(100L)).thenReturn(contract);
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted("PB0919")).thenReturn(List.of());
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted(any(String.class))).thenReturn(null);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setSalesContractId(100L);
        reqVO.setOrderNo(null); // 走自动号
        reqVO.setContractNo(null); // 快照留空，用合同档案合同号回填
        reqVO.setOrderDate(null);
        reqVO.setStatus(null);
        reqVO.setPieceWeight(null);
        reqVO.setSuggestedLoss(null);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        GreigeOrderDO inserted = captor.getValue();
        assertEquals("海宁坯布厂", inserted.getGreigeFactoryName(), "坯布厂公司名快照留空时应用档案名称回填");
        assertEquals("CH20260001", inserted.getContractNo(), "合同号快照留空时应用合同档案合同号回填");
        assertEquals(LocalDate.now(), inserted.getOrderDate(), "单据日期缺省今天");
        assertEquals("ACTIVE", inserted.getStatus(), "状态缺省 ACTIVE");
        assertEquals(new BigDecimal("25"), inserted.getPieceWeight(), "匹重缺省 25");
        assertEquals(BigDecimal.ZERO, inserted.getSuggestedLoss(), "预设损耗缺省 0");
    }

    @Test
    void createGreigeOrder_explicitSnapshots_preservedOverArchiveName() {
        // 前端传了快照（供应商改名场景）时按原值保存
        stubSupplier(1L, "海宁坯布厂（新名称）");
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted("PB0919")).thenReturn(List.of());
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted(any(String.class))).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo(null);
        reqVO.setGreigeFactoryName("海宁坯布厂（旧名称）");

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals("海宁坯布厂（旧名称）", captor.getValue().getGreigeFactoryName(),
                "显式快照应优先于档案当前名称");
    }

    // ==================== getNextGreigeOrderNo：只读预览 ====================

    @Test
    void getNextGreigeOrderNo_previewsWithoutInserting() {
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted("PB0919")).thenReturn(List.of(
                greigeOrder(1L, "PB0919002")
        ));

        String nextNo = greigeOrderService.getNextGreigeOrderNo();

        assertEquals("PB0919003", nextNo, "预览应返回下一个自动编号");
        verify(greigeOrderMapper, never()).insert(any(GreigeOrderDO.class));
        verify(greigeOrderMapper, never()).selectByOrderNoIncludingDeleted(any(String.class));
    }

    // ==================== getGreigeOrder：详情（03 票） ====================

    @Test
    void getGreigeOrder_notExists_throwsNotExists() {
        when(greigeOrderMapper.selectById(404L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.getGreigeOrder(404L));
        assertEquals(1_030_006_000, exception.getCode(), "详情目标不存在应报 GREIGE_ORDER_NOT_EXISTS");
    }

    @Test
    void getGreigeOrder_exists_returnsAllMainFields() {
        GreigeOrderDO order = greigeOrder(1L, "PB0919001");
        order.setGreigeFactoryId(1L);
        order.setGreigeFactoryName("海宁坯布厂");
        order.setOrderDate(LocalDate.of(2026, 9, 19));
        order.setPieceWeight(new BigDecimal("25"));
        order.setPieceCount(40); // 匹数人工值原样返回
        order.setPlannedQty(new BigDecimal("1000.00"));
        order.setStatus("ACTIVE");
        when(greigeOrderMapper.selectById(1L)).thenReturn(order);

        GreigeOrderRespVO respVO = greigeOrderService.getGreigeOrder(1L);

        assertEquals(1L, respVO.getId());
        assertEquals("PB0919001", respVO.getOrderNo());
        assertEquals(1L, respVO.getGreigeFactoryId());
        assertEquals("海宁坯布厂", respVO.getGreigeFactoryName());
        assertEquals(LocalDate.of(2026, 9, 19), respVO.getOrderDate());
        assertEquals(40, respVO.getPieceCount(), "匹数人工值应原样返回");
        assertEquals("ACTIVE", respVO.getStatus());
    }

    // ==================== updateGreigeOrder：修改（03 票） ====================

    @Test
    void updateGreigeOrder_notExists_throwsNotExists() {
        when(greigeOrderMapper.selectById(404L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(404L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.updateGreigeOrder(reqVO));
        assertEquals(1_030_006_000, exception.getCode(), "更新目标不存在应报 GREIGE_ORDER_NOT_EXISTS");
        verify(greigeOrderMapper, never()).updateById(any(GreigeOrderDO.class));
    }

    @Test
    void updateGreigeOrder_success_overwritesAllSubmittedFields() {
        // 存在 + 引用齐备；手填编号命中自身（排除自身后放行）
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919777"))
                .thenReturn(greigeOrder(1L, "PB0919777"));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setOrderNo("PB0919777");
        reqVO.setPieceCount(88); // 匹数人工值
        reqVO.setOrderDate(LocalDate.of(2026, 9, 19));
        reqVO.setStatus("COMPLETED");

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).updateById(captor.capture());
        GreigeOrderDO updated = captor.getValue();
        assertEquals(1L, updated.getId());
        assertEquals("PB0919777", updated.getOrderNo(), "手填编号命中自身时按原值保存");
        assertEquals(88, updated.getPieceCount(), "匹数人工值服务端不重算、原值保留");
        assertEquals("COMPLETED", updated.getStatus());
        assertEquals("海宁坯布厂", updated.getGreigeFactoryName(), "坯布厂快照回填与创建一致");
        // 请求未携带染厂（编辑清空场景）：DO 携带 null，配合 updateStrategy=ALWAYS 落库为清空
        assertNull(updated.getDyeingFactoryId(),
                "编辑清空染厂时应以 null 提交，由 ALWAYS 更新策略落库");
    }

    @Test
    void updateGreigeOrder_manualOrderNo_hitsAnotherOrder_throwsDuplicate() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");
        // 撞号命中另一张单据（含已删除行同样命中）：报 GREIGE_ORDER_NO_DUPLICATE
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919002"))
                .thenReturn(greigeOrder(2L, "PB0919002"));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setOrderNo("PB0919002");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.updateGreigeOrder(reqVO));
        assertEquals(1_030_006_001, exception.getCode());
        verify(greigeOrderMapper, never()).updateById(any(GreigeOrderDO.class));
    }

    @Test
    void updateGreigeOrder_blankOrderNo_regeneratesDailySequence() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted("PB0919")).thenReturn(List.of());
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted("PB0919001")).thenReturn(null);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setOrderNo("  "); // 清空编号：与创建一致走自动号

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).updateById(captor.capture());
        assertEquals("PB0919001", captor.getValue().getOrderNo(), "编号留空时与创建一致自动生成");
    }

    @Test
    void updateGreigeOrder_dyeingFactoryMissing_throwsReferenceNotFound() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");
        when(supplierMapper.selectById(2L)).thenReturn(null);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setDyeingFactoryId(2L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.updateGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode(), "更新时引用校验规则与创建一致");
        verify(greigeOrderMapper, never()).updateById(any(GreigeOrderDO.class));
    }

    // ==================== 测试数据构造 ====================

    /**
     * 构造合法的最小创建请求：坯布厂 1L，编号 PB0919777（不撞号）
     */
    private GreigeOrderSaveReqVO validCreateReqVO() {
        GreigeOrderSaveReqVO reqVO = new GreigeOrderSaveReqVO();
        reqVO.setOrderNo("PB0919777");
        reqVO.setGreigeFactoryId(1L);
        reqVO.setPlannedQty(new BigDecimal("1000.00"));
        return reqVO;
    }

    private void stubSupplier(Long id, String companyName) {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(id);
        supplier.setCompanyName(companyName);
        when(supplierMapper.selectById(id)).thenReturn(supplier);
    }

    private GreigeOrderDO greigeOrder(Long id, String orderNo) {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(id);
        order.setOrderNo(orderNo);
        return order;
    }

}
