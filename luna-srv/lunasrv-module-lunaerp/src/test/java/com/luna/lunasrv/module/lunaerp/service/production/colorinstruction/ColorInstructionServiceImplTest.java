package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS;
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
 * - 01 票地基：分页查询条件透传（单号模糊、客户、状态、日期区间、产品编号/名称模糊）
 * - 客户色号子查询过滤与未命中短路
 * - 防 N+1：当前页记录单次批量加载颜色摘要 chips（聚合最新轮次）
 * - 详情查询与租户隔离（未查到记录抛 COLOR_INSTRUCTION_NOT_EXISTS）
 *
 * @author luna
 */
class ColorInstructionServiceImplTest {

    @Mock
    private ColorInstructionMapper colorInstructionMapper;

    @Mock
    private ColorInstructionColorMapper colorInstructionColorMapper;

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
        // 主表 selectPage 应直接被短路跳过，不发起无效查询
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

        // 构造颜色行：101 有多轮颜色（C01 轮次1驳回、轮次2通过；C02 轮次1待打色）
        ColorInstructionColorDO c1_r1 = ColorInstructionColorDO.builder()
                .id(1L).instructionId(101L).lineNo(2).customerColorNo("C01").colorName("红")
                .round(1).status("REJECTED").colorHex("#FF0000").build();
        ColorInstructionColorDO c1_r2 = ColorInstructionColorDO.builder()
                .id(2L).instructionId(101L).lineNo(1).customerColorNo("C01").colorName("红")
                .round(2).status("APPROVED").colorHex("#FF0000").build();
        ColorInstructionColorDO c2 = ColorInstructionColorDO.builder()
                .id(3L).instructionId(101L).lineNo(3).customerColorNo("C02").colorName("蓝")
                .round(1).status("PENDING").colorHex("#0000FF").build();
        // 102 颜色行
        ColorInstructionColorDO c3 = ColorInstructionColorDO.builder()
                .id(4L).instructionId(102L).lineNo(1).customerColorNo("C03").colorName("绿")
                .round(1).status("APPROVED").colorHex("#00FF00").build();

        when(colorInstructionColorMapper.selectListByInstructionIds(List.of(101L, 102L)))
                .thenReturn(Arrays.asList(c1_r1, c1_r2, c2, c3));

        PageResult<ColorInstructionRespVO> result = colorInstructionService.getColorInstructionPage(reqVO);

        assertEquals(2, result.getList().size());
        assertEquals(2L, result.getTotal());

        // 验证防 N+1：selectListByInstructionIds 必须仅执行 1 次批量查询
        verify(colorInstructionColorMapper, times(1)).selectListByInstructionIds(List.of(101L, 102L));
        // 单行逐条查询禁止执行
        verify(colorInstructionColorMapper, never()).selectListByInstructionId(anyLong());

        // 验证 101 的摘要聚合：C01 提炼为最新轮次 2 (APPROVED)，C02 为轮次 1 (PENDING)
        ColorInstructionRespVO resp1 = result.getList().get(0);
        assertEquals(101L, resp1.getId());
        assertEquals(2, resp1.getColorSummary().size());
        assertEquals("C01", resp1.getColorSummary().get(0).getCustomerColorNo());
        assertEquals(2, resp1.getColorSummary().get(0).getRound());
        assertEquals("APPROVED", resp1.getColorSummary().get(0).getStatus());
        assertEquals("C02", resp1.getColorSummary().get(1).getCustomerColorNo());
        assertEquals(1, resp1.getColorSummary().get(1).getRound());

        // 验证 102 的摘要聚合
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

}
