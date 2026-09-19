package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DyeingOrderServiceImpl} 的纯 Mockito 单元测试。
 *
 * 01 票：覆盖列表分页查询条件透传（keyword OR 模糊、status/customerId/dyeingFactoryId 等值、
 * 单据日期/交货日期区间、默认 order_date 倒序）。
 *
 * @author luna
 */
class DyeingOrderServiceImplTest {

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

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

}
