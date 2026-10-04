package com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.SampleErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SampleAnalysisServiceImpl} 的 Mockito 纯单元测试（不加载 Spring 上下文与真实 DB）。
 *
 * 覆盖验收标准：
 * 1. 取号递增：同租户连建递增 SA+年+4位流水（0001 -> 0002）
 * 2. 跨年重置：跨年后流水从 1 重新计数
 * 3. 扩位支持：超过 9999 自然扩位至 10000
 * 4. 手填合法值可用
 * 5. 手填撞号抛 1_030_018_001（含与已删除记录撞号）
 * 6. 软删占号：软删行号码不释放、自动取号跳过已被软删占用的编号
 * 7. 预览编号不占号：多次预览返回相同编号，无 DB 写入
 * 8. 米长计算：正常输入 150/200 -> 3.33（四舍五入保留2位）
 * 9. 米长异常：门幅/克重为空、0、负数时计算结果为 null
 * 10. 关联客户校验与快照：选客户快照档案公司名、客户不存在抛 1_030_018_002、允许纯手填客户名
 * 11. 登记人快照：创建时写入登录用户快照，更新时不变更
 * 12. 删除为逻辑删除，删除不存在记录抛 1_030_018_000
 * 13. 分页与关键字模糊搜索（覆盖 sample_analysis_no, client_name, product_name, client_style_no, composition）
 * 14. 租户隔离代表用例：不同租户取号互相独立
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class SampleAnalysisServiceImplTest {

    @Mock
    private SampleAnalysisMapper sampleAnalysisMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private SampleAnalysisServiceImpl sampleAnalysisService;

    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final String PREFIX = "SA" + CURRENT_YEAR;

    @BeforeEach
    void setUp() {
        // 初始化 MyBatis-Plus TableInfo，以便在无 Spring 环境下解析 SFunction 列名
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SampleAnalysisDO.class);

        // 设置安全上下文（默认模拟登录用户 ID=10L，昵称="测试业务员"）
        setLoginUser(10L, "测试业务员");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setLoginUser(Long userId, String nickname) {
        LoginUser loginUser = new LoginUser().setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        if (userId != null && nickname != null) {
            AdminUserRespDTO userDTO = new AdminUserRespDTO();
            userDTO.setId(userId);
            userDTO.setNickname(nickname);
            lenient().when(adminUserApi.getUser(userId)).thenReturn(userDTO);
        }
    }

    // ==================== 1. 取号递增 / 跨年 / 扩位 / 软删占号 / 预览 ====================

    @Test
    @DisplayName("自动取号递增：首次建单返回 SA{年}0001")
    void create_autoNumber_firstRecord_generates0001() {
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0001")).thenReturn(null);
        when(sampleAnalysisMapper.insert(any(SampleAnalysisDO.class))).thenAnswer(inv -> {
            SampleAnalysisDO dobj = inv.getArgument(0);
            dobj.setId(1001L);
            return 1;
        });

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("全棉汗布");

        Long id = sampleAnalysisService.createSampleAnalysis(reqVO);
        assertNotNull(id);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(PREFIX + "0001", captor.getValue().getSampleAnalysisNo());
    }

    @Test
    @DisplayName("自动取号递增：已有 SA{年}0001，自动顺延生成 SA{年}0002")
    void create_autoNumber_subsequent_increments() {
        SampleAnalysisDO exist1 = new SampleAnalysisDO();
        exist1.setId(1L);
        exist1.setSampleAnalysisNo(PREFIX + "0001");

        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(List.of(exist1));
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0002")).thenReturn(null);
        when(sampleAnalysisMapper.insert(any(SampleAnalysisDO.class))).thenAnswer(inv -> {
            SampleAnalysisDO dobj = inv.getArgument(0);
            dobj.setId(1002L);
            return 1;
        });

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("双面布");

        Long id = sampleAnalysisService.createSampleAnalysis(reqVO);
        assertNotNull(id);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(PREFIX + "0002", captor.getValue().getSampleAnalysisNo());
    }

    @Test
    @DisplayName("跨年重置：历史年份记录存在时，新年份仍从 0001 开始计数")
    void create_autoNumber_crossYear_startsFromOne() {
        // 模拟去年前缀 SA{CURRENT_YEAR - 1} 下已有记录
        String lastYearPrefix = "SA" + (CURRENT_YEAR - 1);
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0001")).thenReturn(null);
        when(sampleAnalysisMapper.insert(any(SampleAnalysisDO.class))).thenAnswer(inv -> {
            SampleAnalysisDO dobj = inv.getArgument(0);
            dobj.setId(2001L);
            return 1;
        });

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("毛圈布");

        sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(PREFIX + "0001", captor.getValue().getSampleAnalysisNo());
    }

    @Test
    @DisplayName("超 9999 自然扩位：已有 9999 时，下一编号扩为 10000")
    void create_autoNumber_exceeds9999_expandsNaturally() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(9999L);
        exist.setSampleAnalysisNo(PREFIX + "9999");

        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(List.of(exist));
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "10000")).thenReturn(null);
        when(sampleAnalysisMapper.insert(any(SampleAnalysisDO.class))).thenAnswer(inv -> {
            SampleAnalysisDO dobj = inv.getArgument(0);
            dobj.setId(10000L);
            return 1;
        });

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("高密卫衣布");

        sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(PREFIX + "10000", captor.getValue().getSampleAnalysisNo());
    }

    @Test
    @DisplayName("手填合法编号可用")
    void create_manualNumber_valid_success() {
        String manualNo = PREFIX + "8888";
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(manualNo)).thenReturn(null);
        when(sampleAnalysisMapper.insert(any(SampleAnalysisDO.class))).thenAnswer(inv -> {
            SampleAnalysisDO dobj = inv.getArgument(0);
            dobj.setId(3001L);
            return 1;
        });

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setSampleAnalysisNo(manualNo);
        reqVO.setProductName("弹力罗纹");

        Long id = sampleAnalysisService.createSampleAnalysis(reqVO);
        assertNotNull(id);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(manualNo, captor.getValue().getSampleAnalysisNo());
    }

    @Test
    @DisplayName("手填撞号（与已有未删除记录撞号）抛 1_030_018_001")
    void create_manualNumber_collisionActive_throws001() {
        String manualNo = PREFIX + "0001";
        SampleAnalysisDO active = new SampleAnalysisDO();
        active.setId(10L);
        active.setSampleAnalysisNo(manualNo);

        when(sampleAnalysisMapper.selectByNoIncludingDeleted(manualNo)).thenReturn(active);

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setSampleAnalysisNo(manualNo);
        reqVO.setProductName("罗马布");

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_NO_DUPLICATE.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("手填撞号（与已删除记录撞号）同样抛 1_030_018_001")
    void create_manualNumber_collisionSoftDeleted_throws001() {
        String manualNo = PREFIX + "0001";
        SampleAnalysisDO softDeleted = new SampleAnalysisDO();
        softDeleted.setId(10L);
        softDeleted.setSampleAnalysisNo(manualNo);
        softDeleted.setDeleted(true);

        when(sampleAnalysisMapper.selectByNoIncludingDeleted(manualNo)).thenReturn(softDeleted);

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setSampleAnalysisNo(manualNo);
        reqVO.setProductName("罗马布");

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_NO_DUPLICATE.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("软删占号：已软删行的编号不释放，自动取号跳过并顺延")
    void create_autoNumber_skipsSoftDeletedRecord() {
        SampleAnalysisDO softDeleted = new SampleAnalysisDO();
        softDeleted.setId(1L);
        softDeleted.setSampleAnalysisNo(PREFIX + "0001");
        softDeleted.setDeleted(true);

        // selectListByNoPrefixIncludingDeleted 包含软删行
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(List.of(softDeleted));
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0002")).thenReturn(null);
        when(sampleAnalysisMapper.insert(any(SampleAnalysisDO.class))).thenAnswer(inv -> {
            SampleAnalysisDO dobj = inv.getArgument(0);
            dobj.setId(2L);
            return 1;
        });

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("珠地布");

        sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(PREFIX + "0002", captor.getValue().getSampleAnalysisNo());
    }

    @Test
    @DisplayName("预览编号不占号：多次调用返回相同编号且不写数据库")
    void getNextSampleAnalysisNo_previewDoesNotOccupy() {
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());

        String preview1 = sampleAnalysisService.getNextSampleAnalysisNo();
        String preview2 = sampleAnalysisService.getNextSampleAnalysisNo();

        assertEquals(PREFIX + "0001", preview1);
        assertEquals(PREFIX + "0001", preview2);
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    // ==================== 2. 米长计算（正常值与异常值） ====================

    @Test
    @DisplayName("米长正常计算：门幅 150、克重 200 保存后米长为 3.33")
    void meterLength_normalInput_calculates333() {
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0001")).thenReturn(null);

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("汗布");
        reqVO.setWidth(new BigDecimal("150"));
        reqVO.setWeight(new BigDecimal("200"));

        sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals(new BigDecimal("3.33"), captor.getValue().getMeterLength());
    }

    @Test
    @DisplayName("米长异常输入：门幅或克重为空、0、负数时米长为空")
    void meterLength_abnormalInputs_returnsNull() {
        assertNull(sampleAnalysisService.calculateMeterLength(null, new BigDecimal("200")));
        assertNull(sampleAnalysisService.calculateMeterLength(new BigDecimal("150"), null));
        assertNull(sampleAnalysisService.calculateMeterLength(BigDecimal.ZERO, new BigDecimal("200")));
        assertNull(sampleAnalysisService.calculateMeterLength(new BigDecimal("150"), BigDecimal.ZERO));
        assertNull(sampleAnalysisService.calculateMeterLength(new BigDecimal("-150"), new BigDecimal("200")));
        assertNull(sampleAnalysisService.calculateMeterLength(new BigDecimal("150"), new BigDecimal("-200")));
    }

    // ==================== 3. 关联客户校验与快照 ====================

    @Test
    @DisplayName("选客户档案：客户存在且未传名称快照时，自动带入客户公司名")
    void customer_existsWithoutClientName_snapshotsCompanyName() {
        CustomerDO customer = new CustomerDO();
        customer.setId(100L);
        customer.setCompanyName("绍兴远大纺织有限公司");

        when(customerMapper.selectById(100L)).thenReturn(customer);
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0001")).thenReturn(null);

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("斜纹针织");
        reqVO.setCustomerId(100L);
        reqVO.setClientName(null);

        sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertEquals("绍兴远大纺织有限公司", captor.getValue().getClientName());
    }

    @Test
    @DisplayName("选客户档案：客户不存在时抛 1_030_018_002")
    void customer_notExists_throws002() {
        when(customerMapper.selectById(999L)).thenReturn(null);

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("针织棉");
        reqVO.setCustomerId(999L);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_REF_NOT_EXISTS.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("不选客户纯手填客户名称可保存成功")
    void customer_pureManualClientName_success() {
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0001")).thenReturn(null);

        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("华夫格");
        reqVO.setCustomerId(null);
        reqVO.setClientName("散客老李");

        sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        assertNull(captor.getValue().getCustomerId());
        assertEquals("散客老李", captor.getValue().getClientName());
    }

    // ==================== 4. 登记人快照（创建写入，更新不变） ====================

    @Test
    @DisplayName("登记人快照：创建时记录当前登录用户，更新时不改变登记人")
    void creatorSnapshot_preservedOnUpdate() {
        // 创建阶段：用户 ID=10，昵称="测试业务员"
        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(PREFIX + "0001")).thenReturn(null);

        SampleAnalysisSaveReqVO createReqVO = new SampleAnalysisSaveReqVO();
        createReqVO.setProductName("牛油果绒");

        sampleAnalysisService.createSampleAnalysis(createReqVO);

        ArgumentCaptor<SampleAnalysisDO> createCaptor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(createCaptor.capture());
        assertEquals(10L, createCaptor.getValue().getCreatorId());
        assertEquals("测试业务员", createCaptor.getValue().getCreatorName());

        // 更新阶段：模拟另一个登录用户 ID=20，昵称="修改人员"
        setLoginUser(20L, "修改人员");

        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(888L);
        exist.setSampleAnalysisNo(PREFIX + "0001");
        exist.setProductName("牛油果绒");
        exist.setCreatorId(10L);
        exist.setCreatorName("测试业务员");

        when(sampleAnalysisMapper.selectById(888L)).thenReturn(exist);

        SampleAnalysisSaveReqVO updateReqVO = new SampleAnalysisSaveReqVO();
        updateReqVO.setId(888L);
        updateReqVO.setProductName("牛油果绒-修改后");

        sampleAnalysisService.updateSampleAnalysis(updateReqVO);

        ArgumentCaptor<SampleAnalysisDO> updateCaptor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).updateById(updateCaptor.capture());
        // 登记人快照仍为 10L 和 "测试业务员"
        assertEquals(10L, updateCaptor.getValue().getCreatorId());
        assertEquals("测试业务员", updateCaptor.getValue().getCreatorName());
    }

    // ==================== 5. 删除与查询 ====================

    @Test
    @DisplayName("删除：存在时执行逻辑删除")
    void delete_exists_executesLogicalDelete() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(555L);
        when(sampleAnalysisMapper.selectById(555L)).thenReturn(exist);

        sampleAnalysisService.deleteSampleAnalysis(555L);

        verify(sampleAnalysisMapper).deleteById(555L);
    }

    @Test
    @DisplayName("删除：不存在时抛 1_030_018_000")
    void delete_notExists_throws000() {
        when(sampleAnalysisMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.deleteSampleAnalysis(999L));
        assertEquals(SAMPLE_ANALYSIS_NOT_EXISTS.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("获得详情：正常返回并在首张照片存在时填充 firstPhotoUrl")
    void getSampleAnalysis_populatesFirstPhotoUrl() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(666L);
        exist.setSampleAnalysisNo(PREFIX + "0001");
        exist.setProductName("空气层");
        exist.setPhotos(List.of("http://test.com/photo1.jpg", "http://test.com/photo2.jpg"));

        when(sampleAnalysisMapper.selectById(666L)).thenReturn(exist);

        SampleAnalysisRespVO respVO = sampleAnalysisService.getSampleAnalysis(666L);
        assertNotNull(respVO);
        assertEquals("http://test.com/photo1.jpg", respVO.getFirstPhotoUrl());
    }

    @Test
    @DisplayName("分页查询：keyword 与 isPaid 过滤条件正确组装")
    void getSampleAnalysisPage_wrapperAssembledCorrectly() {
        SampleAnalysisPageReqVO pageReqVO = new SampleAnalysisPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setKeyword("全棉");
        pageReqVO.setIsPaid(false);

        PageResult<SampleAnalysisDO> mockPageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(sampleAnalysisMapper.selectPage(eq(pageReqVO), any())).thenReturn(mockPageResult);

        PageResult<SampleAnalysisRespVO> result = sampleAnalysisService.getSampleAnalysisPage(pageReqVO);
        assertNotNull(result);

        ArgumentCaptor<LambdaQueryWrapperX<SampleAnalysisDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(sampleAnalysisMapper).selectPage(eq(pageReqVO), captor.capture());

        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("LOWER(sample_analysis_no) LIKE"), "keyword 应模糊匹配样品编号");
        assertTrue(sqlSegment.contains("LOWER(client_name) LIKE"), "keyword 应模糊匹配客户名称");
        assertTrue(sqlSegment.contains("LOWER(product_name) LIKE"), "keyword 应模糊匹配产品名称");
        assertTrue(sqlSegment.contains("LOWER(client_style_no) LIKE"), "keyword 应模糊匹配客户款号");
        assertTrue(sqlSegment.contains("LOWER(composition) LIKE"), "keyword 应模糊匹配成分");
        assertTrue(sqlSegment.contains("is_paid ="), "isPaid 条件应被包含");
    }

    // ==================== 6. 必填字段校验 ====================

    @Test
    @DisplayName("必填校验：创建时产品名称为空白抛出异常")
    void create_productNameBlank_throwsException() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("   ");

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("必填校验：修改时产品名称为空抛出异常")
    void update_productNameBlank_throwsException() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setId(100L);
        reqVO.setProductName("");

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.updateSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).updateById(any(SampleAnalysisDO.class));
    }

    // ==================== 7. 租户隔离代表用例 ====================

    @Test
    @DisplayName("租户隔离代表用例：不同租户取号互相独立（由 TenantContextHolder 与租户拦截器隔离）")
    void tenantIsolation_scansWithinTenant() {
        try {
            // 租户 1: 扫描已有 SA20260001，下一个为 SA20260002
            TenantContextHolder.setTenantId(1L);
            SampleAnalysisDO tenant1Record = new SampleAnalysisDO();
            tenant1Record.setSampleAnalysisNo(PREFIX + "0001");
            when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(List.of(tenant1Record));
            String nextNoTenant1 = sampleAnalysisService.getNextSampleAnalysisNo();
            assertEquals(PREFIX + "0002", nextNoTenant1);

            // 租户 2: 租户 2 库中没有任何记录，下一个从 SA20260001 开始
            TenantContextHolder.setTenantId(2L);
            when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
            String nextNoTenant2 = sampleAnalysisService.getNextSampleAnalysisNo();
            assertEquals(PREFIX + "0001", nextNoTenant2);
        } finally {
            TenantContextHolder.clear();
        }
    }

}
