package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ColorInstructionServiceImpl} 单元测试
 *
 * 覆盖：
 * - 01 票地基：分页查询条件透传、客户色号过滤短路、防 N+1 批量加载、详情查询
 * - 02 票：打色单新建、单号按天取号含软删行、手填撞软删报错、引用校验、灯源字典校验、软删级联主表和颜色行
 *
 * @author luna
 */
class ColorInstructionServiceImplTest {

    private static final String TODAY_PREFIX = "DS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

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
    private ColorInstructionServiceImpl colorInstructionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionColorDO.class);
    }

    // ==================== 01 票地基测试 ====================

    @Test
    void getColorInstructionPage_empty_returnsEmptyPage_noColorQueries() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        when(colorInstructionMapper.selectPage(any(ColorInstructionPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        PageResult<ColorInstructionRespVO> result = colorInstructionService.getColorInstructionPage(reqVO);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        verify(colorInstructionColorMapper, never()).selectListByInstructionIds(anyCollection());
    }

    @Test
    void getColorInstructionPage_filtersPassedThrough_andOrderedByDateDesc() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setInstructionNo("DS20261010");
        reqVO.setCustomerId(1001L);
        reqVO.setStatus("PENDING");
        reqVO.setInstructionDateRange(new LocalDate[]{LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)});

        when(colorInstructionMapper.selectPage(any(ColorInstructionPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        colorInstructionService.getColorInstructionPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<ColorInstructionDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(colorInstructionMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<ColorInstructionDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("instruction_no LIKE"), "打色单号应透传到 instruction_no 模糊匹配");
        assertTrue(sqlSegment.contains("customer_id ="), "客户ID应透传到 customer_id 等值过滤");
        assertTrue(sqlSegment.contains("status ="), "状态应透传到 status 等值过滤");
        assertTrue(sqlSegment.contains("instruction_date BETWEEN") || sqlSegment.contains("instruction_date >="),
                "单据日期区间应透传到 instruction_date 过滤");
        assertTrue(sqlSegment.contains("ORDER BY instruction_date DESC"), "默认应按单据日期降序");
    }

    @Test
    void getColorInstructionPage_productCodeOrName_fuzzyFilter() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setProductCodeOrName("斜纹面料");

        when(colorInstructionMapper.selectPage(any(ColorInstructionPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        colorInstructionService.getColorInstructionPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<ColorInstructionDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(colorInstructionMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<ColorInstructionDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("LOWER(product_code) LIKE"), "产品模糊应包含 product_code");
        assertTrue(sqlSegment.contains("LOWER(product_name) LIKE"), "产品模糊应包含 product_name");
    }

    @Test
    void getColorInstructionPage_customerColorFilter_matching_appliesInWrapper() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setCustomerColorNo("C01");

        when(colorInstructionColorMapper.selectInstructionIdsByCustomerColorNo("C01"))
                .thenReturn(Set.of(100L, 200L));
        when(colorInstructionMapper.selectPage(any(ColorInstructionPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        colorInstructionService.getColorInstructionPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<ColorInstructionDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(colorInstructionMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<ColorInstructionDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("id IN"), "客户色号命中的 ID 集合应透传给主表 id IN 过滤");
    }

    @Test
    void getColorInstructionPage_customerColorFilter_noMatch_shortCircuitsEmpty() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setCustomerColorNo("NOT_EXIST_COLOR");

        when(colorInstructionColorMapper.selectInstructionIdsByCustomerColorNo("NOT_EXIST_COLOR"))
                .thenReturn(Collections.emptySet());

        PageResult<ColorInstructionRespVO> result = colorInstructionService.getColorInstructionPage(reqVO);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        verify(colorInstructionMapper, never()).selectPage(any(ColorInstructionPageReqVO.class), any(Wrapper.class));
    }

    @Test
    void getColorInstructionPage_preventNPlusOne_batchLoadsColorSummary() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ColorInstructionDO doc1 = ColorInstructionDO.builder()
                .id(101L).instructionNo("DS20261010001").clientName("客户甲").productName("面料A")
                .instructionDate(LocalDate.of(2026, 10, 10)).status("IN_PROGRESS").build();
        ColorInstructionDO doc2 = ColorInstructionDO.builder()
                .id(102L).instructionNo("DS20261010002").clientName("客户乙").productName("面料B")
                .instructionDate(LocalDate.of(2026, 10, 9)).status("CONFIRMED").build();

        when(colorInstructionMapper.selectPage(any(ColorInstructionPageReqVO.class), any(Wrapper.class)))
                .thenReturn(new PageResult<>(Arrays.asList(doc1, doc2), 2L));

        ColorInstructionColorDO c1_r1 = ColorInstructionColorDO.builder()
                .id(1L).instructionId(101L).lineNo(2).customerColorNo("C01").colorName("红")
                .round(1).status("REJECTED").colorHex("#FF0000").build();
        ColorInstructionColorDO c1_r2 = ColorInstructionColorDO.builder()
                .id(2L).instructionId(101L).lineNo(1).customerColorNo("C01").colorName("红")
                .round(2).status("APPROVED").colorHex("#FF0000").build();
        ColorInstructionColorDO c2 = ColorInstructionColorDO.builder()
                .id(3L).instructionId(101L).lineNo(3).customerColorNo("C02").colorName("蓝")
                .round(1).status("PENDING").colorHex("#0000FF").build();
        ColorInstructionColorDO c3 = ColorInstructionColorDO.builder()
                .id(4L).instructionId(102L).lineNo(1).customerColorNo("C03").colorName("绿")
                .round(1).status("APPROVED").colorHex("#00FF00").build();

        when(colorInstructionColorMapper.selectListByInstructionIds(List.of(101L, 102L)))
                .thenReturn(Arrays.asList(c1_r1, c1_r2, c2, c3));

        PageResult<ColorInstructionRespVO> result = colorInstructionService.getColorInstructionPage(reqVO);

        assertEquals(2, result.getList().size());
        assertEquals(2L, result.getTotal());
        verify(colorInstructionColorMapper, times(1)).selectListByInstructionIds(List.of(101L, 102L));
        verify(colorInstructionColorMapper, never()).selectListByInstructionId(anyLong());

        ColorInstructionRespVO resp1 = result.getList().get(0);
        assertEquals(101L, resp1.getId());
        assertEquals(2, resp1.getColorSummary().size());
        assertEquals("C01", resp1.getColorSummary().get(0).getCustomerColorNo());
        assertEquals(2, resp1.getColorSummary().get(0).getRound());
        assertEquals("APPROVED", resp1.getColorSummary().get(0).getStatus());
        assertEquals("C02", resp1.getColorSummary().get(1).getCustomerColorNo());
        assertEquals(1, resp1.getColorSummary().get(1).getRound());

        ColorInstructionRespVO resp2 = result.getList().get(1);
        assertEquals(102L, resp2.getId());
        assertEquals(1, resp2.getColorSummary().size());
        assertEquals("C03", resp2.getColorSummary().get(0).getCustomerColorNo());
    }

    @Test
    void getColorInstruction_success() {
        ColorInstructionDO doc = ColorInstructionDO.builder()
                .id(101L).instructionNo("DS20261010001").clientName("客户甲").productName("面料A")
                .instructionDate(LocalDate.of(2026, 10, 10)).status("PENDING").build();
        when(colorInstructionMapper.selectById(101L)).thenReturn(doc);

        ColorInstructionColorDO colorDO = ColorInstructionColorDO.builder()
                .id(1L).instructionId(101L).lineNo(1).customerColorNo("C01").colorName("红")
                .lightPrimary("D65").round(1).status("PENDING").build();
        when(colorInstructionColorMapper.selectListByInstructionId(101L)).thenReturn(List.of(colorDO));

        ColorInstructionRespVO resp = colorInstructionService.getColorInstruction(101L);

        assertNotNull(resp);
        assertEquals(101L, resp.getId());
        assertEquals("DS20261010001", resp.getInstructionNo());
        assertEquals(1, resp.getColors().size());
        assertEquals(1, resp.getColorSummary().size());
        assertEquals("C01", resp.getColors().get(0).getCustomerColorNo());
    }

    @Test
    void getColorInstruction_notExists_orCrossTenant_throwsException() {
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.getColorInstruction(999L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 02 票：单号生成与取号（ADR-0006） ====================

    @Test
    void getNextNo_noExistingToday_startsFrom001() {
        when(colorInstructionMapper.selectListByInstructionNoPrefixIncludingDeleted(TODAY_PREFIX))
                .thenReturn(Collections.emptyList());

        String nextNo = colorInstructionService.getNextNo();

        assertEquals(TODAY_PREFIX + "001", nextNo, "当天无单据时应从 001 起");
    }

    @Test
    void getNextNo_withExisting_returnsMaxPlusOne_includingDeleted() {
        // 软删行（含 deleted=1）也参与流水比较，保证编号不复用
        when(colorInstructionMapper.selectListByInstructionNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of(
                instruction(1L, TODAY_PREFIX + "002"),
                instruction(2L, TODAY_PREFIX + "001")
        ));

        String nextNo = colorInstructionService.getNextNo();

        assertEquals(TODAY_PREFIX + "003", nextNo, "应取当天最大数字后缀 + 1");
    }

    @Test
    void getNextNo_beyond999_expandsDigits() {
        when(colorInstructionMapper.selectListByInstructionNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of(
                instruction(1L, TODAY_PREFIX + "999")
        ));

        String nextNo = colorInstructionService.getNextNo();

        assertEquals(TODAY_PREFIX + "1000", nextNo, "超过 999 自然扩位");
    }

    // ==================== 02 票：创建打色单 ====================

    @Test
    void createColorInstruction_autoNo_success() {
        when(colorInstructionMapper.selectListByInstructionNoPrefixIncludingDeleted(TODAY_PREFIX)).thenReturn(List.of(
                instruction(1L, TODAY_PREFIX + "001")
        ));
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(TODAY_PREFIX + "002")).thenReturn(null);
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");
        stubSupplier(3001L, "盛泽染厂");

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.setInstructionNo(null); // 留空自动取号
        reqVO.setClientName(null);    // 留空快照回填
        reqVO.setProductCode(null);
        reqVO.setProductName(null);
        reqVO.setLabFactoryName(null);

        colorInstructionService.createColorInstruction(reqVO);

        ArgumentCaptor<ColorInstructionDO> captor = ArgumentCaptor.forClass(ColorInstructionDO.class);
        verify(colorInstructionMapper).insert(captor.capture());
        ColorInstructionDO saved = captor.getValue();

        assertEquals(TODAY_PREFIX + "002", saved.getInstructionNo());
        assertEquals("PENDING", saved.getStatus(), "初始状态应为 PENDING");
        assertEquals("优衣库", saved.getClientName(), "客户公司名快照回填");
        assertEquals("P001", saved.getProductCode(), "产品编号快照回填");
        assertEquals("全棉汗布", saved.getProductName(), "产品名称快照回填");
        assertEquals("盛泽染厂", saved.getLabFactoryName(), "承接方快照回填");
        assertEquals(LocalDate.now(), saved.getInstructionDate(), "单据日期缺省今天");

        // 验证颜色明细保存
        ArgumentCaptor<ColorInstructionColorDO> colorCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper, times(2)).insert(colorCaptor.capture());
        List<ColorInstructionColorDO> savedColors = colorCaptor.getAllValues();

        assertEquals(2, savedColors.size());
        assertEquals(1, savedColors.get(0).getLineNo());
        assertEquals(1, savedColors.get(0).getRound());
        assertEquals("PENDING", savedColors.get(0).getStatus());
        assertEquals("C01", savedColors.get(0).getCustomerColorNo());
        assertEquals("D65", savedColors.get(0).getLightPrimary());

        assertEquals(2, savedColors.get(1).getLineNo());
        assertEquals(1, savedColors.get(1).getRound());
        assertEquals("PENDING", savedColors.get(1).getStatus());
        assertEquals("C02", savedColors.get(1).getCustomerColorNo());
    }

    @Test
    void createColorInstruction_manualNo_success() {
        String manualNo = TODAY_PREFIX + "888";
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(manualNo)).thenReturn(null);
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.setInstructionNo(manualNo);
        reqVO.setLabFactoryId(null); // 自做，无承接方

        colorInstructionService.createColorInstruction(reqVO);

        ArgumentCaptor<ColorInstructionDO> captor = ArgumentCaptor.forClass(ColorInstructionDO.class);
        verify(colorInstructionMapper).insert(captor.capture());
        assertEquals(manualNo, captor.getValue().getInstructionNo());
        assertNull(captor.getValue().getLabFactoryId());
        assertNull(captor.getValue().getLabFactoryName());
    }

    @Test
    void createColorInstruction_manualNo_duplicate_throws1030032001() {
        String manualNo = "DS20261010001";
        // 软删单号被占用（ADR-0006），手填撞号报错不顺延
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(manualNo))
                .thenReturn(instruction(99L, manualNo));

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.setInstructionNo(manualNo);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NO_DUPLICATE.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @Test
    void createColorInstruction_autoNo_concurrencyRetry_exhausted_throwsDuplicate() {
        // 模拟连续 3 次撞号重试均被占用
        when(colorInstructionMapper.selectListByInstructionNoPrefixIncludingDeleted(TODAY_PREFIX))
                .thenReturn(Collections.emptyList());
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(TODAY_PREFIX + "001"))
                .thenReturn(instruction(1L, TODAY_PREFIX + "001"));

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.setInstructionNo(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NO_DUPLICATE.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @Test
    void createColorInstruction_customerNotExists_throws1030032003() {
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(any())).thenReturn(null);
        when(customerMapper.selectById(1001L)).thenReturn(null);

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @Test
    void createColorInstruction_productNotExists_throws1030032003() {
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(any())).thenReturn(null);
        stubCustomer(1001L, "优衣库");
        when(productMapper.selectById(2001L)).thenReturn(null);

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @Test
    void createColorInstruction_labFactoryNotExists_throws1030032003() {
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(any())).thenReturn(null);
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");
        when(supplierMapper.selectById(3001L)).thenReturn(null);

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @Test
    void createColorInstruction_labFactoryNull_allowed() {
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(any())).thenReturn(null);
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.setLabFactoryId(null);
        reqVO.setLabFactoryName(null);

        colorInstructionService.createColorInstruction(reqVO);

        ArgumentCaptor<ColorInstructionDO> captor = ArgumentCaptor.forClass(ColorInstructionDO.class);
        verify(colorInstructionMapper).insert(captor.capture());
        assertNull(captor.getValue().getLabFactoryId());
        assertNull(captor.getValue().getLabFactoryName());
        verify(supplierMapper, never()).selectById(any());
    }

    @Test
    void createColorInstruction_invalidLightPrimary_throws1030032007() {
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");
        stubSupplier(3001L, "盛泽染厂");

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.getColors().get(0).setLightPrimary("INVALID_LIGHT_SOURCE");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_LIGHT_OR_CONFIRM_TYPE_INVALID.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @Test
    void createColorInstruction_invalidLightSecondary_throws1030032007() {
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");
        stubSupplier(3001L, "盛泽染厂");

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.getColors().get(0).setLightSecondary("INVALID_SECONDARY");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_LIGHT_OR_CONFIRM_TYPE_INVALID.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).insert(any(ColorInstructionDO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"D65", "TL84", "CWF", "A", "UV", "TL83"})
    void createColorInstruction_allValidLightSources_accepted(String light) {
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(any())).thenReturn(null);
        stubCustomer(1001L, "优衣库");
        stubProduct(2001L, "P001", "全棉汗布");

        ColorInstructionSaveReqVO reqVO = validCreateReqVO();
        reqVO.setLabFactoryId(null);
        reqVO.getColors().get(0).setLightPrimary(light);
        reqVO.getColors().get(0).setLightSecondary(null);

        assertDoesNotThrow(() -> colorInstructionService.createColorInstruction(reqVO));
    }

    // ==================== 02 票：软删除（仅 PENDING，级联颜色行） ====================

    @Test
    void deleteColorInstruction_notExists_throws1030032000() {
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.deleteColorInstruction(999L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).deleteById(anyLong());
        verify(colorInstructionColorMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void deleteColorInstruction_statusNotPending_throws1030032002() {
        ColorInstructionDO doc = instruction(101L, "DS20261010001");
        doc.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(101L)).thenReturn(doc);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.deleteColorInstruction(101L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).deleteById(anyLong());
        verify(colorInstructionColorMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void deleteColorInstruction_pending_cascadesSoftDelete() {
        ColorInstructionDO doc = instruction(101L, "DS20261010001");
        doc.setStatus("PENDING");
        when(colorInstructionMapper.selectById(101L)).thenReturn(doc);

        colorInstructionService.deleteColorInstruction(101L);

        verify(colorInstructionMapper, times(1)).deleteById(101L);
        ArgumentCaptor<LambdaQueryWrapperX<ColorInstructionColorDO>> colorWrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(colorInstructionColorMapper, times(1)).delete(colorWrapperCaptor.capture());
        assertTrue(colorWrapperCaptor.getValue().getSqlSegment().contains("instruction_id ="));
    }

    // ==================== 辅助方法 ====================

    private void stubCustomer(Long id, String companyName) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setCompanyName(companyName);
        when(customerMapper.selectById(id)).thenReturn(c);
    }

    private void stubProduct(Long id, String code, String name) {
        ProductDO p = new ProductDO();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        p.setComposition("100%全棉");
        p.setWidthCm(new BigDecimal("180.00"));
        p.setWeightGsm(new BigDecimal("200.00"));
        when(productMapper.selectById(id)).thenReturn(p);
    }

    private void stubSupplier(Long id, String companyName) {
        SupplierDO s = new SupplierDO();
        s.setId(id);
        s.setCompanyName(companyName);
        when(supplierMapper.selectById(id)).thenReturn(s);
    }

    private ColorInstructionDO instruction(Long id, String instructionNo) {
        return ColorInstructionDO.builder()
                .id(id)
                .instructionNo(instructionNo)
                .status("PENDING")
                .build();
    }

    private ColorInstructionSaveReqVO validCreateReqVO() {
        return ColorInstructionSaveReqVO.builder()
                .customerId(1001L)
                .productId(2001L)
                .labFactoryId(3001L)
                .contactPerson("张三")
                .instructionDate(LocalDate.now())
                .deliveryDate(LocalDate.now().plusDays(7))
                .remark("加急打色")
                .colors(new ArrayList<>(List.of(
                        ColorInstructionColorSaveReqVO.builder()
                                .customerColorNo("C01")
                                .colorName("中国红")
                                .factoryColorNo("F01")
                                .colorHex("#FF0000")
                                .lightPrimary("D65")
                                .lightSecondary("TL84")
                                .colorCardRef("Pantone 18-1662")
                                .copies(2)
                                .build(),
                        ColorInstructionColorSaveReqVO.builder()
                                .customerColorNo("C02")
                                .colorName("深海蓝")
                                .lightPrimary("D65")
                                .copies(1)
                                .build()
                )))
                .build();
    }

}
