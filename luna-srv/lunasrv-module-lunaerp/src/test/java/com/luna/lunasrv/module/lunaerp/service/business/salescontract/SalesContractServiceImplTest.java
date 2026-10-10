package com.luna.lunasrv.module.lunaerp.service.business.salescontract;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private SalesContractItemMapper salesContractItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private SalesContractServiceImpl salesContractService;

    private static final Long CUSTOMER_ID = 1024L;
    private static final String CUSTOMER_COMPANY_NAME = "杭州织造有限公司";
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
        customer.setCompanyName(CUSTOMER_COMPANY_NAME);
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
        assertEquals(CUSTOMER_COMPANY_NAME, saved.getClientName(), "clientName 快照为空时应用客户公司名称回填");
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
        // 零明细合同：明细查询返回空列表
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Collections.emptyList());

        SalesContractRespVO respVO = salesContractService.getSalesContract(2048L);
        assertEquals(2048L, respVO.getId());
        assertEquals("CH20260001", respVO.getContractNo());
        assertNotNull(respVO.getItems());
        assertTrue(respVO.getItems().isEmpty(), "零明细合同应返回空明细列表");
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
        assertEquals(CUSTOMER_COMPANY_NAME, updated.getClientName(), "update 时 clientName 为空也应用客户公司名称回填");
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
    void deleteSalesContract_exists_logicalDeleteMainTableAndCascadeItems() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        contract.setStatus("ACTIVE");
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        salesContractService.deleteSalesContract(2048L);

        // 逻辑删除主表 + 级联逻辑删除全部明细（明细是合同聚合的一部分，ADR-0003）
        verify(salesContractMapper).deleteById(2048L);
        verify(salesContractItemMapper).deleteByContractId(2048L);
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
        reqVO.setApprovalStatus("APPROVING");
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
        // status / approvalStatus / tradeType / customerId 等值透传
        assertTrue(sqlSegment.contains("status ="), "status 应透传等值过滤");
        assertTrue(sqlSegment.contains("approval_status ="), "approvalStatus 应透传等值过滤");
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

    // ==================== getNextContractNo：取号预览（只读不占号） ====================

    @Test
    void getNextContractNo_noContracts_returnsChYear0001() {
        // 当年无合同（含已删除行均无）→ 预览 CH + 当前年份 + 0001，与自动取号一致
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.emptyList());

        String nextNo = salesContractService.getNextContractNo();

        assertEquals(YEAR_PREFIX + "0001", nextNo);
        // 只读不占号：不做撞号查重、不写库
        verify(salesContractMapper, never()).selectByContractNoIncludingDeleted(anyString());
        verify(salesContractMapper, never()).insert(any(SalesContractDO.class));
    }

    @Test
    void getNextContractNo_normalMaxExists_returnsMaxPlusOne() {
        // 已有未删除的 CH{year}0005 → 预览 0006，与自动取号 +1 行为一致
        SalesContractDO normalMax = new SalesContractDO();
        normalMax.setContractNo(YEAR_PREFIX + "0005");
        normalMax.setDeleted(false);
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Arrays.asList(normalMax));

        assertEquals(YEAR_PREFIX + "0006", salesContractService.getNextContractNo());
    }

    @Test
    void getNextContractNo_deletedRowOccupiesSeq_returnsIncrementedSameAsAutoNo() {
        // 最大序号来自已删除行（ADR-0004：软删序号仍占用）→ 预览同样 +1，与自动取号行为一致
        SalesContractDO deletedMax = new SalesContractDO();
        deletedMax.setContractNo(YEAR_PREFIX + "0007");
        deletedMax.setDeleted(true);
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Arrays.asList(deletedMax));

        assertEquals(YEAR_PREFIX + "0008", salesContractService.getNextContractNo());
    }

    // ==================== 商品明细聚合（ADR-0003） ====================

    /** 构造两行明细请求 */
    private List<SalesContractItemSaveReqVO> buildTwoItems() {
        SalesContractItemSaveReqVO item1 = new SalesContractItemSaveReqVO();
        item1.setProductCode("P001");
        item1.setProductName("纯棉平布");
        item1.setQuantity(new BigDecimal("100"));
        item1.setUnitPrice(new BigDecimal("15.50"));
        item1.setOtherFees(new BigDecimal("10"));

        SalesContractItemSaveReqVO item2 = new SalesContractItemSaveReqVO();
        item2.setProductCode("P002");
        item2.setQuantity(new BigDecimal("200"));
        item2.setUnitPrice(new BigDecimal("20"));
        item2.setOtherFees(null); // null 应按 0 处理

        return Arrays.asList(item1, item2);
    }

    @Test
    void createSalesContract_withItems_insertsItemsReordersLineNoAndRecalculates() {
        // 行1: 100 × 15.50 + 10 = 1560；行2: 200 × 20 + 0 = 4000
        // totalQuantity = 300，totalAmount = 5560
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setItems(buildTwoItems());
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString())).thenReturn(null);

        salesContractService.createSalesContract(req);

        // 主表汇总由服务端重算
        ArgumentCaptor<SalesContractDO> contractCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(contractCaptor.capture());
        SalesContractDO saved = contractCaptor.getValue();
        assertEquals(new BigDecimal("300.00"), saved.getTotalQuantity(), "总数量应为两行数量之和");
        assertEquals(new BigDecimal("5560.00"), saved.getTotalAmount(), "总金额应为两行金额之和");

        // 明细批量插入，line_no 按提交顺序从 1 重排，行金额权威重算
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SalesContractItemDO>> itemsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(salesContractItemMapper).insertBatch(itemsCaptor.capture());
        List<SalesContractItemDO> savedItems = itemsCaptor.getValue();
        assertEquals(2, savedItems.size());
        assertEquals(1, savedItems.get(0).getLineNo());
        assertEquals(2, savedItems.get(1).getLineNo());
        assertEquals(new BigDecimal("1560.00"), savedItems.get(0).getAmount(),
                "行1金额 = 100×15.50+10");
        assertEquals(new BigDecimal("4000.00"), savedItems.get(1).getAmount(),
                "行2金额 = 200×20+0（otherFees null 按 0）");
        // contractId 回填到主表主键
        assertEquals(2048L, savedItems.get(0).getContractId());
    }

    @Test
    void createSalesContract_withWrongTotalsInRequest_serverOverridesWithRecalculated() {
        // 前端传入错误的 totalQuantity/totalAmount，服务端必须忽略并按明细重算
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setItems(buildTwoItems());
        req.setTotalQuantity(new BigDecimal("999"));
        req.setTotalAmount(new BigDecimal("9999"));
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString())).thenReturn(null);

        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        assertEquals(new BigDecimal("300.00"), captor.getValue().getTotalQuantity(),
                "请求传入的错误 totalQuantity 必须被服务端重算值覆盖");
        assertEquals(new BigDecimal("5560.00"), captor.getValue().getTotalAmount(),
                "请求传入的错误 totalAmount 必须被服务端重算值覆盖");
    }

    @Test
    void createSalesContract_zeroItems_totalsAreZeroAndNoItemInsert() {
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setItems(Collections.emptyList());
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(salesContractMapper.selectByContractNoIncludingDeleted(anyString())).thenReturn(null);

        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        assertEquals(new BigDecimal("0.00"), captor.getValue().getTotalQuantity(), "零明细总数量应为 0");
        assertEquals(new BigDecimal("0.00"), captor.getValue().getTotalAmount(), "零明细总金额应为 0");
        verify(salesContractItemMapper, never()).insertBatch(any());
    }

    private SalesContractItemDO buildItemDO(Long id, Long contractId, int lineNo, String productCode, BigDecimal quantity, BigDecimal unitPrice, BigDecimal otherFees) {
        SalesContractItemDO item = new SalesContractItemDO();
        item.setId(id);
        item.setContractId(contractId);
        item.setLineNo(lineNo);
        item.setProductCode(productCode);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setOtherFees(otherFees);
        item.setAmount(quantity.multiply(unitPrice).add(otherFees == null ? BigDecimal.ZERO : otherFees));
        return item;
    }

    @Test
    void updateSalesContract_allExistingItems_updatesItemsZeroDeleteZeroInsert() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), new BigDecimal("10"));
        SalesContractItemDO dbItem2 = buildItemDO(102L, 2048L, 2, "P002", new BigDecimal("200"), new BigDecimal("20.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        List<SalesContractItemSaveReqVO> items = buildTwoItems();
        items.get(0).setId(101L);
        items.get(1).setId(102L);
        req.setItems(items);

        salesContractService.updateSalesContract(req);

        // 零逻辑删除、零 insert
        verify(salesContractItemMapper, never()).deleteByIds(any());
        verify(salesContractItemMapper, never()).deleteByContractId(any());
        verify(salesContractItemMapper, never()).insert(any(SalesContractItemDO.class));
        verify(salesContractItemMapper, never()).insertBatch(any());

        // 存量行调用 updateById
        ArgumentCaptor<SalesContractItemDO> updateCaptor = ArgumentCaptor.forClass(SalesContractItemDO.class);
        verify(salesContractItemMapper, times(2)).updateById(updateCaptor.capture());
        List<SalesContractItemDO> updatedItems = updateCaptor.getAllValues();
        assertEquals(101L, updatedItems.get(0).getId());
        assertEquals(1, updatedItems.get(0).getLineNo());
        assertEquals(new BigDecimal("1560.00"), updatedItems.get(0).getAmount());
        assertEquals(102L, updatedItems.get(1).getId());
        assertEquals(2, updatedItems.get(1).getLineNo());
        assertEquals(new BigDecimal("4000.00"), updatedItems.get(1).getAmount());

        // 主表汇总重算
        ArgumentCaptor<SalesContractDO> contractCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(contractCaptor.capture());
        assertEquals(new BigDecimal("300.00"), contractCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("5560.00"), contractCaptor.getValue().getTotalAmount());
    }

    @Test
    void updateSalesContract_modifyItem_updatesTargetItemWithNewValues() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), new BigDecimal("10"));
        SalesContractItemDO dbItem2 = buildItemDO(102L, 2048L, 2, "P002", new BigDecimal("200"), new BigDecimal("20.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        List<SalesContractItemSaveReqVO> items = buildTwoItems();
        items.get(0).setId(101L);
        items.get(0).setQuantity(new BigDecimal("150")); // 150 × 15.50 + 10 = 2335.00
        items.get(1).setId(102L);
        req.setItems(items);

        salesContractService.updateSalesContract(req);

        verify(salesContractItemMapper, never()).deleteByIds(any());
        verify(salesContractItemMapper, never()).insert(any(SalesContractItemDO.class));
        verify(salesContractItemMapper, never()).insertBatch(any());

        ArgumentCaptor<SalesContractItemDO> updateCaptor = ArgumentCaptor.forClass(SalesContractItemDO.class);
        verify(salesContractItemMapper, times(2)).updateById(updateCaptor.capture());
        SalesContractItemDO item1Updated = updateCaptor.getAllValues().get(0);
        assertEquals(101L, item1Updated.getId());
        assertEquals(new BigDecimal("150.00"), item1Updated.getQuantity());
        assertEquals(new BigDecimal("2335.00"), item1Updated.getAmount());

        // 主表汇总: 150 + 200 = 350, 2335 + 4000 = 6335
        ArgumentCaptor<SalesContractDO> contractCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(contractCaptor.capture());
        assertEquals(new BigDecimal("350.00"), contractCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("6335.00"), contractCaptor.getValue().getTotalAmount());
    }

    @Test
    void updateSalesContract_removeItem_logicallyDeletesRemovedItem() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), new BigDecimal("10"));
        SalesContractItemDO dbItem2 = buildItemDO(102L, 2048L, 2, "P002", new BigDecimal("200"), new BigDecimal("20.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        // 只保留 item1，移除 item2
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        List<SalesContractItemSaveReqVO> items = Collections.singletonList(buildTwoItems().get(0));
        items.get(0).setId(101L);
        req.setItems(items);

        salesContractService.updateSalesContract(req);

        // 恰有未提交的 102L 被逻辑删除
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> deleteCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(salesContractItemMapper).deleteByIds(deleteCaptor.capture());
        assertEquals(Collections.singleton(102L), new java.util.HashSet<>(deleteCaptor.getValue()));
        verify(salesContractItemMapper, never()).insert(any(SalesContractItemDO.class));
        verify(salesContractItemMapper, never()).insertBatch(any());

        // 保留的行 updateById，行号重排为 1
        ArgumentCaptor<SalesContractItemDO> updateCaptor = ArgumentCaptor.forClass(SalesContractItemDO.class);
        verify(salesContractItemMapper).updateById(updateCaptor.capture());
        assertEquals(101L, updateCaptor.getValue().getId());
        assertEquals(1, updateCaptor.getValue().getLineNo());

        // 汇总仅包含 item1
        ArgumentCaptor<SalesContractDO> contractCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(contractCaptor.capture());
        assertEquals(new BigDecimal("100.00"), contractCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("1560.00"), contractCaptor.getValue().getTotalAmount());
    }

    @Test
    void updateSalesContract_addItem_insertsNewItemAndPreservesExisting() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), new BigDecimal("10"));
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Collections.singletonList(dbItem1));

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        List<SalesContractItemSaveReqVO> items = buildTwoItems();
        items.get(0).setId(101L); // 存量行
        items.get(1).setId(null); // 新增行（无 id）
        req.setItems(items);

        salesContractService.updateSalesContract(req);

        // 零删除
        verify(salesContractItemMapper, never()).deleteByIds(any());

        // 存量行 updateById
        ArgumentCaptor<SalesContractItemDO> updateCaptor = ArgumentCaptor.forClass(SalesContractItemDO.class);
        verify(salesContractItemMapper).updateById(updateCaptor.capture());
        assertEquals(101L, updateCaptor.getValue().getId());
        assertEquals(1, updateCaptor.getValue().getLineNo());

        // 新增行 insert，回填 contractId 与重排 lineNo=2
        ArgumentCaptor<SalesContractItemDO> insertCaptor = ArgumentCaptor.forClass(SalesContractItemDO.class);
        verify(salesContractItemMapper, times(1)).insert(insertCaptor.capture());
        SalesContractItemDO inserted = insertCaptor.getValue();
        assertNull(inserted.getId());
        assertEquals(2048L, inserted.getContractId());
        assertEquals(2, inserted.getLineNo());
        assertEquals("P002", inserted.getProductCode());
        assertEquals(new BigDecimal("200.00"), inserted.getQuantity());
        assertEquals(new BigDecimal("4000.00"), inserted.getAmount());

        // 汇总为 100+200=300, 1560+4000=5560
        ArgumentCaptor<SalesContractDO> contractCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(contractCaptor.capture());
        assertEquals(new BigDecimal("300.00"), contractCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("5560.00"), contractCaptor.getValue().getTotalAmount());
    }

    @Test
    void updateSalesContract_itemNotBelongingToContract_throwsInvalidParamException() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Collections.singletonList(dbItem1));

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        SalesContractItemSaveReqVO alienItem = new SalesContractItemSaveReqVO();
        alienItem.setId(999L); // 不属于本合同的明细 ID
        alienItem.setProductCode("P999");
        req.setItems(Collections.singletonList(alienItem));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesContractService.updateSalesContract(req));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不属于当前合同"));

        verify(salesContractMapper, never()).updateById(any(SalesContractDO.class));
        verify(salesContractItemMapper, never()).updateById(any(SalesContractItemDO.class));
        verify(salesContractItemMapper, never()).insert(any(SalesContractItemDO.class));
        verify(salesContractItemMapper, never()).deleteByIds(any());
    }

    @Test
    void updateSalesContract_duplicateItemIds_throwsInvalidParamException() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Collections.singletonList(dbItem1));

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        SalesContractItemSaveReqVO row1 = new SalesContractItemSaveReqVO();
        row1.setId(101L);
        SalesContractItemSaveReqVO row2 = new SalesContractItemSaveReqVO();
        row2.setId(101L); // 重复 ID
        req.setItems(Arrays.asList(row1, row2));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesContractService.updateSalesContract(req));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    void updateSalesContract_toEmptyItems_deletesAllOldItemsAndTotalsZero() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), BigDecimal.ZERO);
        SalesContractItemDO dbItem2 = buildItemDO(102L, 2048L, 2, "P002", new BigDecimal("200"), new BigDecimal("20.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        req.setItems(Collections.emptyList());
        salesContractService.updateSalesContract(req);

        // 全部旧行逻辑删除
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> deleteCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(salesContractItemMapper).deleteByIds(deleteCaptor.capture());
        assertEquals(new java.util.HashSet<>(Arrays.asList(101L, 102L)), new java.util.HashSet<>(deleteCaptor.getValue()));
        verify(salesContractItemMapper, never()).insert(any(SalesContractItemDO.class));
        verify(salesContractItemMapper, never()).insertBatch(any());
        verify(salesContractItemMapper, never()).updateById(any(SalesContractItemDO.class));

        // 汇总归零
        ArgumentCaptor<SalesContractDO> contractCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(contractCaptor.capture());
        assertEquals(new BigDecimal("0.00"), contractCaptor.getValue().getTotalQuantity());
        assertEquals(new BigDecimal("0.00"), contractCaptor.getValue().getTotalAmount());
    }

    @Test
    void updateSalesContract_reorderItems_updatesLineNoAccordingToSubmitOrder() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO dbItem1 = buildItemDO(101L, 2048L, 1, "P001", new BigDecimal("100"), new BigDecimal("15.50"), BigDecimal.ZERO);
        SalesContractItemDO dbItem2 = buildItemDO(102L, 2048L, 2, "P002", new BigDecimal("200"), new BigDecimal("20.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(2048L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        // 提交顺序反转：102 排第一，101 排第二
        SalesContractSaveReqVO req = buildCreateReqVO();
        req.setId(2048L);
        List<SalesContractItemSaveReqVO> items = buildTwoItems();
        items.get(0).setId(101L);
        items.get(1).setId(102L);
        req.setItems(Arrays.asList(items.get(1), items.get(0)));

        salesContractService.updateSalesContract(req);

        ArgumentCaptor<SalesContractItemDO> updateCaptor = ArgumentCaptor.forClass(SalesContractItemDO.class);
        verify(salesContractItemMapper, times(2)).updateById(updateCaptor.capture());
        List<SalesContractItemDO> updatedItems = updateCaptor.getAllValues();
        assertEquals(102L, updatedItems.get(0).getId());
        assertEquals(1, updatedItems.get(0).getLineNo(), "原第2行应重排为第1行");
        assertEquals(101L, updatedItems.get(1).getId());
        assertEquals(2, updatedItems.get(1).getLineNo(), "原第1行应重排为第2行");
    }

    @Test
    void getSalesContract_withItems_returnsNestedItemsOrderedByLineNo() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setContractId(2048L);
        item1.setLineNo(1);
        item1.setProductCode("P001");
        item1.setQuantity(new BigDecimal("100"));
        SalesContractItemDO item2 = new SalesContractItemDO();
        item2.setId(2L);
        item2.setContractId(2048L);
        item2.setLineNo(2);
        item2.setProductCode("P002");
        when(salesContractItemMapper.selectListByContractId(2048L))
                .thenReturn(Arrays.asList(item1, item2));

        SalesContractRespVO respVO = salesContractService.getSalesContract(2048L);
        assertEquals(2, respVO.getItems().size());
        assertEquals(1, respVO.getItems().get(0).getLineNo());
        assertEquals("P001", respVO.getItems().get(0).getProductCode());
        assertEquals(2, respVO.getItems().get(1).getLineNo());
        assertEquals("P002", respVO.getItems().get(1).getProductCode());
    }

    @Test
    void updateSalesContract_whenApproving_throwsException() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        contract.setApprovalStatus("APPROVING");
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        SalesContractSaveReqVO reqVO = new SalesContractSaveReqVO();
        reqVO.setId(2048L);
        reqVO.setCustomerId(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.updateSalesContract(reqVO));
        assertEquals(1_030_030_004, ex.getCode());
    }

    @Test
    void deleteSalesContract_whenApproving_throwsException() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(2048L);
        contract.setApprovalStatus("APPROVING");
        when(salesContractMapper.selectById(2048L)).thenReturn(contract);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesContractService.deleteSalesContract(2048L));
        assertEquals(1_030_030_004, ex.getCode());
    }

    @Test
    void testForeignTradeFields_createAndUpdateAndGet() {
        // 1. Create with foreign trade fields
        SalesContractSaveReqVO createReqVO = new SalesContractSaveReqVO();
        createReqVO.setCustomerId(CUSTOMER_ID);
        createReqVO.setTradeType("EXPORT");
        createReqVO.setIncoterm("FOB");
        createReqVO.setPortOfLoading("SHANGHAI");
        createReqVO.setPortOfDischarge("LOS ANGELES");
        createReqVO.setPaymentTerm("TT");

        salesContractService.createSalesContract(createReqVO);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        SalesContractDO inserted = captor.getValue();
        assertEquals("EXPORT", inserted.getTradeType());
        assertEquals("FOB", inserted.getIncoterm());
        assertEquals("SHANGHAI", inserted.getPortOfLoading());
        assertEquals("LOS ANGELES", inserted.getPortOfDischarge());
        assertEquals("TT", inserted.getPaymentTerm());

        // 2. Get with foreign trade fields
        when(salesContractMapper.selectById(2048L)).thenReturn(inserted);
        SalesContractRespVO respVO = salesContractService.getSalesContract(2048L);
        assertEquals("FOB", respVO.getIncoterm());
        assertEquals("SHANGHAI", respVO.getPortOfLoading());
        assertEquals("LOS ANGELES", respVO.getPortOfDischarge());
        assertEquals("TT", respVO.getPaymentTerm());

        // 3. Update foreign trade fields
        SalesContractSaveReqVO updateReqVO = new SalesContractSaveReqVO();
        updateReqVO.setId(2048L);
        updateReqVO.setCustomerId(CUSTOMER_ID);
        updateReqVO.setTradeType("EXPORT");
        updateReqVO.setIncoterm("CIF");
        updateReqVO.setPortOfLoading("NINGBO");
        updateReqVO.setPortOfDischarge("HAMBURG");
        updateReqVO.setPaymentTerm("LC");
        salesContractService.updateSalesContract(updateReqVO);

        ArgumentCaptor<SalesContractDO> updateCaptor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(updateCaptor.capture());
        SalesContractDO updated = updateCaptor.getValue();
        assertEquals("CIF", updated.getIncoterm());
        assertEquals("NINGBO", updated.getPortOfLoading());
        assertEquals("HAMBURG", updated.getPortOfDischarge());
        assertEquals("LC", updated.getPaymentTerm());
    }

}
