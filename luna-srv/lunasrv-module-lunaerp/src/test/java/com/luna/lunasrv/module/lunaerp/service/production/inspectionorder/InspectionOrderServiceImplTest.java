package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InspectionOrderServiceImpl} 的纯 Mockito 单元测试。
 *
 * 01 票：覆盖列表分页查询条件透传（keyword OR 模糊、type/status/inspectionFactoryId 等值、
 * 单据日期区间、默认 order_date 倒序）。
 *
 * @author luna
 */
class InspectionOrderServiceImplTest {

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;

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

}
