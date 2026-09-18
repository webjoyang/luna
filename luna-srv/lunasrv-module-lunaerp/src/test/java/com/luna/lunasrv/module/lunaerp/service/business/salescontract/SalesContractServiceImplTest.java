package com.luna.lunasrv.module.lunaerp.service.business.salescontract;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SalesContractServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖关键业务语义：
 *  - createSalesContract：自动号 CH+年份+4位序号、序号 max+1；扫描与查重入口含已删除行（ADR-0004）；
 *    手填号撞已删除合同抛 1_030_003_001；自动号撞号有限次顺延重试；
 *    customerId 必填/客户存在校验；signDate/status/tradeType/orderType/unit/currency 默认值；
 *    clientName 快照为空时用客户公司名称回填。
 *  - getSalesContract/updateSalesContract/deleteSalesContract：不存在抛 1_030_003_000。
 *  - getSalesContractPage：keyword/status/tradeType/customerId 透传查询构造，sign_date 倒序。
 *
 * 说明：分页查询条件在 Service 构造（对齐 product 模块），测试通过捕获 Wrapper 断言 SQL 片段
 * 与参数；Lambda 列名解析需先初始化 MyBatis-Plus TableInfo 缓存（见 setUp）。
 *
 * @author luna
 */
class SalesContractServiceImplTest {

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private SalesContractServiceImpl salesContractService;

    private static final Long CUSTOMER_ID = 1024L;
    private static final String CUSTOMER_COMPANY = "杭州织造有限公司";
    private static final String YEAR_PREFIX = "CH" + LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 MyBatis 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesContractDO.class);
        // 模拟 insert 时回填主键（@KeySequence 在真实环境由 MyBatis-Plus 填充）
        when(salesContractMapper.insert(any(SalesContractDO.class))).thenAnswer(invocation -> {
            SalesContractDO dobj = invocation.getArgument(0);
            dobj.setId(2048L);
            return 1;
        });
        // 默认客户存在（各用例按需覆盖）
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(buildCustomer());
    }

    private CustomerDO buildCustomer() {
        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompany(CUSTOMER_COMPANY);
        return customer;
    }

    private SalesContractSaveReqVO buildCreateReqVO() {
        SalesContractSaveReqVO req = new SalesContractSaveReqVO();
        req.setCustomerId(CUSTOMER_ID);
        req.setSignDate(LocalDate.of(2026, 9, 1));
        return req;
    }

    // ==================== createSalesContract：自动编号 ====================

    @Test
    void createSalesContract_noContractNo_generatesChYearSeq() {
        // 无历史合同 → 生成 CH + 当前年份 + 0001
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo(null);
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString())).thenReturn(null);

        Long id = salesContractService.createSalesContract(req);

        assertEquals(2048L, id);
        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "0001", captor.getValue().getContractNo(),
                "编号缺省应为 CH+当前年份+4 位顺序号 0001");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createSalesContract_existingMax_incrementsSequenceByOne() {
        // 已有 CH20260001（未删）与 CH20260003（已删除，deleted=1）→ 取号扫描含已删除行，生成 0004
        SalesContractDO deletedMax = new SalesContractDO();
        deletedMax.setContractNo(YEAR_PREFIX + "0003");
        deletedMax.setDeleted(true); // 已删除行同样参与取号扫描（ADR-0004）
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Arrays.asList(deletedMax));
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString())).thenReturn(null);

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo(null);
        salesContractService.createSalesContract(req);

        // 取号扫描入口必须是"包含已删除行"的方法（ADR-0004），且前缀为 CH+年份
        verify(salesContractMapper).selectListByContractNoPrefixIncludingDeleted(YEAR_PREFIX);
        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "0004", captor.getValue().getContractNo(),
                "应取含已删除行在内的最大序号 +1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createSalesContract_autoNoCollides_retriesWithBumpedNumber() {
        // 第一次取号得 0002 撞号（如并发写入），扫描结果更新后第二次顺延为 0003
        SalesContractDO firstScan = new SalesContractDO();
        firstScan.setContractNo(YEAR_PREFIX + "0001");
        SalesContractDO secondScan = new SalesContractDO();
        secondScan.setContractNo(YEAR_PREFIX + "0002");
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.singletonList(firstScan))
                .thenReturn(Collections.singletonList(secondScan));
        // 第一次查重命中（撞号），第二次放行
        when(salesContractMapper.selectByContractNoIncludingDeleted(YEAR_PREFIX + "0002"))
                .thenReturn(new SalesContractDO());
        when(salesContractMapper.selectByContractNoIncludingDeleted(YEAR_PREFIX + "0003"))
                .thenReturn(null);

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo(null);
        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "0003", captor.getValue().getContractNo(),
                "自动号撞号应顺延重试后取下一个可用号");
    }

    @Test
    void createSalesContract_autoNoAlwaysCollides_throwsDuplicateAfterLimitedRetries() {
        // 持续撞号 → 有限次顺延后放弃并报 1_030_003_001，避免无限循环
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString()))
                .thenReturn(new SalesContractDO());

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.createSalesContract(req));
        assertEquals(1_030_003_001, ex.getCode());
        verify(salesContractMapper, times(3)).selectListByContractNoPrefixIncludingDeleted(anyString());
        verify(salesContractMapper, never()).insert(any(SalesContractDO.class));
    }

    // ==================== createSalesContract：手填编号 ====================

    @Test
    void createSalesContract_manualNoCollidesWithDeletedContract_throwsCode1030003001() {
        // 手填编号与已删除合同撞号（ADR-0004：软删不释放编号）→ 报 1_030_003_001，不顺延
        SalesContractDO deletedContract = new SalesContractDO();
        deletedContract.setId(1L);
        deletedContract.setContractNo("CH20260001");
        deletedContract.setDeleted(true);
        when(salesContractMapper.selectByContractNoIncludingDeleted("CH20260001"))
                .thenReturn(deletedContract);

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo("CH20260001");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.createSalesContract(req));
        assertEquals(1_030_003_001, ex.getCode());
        verify(salesContractMapper, never()).insert(any(SalesContractDO.class));
    }

    @Test
    void createSalesContract_manualNoUnique_accepted() {
        when(salesContractMapper.selectByContractNoIncludingDeleted("CH20269999")).thenReturn(null);

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo("CH20269999");
        salesContractService.createSalesContract(req);

        // 手填编号不触发取号扫描
        verify(salesContractMapper, never()).selectListByContractNoPrefixIncludingDeleted(anyString());
    }

    // ==================== createSalesContract：校验与默认值 ====================

    @Test
    void createSalesContract_customerIdNull_throwsCustomerNotExists() {
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setCustomerId(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.createSalesContract(req));
        assertEquals(1_030_004_000, ex.getCode());
        verify(salesContractMapper, never()).insert(any(SalesContractDO.class));
    }

    @Test
    void createSalesContract_customerNotExists_throwsCustomerNotExists() {
        when(customerMapper.selectById(9999L)).thenReturn(null);
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setCustomerId(9999L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.createSalesContract(req));
        assertEquals(1_030_004_000, ex.getCode());
        verify(salesContractMapper, never()).insert(any(SalesContractDO.class));
    }

    @Test
    void createSalesContract_blankEnumsAndDate_appliedDefaults() {
        // signDate/status/tradeType/orderType/unit/currency 全部留空 → 应用默认值；
        // clientName 快照为空 → 用客户公司名称回填
        SalesContractSaveReqVO req = new SalesContractSaveReqVO();
        req.setCustomerId(CUSTOMER_ID);
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString())).thenReturn(null);

        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        SalesContractDO saved = captor.getValue();
        assertEquals(LocalDate.now(), saved.getSignDate(), "签订日期缺省应为当天");
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("DOMESTIC", saved.getTradeType());
        assertEquals("BULK", saved.getOrderType());
        assertEquals("KG", saved.getUnit());
        assertEquals("RMB", saved.getCurrency());
        assertEquals(CUSTOMER_COMPANY, saved.getClientName(), "clientName 快照为空时应用客户公司名称回填");
    }

    @Test
    void createSalesContract_explicitValues_preserved() {
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setContractNo("CH20260010");
        req.setClientName("快照客户名");
        req.setStatus("COMPLETED");
        req.setTradeType("EXPORT");
        req.setOrderType("SAMPLE");
        req.setUnit("M");
        req.setCurrency("USD");
        when(salesContractMapper.selectByContractNoIncludingDeleted("CH20260010")).thenReturn(null);

        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        SalesContractDO saved = captor.getValue();
        assertEquals(LocalDate.of(2026, 9, 1), saved.getSignDate(), "显式传入的签订日期应保留");
        assertEquals("快照客户名", saved.getClientName(), "clientName 显式传入应保留");
        assertEquals("COMPLETED", saved.getStatus());
        assertEquals("EXPORT", saved.getTradeType());
        assertEquals("SAMPLE", saved.getOrderType());
        assertEquals("M", saved.getUnit());
        assertEquals("USD", saved.getCurrency());
    }

    // ==================== getSalesContract / updateSalesContract / deleteSalesContract ====================

    @Test
    void getSalesContract_notExists_throwsCode1030003000() {
        when(salesContractMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.getSalesContract(9999L));
        assertEquals(1_030_003_000, ex.getCode());
    }

    @Test
    void getSalesContract_exists_returnsRespVO() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        contract.setContractNo("CH20260001");
        contract.setCustomerId(CUSTOMER_ID);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractRespVO respVO = salesContractService.getSalesContract(2048L);
        assertEquals(2048L, respVO.getId());
        assertEquals("CH20260001", respVO.getContractNo());
    }

    @Test
    void updateSalesContract_notExists_throwsCode1030003000() {
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(9999L);
        when(salesContractMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.updateSalesContract(req));
        assertEquals(1_030_003_000, ex.getCode());
        verify(salesContractMapper, never()).updateById(any(SalesContractDO.class));
    }

    @Test
    void updateSalesContract_exists_updatesAndBackfillsClientName() {
        when(salesContractMapper.selectById(2048L)).thenReturn(new SalesContractDO());

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        req.setContractNo("CH20260001");
        salesContractService.updateSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(captor.capture());
        SalesContractDO updated = captor.getValue();
        assertEquals(2048L, updated.getId());
        assertEquals(CUSTOMER_COMPANY, updated.getClientName(), "update 时 clientName 为空也应用客户公司名称回填");
    }

    @Test
    void updateSalesContract_noChangedToOtherContract_throwsDuplicate() {
        // 编号改为他人合同（含已删除行）的编号 → 报 1_030_003_001
        SalesContractDO other = new SalesContractDO();
        other.setId(2L);
        other.setContractNo("CH20260002");
        when(salesContractMapper.selectById(2048L)).thenReturn(new SalesContractDO());
        when(salesContractMapper.selectByContractNoIncludingDeleted("CH20260002")).thenReturn(other);

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        req.setContractNo("CH20260002");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.updateSalesContract(req));
        assertEquals(1_030_003_001, ex.getCode());
        verify(salesContractMapper, never()).updateById(any(SalesContractDO.class));
    }

    @Test
    void updateSalesContract_blankContractNo_keepsExistingNumber() {
        // 编号留空视为"不修改"：置 null 跳过该列更新，避免写空串绕过编号全量唯一语义（ADR-0004）
        when(salesContractMapper.selectById(2048L)).thenReturn(new SalesContractDO());

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        req.setContractNo("");
        salesContractService.updateSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(captor.capture());
        assertEquals(null, captor.getValue().getContractNo(),
                "编号留空时应置 null 跳过该列，不应写空串");
        // 留空编号也不应触发取号扫描
        verify(salesContractMapper, never()).selectListByContractNoPrefixIncludingDeleted(anyString());
    }

    @Test
    void deleteSalesContract_notExists_throwsCode1030003000() {
        when(salesContractMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.deleteSalesContract(9999L));
        assertEquals(1_030_003_000, ex.getCode());
        verify(salesContractMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteSalesContract_exists_logicalDeleteMainTableOnly() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        contract.setStatus("ACTIVE");
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        salesContractService.deleteSalesContract(2048L);

        // 本票无明细：仅逻辑删除主表，不做状态联动
        verify(salesContractMapper).deleteById(2048L);
        verify(salesContractMapper, never()).updateById(any(SalesContractDO.class));
    }

    // ==================== getSalesContractPage：查询条件透传 ====================

    @Test
    @SuppressWarnings("unchecked")
    void getSalesContractPage_filtersPassedThrough_andOrderedBySignDateDesc() {
        SalesContractPageReqVO reqVO = new SalesContractPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("杭州");
        reqVO.setStatus("ACTIVE");
        reqVO.setTradeType("DOMESTIC");
        reqVO.setCustomerId(CUSTOMER_ID);
        when(salesContractMapper.selectPage(any(SalesContractPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        salesContractService.getSalesContractPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<SalesContractDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(salesContractMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<SalesContractDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        // keyword OR 模糊命中 contract_no / client_name / client_contract_no
        assertTrue(sqlSegment.contains("LOWER(contract_no) LIKE"), "keyword 应透传到 contract_no 模糊");
        assertTrue(sqlSegment.contains("LOWER(client_name) LIKE"), "keyword 应透传到 client_name 模糊");
        assertTrue(sqlSegment.contains("LOWER(client_contract_no) LIKE"), "keyword 应透传到 client_contract_no 模糊");
        // status / tradeType / customerId 等值透传
        assertTrue(sqlSegment.contains("status ="), "status 应透传等值过滤");
        assertTrue(sqlSegment.contains("trade_type ="), "tradeType 应透传等值过滤");
        assertTrue(sqlSegment.contains("customer_id ="), "customerId 应透传等值过滤");
        // 默认签订日期倒序
        assertTrue(sqlSegment.contains("ORDER BY sign_date DESC"), "默认应按签订日期倒序");
        // keyword 参数值透传
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%杭州%"),
                "keyword 参数值应预编译透传");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getSalesContractPage_noFilters_onlyOrderBySignDateDesc() {
        SalesContractPageReqVO reqVO = new SalesContractPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(salesContractMapper.selectPage(any(SalesContractPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        salesContractService.getSalesContractPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<SalesContractDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(salesContractMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("ORDER BY sign_date DESC"));
        assertTrue(!sqlSegment.contains("LOWER(contract_no) LIKE"), "无 keyword 时不应拼接模糊条件");
        assertTrue(!sqlSegment.contains("status ="), "无 status 时不应拼接等值过滤");
    }

}
