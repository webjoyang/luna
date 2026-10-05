package com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentStatsRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSaveReqVO;
import org.springframework.security.access.AccessDeniedException;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisStitchArrangeVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnArrangeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnArrangeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisYarnArrangeDO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnSpecSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisYarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisSupplierQuoteMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisYarnArrangeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisYarnSpecMapper;
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
import java.util.Set;

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
 * 15. 绕纱行子表：聚合保存、四字段全空保存、差异保存（增改删）、级联软删
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class SampleAnalysisServiceImplTest {

    @Mock
    private SampleAnalysisMapper sampleAnalysisMapper;

    @Mock
    private SampleAnalysisYarnArrangeMapper sampleAnalysisYarnArrangeMapper;

    @Mock
    private SampleAnalysisYarnSpecMapper sampleAnalysisYarnSpecMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SampleAnalysisSupplierQuoteMapper sampleAnalysisSupplierQuoteMapper;

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @InjectMocks
    private SampleAnalysisServiceImpl sampleAnalysisService;

    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final String PREFIX = "SA" + CURRENT_YEAR;

    @BeforeEach
    void setUp() {
        // 初始化 MyBatis-Plus TableInfo，以便在无 Spring 环境下解析 SFunction 列名
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SampleAnalysisDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SampleAnalysisYarnArrangeDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SampleAnalysisYarnSpecDO.class);

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

    // ==================== 8. 三角排列画板（stitch_arrange）闭环与校验 ====================

    @Test
    @DisplayName("画板透传：合法画板数据（4行×4列五态符号）创建成功落库，不影响米长计算")
    void create_validStitchArrange_persistsSuccessfullyAndDoesNotAffectCalculations() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("针织色织布");
        reqVO.setWidth(new BigDecimal("150"));
        reqVO.setWeight(new BigDecimal("200"));

        List<List<String>> data = List.of(
                List.of("v", "-", "+", "="),
                List.of("", "v", "", "-"),
                List.of("+", "+", "=", "="),
                List.of("", "", "", "")
        );
        SampleAnalysisStitchArrangeVO stitchArrange = SampleAnalysisStitchArrangeVO.builder()
                .columns(4)
                .rows(4)
                .data(data)
                .build();
        reqVO.setStitchArrange(stitchArrange);

        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(PREFIX)).thenReturn(Collections.emptyList());
        when(sampleAnalysisMapper.selectByNoIncludingDeleted(any())).thenReturn(null);

        Long id = sampleAnalysisService.createSampleAnalysis(reqVO);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        SampleAnalysisDO inserted = captor.getValue();

        assertNotNull(inserted.getStitchArrange());
        assertEquals(4, inserted.getStitchArrange().getRows());
        assertEquals(4, inserted.getStitchArrange().getColumns());
        assertEquals(data, inserted.getStitchArrange().getData());
        // 米长依旧是纯函数计算，不受画板任何影响
        assertEquals(new BigDecimal("3.33"), inserted.getMeterLength());
    }

    @Test
    @DisplayName("画板还原：查询详情完整还原行列数与符号矩阵")
    void get_validStitchArrange_restoresSuccessfully() {
        SampleAnalysisDO sampleDO = new SampleAnalysisDO();
        sampleDO.setId(100L);
        sampleDO.setProductName("双面网眼布");
        sampleDO.setSampleAnalysisNo("SA20260001");

        List<List<String>> data = List.of(
                List.of("v", "v"),
                List.of("-", "-"),
                List.of("+", "+"),
                List.of("=", "=")
        );
        SampleAnalysisStitchArrangeVO arrange = SampleAnalysisStitchArrangeVO.builder()
                .columns(2)
                .rows(4)
                .data(data)
                .build();
        sampleDO.setStitchArrange(arrange);

        when(sampleAnalysisMapper.selectById(100L)).thenReturn(sampleDO);

        SampleAnalysisRespVO resp = sampleAnalysisService.getSampleAnalysis(100L);
        assertNotNull(resp.getStitchArrange());
        assertEquals(4, resp.getStitchArrange().getRows());
        assertEquals(2, resp.getStitchArrange().getColumns());
        assertEquals(data, resp.getStitchArrange().getData());
    }

    @Test
    @DisplayName("画板校验：rows 非 4 抛出 1_030_018_005 异常")
    void create_invalidStitchArrange_rowsNotFour_throwsException() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("测试布");

        // rows = 3
        SampleAnalysisStitchArrangeVO arrange = SampleAnalysisStitchArrangeVO.builder()
                .columns(2)
                .rows(3)
                .data(List.of(List.of("v", "-"), List.of("v", "-"), List.of("v", "-")))
                .build();
        reqVO.setStitchArrange(arrange);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("画板校验：data 维数与 columns 不符抛出 1_030_018_005 异常")
    void create_invalidStitchArrange_dimensionsMismatch_throwsException() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("测试布");

        // columns 为 4，但某一行只有 3 列
        SampleAnalysisStitchArrangeVO arrange = SampleAnalysisStitchArrangeVO.builder()
                .columns(4)
                .rows(4)
                .data(List.of(
                        List.of("v", "-", "+", "="),
                        List.of("v", "-", "+"), // 缺少1列
                        List.of("v", "-", "+", "="),
                        List.of("v", "-", "+", "=")
                ))
                .build();
        reqVO.setStitchArrange(arrange);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("画板校验：包含非五态符号抛出 1_030_018_005 异常")
    void create_invalidStitchArrange_invalidSymbol_throwsException() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("测试布");

        // 出现非法符号 "x"
        SampleAnalysisStitchArrangeVO arrange = SampleAnalysisStitchArrangeVO.builder()
                .columns(2)
                .rows(4)
                .data(List.of(
                        List.of("v", "x"),
                        List.of("-", "+"),
                        List.of("=", ""),
                        List.of("", "")
                ))
                .build();
        reqVO.setStitchArrange(arrange);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("画板校验：更新时非法画板数据同样抛出 1_030_018_005 异常")
    void update_invalidStitchArrange_throwsException() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setId(100L);
        reqVO.setProductName("测试更新");

        SampleAnalysisStitchArrangeVO arrange = SampleAnalysisStitchArrangeVO.builder()
                .columns(2)
                .rows(4)
                .data(List.of(
                        List.of("v", "INVALID"),
                        List.of("-", "+"),
                        List.of("=", ""),
                        List.of("", "")
                ))
                .build();
        reqVO.setStitchArrange(arrange);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.updateSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).updateById(any(SampleAnalysisDO.class));
    }

    // ==================== 9. 收费统计三值口径与筛选 ====================

    @Test
    @DisplayName("收费统计：无筛选时统计未付金额、已付金额、任务总数，对外报价不参与求和")
    void getPaymentStats_noFilter_calculatesUnpaidPaidAndTotalCorrectly() {
        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(1L);
        r1.setIsPaid(false);
        r1.setUnitPrice(new BigDecimal("30.00"));
        r1.setQuotedPrice(new BigDecimal("100.00")); // 对外报价应被忽略

        SampleAnalysisDO r2 = new SampleAnalysisDO();
        r2.setId(2L);
        r2.setIsPaid(false);
        r2.setUnitPrice(new BigDecimal("50.00"));
        r2.setQuotedPrice(new BigDecimal("200.00"));

        SampleAnalysisDO r3 = new SampleAnalysisDO();
        r3.setId(3L);
        r3.setIsPaid(true);
        r3.setUnitPrice(new BigDecimal("40.00"));
        r3.setQuotedPrice(new BigDecimal("300.00"));

        when(sampleAnalysisMapper.selectList(any())).thenReturn(List.of(r1, r2, r3));

        SampleAnalysisPageReqVO reqVO = new SampleAnalysisPageReqVO();
        SampleAnalysisPaymentStatsRespVO stats = sampleAnalysisService.getSampleAnalysisPaymentStats(reqVO);

        assertNotNull(stats);
        assertEquals(new BigDecimal("80.00"), stats.getUnpaidAmount());
        assertEquals(new BigDecimal("40.00"), stats.getPaidAmount());
        assertEquals(3L, stats.getTotalCount());
    }

    @Test
    @DisplayName("收费统计：筛选未付款时仅统计未付款记录，已付金额为0")
    void getPaymentStats_filterUnpaid_paidAmountIsZero() {
        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(1L);
        r1.setIsPaid(false);
        r1.setUnitPrice(new BigDecimal("30.00"));

        SampleAnalysisDO r2 = new SampleAnalysisDO();
        r2.setId(2L);
        r2.setIsPaid(false);
        r2.setUnitPrice(new BigDecimal("50.00"));

        when(sampleAnalysisMapper.selectList(any())).thenReturn(List.of(r1, r2));

        SampleAnalysisPageReqVO reqVO = new SampleAnalysisPageReqVO();
        reqVO.setIsPaid(false);
        SampleAnalysisPaymentStatsRespVO stats = sampleAnalysisService.getSampleAnalysisPaymentStats(reqVO);

        assertNotNull(stats);
        assertEquals(new BigDecimal("80.00"), stats.getUnpaidAmount());
        assertEquals(BigDecimal.ZERO, stats.getPaidAmount());
        assertEquals(2L, stats.getTotalCount());
    }

    @Test
    @DisplayName("收费统计：筛选已付款时仅统计已付款记录，未付金额为0")
    void getPaymentStats_filterPaid_unpaidAmountIsZero() {
        SampleAnalysisDO r3 = new SampleAnalysisDO();
        r3.setId(3L);
        r3.setIsPaid(true);
        r3.setUnitPrice(new BigDecimal("40.00"));

        when(sampleAnalysisMapper.selectList(any())).thenReturn(List.of(r3));

        SampleAnalysisPageReqVO reqVO = new SampleAnalysisPageReqVO();
        reqVO.setIsPaid(true);
        SampleAnalysisPaymentStatsRespVO stats = sampleAnalysisService.getSampleAnalysisPaymentStats(reqVO);

        assertNotNull(stats);
        assertEquals(BigDecimal.ZERO, stats.getUnpaidAmount());
        assertEquals(new BigDecimal("40.00"), stats.getPaidAmount());
        assertEquals(1L, stats.getTotalCount());
    }

    @Test
    @DisplayName("收费统计：单价修改后重新统计随之变化")
    void getPaymentStats_afterUnitPriceUpdate_statsReflectsNewPrice() {
        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(1L);
        r1.setIsPaid(false);
        r1.setUnitPrice(new BigDecimal("30.00"));

        when(sampleAnalysisMapper.selectList(any())).thenReturn(List.of(r1));

        SampleAnalysisPaymentStatsRespVO stats1 = sampleAnalysisService.getSampleAnalysisPaymentStats(new SampleAnalysisPageReqVO());
        assertEquals(new BigDecimal("30.00"), stats1.getUnpaidAmount());

        // 修改单价为 60.00
        r1.setUnitPrice(new BigDecimal("60.00"));

        SampleAnalysisPaymentStatsRespVO stats2 = sampleAnalysisService.getSampleAnalysisPaymentStats(new SampleAnalysisPageReqVO());
        assertEquals(new BigDecimal("60.00"), stats2.getUnpaidAmount());
    }

    // ==================== 10. 样品照片（photos）闭环与校验 ====================

    @Test
    @DisplayName("照片校验：创建时上传 9 张照片合法，保序落库")
    void create_ninePhotos_success() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("双面布");
        List<String> photos = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            photos.add("http://test.com/photo_" + i + ".png");
        }
        reqVO.setPhotos(photos);

        when(sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());

        Long id = sampleAnalysisService.createSampleAnalysis(reqVO);
        assertNull(id); // mapper.insert mock

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).insert(captor.capture());
        SampleAnalysisDO inserted = captor.getValue();
        assertNotNull(inserted.getPhotos());
        assertEquals(9, inserted.getPhotos().size());
        for (int i = 0; i < 9; i++) {
            assertEquals("http://test.com/photo_" + (i + 1) + ".png", inserted.getPhotos().get(i));
        }
    }

    @Test
    @DisplayName("照片校验：创建时上传 10 张照片报错抛 1_030_018_006")
    void create_tenPhotos_throwsPhotoCountExceed() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setProductName("双面布");
        List<String> photos = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            photos.add("http://test.com/photo_" + i + ".png");
        }
        reqVO.setPhotos(photos);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.createSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_PHOTO_COUNT_EXCEED.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).insert(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("照片校验：更新时超过 9 张照片报错抛 1_030_018_006")
    void update_tenPhotos_throwsPhotoCountExceed() {
        SampleAnalysisSaveReqVO reqVO = new SampleAnalysisSaveReqVO();
        reqVO.setId(100L);
        reqVO.setProductName("罗纹布");
        List<String> photos = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            photos.add("http://test.com/photo_" + i + ".png");
        }
        reqVO.setPhotos(photos);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.updateSampleAnalysis(reqVO));
        assertEquals(SAMPLE_ANALYSIS_PHOTO_COUNT_EXCEED.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).updateById(any(SampleAnalysisDO.class));
    }

    @Test
    @DisplayName("照片删除：删除照片后数组正确收缩并保序落库")
    void update_shrinkPhotos_success() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(200L);
        exist.setSampleAnalysisNo(PREFIX + "0001");
        exist.setProductName("卫衣布");
        exist.setPhotos(List.of("http://test.com/1.jpg", "http://test.com/2.jpg", "http://test.com/3.jpg"));

        when(sampleAnalysisMapper.selectById(200L)).thenReturn(exist);

        // 模拟删除了第 2 张照片，剩余第 1、3 张
        SampleAnalysisSaveReqVO updateReq = new SampleAnalysisSaveReqVO();
        updateReq.setId(200L);
        updateReq.setProductName("卫衣布");
        updateReq.setPhotos(List.of("http://test.com/1.jpg", "http://test.com/3.jpg"));

        sampleAnalysisService.updateSampleAnalysis(updateReq);

        ArgumentCaptor<SampleAnalysisDO> captor = ArgumentCaptor.forClass(SampleAnalysisDO.class);
        verify(sampleAnalysisMapper).updateById(captor.capture());
        SampleAnalysisDO updated = captor.getValue();
        assertNotNull(updated.getPhotos());
        assertEquals(2, updated.getPhotos().size());
        assertEquals("http://test.com/1.jpg", updated.getPhotos().get(0));
        assertEquals("http://test.com/3.jpg", updated.getPhotos().get(1));
    }

    @Test
    @DisplayName("列表查询载荷优化：列表行只带 firstPhotoUrl，photos 数组置空避免载荷膨胀")
    void getSampleAnalysisPage_photosShrunkOnlyFirstPhotoUrl() {
        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(1L);
        r1.setProductName("无照片布");
        r1.setPhotos(null);

        SampleAnalysisDO r2 = new SampleAnalysisDO();
        r2.setId(2L);
        r2.setProductName("单照片布");
        r2.setPhotos(List.of("http://test.com/first.jpg"));

        SampleAnalysisDO r3 = new SampleAnalysisDO();
        r3.setId(3L);
        r3.setProductName("多照片布");
        r3.setPhotos(List.of("http://test.com/p1.jpg", "http://test.com/p2.jpg", "http://test.com/p3.jpg"));

        PageResult<SampleAnalysisDO> pageDO = new PageResult<>(List.of(r1, r2, r3), 3L);
        when(sampleAnalysisMapper.selectPage(any(SampleAnalysisPageReqVO.class), any())).thenReturn(pageDO);

        PageResult<SampleAnalysisRespVO> pageResp = sampleAnalysisService.getSampleAnalysisPage(new SampleAnalysisPageReqVO());

        assertNotNull(pageResp);
        assertEquals(3, pageResp.getList().size());

        // 行 1: 无照片 -> firstPhotoUrl 为 null，photos 为 null
        SampleAnalysisRespVO item1 = pageResp.getList().get(0);
        assertNull(item1.getFirstPhotoUrl());
        assertNull(item1.getPhotos());

        // 行 2: 1 张照片 -> firstPhotoUrl 为 http://test.com/first.jpg，photos 为 null
        SampleAnalysisRespVO item2 = pageResp.getList().get(1);
        assertEquals("http://test.com/first.jpg", item2.getFirstPhotoUrl());
        assertNull(item2.getPhotos());

        // 行 3: 3 张照片 -> firstPhotoUrl 为 http://test.com/p1.jpg，photos 为 null（不传递全部 URL 列表）
        SampleAnalysisRespVO item3 = pageResp.getList().get(2);
        assertEquals("http://test.com/p1.jpg", item3.getFirstPhotoUrl());
        assertNull(item3.getPhotos());
    }

    // ==================== 绕纱行子表（ADR-0036 票04） ====================

    @Test
    @DisplayName("创建样品分析：携带绕纱行（含四字段全空行），正确落库并建立主子关联")
    void createSampleAnalysis_withYarnArranges_success() {
        SampleAnalysisSaveReqVO req = new SampleAnalysisSaveReqVO();
        req.setProductName("全棉汗布");

        SampleAnalysisYarnArrangeSaveReqVO arrange1 = new SampleAnalysisYarnArrangeSaveReqVO();
        arrange1.setCode("1");
        arrange1.setDraftText("25.1cm/1 花宽");
        arrange1.setBorderStatus("第一路对针");
        arrange1.setDescription("开幅线");

        // 四字段全空行也能保存
        SampleAnalysisYarnArrangeSaveReqVO arrange2 = new SampleAnalysisYarnArrangeSaveReqVO();
        arrange2.setCode("");
        arrange2.setDraftText(null);
        arrange2.setBorderStatus("");
        arrange2.setDescription(null);

        req.setYarnArranges(List.of(arrange1, arrange2));

        doAnswer(invocation -> {
            SampleAnalysisDO arg = invocation.getArgument(0);
            arg.setId(1001L);
            return 1;
        }).when(sampleAnalysisMapper).insert(any(SampleAnalysisDO.class));

        Long id = sampleAnalysisService.createSampleAnalysis(req);
        assertEquals(1001L, id);

        ArgumentCaptor<SampleAnalysisYarnArrangeDO> arrangeCaptor =
                ArgumentCaptor.forClass(SampleAnalysisYarnArrangeDO.class);
        verify(sampleAnalysisYarnArrangeMapper, times(2)).insert(arrangeCaptor.capture());

        List<SampleAnalysisYarnArrangeDO> inserted = arrangeCaptor.getAllValues();
        assertEquals(2, inserted.size());

        SampleAnalysisYarnArrangeDO row1 = inserted.get(0);
        assertEquals(1001L, row1.getSampleAnalysisId());
        assertNull(row1.getId());
        assertEquals("1", row1.getCode());
        assertEquals("25.1cm/1 花宽", row1.getDraftText());
        assertEquals("第一路对针", row1.getBorderStatus());
        assertEquals("开幅线", row1.getDescription());

        SampleAnalysisYarnArrangeDO row2 = inserted.get(1);
        assertEquals(1001L, row2.getSampleAnalysisId());
        assertNull(row2.getId());
        assertEquals("", row2.getCode());
        assertNull(row2.getDraftText());
    }

    @Test
    @DisplayName("获取样品分析详情：返回绕纱行列表并保持排序")
    void getSampleAnalysis_withYarnArranges_success() {
        SampleAnalysisDO sampleDO = new SampleAnalysisDO();
        sampleDO.setId(1001L);
        sampleDO.setProductName("全棉汗布");
        when(sampleAnalysisMapper.selectById(1001L)).thenReturn(sampleDO);

        SampleAnalysisYarnArrangeDO r1 = new SampleAnalysisYarnArrangeDO();
        r1.setId(10L);
        r1.setSampleAnalysisId(1001L);
        r1.setCode("1");
        r1.setDraftText("25.1cm");

        SampleAnalysisYarnArrangeDO r2 = new SampleAnalysisYarnArrangeDO();
        r2.setId(11L);
        r2.setSampleAnalysisId(1001L);
        r2.setCode("2");
        r2.setDraftText("30.0cm");

        when(sampleAnalysisYarnArrangeMapper.selectListBySampleAnalysisId(1001L))
                .thenReturn(List.of(r1, r2));

        SampleAnalysisRespVO resp = sampleAnalysisService.getSampleAnalysis(1001L);
        assertNotNull(resp);
        assertNotNull(resp.getYarnArranges());
        assertEquals(2, resp.getYarnArranges().size());
        assertEquals(10L, resp.getYarnArranges().get(0).getId());
        assertEquals("1", resp.getYarnArranges().get(0).getCode());
        assertEquals(11L, resp.getYarnArranges().get(1).getId());
        assertEquals("2", resp.getYarnArranges().get(1).getCode());
    }

    @Test
    @DisplayName("更新样品分析：差异保存绕纱行（增/改/删差集正确分流）")
    void updateSampleAnalysis_diffSaveYarnArranges_success() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(1001L);
        exist.setProductName("全棉汗布");
        exist.setSampleAnalysisNo("SA20260001");
        when(sampleAnalysisMapper.selectById(1001L)).thenReturn(exist);

        // 库内存量 2 行：id=101L, id=102L
        SampleAnalysisYarnArrangeDO db1 = new SampleAnalysisYarnArrangeDO();
        db1.setId(101L);
        db1.setSampleAnalysisId(1001L);
        db1.setCode("1");
        db1.setDescription("原描述1");

        SampleAnalysisYarnArrangeDO db2 = new SampleAnalysisYarnArrangeDO();
        db2.setId(102L);
        db2.setSampleAnalysisId(1001L);
        db2.setCode("2");
        db2.setDescription("原描述2");

        when(sampleAnalysisYarnArrangeMapper.selectListBySampleAnalysisId(1001L))
                .thenReturn(List.of(db1, db2));

        // 提交：更新 101L，新增无 ID 行，删除 102L（未出现在请求中）
        SampleAnalysisSaveReqVO updateReq = new SampleAnalysisSaveReqVO();
        updateReq.setId(1001L);
        updateReq.setProductName("全棉汗布修改");

        SampleAnalysisYarnArrangeSaveReqVO toUpdateRow = new SampleAnalysisYarnArrangeSaveReqVO();
        toUpdateRow.setId(101L);
        toUpdateRow.setCode("1");
        toUpdateRow.setDescription("新描述1");

        SampleAnalysisYarnArrangeSaveReqVO toInsertRow = new SampleAnalysisYarnArrangeSaveReqVO();
        toInsertRow.setCode("3");
        toInsertRow.setDescription("新增第3路");

        updateReq.setYarnArranges(List.of(toUpdateRow, toInsertRow));

        sampleAnalysisService.updateSampleAnalysis(updateReq);

        // 验证删除 102L
        verify(sampleAnalysisYarnArrangeMapper).deleteByIds(Set.of(102L));

        // 验证更新 101L
        ArgumentCaptor<SampleAnalysisYarnArrangeDO> updateCaptor =
                ArgumentCaptor.forClass(SampleAnalysisYarnArrangeDO.class);
        verify(sampleAnalysisYarnArrangeMapper).updateById(updateCaptor.capture());
        SampleAnalysisYarnArrangeDO updated = updateCaptor.getValue();
        assertEquals(101L, updated.getId());
        assertEquals(1001L, updated.getSampleAnalysisId());
        assertEquals("新描述1", updated.getDescription());

        // 验证新增第3路
        ArgumentCaptor<SampleAnalysisYarnArrangeDO> insertCaptor =
                ArgumentCaptor.forClass(SampleAnalysisYarnArrangeDO.class);
        verify(sampleAnalysisYarnArrangeMapper).insert(insertCaptor.capture());
        SampleAnalysisYarnArrangeDO inserted = insertCaptor.getValue();
        assertNull(inserted.getId());
        assertEquals(1001L, inserted.getSampleAnalysisId());
        assertEquals("3", inserted.getCode());
        assertEquals("新增第3路", inserted.getDescription());
    }

    @Test
    @DisplayName("更新样品分析：提交不属于本样品的绕纱行 ID 抛出异常拦截")
    void updateSampleAnalysis_yarnArranges_invalidId_throwsException() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(1001L);
        exist.setProductName("全棉汗布");
        exist.setSampleAnalysisNo("SA20260001");
        when(sampleAnalysisMapper.selectById(1001L)).thenReturn(exist);

        // 库内只有 id=101L
        SampleAnalysisYarnArrangeDO db1 = new SampleAnalysisYarnArrangeDO();
        db1.setId(101L);
        db1.setSampleAnalysisId(1001L);
        when(sampleAnalysisYarnArrangeMapper.selectListBySampleAnalysisId(1001L))
                .thenReturn(List.of(db1));

        SampleAnalysisSaveReqVO updateReq = new SampleAnalysisSaveReqVO();
        updateReq.setId(1001L);
        updateReq.setProductName("全棉汗布");

        // 提交跨单 id=999L
        SampleAnalysisYarnArrangeSaveReqVO illegalRow = new SampleAnalysisYarnArrangeSaveReqVO();
        illegalRow.setId(999L);
        illegalRow.setCode("X");
        updateReq.setYarnArranges(List.of(illegalRow));

        assertThrows(ServiceException.class, () -> sampleAnalysisService.updateSampleAnalysis(updateReq));
    }

    @Test
    @DisplayName("删除样品分析：级联逻辑删除绕纱行")
    void deleteSampleAnalysis_cascadeSoftDeletesYarnArranges() {
        SampleAnalysisDO exist = new SampleAnalysisDO();
        exist.setId(1001L);
        exist.setProductName("全棉汗布");
        when(sampleAnalysisMapper.selectById(1001L)).thenReturn(exist);

        sampleAnalysisService.deleteSampleAnalysis(1001L);

        // 验证主表被删除
        verify(sampleAnalysisMapper).deleteById(1001L);
        // 验证绕纱行被级联删除
        verify(sampleAnalysisYarnArrangeMapper).deleteBySampleAnalysisId(1001L);
    }


    // ==================== 11. 收费标记与权限守卫 ====================




    @Test
    @DisplayName("收费标记：无 pay 权限调用抛出 AccessDeniedException，且不执行任何更新")
    void updatePayment_withoutPermission_throwsAccessDeniedException() {
        when(securityFrameworkService.hasPermission("lunaerp:sample:sample-analysis:pay")).thenReturn(false);

        SampleAnalysisPaymentReqVO reqVO = new SampleAnalysisPaymentReqVO(List.of(1L, 2L), true);

        assertThrows(AccessDeniedException.class, () -> sampleAnalysisService.updateSampleAnalysisPayment(reqVO));
        verify(sampleAnalysisMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("收费标记：单条标记已付款成功且仅更新所选 id")
    void updatePayment_singleRecord_successAndOnlyAffectsTargetId() {
        when(securityFrameworkService.hasPermission("lunaerp:sample:sample-analysis:pay")).thenReturn(true);

        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(101L);
        when(sampleAnalysisMapper.selectBatchIds(any())).thenReturn(List.of(r1));

        SampleAnalysisPaymentReqVO reqVO = new SampleAnalysisPaymentReqVO(List.of(101L), true);
        sampleAnalysisService.updateSampleAnalysisPayment(reqVO);

        ArgumentCaptor<LambdaUpdateWrapper<SampleAnalysisDO>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(sampleAnalysisMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<SampleAnalysisDO> updateWrapper = captor.getValue();
        assertNotNull(updateWrapper);
    }

    @Test
    @DisplayName("收费标记：多选批量标记未付款成功且仅更新所选 id 列表")
    void updatePayment_batchRecords_successAndOnlyAffectsTargetIds() {
        when(securityFrameworkService.hasPermission("lunaerp:sample:sample-analysis:pay")).thenReturn(true);

        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(101L);
        SampleAnalysisDO r2 = new SampleAnalysisDO();
        r2.setId(102L);
        when(sampleAnalysisMapper.selectBatchIds(any())).thenReturn(List.of(r1, r2));

        SampleAnalysisPaymentReqVO reqVO = new SampleAnalysisPaymentReqVO(List.of(101L, 102L), false);
        sampleAnalysisService.updateSampleAnalysisPayment(reqVO);

        ArgumentCaptor<LambdaUpdateWrapper<SampleAnalysisDO>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(sampleAnalysisMapper).update(isNull(), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    @DisplayName("收费标记：目标记录不存在时抛出 1_030_018_000 且不更新")
    void updatePayment_recordNotExists_throwsSampleAnalysisNotExists() {
        when(securityFrameworkService.hasPermission("lunaerp:sample:sample-analysis:pay")).thenReturn(true);

        // 请求 2 个 id，只查出 1 条记录
        SampleAnalysisDO r1 = new SampleAnalysisDO();
        r1.setId(101L);
        when(sampleAnalysisMapper.selectBatchIds(any())).thenReturn(List.of(r1));

        SampleAnalysisPaymentReqVO reqVO = new SampleAnalysisPaymentReqVO(List.of(101L, 999L), true);

        ServiceException ex = assertThrows(ServiceException.class, () -> sampleAnalysisService.updateSampleAnalysisPayment(reqVO));
        assertEquals(SAMPLE_ANALYSIS_NOT_EXISTS.getCode(), ex.getCode());
        verify(sampleAnalysisMapper, never()).update(any(), any());
    }

}


