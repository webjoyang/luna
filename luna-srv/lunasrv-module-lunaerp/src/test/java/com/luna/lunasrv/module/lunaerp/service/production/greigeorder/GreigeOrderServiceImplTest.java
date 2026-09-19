package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GreigeOrderServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 本票（01）覆盖列表分页语义：
 *  - getGreigeOrderPage：keyword OR 模糊命中 order_no/contract_no/product_name/product_code、
 *    status 等值过滤、默认 order_date 倒序；无过滤条件时不拼接多余谓词。
 *
 * 说明：分页查询条件在 Service 构造（对齐 sales-contract 模块），测试通过捕获 Wrapper
 * 断言 SQL 片段与参数；Lambda 列名解析需先初始化 MyBatis-Plus TableInfo 缓存（见 setUp）。
 *
 * @author luna
 */
class GreigeOrderServiceImplTest {

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @InjectMocks
    private GreigeOrderServiceImpl greigeOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 MyBatis 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeOrderDO.class);
    }

    // ==================== getGreigeOrderPage：查询条件透传 ====================

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

}
