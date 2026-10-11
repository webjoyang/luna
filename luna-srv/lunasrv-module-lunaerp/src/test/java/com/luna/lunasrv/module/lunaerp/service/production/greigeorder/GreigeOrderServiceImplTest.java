package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderColorRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderWeavingProgressVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color.GreigeOrderColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color.GreigeOrderColorMapper;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.GREIGE_ORDER_REFERENCE_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
 *  - 04 票：颜色投胚分配（聚合保存删旧插新/重排 line_no、投胚需求重算口径
 *    成品量÷(1−损耗率)、行损耗缺省取主单、null 成品量按 0、详情嵌套返回、零颜色合法）
 *  - 05 票：删除与收尾（删除级联逻辑删除主表与颜色行、删除不存在报错、软删编号不重用）
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
    private ProductMapper productMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private GreigeTypeMapper greigeTypeMapper;

    @Mock
    private GreigeOrderColorMapper greigeOrderColorMapper;

    @Mock
    private GreigeOrderWeavingProgressProvider weavingProgressProvider;

    @Mock
    private ColorInstructionMapper colorInstructionMapper;

    @InjectMocks
    private GreigeOrderServiceImpl greigeOrderService;

    private static final String TODAY_PREFIX = "PB" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMdd"));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 MyBatis 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeOrderDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeOrderColorDO.class);
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
        assertFalse(sqlSegment.contains("production_mode ="), "无 productionMode 时不应拼接等值过滤");
    }

    @Test
    void getGreigeOrderPage_withProductionMode_filtersCondition() {
        GreigeOrderPageReqVO reqVO = new GreigeOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setProductionMode("SELF");
        when(greigeOrderMapper.selectPage(any(GreigeOrderPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        greigeOrderService.getGreigeOrderPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<GreigeOrderDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(greigeOrderMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("production_mode ="), "productionMode 应透传等值过滤");
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
                "空白 keyword 视为未输入，不拼接模糊条件");
    }

    // ==================== createGreigeOrder：自动编号生成（ADR-0006） ====================

    @Test
    void createGreigeOrder_blankOrderNo_generatesDailySequenceMaxPlusOne() {
        // 当天前缀：已有 002 与一条已删除的 001（软删行也参与扫描）
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of(
                greigeOrder(1L, TODAY_PREFIX + "002"),
                greigeOrder(2L, TODAY_PREFIX + "001")
        ));
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted(TODAY_PREFIX + "003")).thenReturn(null);
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo(null);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals(TODAY_PREFIX + "003", captor.getValue().getOrderNo(),
                "自动号应取当天前缀最大数字后缀 +1（扫描含已删除行）");
    }

    @Test
    void createGreigeOrder_blankOrderNo_noExistingToday_startsFrom001() {
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of());
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted(TODAY_PREFIX + "001")).thenReturn(null);
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo("");

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals(TODAY_PREFIX + "001", captor.getValue().getOrderNo(), "当天无单据时应从 001 起");
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
    void createGreigeOrder_outsourceMode_missingGreigeFactory_throwsReferenceNotFound() {
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductionMode("OUTSOURCE");
        reqVO.setGreigeFactoryId(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    @Test
    void createGreigeOrder_defaultMode_nullGreigeFactory_throwsReferenceNotFound() {
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductionMode(null); // 缺省 OUTSOURCE
        reqVO.setGreigeFactoryId(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
    }

    @Test
    void createGreigeOrder_selfMode_nullGreigeFactory_success() {
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductionMode("SELF");
        reqVO.setGreigeFactoryId(null);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        GreigeOrderDO inserted = captor.getValue();
        assertEquals("SELF", inserted.getProductionMode(), "自织单 productionMode 应为 SELF");
        assertNull(inserted.getGreigeFactoryId(), "自织单承织厂可空");
        assertNull(inserted.getGreigeFactoryName(), "自织单承织厂名称快照可空");
    }

    @Test
    void createGreigeOrder_selfMode_withGreigeFactory_validatesAndSucceeds() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductionMode("SELF");
        reqVO.setGreigeFactoryId(1L);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        GreigeOrderDO inserted = captor.getValue();
        assertEquals("SELF", inserted.getProductionMode());
        assertEquals(1L, inserted.getGreigeFactoryId());
        assertEquals("海宁坯布厂", inserted.getGreigeFactoryName());
    }

    @Test
    void createGreigeOrder_selfMode_invalidGreigeFactory_throwsReferenceNotFound() {
        when(supplierMapper.selectById(999L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductionMode("SELF");
        reqVO.setGreigeFactoryId(999L);

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

    @Test
    void createGreigeOrder_productIdNotFound_throwsReferenceNotFound() {
        stubSupplier(1L, "海宁坯布厂");
        when(productMapper.selectById(999L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductId(999L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
        assertTrue(exception.getMessage().contains("产品档案"), "错误提示应指出产品档案不存在或不可用");
    }

    @Test
    void createGreigeOrder_productIdExists_productCodeAndNameSnapshotsWritten() {
        stubSupplier(1L, "海宁坯布厂");
        stubProduct(10L, "P001", "全棉汗布");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductId(10L);
        // 即使请求中传了旧值或空值，也由产品档案快照覆盖
        reqVO.setProductCode("OLD_CODE");
        reqVO.setProductName("OLD_NAME");

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        GreigeOrderDO inserted = captor.getValue();
        assertEquals(10L, inserted.getProductId());
        assertEquals("P001", inserted.getProductCode(), "product_code 快照应写入产品档案最新值");
        assertEquals("全棉汗布", inserted.getProductName(), "product_name 快照应写入产品档案最新值");
    }

    // ==================== createGreigeOrder：快照回填与缺省值 ====================

    @Test
    void createGreigeOrder_snapshotsBackfilled_andDefaultsApplied() {
        stubSupplier(1L, "海宁坯布厂");
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        when(salesContractMapper.selectById(100L)).thenReturn(contract);
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of());
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
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of());
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
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of(
                greigeOrder(1L, TODAY_PREFIX + "002")
        ));

        String nextNo = greigeOrderService.getNextGreigeOrderNo();

        assertEquals(TODAY_PREFIX + "003", nextNo, "预览应返回下一个自动编号");
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

    @Test
    void getGreigeOrder_selfMode_enrichesWeavingProgress() {
        GreigeOrderDO order = greigeOrder(1L, "PB0919001");
        order.setProductionMode("SELF");
        order.setStatus("ACTIVE");
        when(greigeOrderMapper.selectById(1L)).thenReturn(order);
        when(weavingProgressProvider.getWeavingProgress(1L))
                .thenReturn(new GreigeOrderWeavingProgressVO(50, 30));

        GreigeOrderRespVO respVO = greigeOrderService.getGreigeOrder(1L);

        assertEquals("SELF", respVO.getProductionMode());
        org.junit.jupiter.api.Assertions.assertNotNull(respVO.getWeavingProgress(), "自织单应聚合织造进度");
        assertEquals(50, respVO.getWeavingProgress().getPlannedPieces());
        assertEquals(30, respVO.getWeavingProgress().getCompletedPieces());
        assertEquals(50, respVO.getWeavingPlannedPieces());
        assertEquals(30, respVO.getWeavingCompletedPieces());
        verify(weavingProgressProvider).getWeavingProgress(1L);
    }

    @Test
    void getGreigeOrder_selfMode_nullProgressProviderResult_defaultsToZero() {
        GreigeOrderDO order = greigeOrder(1L, "PB0919001");
        order.setProductionMode("SELF");
        when(greigeOrderMapper.selectById(1L)).thenReturn(order);
        when(weavingProgressProvider.getWeavingProgress(1L)).thenReturn(null);

        GreigeOrderRespVO respVO = greigeOrderService.getGreigeOrder(1L);

        assertEquals("SELF", respVO.getProductionMode());
        org.junit.jupiter.api.Assertions.assertNotNull(respVO.getWeavingProgress());
        assertEquals(0, respVO.getWeavingProgress().getPlannedPieces(), "无进度数据时计划匹数缺省 0");
        assertEquals(0, respVO.getWeavingProgress().getCompletedPieces(), "无进度数据时完工匹数缺省 0");
        assertEquals(0, respVO.getWeavingPlannedPieces());
        assertEquals(0, respVO.getWeavingCompletedPieces());
    }

    @Test
    void getGreigeOrder_outsourceMode_weavingProgressIsNull() {
        GreigeOrderDO order = greigeOrder(1L, "PB0919001");
        order.setProductionMode("OUTSOURCE");
        when(greigeOrderMapper.selectById(1L)).thenReturn(order);

        GreigeOrderRespVO respVO = greigeOrderService.getGreigeOrder(1L);

        assertEquals("OUTSOURCE", respVO.getProductionMode());
        assertNull(respVO.getWeavingProgress(), "外发单织造进度应为 null");
        assertNull(respVO.getWeavingPlannedPieces());
        assertNull(respVO.getWeavingCompletedPieces());
        verify(weavingProgressProvider, never()).getWeavingProgress(any());
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
    void updateGreigeOrder_productIdExists_refreshesProductSnapshots() {
        GreigeOrderDO existing = greigeOrder(1L, "PB0919777");
        existing.setProductId(10L);
        existing.setProductCode("OLD_CODE");
        existing.setProductName("旧品名");
        when(greigeOrderMapper.selectById(1L)).thenReturn(existing);
        stubSupplier(1L, "海宁坯布厂");
        // 产品档案已改名/改编号
        stubProduct(10L, "P002-NEW", "新全棉汗布");

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setProductId(10L);
        reqVO.setProductCode("OLD_CODE");
        reqVO.setProductName("旧品名");

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).updateById(captor.capture());
        GreigeOrderDO updated = captor.getValue();
        assertEquals(10L, updated.getProductId());
        assertEquals("P002-NEW", updated.getProductCode(), "更新时应刷新 product_code 快照");
        assertEquals("新全棉汗布", updated.getProductName(), "更新时应刷新 product_name 快照");
    }

    @Test
    void updateGreigeOrder_productIdNotFound_throwsReferenceNotFound() {
        GreigeOrderDO existing = greigeOrder(1L, "PB0919777");
        when(greigeOrderMapper.selectById(1L)).thenReturn(existing);
        stubSupplier(1L, "海宁坯布厂");
        when(productMapper.selectById(999L)).thenReturn(null);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setProductId(999L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.updateGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
        assertTrue(exception.getMessage().contains("产品档案"), "错误提示应指出产品档案不存在或不可用");
    }

    @Test
    void updateGreigeOrder_switchOutsourceToSelf_clearsGreigeFactory() {
        GreigeOrderDO existing = greigeOrder(1L, "PB0919777");
        existing.setGreigeFactoryId(1L);
        existing.setGreigeFactoryName("海宁坯布厂");
        existing.setProductionMode("OUTSOURCE");
        when(greigeOrderMapper.selectById(1L)).thenReturn(existing);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setOrderNo("PB0919777");
        reqVO.setProductionMode("SELF");
        reqVO.setGreigeFactoryId(null);
        reqVO.setGreigeFactoryName(null);

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).updateById(captor.capture());
        GreigeOrderDO updated = captor.getValue();
        assertEquals("SELF", updated.getProductionMode());
        assertNull(updated.getGreigeFactoryId(), "外发切自织时坯布厂清空为 null");
        assertNull(updated.getGreigeFactoryName(), "外发切自织时坯布厂快照清空为 null");
    }

    @Test
    void updateGreigeOrder_switchSelfToOutsource_nullGreigeFactory_throwsReferenceNotFound() {
        GreigeOrderDO existing = greigeOrder(1L, "PB0919777");
        existing.setProductionMode("SELF");
        when(greigeOrderMapper.selectById(1L)).thenReturn(existing);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setOrderNo("PB0919777");
        reqVO.setProductionMode("OUTSOURCE");
        reqVO.setGreigeFactoryId(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.updateGreigeOrder(reqVO));
        assertEquals(1_030_006_002, exception.getCode());
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
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of());
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted(TODAY_PREFIX + "001")).thenReturn(null);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setOrderNo("  "); // 清空编号：与创建一致走自动号

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).updateById(captor.capture());
        assertEquals(TODAY_PREFIX + "001", captor.getValue().getOrderNo(), "编号留空时与创建一致自动生成");
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

    // ==================== 颜色投胚分配（04 票） ====================

    @Test
    void createGreigeOrder_withColors_renumbersLineNoAndRecalculatesDemand() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        // 两行颜色：500÷(1−0.05)=526.32；300÷(1−0.10)=333.33
        reqVO.setColors(List.of(
                colorReq("大红", "500.00", "0.05"),
                colorReq("藏青", "300.00", "0.10")
        ));

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<List<GreigeOrderColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(greigeOrderColorMapper).insertBatch(captor.capture());
        List<GreigeOrderColorDO> inserted = captor.getValue();
        assertEquals(2, inserted.size());
        assertEquals(1, inserted.get(0).getLineNo(), "line_no 按提交顺序从 1 重排");
        assertEquals(2, inserted.get(1).getLineNo());
        assertEquals(new BigDecimal("526.32"), inserted.get(0).getDemandQty(),
                "投胚需求应按 成品量÷(1−损耗率) 服务端权威重算");
        assertEquals(new BigDecimal("333.33"), inserted.get(1).getDemandQty());
    }

    @Test
    void createGreigeOrder_frontEndDemandQtyIgnored_overwrittenByServer() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        GreigeOrderColorSaveReqVO color = colorReq("大红", "500.00", "0.05");
        color.setDemandQty(new BigDecimal("9999.99")); // 前端传入值应被服务端覆盖
        reqVO.setColors(List.of(color));

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<List<GreigeOrderColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(greigeOrderColorMapper).insertBatch(captor.capture());
        assertEquals(new BigDecimal("526.32"), captor.getValue().get(0).getDemandQty(),
                "前端传的投胚需求应被服务端重算值覆盖");
    }

    @Test
    void createGreigeOrder_rowLossNull_fallsBackToOrderSuggestedLoss() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setSuggestedLoss(new BigDecimal("0.08")); // 主单预设损耗 8%
        // 行损耗为空：取主单预设损耗 0.08；100÷(1−0.08)=108.70
        reqVO.setColors(List.of(colorReq("大红", "100.00", null)));

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<List<GreigeOrderColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(greigeOrderColorMapper).insertBatch(captor.capture());
        GreigeOrderColorDO row = captor.getValue().get(0);
        assertEquals(new BigDecimal("0.0800"), row.getLossRate(),
                "行损耗为空时应取主单预设损耗并按 numeric(6,4) 精度保存");
        assertEquals(new BigDecimal("108.70"), row.getDemandQty());
    }

    @Test
    void createGreigeOrder_nullFinishedQty_treatedAsZero() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setColors(List.of(colorReq("大红", null, "0.05")));

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<List<GreigeOrderColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(greigeOrderColorMapper).insertBatch(captor.capture());
        GreigeOrderColorDO row = captor.getValue().get(0);
        assertEquals(new BigDecimal("0.00"), row.getFinishedQty(), "null 成品量应按 0 保存");
        assertEquals(new BigDecimal("0.00"), row.getDemandQty(), "null 成品量按 0 时投胚需求为 0");
    }

    @Test
    void createGreigeOrder_zeroColors_legal_noColorRowInserted() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setColors(List.of()); // 零颜色合法

        greigeOrderService.createGreigeOrder(reqVO);

        verify(greigeOrderMapper).insert(any(GreigeOrderDO.class));
        verify(greigeOrderColorMapper, never()).insertBatch(anyList());
        verify(greigeOrderColorMapper, never()).deleteByOrderId(any(Long.class));
    }

    @Test
    void createGreigeOrder_withColors_clearsIdAndSetsLineNo() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        GreigeOrderColorSaveReqVO c1 = colorReq(999L, "大红", "500.00", "0.05");
        reqVO.setColors(List.of(c1));

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<List<GreigeOrderColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(greigeOrderColorMapper).insertBatch(captor.capture());
        List<GreigeOrderColorDO> inserted = captor.getValue();
        assertEquals(1, inserted.size());
        assertNull(inserted.get(0).getId(), "新建时即使传入 id 也应清空");
        assertEquals(1, inserted.get(0).getLineNo());
    }

    // ==================== 颜色投胚分配差异保存（ADR-0032） ====================

    @Test
    void updateGreigeOrder_allExistingColors_updatesColorsZeroDeleteZeroInsert() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        GreigeOrderColorDO c2 = stubColorDO(102L, 1L, 2, "藏青", "300.00", "0.1000", "333.33");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1, c2));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setColors(List.of(
                colorReq(101L, "大红", "500.00", "0.05"),
                colorReq(102L, "藏青", "300.00", "0.10")
        ));

        greigeOrderService.updateGreigeOrder(reqVO);

        verify(greigeOrderMapper).updateById(any(GreigeOrderDO.class));
        verify(greigeOrderColorMapper, never()).deleteByIds(any());
        verify(greigeOrderColorMapper, never()).deleteByOrderId(any());
        verify(greigeOrderColorMapper, never()).insert(any(GreigeOrderColorDO.class));

        ArgumentCaptor<GreigeOrderColorDO> captor = ArgumentCaptor.forClass(GreigeOrderColorDO.class);
        verify(greigeOrderColorMapper, times(2)).updateById(captor.capture());
        List<GreigeOrderColorDO> updated = captor.getAllValues();
        assertEquals(101L, updated.get(0).getId());
        assertEquals(1, updated.get(0).getLineNo());
        assertEquals(new BigDecimal("526.32"), updated.get(0).getDemandQty());
        assertEquals(102L, updated.get(1).getId());
        assertEquals(2, updated.get(1).getLineNo());
        assertEquals(new BigDecimal("333.33"), updated.get(1).getDemandQty());
    }

    @Test
    void updateGreigeOrder_modifyColor_updatesTargetColorWithNewValues() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        GreigeOrderColorDO c2 = stubColorDO(102L, 1L, 2, "藏青", "300.00", "0.1000", "333.33");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1, c2));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        // 修改 c1：成色量变为 600.00，损耗率变为 0.08 → 600 ÷ (1 - 0.08) = 652.17
        reqVO.setColors(List.of(
                colorReq(101L, "枣红", "600.00", "0.08"),
                colorReq(102L, "藏青", "300.00", "0.10")
        ));

        greigeOrderService.updateGreigeOrder(reqVO);

        verify(greigeOrderColorMapper, never()).deleteByIds(any());
        verify(greigeOrderColorMapper, never()).insert(any(GreigeOrderColorDO.class));

        ArgumentCaptor<GreigeOrderColorDO> captor = ArgumentCaptor.forClass(GreigeOrderColorDO.class);
        verify(greigeOrderColorMapper, times(2)).updateById(captor.capture());
        GreigeOrderColorDO updatedC1 = captor.getAllValues().stream()
                .filter(c -> c.getId().equals(101L)).findFirst().orElseThrow();
        assertEquals("枣红", updatedC1.getColor());
        assertEquals(new BigDecimal("600.00"), updatedC1.getFinishedQty());
        assertEquals(new BigDecimal("0.0800"), updatedC1.getLossRate());
        assertEquals(new BigDecimal("652.17"), updatedC1.getDemandQty());
    }

    @Test
    void updateGreigeOrder_removeColor_logicallyDeletesRemovedColor() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        GreigeOrderColorDO c2 = stubColorDO(102L, 1L, 2, "藏青", "300.00", "0.1000", "333.33");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1, c2));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        // 仅保留 101，删除 102
        reqVO.setColors(List.of(
                colorReq(101L, "大红", "500.00", "0.05")
        ));

        greigeOrderService.updateGreigeOrder(reqVO);

        verify(greigeOrderColorMapper).deleteByIds(eq(Set.of(102L)));

        ArgumentCaptor<GreigeOrderColorDO> captor = ArgumentCaptor.forClass(GreigeOrderColorDO.class);
        verify(greigeOrderColorMapper, times(1)).updateById(captor.capture());
        assertEquals(101L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getLineNo());

        verify(greigeOrderColorMapper, never()).insert(any(GreigeOrderColorDO.class));
    }

    @Test
    void updateGreigeOrder_addColor_insertsNewColorAndPreservesExisting() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setColors(List.of(
                colorReq(101L, "大红", "500.00", "0.05"),
                colorReq(null, "翠绿", "200.00", "0.05")
        ));

        greigeOrderService.updateGreigeOrder(reqVO);

        verify(greigeOrderColorMapper, never()).deleteByIds(any());
        verify(greigeOrderColorMapper, times(1)).updateById(any(GreigeOrderColorDO.class));

        ArgumentCaptor<GreigeOrderColorDO> insertCaptor = ArgumentCaptor.forClass(GreigeOrderColorDO.class);
        verify(greigeOrderColorMapper).insert(insertCaptor.capture());
        GreigeOrderColorDO inserted = insertCaptor.getValue();
        assertNull(inserted.getId());
        assertEquals(1L, inserted.getOrderId());
        assertEquals(2, inserted.getLineNo());
        assertEquals("翠绿", inserted.getColor());
        // 200 ÷ (1 - 0.05) = 210.53
        assertEquals(new BigDecimal("210.53"), inserted.getDemandQty());
    }

    @Test
    void updateGreigeOrder_colorNotBelongingToOrder_throwsInvalidParamException() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        // 传入不属于本单据的 ID 999L
        reqVO.setColors(List.of(
                colorReq(999L, "错误颜色", "100.00", "0.05")
        ));

        assertThrows(ServiceException.class, () -> greigeOrderService.updateGreigeOrder(reqVO));
        verify(greigeOrderColorMapper, never()).updateById(any(GreigeOrderColorDO.class));
        verify(greigeOrderColorMapper, never()).insert(any(GreigeOrderColorDO.class));
        verify(greigeOrderColorMapper, never()).deleteByIds(any());
    }

    @Test
    void updateGreigeOrder_duplicateColorIds_throwsInvalidParamException() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setColors(List.of(
                colorReq(101L, "大红", "500.00", "0.05"),
                colorReq(101L, "大红重复", "500.00", "0.05")
        ));

        assertThrows(ServiceException.class, () -> greigeOrderService.updateGreigeOrder(reqVO));
        verify(greigeOrderColorMapper, never()).updateById(any(GreigeOrderColorDO.class));
        verify(greigeOrderColorMapper, never()).insert(any(GreigeOrderColorDO.class));
        verify(greigeOrderColorMapper, never()).deleteByIds(any());
    }

    @Test
    void updateGreigeOrder_toEmptyColors_deletesAllOldColors() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        GreigeOrderColorDO c2 = stubColorDO(102L, 1L, 2, "藏青", "300.00", "0.1000", "333.33");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1, c2));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setColors(List.of());

        greigeOrderService.updateGreigeOrder(reqVO);

        verify(greigeOrderMapper).updateById(any(GreigeOrderDO.class));
        verify(greigeOrderColorMapper).deleteByIds(eq(Set.of(101L, 102L)));
        verify(greigeOrderColorMapper, never()).updateById(any(GreigeOrderColorDO.class));
        verify(greigeOrderColorMapper, never()).insert(any(GreigeOrderColorDO.class));
    }

    @Test
    void updateGreigeOrder_reorderColors_updatesLineNoAccordingToSubmitOrder() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "500.00", "0.0500", "526.32");
        GreigeOrderColorDO c2 = stubColorDO(102L, 1L, 2, "藏青", "300.00", "0.1000", "333.33");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1, c2));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        // 调换顺序：c2 先，c1 后
        reqVO.setColors(List.of(
                colorReq(102L, "藏青", "300.00", "0.10"),
                colorReq(101L, "大红", "500.00", "0.05")
        ));

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderColorDO> captor = ArgumentCaptor.forClass(GreigeOrderColorDO.class);
        verify(greigeOrderColorMapper, times(2)).updateById(captor.capture());
        List<GreigeOrderColorDO> updatedList = captor.getAllValues();
        assertEquals(102L, updatedList.get(0).getId());
        assertEquals(1, updatedList.get(0).getLineNo());
        assertEquals(101L, updatedList.get(1).getId());
        assertEquals(2, updatedList.get(1).getLineNo());
    }

    @Test
    void updateGreigeOrder_rowLossTakesOrderSuggestedLossWhenNull() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919777"));
        stubSupplier(1L, "海宁坯布厂");

        GreigeOrderColorDO c1 = stubColorDO(101L, 1L, 1, "大红", "100.00", "0.0500", "105.26");
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(c1));

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setSuggestedLoss(new BigDecimal("0.08"));
        reqVO.setColors(List.of(colorReq(101L, "大红", "100.00", null)));

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderColorDO> captor = ArgumentCaptor.forClass(GreigeOrderColorDO.class);
        verify(greigeOrderColorMapper).updateById(captor.capture());
        GreigeOrderColorDO row = captor.getValue();
        assertEquals(new BigDecimal("0.0800"), row.getLossRate(), "更新时行损耗缺省同样取主单预设损耗");
        assertEquals(new BigDecimal("108.70"), row.getDemandQty());
    }

    @Test
    void createGreigeOrder_lossRateAtOrAboveOne_demandNotAmplified() {
        // 损耗率 ≥ 100%（除数 ≤ 0）为异常口径：按不放大处理（需求 = 成品量），避免除零
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setColors(List.of(colorReq("大红", "100.00", "1")));

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<List<GreigeOrderColorDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(greigeOrderColorMapper).insertBatch(captor.capture());
        assertEquals(new BigDecimal("100.00"), captor.getValue().get(0).getDemandQty());
    }

    @Test
    void getGreigeOrder_nestsColorRows() {
        GreigeOrderDO order = greigeOrder(1L, "PB0919001");
        order.setStatus("ACTIVE");
        when(greigeOrderMapper.selectById(1L)).thenReturn(order);
        GreigeOrderColorDO row = new GreigeOrderColorDO();
        row.setId(11L);
        row.setOrderId(1L);
        row.setLineNo(1);
        row.setColor("大红");
        row.setFinishedQty(new BigDecimal("500.00"));
        row.setLossRate(new BigDecimal("0.0500"));
        row.setDemandQty(new BigDecimal("526.32"));
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of(row));

        GreigeOrderRespVO respVO = greigeOrderService.getGreigeOrder(1L);

        assertEquals(1, respVO.getColors().size(), "详情应嵌套返回颜色行");
        GreigeOrderColorRespVO colorVO = respVO.getColors().get(0);
        assertEquals(11L, colorVO.getId());
        assertEquals("大红", colorVO.getColor());
        assertEquals(new BigDecimal("526.32"), colorVO.getDemandQty());
    }

    // ==================== deleteGreigeOrder：删除与收尾（05 票） ====================

    @Test
    void deleteGreigeOrder_exists_cascadesMainAndColorRows() {
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder(1L, "PB0919001"));

        greigeOrderService.deleteGreigeOrder(1L);

        // 主表与全部颜色行一并逻辑删除（颜色行随单聚合，ADR-0003）
        verify(greigeOrderMapper).deleteById(1L);
        verify(greigeOrderColorMapper).deleteByOrderId(1L);
    }

    @Test
    void deleteGreigeOrder_notExists_throwsNotExists() {
        when(greigeOrderMapper.selectById(404L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> greigeOrderService.deleteGreigeOrder(404L));
        assertEquals(1_030_006_000, exception.getCode(), "删除目标不存在应报 GREIGE_ORDER_NOT_EXISTS");
        verify(greigeOrderMapper, never()).deleteById(any(Long.class));
        verify(greigeOrderColorMapper, never()).deleteByOrderId(any(Long.class));
    }

    @Test
    void createGreigeOrder_afterDelete_nextNoSkipsDeletedNumber() {
        // 软删不释放编号（ADR-0006）：当天唯一的单据 PB0919001 已删除，取号扫描含软删行，
        // 新单应顺延为 PB0919002 而非重用 PB0919001
        when(greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of(
                greigeOrder(1L, TODAY_PREFIX + "001") // 该行 deleted=1
        ));
        when(greigeOrderMapper.selectByOrderNoIncludingDeleted(TODAY_PREFIX + "002")).thenReturn(null);
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setOrderNo(null);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        assertEquals(TODAY_PREFIX + "002", captor.getValue().getOrderNo(),
                "软删编号不应被新单重用，自动号应顺延");
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

    private void stubProduct(Long id, String code, String name) {
        ProductDO product = new ProductDO();
        product.setId(id);
        product.setCode(code);
        product.setName(name);
        when(productMapper.selectById(id)).thenReturn(product);
    }

    /**
     * 构造颜色行请求：颜色 / 成品下单量（可为 null）/ 损耗率（可为 null）
     */
    private GreigeOrderColorSaveReqVO colorReq(String color, String finishedQty, String lossRate) {
        GreigeOrderColorSaveReqVO colorVO = new GreigeOrderColorSaveReqVO();
        colorVO.setColor(color);
        colorVO.setFinishedQty(finishedQty == null ? null : new BigDecimal(finishedQty));
        colorVO.setLossRate(lossRate == null ? null : new BigDecimal(lossRate));
        return colorVO;
    }

    private GreigeOrderColorSaveReqVO colorReq(Long id, String color, String finishedQty, String lossRate) {
        GreigeOrderColorSaveReqVO colorVO = colorReq(color, finishedQty, lossRate);
        colorVO.setId(id);
        return colorVO;
    }

    private GreigeOrderColorDO stubColorDO(Long id, Long orderId, Integer lineNo, String color, String finishedQty, String lossRate, String demandQty) {
        GreigeOrderColorDO colorDO = new GreigeOrderColorDO();
        colorDO.setId(id);
        colorDO.setOrderId(orderId);
        colorDO.setLineNo(lineNo);
        colorDO.setColor(color);
        colorDO.setFinishedQty(finishedQty == null ? BigDecimal.ZERO : new BigDecimal(finishedQty));
        colorDO.setLossRate(lossRate == null ? BigDecimal.ZERO : new BigDecimal(lossRate));
        colorDO.setDemandQty(demandQty == null ? BigDecimal.ZERO : new BigDecimal(demandQty));
        return colorDO;
    }

    private GreigeOrderDO greigeOrder(Long id, String orderNo) {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(id);
        order.setOrderNo(orderNo);
        return order;
    }

    private ColorInstructionDO stubColorInstruction(Long id, String no, String status, Long productId, Long customerId) {
        ColorInstructionDO instruction = new ColorInstructionDO();
        instruction.setId(id);
        instruction.setInstructionNo(no);
        instruction.setStatus(status);
        instruction.setProductId(productId);
        instruction.setCustomerId(customerId);
        instruction.setDeleted(false);
        instruction.setTenantId(1L);
        when(colorInstructionMapper.selectById(id)).thenReturn(instruction);
        return instruction;
    }

    // ==================== 下游引用打色单保存校验（06 票） ====================

    @Test
    void createGreigeOrder_colorInstructionNotFound_throwsException() {
        stubSupplier(1L, "海宁坯布厂");
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);
        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setColorInstructionId(999L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(GREIGE_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("打色指示单"));
    }

    @Test
    void createGreigeOrder_colorInstructionSoftDeleted_throwsException() {
        stubSupplier(1L, "海宁坯布厂");
        ColorInstructionDO instruction = stubColorInstruction(100L, "DS20261010001", "CONFIRMED", 10L, 20L);
        instruction.setDeleted(true); // 软删除

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setColorInstructionId(100L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(GREIGE_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("打色指示单"));
    }

    @Test
    void createGreigeOrder_colorInstructionCrossTenant_throwsException() {
        stubSupplier(1L, "海宁坯布厂");
        ColorInstructionDO instruction = stubColorInstruction(100L, "DS20261010001", "CONFIRMED", 10L, 20L);
        instruction.setTenantId(9999L); // 他租户

        TenantContextHolder.setTenantId(1L);
        try {
            GreigeOrderSaveReqVO reqVO = validCreateReqVO();
            reqVO.setColorInstructionId(100L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> greigeOrderService.createGreigeOrder(reqVO));
            assertEquals(GREIGE_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("打色指示单"));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void createGreigeOrder_colorInstructionStatusInvalid_throwsException() {
        stubSupplier(1L, "海宁坯布厂");
        for (String invalidStatus : List.of("PENDING", "IN_PROGRESS", "VOID")) {
            stubColorInstruction(100L, "DS20261010001", invalidStatus, 10L, 20L);
            GreigeOrderSaveReqVO reqVO = validCreateReqVO();
            reqVO.setColorInstructionId(100L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> greigeOrderService.createGreigeOrder(reqVO),
                    "状态为 " + invalidStatus + " 时应拦截");
            assertEquals(GREIGE_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("打色指示单状态不是已确认或已投产"));
        }
    }

    @Test
    void createGreigeOrder_colorInstructionProductMismatch_throwsException() {
        stubSupplier(1L, "海宁坯布厂");
        stubProduct(10L, "P01", "全棉");
        stubColorInstruction(100L, "DS20261010001", "CONFIRMED", 20L, 30L); // product 20L != 10L

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductId(10L);
        reqVO.setColorInstructionId(100L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(GREIGE_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("打色指示单产品与胚布单产品不一致"));
    }

    @Test
    void createGreigeOrder_colorInstructionCustomerMismatch_throwsException() {
        stubSupplier(1L, "海宁坯布厂");
        SalesContractDO contract = new SalesContractDO();
        contract.setId(50L);
        contract.setCustomerId(88L); // customer 88L
        when(salesContractMapper.selectById(50L)).thenReturn(contract);

        stubColorInstruction(100L, "DS20261010001", "CONFIRMED", 10L, 99L); // customer 99L != 88L

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setSalesContractId(50L);
        reqVO.setColorInstructionId(100L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeOrderService.createGreigeOrder(reqVO));
        assertEquals(GREIGE_ORDER_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("打色指示单客户与销售合同客户不一致"));
    }

    @Test
    void createGreigeOrder_withConfirmedOrInProductionColorInstruction_succeedsAndSnapshotsNo() {
        stubSupplier(1L, "海宁坯布厂");
        stubProduct(10L, "P01", "全棉");
        stubColorInstruction(100L, "DS20261010001", "CONFIRMED", 10L, 20L);

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setProductId(10L);
        reqVO.setColorInstructionId(100L);

        greigeOrderService.createGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).insert(captor.capture());
        GreigeOrderDO inserted = captor.getValue();
        assertEquals(100L, inserted.getColorInstructionId());
        assertEquals("DS20261010001", inserted.getColorInstructionNo(), "应快照打色单号");
    }

    @Test
    void updateGreigeOrder_clearingColorInstructionId_clearsSnapshotNo() {
        stubSupplier(1L, "海宁坯布厂");
        GreigeOrderDO existing = greigeOrder(1L, "PB0919777");
        existing.setColorInstructionId(100L);
        existing.setColorInstructionNo("DS20261010001");
        when(greigeOrderMapper.selectById(1L)).thenReturn(existing);
        when(greigeOrderColorMapper.selectListByOrderId(1L)).thenReturn(List.of());

        GreigeOrderSaveReqVO reqVO = validCreateReqVO();
        reqVO.setId(1L);
        reqVO.setColorInstructionId(null); // 清空打色指示单引用
        reqVO.setColorInstructionNo(null);

        greigeOrderService.updateGreigeOrder(reqVO);

        ArgumentCaptor<GreigeOrderDO> captor = ArgumentCaptor.forClass(GreigeOrderDO.class);
        verify(greigeOrderMapper).updateById(captor.capture());
        GreigeOrderDO updated = captor.getValue();
        assertNull(updated.getColorInstructionId());
        assertNull(updated.getColorInstructionNo(), "清空打色指示单引用时快照应置空");
    }

}
