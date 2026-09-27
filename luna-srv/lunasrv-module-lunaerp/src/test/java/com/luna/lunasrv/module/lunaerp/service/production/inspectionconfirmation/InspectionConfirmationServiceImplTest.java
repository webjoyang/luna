package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationManualCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRevokeReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRevokeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPayrollSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPrintViewRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationConstants;
import com.luna.lunasrv.module.system.api.dept.PostApi;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link InspectionConfirmationServiceImpl} 单元测试（Mockito 单测）
 *
 * 覆盖行为：
 * 1. 分页查询 status 缺省等价于 PENDING+CONFIRMED（有效行）
 * 2. 分页查询显式 REVOKED 可查冻结行
 * 3. 分页查询各筛选参数（keyword、source、responsibility、workerUserId、inspectionOrderId、createTime）正确透传
 * 4. 空白 keyword 不追加 LIKE 条件
 * 5. 默认按 create_time DESC 排序
 * 6. worker-options 岗位存在并返回用户列表
 * 7. worker-options 岗位不存在返回空列表不报错
 * 8. worker-options 岗位无用户返回空列表不报错
 * 9. 手工登记：缺必填三分支（rollNo/grade/responsibility 缺失）报错 1_030_014_006
 * 10. 手工登记：非法 responsibility 报错 1_030_014_004
 * 11. 手工登记：OPERATOR 缺 workerUserId 报错 1_030_014_005
 * 12. 手工登记：关联检验单不存在报错 1_030_014_002
 * 13. 手工登记：成功落库 CONFIRMED/MANUAL/新 chain_key/认定人/挡车工快照
 * 14. 手工登记：补录认定时间采用
 * 15. 手工登记：EQUIPMENT/WAIVED 挡车工可空
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class InspectionConfirmationServiceImplTest {

    @Mock
    private InspectionConfirmationMapper inspectionConfirmationMapper;

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;

    @Mock
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Mock
    private PostApi postApi;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private InspectionConfirmationServiceImpl inspectionConfirmationService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionConfirmationDO.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== getInspectionConfirmationPage ====================

    @Test
    @DisplayName("分页查询：未传 status 时，缺省过滤 PENDING 与 CONFIRMED 两态（只查有效行）")
    void getInspectionConfirmationPage_defaultStatus_queriesPendingAndConfirmed() {
        // 准备参数
        InspectionConfirmationPageReqVO reqVO = new InspectionConfirmationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setStatus(null);

        when(inspectionConfirmationMapper.selectPage(any(InspectionConfirmationPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        // 执行
        inspectionConfirmationService.getInspectionConfirmationPage(reqVO);

        // 验证 wrapper
        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("status IN"), "未传 status 时应为 status IN 查询");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("PENDING"), "应包含 PENDING 态");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("CONFIRMED"), "应包含 CONFIRMED 态");
        assertFalse(wrapper.getParamNameValuePairs().containsValue("REVOKED"), "缺省不应包含 REVOKED 态");
        assertTrue(sqlSegment.contains("ORDER BY create_time DESC"), "默认应按创建时间倒序");
    }

    @Test
    @DisplayName("分页查询：显式传入 REVOKED 时，仅查已撤销冻结行")
    void getInspectionConfirmationPage_explicitRevoked_queriesRevokedOnly() {
        InspectionConfirmationPageReqVO reqVO = new InspectionConfirmationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setStatus("REVOKED");

        when(inspectionConfirmationMapper.selectPage(any(InspectionConfirmationPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        inspectionConfirmationService.getInspectionConfirmationPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("status ="), "显式传 status 应为等值查询");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("REVOKED"), "应仅查询 REVOKED");
        assertFalse(wrapper.getParamNameValuePairs().containsValue("PENDING"), "不应包含 PENDING");
    }

    @Test
    @DisplayName("分页查询：全部筛选参数（keyword、source、responsibility、workerUserId、inspectionOrderId、createTime）正确透传")
    void getInspectionConfirmationPage_allFiltersPassedThrough_andOrderedByCreateTimeDesc() {
        InspectionConfirmationPageReqVO reqVO = new InspectionConfirmationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setKeyword("P0309");
        reqVO.setStatus("CONFIRMED");
        reqVO.setSource("INSPECTION");
        reqVO.setResponsibility("OPERATOR");
        reqVO.setWorkerUserId(1024L);
        reqVO.setInspectionOrderId(2048L);
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 27, 23, 59, 59);
        reqVO.setCreateTime(new LocalDateTime[]{start, end});

        when(inspectionConfirmationMapper.selectPage(any(InspectionConfirmationPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        inspectionConfirmationService.getInspectionConfirmationPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();

        String sqlSegment = wrapper.getSqlSegment();
        // keyword 模糊匹配 4 列
        assertTrue(sqlSegment.contains("LOWER(roll_no) LIKE"), "keyword 应透传到 roll_no 模糊");
        assertTrue(sqlSegment.contains("LOWER(vat_no) LIKE"), "keyword 应透传到 vat_no 模糊");
        assertTrue(sqlSegment.contains("LOWER(product_name) LIKE"), "keyword 应透传到 product_name 模糊");
        assertTrue(sqlSegment.contains("LOWER(inspection_order_code) LIKE"), "keyword 应透传到 inspection_order_code 模糊");
        // 等值过滤
        assertTrue(sqlSegment.contains("source ="), "source 应透传等值过滤");
        assertTrue(sqlSegment.contains("responsibility ="), "responsibility 应透传等值过滤");
        assertTrue(sqlSegment.contains("worker_user_id ="), "workerUserId 应透传等值过滤");
        assertTrue(sqlSegment.contains("inspection_order_id ="), "inspectionOrderId 应透传等值过滤");
        // 创建时间区间
        assertTrue(sqlSegment.contains("create_time BETWEEN") || (sqlSegment.contains("create_time >=") && sqlSegment.contains("create_time <=")),
                "createTime 区间应透传");
        // 默认创建时间倒序
        assertTrue(sqlSegment.contains("ORDER BY create_time DESC"), "默认应按创建时间倒序");
        // 参数值透传
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%P0309%"), "keyword 参数值应预编译透传");
    }

    @Test
    @DisplayName("分页查询：空白 keyword 不添加 LIKE 条件")
    void getInspectionConfirmationPage_blankKeyword_doesNotAddLikeCondition() {
        InspectionConfirmationPageReqVO reqVO = new InspectionConfirmationPageReqVO();
        reqVO.setKeyword("   ");
        when(inspectionConfirmationMapper.selectPage(any(InspectionConfirmationPageReqVO.class), any(Wrapper.class)))
                .thenReturn(PageResult.empty());

        inspectionConfirmationService.getInspectionConfirmationPage(reqVO);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectPage(eq(reqVO), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertFalse(sqlSegment.contains("LIKE"), "空白 keyword 不应产生 LIKE 条件");
        assertTrue(sqlSegment.contains("ORDER BY create_time DESC"), "仍应按创建时间倒序");
    }

    @Test
    @DisplayName("分页查询：正确映射 DO 字段到 InspectionConfirmationRespVO")
    void getInspectionConfirmationPage_returnsMappedRespVOList() {
        InspectionConfirmationPageReqVO reqVO = new InspectionConfirmationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        InspectionConfirmationDO record = new InspectionConfirmationDO();
        record.setId(1024L);
        record.setChainKey("1024");
        record.setSource("INSPECTION");
        record.setStatus("CONFIRMED");
        record.setInspectionOrderId(2048L);
        record.setInspectionOrderCode("INS-20260925-001");
        record.setInspectionOrderItemId(4096L);
        record.setRollNo("P0309");
        record.setVatNo("V12");
        record.setGrade("B");
        record.setMachineNo("M01");
        record.setProductCode("P20260901");
        record.setProductName("全棉汗布");
        record.setResponsibility("OPERATOR");
        record.setWorkerUserId(101L);
        record.setWorkerName("李桂香");
        record.setConfirmedByUserId(1L);
        record.setConfirmedByName("张领班");
        record.setConfirmedAt(LocalDateTime.of(2026, 9, 27, 14, 0, 0));

        PageResult<InspectionConfirmationDO> pageResult = new PageResult<>(Collections.singletonList(record), 1L);
        when(inspectionConfirmationMapper.selectPage(any(InspectionConfirmationPageReqVO.class), any(Wrapper.class)))
                .thenReturn(pageResult);

        PageResult<InspectionConfirmationRespVO> result = inspectionConfirmationService.getInspectionConfirmationPage(reqVO);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        InspectionConfirmationRespVO respVO = result.getList().get(0);
        assertEquals(1024L, respVO.getId());
        assertEquals("1024", respVO.getChainKey());
        assertEquals("INSPECTION", respVO.getSource());
        assertEquals("CONFIRMED", respVO.getStatus());
        assertEquals("INS-20260925-001", respVO.getInspectionOrderCode());
        assertEquals("P0309", respVO.getRollNo());
        assertEquals("V12", respVO.getVatNo());
        assertEquals("B", respVO.getGrade());
        assertEquals("M01", respVO.getMachineNo());
        assertEquals("全棉汗布", respVO.getProductName());
        assertEquals("OPERATOR", respVO.getResponsibility());
        assertEquals(101L, respVO.getWorkerUserId());
        assertEquals("李桂香", respVO.getWorkerName());
        assertEquals("张领班", respVO.getConfirmedByName());
    }

    // ==================== getWorkerOptions ====================

    @Test
    @DisplayName("挡车工候选：岗位存在且有关联用户，成功返回用户候选列表")
    void getWorkerOptions_postFound_returnsWorkerList() {
        PostRespDTO post = new PostRespDTO();
        post.setId(20L);
        post.setCode(InspectionConfirmationConstants.POST_CODE_LUNA_WORKER);
        when(postApi.getPostByCode(InspectionConfirmationConstants.POST_CODE_LUNA_WORKER)).thenReturn(post);

        AdminUserRespDTO user1 = new AdminUserRespDTO();
        user1.setId(101L);
        user1.setNickname("李桂香");

        AdminUserRespDTO user2 = new AdminUserRespDTO();
        user2.setId(102L);
        user2.setNickname("王建国");

        when(adminUserApi.getUserListByPostIds(Collections.singleton(20L)))
                .thenReturn(Arrays.asList(user1, user2));

        List<WorkerOptionRespVO> result = inspectionConfirmationService.getWorkerOptions();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(101L, result.get(0).getId());
        assertEquals("李桂香", result.get(0).getNickname());
        assertEquals(102L, result.get(1).getId());
        assertEquals("王建国", result.get(1).getNickname());
    }

    @Test
    @DisplayName("挡车工候选：岗位不存在，返回空列表且不报错")
    void getWorkerOptions_postNotFound_returnsEmptyList() {
        when(postApi.getPostByCode(InspectionConfirmationConstants.POST_CODE_LUNA_WORKER)).thenReturn(null);

        List<WorkerOptionRespVO> result = inspectionConfirmationService.getWorkerOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(adminUserApi, never()).getUserListByPostIds(any());
    }

    @Test
    @DisplayName("挡车工候选：岗位存在但未分配用户，返回空列表且不报错")
    void getWorkerOptions_postHasNoUsers_returnsEmptyList() {
        PostRespDTO post = new PostRespDTO();
        post.setId(20L);
        post.setCode(InspectionConfirmationConstants.POST_CODE_LUNA_WORKER);
        when(postApi.getPostByCode(InspectionConfirmationConstants.POST_CODE_LUNA_WORKER)).thenReturn(post);
        when(adminUserApi.getUserListByPostIds(Collections.singleton(20L))).thenReturn(Collections.emptyList());

        List<WorkerOptionRespVO> result = inspectionConfirmationService.getWorkerOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== manualCreateInspectionConfirmation ====================

    @Test
    @DisplayName("手工登记：缺必填项 rollNo，报错 1_030_014_006")
    void manualCreate_rollNoEmpty_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("   ");
        reqVO.setGrade("B");
        reqVO.setResponsibility("EQUIPMENT");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：缺必填项 grade，报错 1_030_014_006")
    void manualCreate_gradeEmpty_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade(null);
        reqVO.setResponsibility("EQUIPMENT");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：非法 grade 值，报错 1_030_014_006")
    void manualCreate_invalidGrade_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("INVALID_GRADE");
        reqVO.setResponsibility("EQUIPMENT");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：认定时间为未来时间，报错 1_030_014_006")
    void manualCreate_futureConfirmedAt_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("B");
        reqVO.setResponsibility("EQUIPMENT");
        reqVO.setConfirmedAt(LocalDateTime.now().plusHours(2));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：缺必填项 responsibility，报错 1_030_014_006")
    void manualCreate_responsibilityEmpty_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("B");
        reqVO.setResponsibility("");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：非法 responsibility 报错 1_030_014_004")
    void manualCreate_invalidResponsibility_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("B");
        reqVO.setResponsibility("UNKNOWN_TYPE");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_RESPONSIBILITY_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：责任为 OPERATOR 时未指定挡车工，报错 1_030_014_005")
    void manualCreate_operatorMissingWorker_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("B");
        reqVO.setResponsibility("OPERATOR");
        reqVO.setWorkerUserId(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_WORKER_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：关联检验单不存在，报错 1_030_014_002")
    void manualCreate_inspectionOrderNotExists_throwsException() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("B");
        reqVO.setResponsibility("EQUIPMENT");
        reqVO.setInspectionOrderId(999L);

        when(inspectionOrderMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("手工登记：成功落库 CONFIRMED/MANUAL/新 chain_key/认定人/挡车工快照")
    void manualCreate_success_recordsConfirmedManualNewChainKeyAndConfirmedBy() {
        // 模拟登录用户
        LoginUser loginUser = new LoginUser().setId(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        AdminUserRespDTO loginUserDTO = new AdminUserRespDTO();
        loginUserDTO.setId(10L);
        loginUserDTO.setNickname("张领班");
        when(adminUserApi.getUser(10L)).thenReturn(loginUserDTO);

        // 模拟挡车工有效
        AdminUserRespDTO workerDTO = new AdminUserRespDTO();
        workerDTO.setId(101L);
        workerDTO.setNickname("李桂香");
        when(adminUserApi.getUser(101L)).thenReturn(workerDTO);

        // 模拟检验单
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(2048L);
        order.setCode("INS-20260927-001");
        when(inspectionOrderMapper.selectById(2048L)).thenReturn(order);

        // 构造手工登记请求
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R001");
        reqVO.setGrade("B");
        reqVO.setResponsibility("OPERATOR");
        reqVO.setWorkerUserId(101L);
        reqVO.setVatNo("V001");
        reqVO.setMachineNo("M-01");
        reqVO.setProductCode("P001");
        reqVO.setProductName("全棉汗布");
        reqVO.setColorName("藏青");
        reqVO.setColorNo("C-01");
        reqVO.setStyleNo("S-2026");
        reqVO.setMeasuredWeight(new BigDecimal("25.50"));
        reqVO.setLengthM(new BigDecimal("100.00"));
        reqVO.setInspectionOrderId(2048L);
        reqVO.setNote("织疵破洞超标");

        Long newId = inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO);

        // 校验入库对象
        ArgumentCaptor<InspectionConfirmationDO> captor = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).insert(captor.capture());
        InspectionConfirmationDO saved = captor.getValue();

        assertNotNull(saved.getChainKey(), "chainKey 必须生成");
        assertFalse(saved.getChainKey().isEmpty(), "chainKey 不能为空");
        assertEquals("MANUAL", saved.getSource(), "来源应为 MANUAL");
        assertEquals("CONFIRMED", saved.getStatus(), "状态应为 CONFIRMED");
        assertEquals("R001", saved.getRollNo());
        assertEquals("V001", saved.getVatNo());
        assertEquals("B", saved.getGrade());
        assertEquals("M-01", saved.getMachineNo());
        assertEquals("P001", saved.getProductCode());
        assertEquals("全棉汗布", saved.getProductName());
        assertEquals("藏青", saved.getColorName());
        assertEquals("C-01", saved.getColorNo());
        assertEquals("S-2026", saved.getStyleNo());
        assertEquals(new BigDecimal("25.50"), saved.getMeasuredWeight());
        assertEquals(new BigDecimal("100.00"), saved.getLengthM());
        assertEquals("OPERATOR", saved.getResponsibility());
        assertEquals(101L, saved.getWorkerUserId());
        assertEquals("李桂香", saved.getWorkerName());
        assertEquals(2048L, saved.getInspectionOrderId());
        assertEquals("INS-20260927-001", saved.getInspectionOrderCode());
        assertEquals(10L, saved.getConfirmedByUserId(), "认定人应记录当前登录用户ID");
        assertEquals("张领班", saved.getConfirmedByName(), "认定人应记录当前登录用户姓名快照");
        assertNotNull(saved.getConfirmedAt(), "缺省应写入认定时间");
        assertEquals("织疵破洞超标", saved.getNote());

        verify(adminUserApi).validateUser(101L);
    }

    @Test
    @DisplayName("手工登记：传过去时间补录时，采用传入的 confirmedAt")
    void manualCreate_withCustomConfirmedAt_usesProvidedTime() {
        LocalDateTime pastTime = LocalDateTime.of(2026, 9, 20, 10, 0, 0);

        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R002");
        reqVO.setGrade("C");
        reqVO.setResponsibility("EQUIPMENT");
        reqVO.setConfirmedAt(pastTime);

        inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO);

        ArgumentCaptor<InspectionConfirmationDO> captor = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).insert(captor.capture());
        InspectionConfirmationDO saved = captor.getValue();

        assertEquals(pastTime, saved.getConfirmedAt(), "应采用传入的补录时间");
        assertEquals("CONFIRMED", saved.getStatus());
        assertEquals("MANUAL", saved.getSource());
    }

    @Test
    @DisplayName("手工登记：责任类型为 EQUIPMENT/WAIVED 时，挡车工可为空且不校验用户")
    void manualCreate_equipmentOrWaivedWithoutWorker_success() {
        InspectionConfirmationManualCreateReqVO reqVO = new InspectionConfirmationManualCreateReqVO();
        reqVO.setRollNo("R003");
        reqVO.setGrade("DEFECTIVE");
        reqVO.setResponsibility("WAIVED");
        reqVO.setWorkerUserId(null);

        inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO);

        ArgumentCaptor<InspectionConfirmationDO> captor = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).insert(captor.capture());
        InspectionConfirmationDO saved = captor.getValue();

        assertEquals("WAIVED", saved.getResponsibility());
        assertNull(saved.getWorkerUserId());
        assertNull(saved.getWorkerName());
        verify(adminUserApi, never()).validateUser(any());
    }

    // ==================== generateFromOrder ====================

    @Test
    @DisplayName("从检验单生成：B/C/DEFECTIVE 各生成且快照完整（明细有优先用明细，明细无回退检验单主表）")
    void generateFromOrder_defectGrades_generatesRecordsWithFullSnapshot() {
        // 准备主表
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(200L);
        order.setCode("INS-20260927-001");
        order.setStatus("ACTIVE");
        order.setProductCode("P001");
        order.setProductName("全棉汗布主表");
        when(inspectionOrderMapper.selectById(200L)).thenReturn(order);

        // 准备明细：B、C、DEFECTIVE
        InspectionOrderItemDO item1 = new InspectionOrderItemDO();
        item1.setId(101L);
        item1.setInspectionOrderId(200L);
        item1.setRollNo("R01");
        item1.setVatNo("V01");
        item1.setGrade("B");
        item1.setMachineNo("M01");
        item1.setProductCode("P001-Custom");
        item1.setProductName("自定义面料名称");
        item1.setColorName("藏青");
        item1.setColorNo("C01");
        item1.setStyleNo("S01");
        item1.setMeasuredWeight(new BigDecimal("20.50"));
        item1.setLengthM(new BigDecimal("100.00"));

        InspectionOrderItemDO item2 = new InspectionOrderItemDO();
        item2.setId(102L);
        item2.setInspectionOrderId(200L);
        item2.setRollNo("R02");
        item2.setVatNo("V02");
        item2.setGrade("C");
        item2.setMachineNo("M02");
        item2.setProductCode(null); // 无明细产品编码，回退主表
        item2.setProductName(null);
        item2.setColorName("黑色");
        item2.setColorNo("C02");
        item2.setStyleNo("S02");
        item2.setMeasuredWeight(new BigDecimal("22.00"));
        item2.setLengthM(new BigDecimal("105.00"));

        InspectionOrderItemDO item3 = new InspectionOrderItemDO();
        item3.setId(103L);
        item3.setInspectionOrderId(200L);
        item3.setRollNo("R03");
        item3.setVatNo("V03");
        item3.setGrade("defective"); // 小写测试
        item3.setMachineNo("M03");
        item3.setProductCode("");
        item3.setProductName("");
        item3.setColorName("白色");
        item3.setColorNo("C03");
        item3.setStyleNo("S03");
        item3.setMeasuredWeight(new BigDecimal("19.80"));
        item3.setLengthM(new BigDecimal("95.00"));

        when(inspectionOrderItemMapper.selectListByInspectionOrderId(200L))
                .thenReturn(Arrays.asList(item1, item2, item3));
        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.emptyList());

        InspectionConfirmationGenerateReqVO reqVO = new InspectionConfirmationGenerateReqVO(200L);
        InspectionConfirmationGenerateRespVO resp = inspectionConfirmationService.generateFromOrder(reqVO);

        assertNotNull(resp);
        assertEquals(3, resp.getGeneratedCount());
        assertEquals(0, resp.getSkippedCount());

        ArgumentCaptor<InspectionConfirmationDO> captor = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper, times(3)).insert(captor.capture());
        List<InspectionConfirmationDO> captured = captor.getAllValues();

        // 校验第 1 卷（B 级）
        InspectionConfirmationDO c1 = captured.get(0);
        assertNotNull(c1.getChainKey());
        assertFalse(c1.getChainKey().isEmpty());
        assertEquals("INSPECTION", c1.getSource());
        assertEquals("PENDING", c1.getStatus());
        assertEquals(200L, c1.getInspectionOrderId());
        assertEquals("INS-20260927-001", c1.getInspectionOrderCode());
        assertEquals(101L, c1.getInspectionOrderItemId());
        assertEquals("R01", c1.getRollNo());
        assertEquals("V01", c1.getVatNo());
        assertEquals("B", c1.getGrade());
        assertEquals("M01", c1.getMachineNo());
        assertEquals("P001-Custom", c1.getProductCode());
        assertEquals("自定义面料名称", c1.getProductName());
        assertEquals("藏青", c1.getColorName());
        assertEquals("C01", c1.getColorNo());
        assertEquals("S01", c1.getStyleNo());
        assertEquals(new BigDecimal("20.50"), c1.getMeasuredWeight());
        assertEquals(new BigDecimal("100.00"), c1.getLengthM());
        assertNull(c1.getResponsibility());
        assertNull(c1.getWorkerUserId());

        // 校验第 2 卷（C 级，回退主表产品信息）
        InspectionConfirmationDO c2 = captured.get(1);
        assertEquals("PENDING", c2.getStatus());
        assertEquals("C", c2.getGrade());
        assertEquals(102L, c2.getInspectionOrderItemId());
        assertEquals("P001", c2.getProductCode());
        assertEquals("全棉汗布主表", c2.getProductName());
        assertEquals("黑色", c2.getColorName());

        // 校验第 3 卷（DEFECTIVE，回退主表产品信息）
        InspectionConfirmationDO c3 = captured.get(2);
        assertEquals("PENDING", c3.getStatus());
        assertEquals("DEFECTIVE", c3.getGrade());
        assertEquals(103L, c3.getInspectionOrderItemId());
        assertEquals("P001", c3.getProductCode());
        assertEquals("全棉汗布主表", c3.getProductName());
    }

    @Test
    @DisplayName("从检验单生成：A 级与无等级明细跳过不生成，仅缺陷卷生成")
    void generateFromOrder_gradeAOrEmpty_skipped() {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(200L);
        order.setCode("INS-20260927-001");
        order.setStatus("ACTIVE");
        when(inspectionOrderMapper.selectById(200L)).thenReturn(order);

        InspectionOrderItemDO itemA = new InspectionOrderItemDO();
        itemA.setId(101L);
        itemA.setGrade("A");

        InspectionOrderItemDO itemNull = new InspectionOrderItemDO();
        itemNull.setId(102L);
        itemNull.setGrade(null);

        InspectionOrderItemDO itemBlank = new InspectionOrderItemDO();
        itemBlank.setId(103L);
        itemBlank.setGrade("   ");

        InspectionOrderItemDO itemB = new InspectionOrderItemDO();
        itemB.setId(104L);
        itemB.setGrade("B");

        when(inspectionOrderItemMapper.selectListByInspectionOrderId(200L))
                .thenReturn(Arrays.asList(itemA, itemNull, itemBlank, itemB));
        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.emptyList());

        InspectionConfirmationGenerateReqVO reqVO = new InspectionConfirmationGenerateReqVO(200L);
        InspectionConfirmationGenerateRespVO resp = inspectionConfirmationService.generateFromOrder(reqVO);

        assertNotNull(resp);
        assertEquals(1, resp.getGeneratedCount());
        assertEquals(3, resp.getSkippedCount());
        verify(inspectionConfirmationMapper, times(1)).insert(any(InspectionConfirmationDO.class));
    }

    @Test
    @DisplayName("从检验单生成：重复调用幂等计数正确（已存在有效确认行跳过，新增 0 跳过 2）")
    void generateFromOrder_repeatCall_idempotentCountsCorrect() {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(200L);
        order.setCode("INS-20260927-001");
        order.setStatus("ACTIVE");
        when(inspectionOrderMapper.selectById(200L)).thenReturn(order);

        InspectionOrderItemDO item1 = new InspectionOrderItemDO();
        item1.setId(101L);
        item1.setGrade("B");

        InspectionOrderItemDO item2 = new InspectionOrderItemDO();
        item2.setId(102L);
        item2.setGrade("C");

        when(inspectionOrderItemMapper.selectListByInspectionOrderId(200L))
                .thenReturn(Arrays.asList(item1, item2));

        // 模拟已有有效行（101L 为 CONFIRMED，102L 为 PENDING）
        InspectionConfirmationDO conf1 = new InspectionConfirmationDO();
        conf1.setInspectionOrderItemId(101L);
        conf1.setStatus("CONFIRMED");

        InspectionConfirmationDO conf2 = new InspectionConfirmationDO();
        conf2.setInspectionOrderItemId(102L);
        conf2.setStatus("PENDING");

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Arrays.asList(conf1, conf2));

        InspectionConfirmationGenerateReqVO reqVO = new InspectionConfirmationGenerateReqVO(200L);
        InspectionConfirmationGenerateRespVO resp = inspectionConfirmationService.generateFromOrder(reqVO);

        assertNotNull(resp);
        assertEquals(0, resp.getGeneratedCount());
        assertEquals(2, resp.getSkippedCount());
        verify(inspectionConfirmationMapper, never()).insert(any(InspectionConfirmationDO.class));
    }

    @Test
    @DisplayName("从检验单生成：REVOKED 链场景（含克隆出的 PENDING 行）仍正确识别为已有确认行并跳过")
    void generateFromOrder_revokedChainScenario_stillSkipped() {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(200L);
        order.setCode("INS-20260927-001");
        order.setStatus("ACTIVE");
        when(inspectionOrderMapper.selectById(200L)).thenReturn(order);

        InspectionOrderItemDO item1 = new InspectionOrderItemDO();
        item1.setId(101L);
        item1.setGrade("B");

        when(inspectionOrderItemMapper.selectListByInspectionOrderId(200L))
                .thenReturn(Collections.singletonList(item1));

        // REVOKED 链中包含克隆出的 PENDING 行（status ∈ {PENDING, CONFIRMED} 查出该行）
        InspectionConfirmationDO clonePending = new InspectionConfirmationDO();
        clonePending.setInspectionOrderItemId(101L);
        clonePending.setChainKey("chain-001");
        clonePending.setStatus("PENDING");

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(clonePending));

        InspectionConfirmationGenerateReqVO reqVO = new InspectionConfirmationGenerateReqVO(200L);
        InspectionConfirmationGenerateRespVO resp = inspectionConfirmationService.generateFromOrder(reqVO);

        assertNotNull(resp);
        assertEquals(0, resp.getGeneratedCount());
        assertEquals(1, resp.getSkippedCount());
        verify(inspectionConfirmationMapper, never()).insert(any(InspectionConfirmationDO.class));
    }

    @Test
    @DisplayName("从检验单生成：检验单不存在时报 1_030_014_002")
    void generateFromOrder_orderNotExists_throws002() {
        when(inspectionOrderMapper.selectById(999L)).thenReturn(null);

        InspectionConfirmationGenerateReqVO reqVO = new InspectionConfirmationGenerateReqVO(999L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.generateFromOrder(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("从检验单生成：检验单状态为 DRAFT 或 CANCELLED 时报 1_030_014_003")
    void generateFromOrder_orderStatusDraftOrCancelled_throws003() {
        // DRAFT
        InspectionOrderDO draftOrder = new InspectionOrderDO();
        draftOrder.setId(201L);
        draftOrder.setStatus("DRAFT");
        when(inspectionOrderMapper.selectById(201L)).thenReturn(draftOrder);

        InspectionConfirmationGenerateReqVO reqDraft = new InspectionConfirmationGenerateReqVO(201L);
        ServiceException exDraft = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.generateFromOrder(reqDraft));
        assertEquals(INSPECTION_CONFIRMATION_ORDER_STATUS_NOT_ALLOWED.getCode(), exDraft.getCode());

        // CANCELLED
        InspectionOrderDO cancelledOrder = new InspectionOrderDO();
        cancelledOrder.setId(202L);
        cancelledOrder.setStatus("CANCELLED");
        when(inspectionOrderMapper.selectById(202L)).thenReturn(cancelledOrder);

        InspectionConfirmationGenerateReqVO reqCancelled = new InspectionConfirmationGenerateReqVO(202L);
        ServiceException exCancelled = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.generateFromOrder(reqCancelled));
        assertEquals(INSPECTION_CONFIRMATION_ORDER_STATUS_NOT_ALLOWED.getCode(), exCancelled.getCode());
    }

    @Test
    @DisplayName("从检验单生成：检验单状态为 ACTIVE 或 COMPLETED 均放行")
    void generateFromOrder_orderStatusActiveOrCompleted_allowed() {
        // ACTIVE
        InspectionOrderDO activeOrder = new InspectionOrderDO();
        activeOrder.setId(203L);
        activeOrder.setStatus("ACTIVE");
        when(inspectionOrderMapper.selectById(203L)).thenReturn(activeOrder);
        when(inspectionOrderItemMapper.selectListByInspectionOrderId(203L)).thenReturn(Collections.emptyList());

        InspectionConfirmationGenerateRespVO respActive =
                inspectionConfirmationService.generateFromOrder(new InspectionConfirmationGenerateReqVO(203L));
        assertNotNull(respActive);
        assertEquals(0, respActive.getGeneratedCount());
        assertEquals(0, respActive.getSkippedCount());

        // COMPLETED
        InspectionOrderDO completedOrder = new InspectionOrderDO();
        completedOrder.setId(204L);
        completedOrder.setStatus("COMPLETED");
        when(inspectionOrderMapper.selectById(204L)).thenReturn(completedOrder);
        when(inspectionOrderItemMapper.selectListByInspectionOrderId(204L)).thenReturn(Collections.emptyList());

        InspectionConfirmationGenerateRespVO respCompleted =
                inspectionConfirmationService.generateFromOrder(new InspectionConfirmationGenerateReqVO(204L));
        assertNotNull(respCompleted);
        assertEquals(0, respCompleted.getGeneratedCount());
        assertEquals(0, respCompleted.getSkippedCount());
    }

    // ==================== confirmInspectionConfirmation ====================

    @Test
    @DisplayName("责任认定：记录不存在报 1_030_014_000")
    void confirmInspectionConfirmation_notExists_throws000() {
        when(inspectionConfirmationMapper.selectById(999L)).thenReturn(null);

        InspectionConfirmationConfirmReqVO reqVO = new InspectionConfirmationConfirmReqVO();
        reqVO.setId(999L);
        reqVO.setResponsibility("EQUIPMENT");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.confirmInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("责任认定：状态非 PENDING（已认定或已撤销）报 1_030_014_001")
    void confirmInspectionConfirmation_statusNotPending_throws001() {
        // 已认定 CONFIRMED
        InspectionConfirmationDO confirmedDO = new InspectionConfirmationDO();
        confirmedDO.setId(101L);
        confirmedDO.setStatus("CONFIRMED");
        when(inspectionConfirmationMapper.selectById(101L)).thenReturn(confirmedDO);

        InspectionConfirmationConfirmReqVO reqConfirmed = new InspectionConfirmationConfirmReqVO();
        reqConfirmed.setId(101L);
        reqConfirmed.setResponsibility("EQUIPMENT");
        ServiceException ex1 = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.confirmInspectionConfirmation(reqConfirmed));
        assertEquals(INSPECTION_CONFIRMATION_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        // 已撤销 REVOKED
        InspectionConfirmationDO revokedDO = new InspectionConfirmationDO();
        revokedDO.setId(102L);
        revokedDO.setStatus("REVOKED");
        when(inspectionConfirmationMapper.selectById(102L)).thenReturn(revokedDO);

        InspectionConfirmationConfirmReqVO reqRevoked = new InspectionConfirmationConfirmReqVO();
        reqRevoked.setId(102L);
        reqRevoked.setResponsibility("EQUIPMENT");
        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.confirmInspectionConfirmation(reqRevoked));
        assertEquals(INSPECTION_CONFIRMATION_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("责任认定：责任类型非法报 1_030_014_004")
    void confirmInspectionConfirmation_responsibilityInvalid_throws004() {
        InspectionConfirmationDO pendingDO = new InspectionConfirmationDO();
        pendingDO.setId(103L);
        pendingDO.setStatus("PENDING");
        when(inspectionConfirmationMapper.selectById(103L)).thenReturn(pendingDO);

        InspectionConfirmationConfirmReqVO reqVO = new InspectionConfirmationConfirmReqVO();
        reqVO.setId(103L);
        reqVO.setResponsibility("UNKNOWN_RESPONSIBILITY");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.confirmInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_RESPONSIBILITY_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("责任认定：OPERATOR 缺挡车工报 1_030_014_005")
    void confirmInspectionConfirmation_operatorWithoutWorker_throws005() {
        InspectionConfirmationDO pendingDO = new InspectionConfirmationDO();
        pendingDO.setId(104L);
        pendingDO.setStatus("PENDING");
        when(inspectionConfirmationMapper.selectById(104L)).thenReturn(pendingDO);

        InspectionConfirmationConfirmReqVO reqVO = new InspectionConfirmationConfirmReqVO();
        reqVO.setId(104L);
        reqVO.setResponsibility("OPERATOR");
        reqVO.setWorkerUserId(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.confirmInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_WORKER_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("责任认定：OPERATOR 认定成功，校验挡车工并保存快照，写认定人与当前时间，status=CONFIRMED")
    void confirmInspectionConfirmation_operator_success() {
        InspectionConfirmationDO pendingDO = new InspectionConfirmationDO();
        pendingDO.setId(105L);
        pendingDO.setStatus("PENDING");
        pendingDO.setChainKey("chain-105");
        when(inspectionConfirmationMapper.selectById(105L)).thenReturn(pendingDO);

        LoginUser loginUser = new LoginUser().setId(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        AdminUserRespDTO loginUserDTO = new AdminUserRespDTO();
        loginUserDTO.setId(10L);
        loginUserDTO.setNickname("张领班");
        when(adminUserApi.getUser(10L)).thenReturn(loginUserDTO);

        AdminUserRespDTO workerDTO = new AdminUserRespDTO();
        workerDTO.setId(200L);
        workerDTO.setNickname("王挡车");
        when(adminUserApi.getUser(200L)).thenReturn(workerDTO);

        InspectionConfirmationConfirmReqVO reqVO = new InspectionConfirmationConfirmReqVO();
        reqVO.setId(105L);
        reqVO.setResponsibility("OPERATOR");
        reqVO.setWorkerUserId(200L);
        reqVO.setNote("断氨纶");

        Long resultId = inspectionConfirmationService.confirmInspectionConfirmation(reqVO);
        assertEquals(105L, resultId);

        verify(adminUserApi).validateUser(200L);

        ArgumentCaptor<InspectionConfirmationDO> captor = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).updateById(captor.capture());
        InspectionConfirmationDO updated = captor.getValue();

        assertEquals("CONFIRMED", updated.getStatus());
        assertEquals("OPERATOR", updated.getResponsibility());
        assertEquals(200L, updated.getWorkerUserId());
        assertEquals("王挡车", updated.getWorkerName());
        assertEquals(10L, updated.getConfirmedByUserId());
        assertEquals("张领班", updated.getConfirmedByName());
        assertNotNull(updated.getConfirmedAt());
        assertEquals("断氨纶", updated.getNote());
    }

    @Test
    @DisplayName("责任认定：EQUIPMENT 与 WAIVED 成功，挡车工可空且清空，写认定人/时间/状态")
    void confirmInspectionConfirmation_equipmentAndWaived_success() {
        LoginUser loginUser = new LoginUser().setId(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        AdminUserRespDTO loginUserDTO = new AdminUserRespDTO();
        loginUserDTO.setId(10L);
        loginUserDTO.setNickname("张领班");
        when(adminUserApi.getUser(10L)).thenReturn(loginUserDTO);

        // EQUIPMENT
        InspectionConfirmationDO pending1 = new InspectionConfirmationDO();
        pending1.setId(106L);
        pending1.setStatus("PENDING");
        pending1.setWorkerUserId(999L);
        pending1.setWorkerName("原挡车工");
        when(inspectionConfirmationMapper.selectById(106L)).thenReturn(pending1);

        InspectionConfirmationConfirmReqVO req1 = new InspectionConfirmationConfirmReqVO();
        req1.setId(106L);
        req1.setResponsibility("EQUIPMENT");
        req1.setNote("机器坏了");

        Long id1 = inspectionConfirmationService.confirmInspectionConfirmation(req1);
        assertEquals(106L, id1);

        ArgumentCaptor<InspectionConfirmationDO> captor1 = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).updateById(captor1.capture());
        InspectionConfirmationDO updated1 = captor1.getValue();
        assertEquals("CONFIRMED", updated1.getStatus());
        assertEquals("EQUIPMENT", updated1.getResponsibility());
        assertNull(updated1.getWorkerUserId());
        assertNull(updated1.getWorkerName());
        assertEquals(10L, updated1.getConfirmedByUserId());
        assertEquals("张领班", updated1.getConfirmedByName());
        assertNotNull(updated1.getConfirmedAt());
        assertEquals("机器坏了", updated1.getNote());

        // WAIVED
        InspectionConfirmationDO pending2 = new InspectionConfirmationDO();
        pending2.setId(107L);
        pending2.setStatus("PENDING");
        when(inspectionConfirmationMapper.selectById(107L)).thenReturn(pending2);

        InspectionConfirmationConfirmReqVO req2 = new InspectionConfirmationConfirmReqVO();
        req2.setId(107L);
        req2.setResponsibility("WAIVED");

        Long id2 = inspectionConfirmationService.confirmInspectionConfirmation(req2);
        assertEquals(107L, id2);

        ArgumentCaptor<InspectionConfirmationDO> captor2 = ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper, times(2)).updateById(captor2.capture());
        InspectionConfirmationDO updated2 = captor2.getAllValues().get(1);
        assertEquals("CONFIRMED", updated2.getStatus());
        assertEquals("WAIVED", updated2.getResponsibility());
        assertNull(updated2.getWorkerUserId());
        assertNull(updated2.getWorkerName());
        assertEquals("张领班", updated2.getConfirmedByName());
        assertNotNull(updated2.getConfirmedAt());
    }

    // ==================== revokeInspectionConfirmation ====================

    @Test
    @DisplayName("撤销重录：记录不存在报 1_030_014_000")
    void revokeInspectionConfirmation_notExists_throws000() {
        when(inspectionConfirmationMapper.selectById(999L)).thenReturn(null);

        InspectionConfirmationRevokeReqVO reqVO = new InspectionConfirmationRevokeReqVO(999L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.revokeInspectionConfirmation(reqVO));
        assertEquals(INSPECTION_CONFIRMATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤销重录：状态非 CONFIRMED（PENDING 或 REVOKED）报 1_030_014_001")
    void revokeInspectionConfirmation_statusNotConfirmed_throws001() {
        // PENDING
        InspectionConfirmationDO pendingDO = new InspectionConfirmationDO();
        pendingDO.setId(111L);
        pendingDO.setStatus("PENDING");
        when(inspectionConfirmationMapper.selectById(111L)).thenReturn(pendingDO);

        ServiceException ex1 = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.revokeInspectionConfirmation(new InspectionConfirmationRevokeReqVO(111L)));
        assertEquals(INSPECTION_CONFIRMATION_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        // REVOKED
        InspectionConfirmationDO revokedDO = new InspectionConfirmationDO();
        revokedDO.setId(112L);
        revokedDO.setStatus("REVOKED");
        when(inspectionConfirmationMapper.selectById(112L)).thenReturn(revokedDO);

        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.revokeInspectionConfirmation(new InspectionConfirmationRevokeReqVO(112L)));
        assertEquals(INSPECTION_CONFIRMATION_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("撤销重录：旧记录冻结为 REVOKED 保留原字段并记录撤销人时间，克隆新 PENDING 沿用快照与挡车工同 chain_key")
    void revokeInspectionConfirmation_confirmed_success_freezesOldAndClonesNewWithSameChainKey() {
        // 准备旧 CONFIRMED 记录
        InspectionConfirmationDO oldDO = new InspectionConfirmationDO();
        oldDO.setId(88L);
        oldDO.setChainKey("chain-888");
        oldDO.setSource("INSPECTION");
        oldDO.setStatus("CONFIRMED");
        oldDO.setInspectionOrderId(101L);
        oldDO.setInspectionOrderCode("INS-001");
        oldDO.setInspectionOrderItemId(501L);
        oldDO.setRollNo("R01");
        oldDO.setVatNo("V01");
        oldDO.setGrade("B");
        oldDO.setMachineNo("M01");
        oldDO.setProductCode("P01");
        oldDO.setProductName("全棉汗布");
        oldDO.setColorName("黑色");
        oldDO.setColorNo("BK01");
        oldDO.setStyleNo("ST01");
        oldDO.setMeasuredWeight(new BigDecimal("25.50"));
        oldDO.setLengthM(new BigDecimal("100.00"));
        oldDO.setResponsibility("OPERATOR");
        oldDO.setWorkerUserId(300L);
        oldDO.setWorkerName("赵挡车");
        oldDO.setConfirmedByUserId(10L);
        oldDO.setConfirmedByName("张领班");
        oldDO.setConfirmedAt(LocalDateTime.of(2026, 9, 27, 10, 0, 0));
        oldDO.setNote("断纱停台");

        when(inspectionConfirmationMapper.selectById(88L)).thenReturn(oldDO);

        // 模拟登录用户（撤销人）
        LoginUser loginUser = new LoginUser().setId(20L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        AdminUserRespDTO loginUserDTO = new AdminUserRespDTO();
        loginUserDTO.setId(20L);
        loginUserDTO.setNickname("李主管");
        when(adminUserApi.getUser(20L)).thenReturn(loginUserDTO);

        // 模拟 insert 生成新 ID 99L
        doAnswer(invocation -> {
            InspectionConfirmationDO inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        }).when(inspectionConfirmationMapper).insert(any(InspectionConfirmationDO.class));

        // 执行撤销
        InspectionConfirmationRevokeRespVO resp =
                inspectionConfirmationService.revokeInspectionConfirmation(new InspectionConfirmationRevokeReqVO(88L));

        assertNotNull(resp);
        assertEquals(88L, resp.getRevokedId());
        assertEquals(99L, resp.getFreshId());

        // 验证 1：旧记录 updateById 冻结为 REVOKED，撤销人/时间写入，其他字段原样保留
        ArgumentCaptor<InspectionConfirmationDO> updateCaptor =
                ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).updateById(updateCaptor.capture());
        InspectionConfirmationDO updatedOld = updateCaptor.getValue();
        assertEquals("REVOKED", updatedOld.getStatus());
        assertEquals(20L, updatedOld.getRevokedByUserId());
        assertEquals("李主管", updatedOld.getRevokedByName());
        assertNotNull(updatedOld.getRevokedAt());
        // 原责任与认定字段原样保留
        assertEquals("OPERATOR", updatedOld.getResponsibility());
        assertEquals(300L, updatedOld.getWorkerUserId());
        assertEquals("赵挡车", updatedOld.getWorkerName());
        assertEquals(10L, updatedOld.getConfirmedByUserId());
        assertEquals("张领班", updatedOld.getConfirmedByName());
        assertEquals(LocalDateTime.of(2026, 9, 27, 10, 0, 0), updatedOld.getConfirmedAt());
        assertEquals("断纱停台", updatedOld.getNote());

        // 验证 2：新克隆记录 insert，同 chain_key、快照与挡车工沿用、责任与认定字段清空、状态为 PENDING
        ArgumentCaptor<InspectionConfirmationDO> insertCaptor =
                ArgumentCaptor.forClass(InspectionConfirmationDO.class);
        verify(inspectionConfirmationMapper).insert(insertCaptor.capture());
        InspectionConfirmationDO insertedNew = insertCaptor.getValue();

        assertEquals("chain-888", insertedNew.getChainKey(), "新记录必须同挂一条认定链（chain_key一致）");
        assertEquals("INSPECTION", insertedNew.getSource());
        assertEquals("PENDING", insertedNew.getStatus());
        assertEquals(101L, insertedNew.getInspectionOrderId());
        assertEquals("INS-001", insertedNew.getInspectionOrderCode());
        assertEquals(501L, insertedNew.getInspectionOrderItemId());
        assertEquals("R01", insertedNew.getRollNo());
        assertEquals("V01", insertedNew.getVatNo());
        assertEquals("B", insertedNew.getGrade());
        assertEquals("M01", insertedNew.getMachineNo());
        assertEquals("P01", insertedNew.getProductCode());
        assertEquals("全棉汗布", insertedNew.getProductName());
        assertEquals("黑色", insertedNew.getColorName());
        assertEquals("BK01", insertedNew.getColorNo());
        assertEquals("ST01", insertedNew.getStyleNo());
        assertEquals(new BigDecimal("25.50"), insertedNew.getMeasuredWeight());
        assertEquals(new BigDecimal("100.00"), insertedNew.getLengthM());
        // 挡车工沿用
        assertEquals(300L, insertedNew.getWorkerUserId());
        assertEquals("赵挡车", insertedNew.getWorkerName());
        // 责任与认定字段清空
        assertNull(insertedNew.getResponsibility());
        assertNull(insertedNew.getConfirmedByUserId());
        assertNull(insertedNew.getConfirmedByName());
        assertNull(insertedNew.getConfirmedAt());
        assertNull(insertedNew.getNote());
        assertNull(insertedNew.getRevokedByUserId());
        assertNull(insertedNew.getRevokedByName());
        assertNull(insertedNew.getRevokedAt());
    }

    // ==================== getInspectionConfirmation ====================

    @Test
    @DisplayName("详情查询：记录不存在报 1_030_014_000")
    void getInspectionConfirmation_notExists_throws000() {
        when(inspectionConfirmationMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionConfirmationService.getInspectionConfirmation(999L));
        assertEquals(INSPECTION_CONFIRMATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情查询：成功返回记录详情，附带同 chain_key 全部行（含 REVOKED）按创建时间正序")
    void getInspectionConfirmation_success_returnsDetailWithOrderedChainRecords() {
        InspectionConfirmationDO currentDO = new InspectionConfirmationDO();
        currentDO.setId(10L);
        currentDO.setChainKey("chain-001");
        currentDO.setRollNo("R01");
        currentDO.setStatus("CONFIRMED");
        currentDO.setCreateTime(LocalDateTime.of(2026, 9, 27, 12, 0, 0));
        when(inspectionConfirmationMapper.selectById(10L)).thenReturn(currentDO);

        InspectionConfirmationDO historyRevokedDO = new InspectionConfirmationDO();
        historyRevokedDO.setId(1L);
        historyRevokedDO.setChainKey("chain-001");
        historyRevokedDO.setRollNo("R01");
        historyRevokedDO.setStatus("REVOKED");
        historyRevokedDO.setCreateTime(LocalDateTime.of(2026, 9, 27, 10, 0, 0));

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Arrays.asList(historyRevokedDO, currentDO));

        InspectionConfirmationRespVO resp = inspectionConfirmationService.getInspectionConfirmation(10L);

        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals("chain-001", resp.getChainKey());
        assertEquals("CONFIRMED", resp.getStatus());

        // 验证 chainRecords
        assertNotNull(resp.getChainRecords());
        assertEquals(2, resp.getChainRecords().size());
        assertEquals(1L, resp.getChainRecords().get(0).getId());
        assertEquals("REVOKED", resp.getChainRecords().get(0).getStatus());
        assertEquals(10L, resp.getChainRecords().get(1).getId());
        assertEquals("CONFIRMED", resp.getChainRecords().get(1).getStatus());

        // 验证查询条件与排序
        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectList(captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("chain_key ="), "应按 chainKey 过滤");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("chain-001"), "应包含 chainKey 参数值");
        assertTrue(sqlSegment.contains("ORDER BY create_time ASC"), "应按创建时间正序排序");
    }

    // ==================== getPrintView ====================

    @Test
    @DisplayName("打印视图：未传 date 缺省今天，查询 [今天00:00, 明天00:00) 边界，并按 create_time 正序")
    void getPrintView_defaultToday_queriesTodayRangeAscending() {
        LocalDate today = LocalDate.now();
        InspectionConfirmationDO item1 = new InspectionConfirmationDO();
        item1.setId(101L);
        item1.setRollNo("R01");
        item1.setStatus("PENDING");
        item1.setCreateTime(today.atTime(8, 0, 0));

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(item1));

        List<InspectionConfirmationPrintViewRespVO> result = inspectionConfirmationService.getPrintView(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("R01", result.get(0).getRollNo());

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectList(captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("create_time >="), "应包含 create_time >= 起始时间");
        assertTrue(sql.contains("create_time <"), "应包含 create_time < 截止时间");
        assertTrue(sql.contains("ORDER BY create_time ASC"), "应按创建时间正序排序");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(today.atStartOfDay()), "应以今天 00:00:00 为下界");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(today.plusDays(1).atStartOfDay()), "应以明天 00:00:00 为上界");
    }

    @Test
    @DisplayName("打印视图：指定日期查询该日范围全部状态行（含 REVOKED 标记），正序并映射全字段")
    void getPrintView_specificDate_returnsAllStatusesWithCorrectFields() {
        LocalDate targetDate = LocalDate.of(2026, 9, 25);
        InspectionConfirmationDO itemPending = new InspectionConfirmationDO();
        itemPending.setId(1L);
        itemPending.setRollNo("R01");
        itemPending.setVatNo("V01");
        itemPending.setGrade("A");
        itemPending.setInspectionOrderCode("INS-001");
        itemPending.setProductName("全棉汗布");
        itemPending.setMachineNo("M01");
        itemPending.setStatus("PENDING");
        itemPending.setCreateTime(targetDate.atTime(9, 0, 0));

        InspectionConfirmationDO itemConfirmed = new InspectionConfirmationDO();
        itemConfirmed.setId(2L);
        itemConfirmed.setRollNo("R02");
        itemConfirmed.setVatNo("V02");
        itemConfirmed.setGrade("B");
        itemConfirmed.setInspectionOrderCode("INS-001");
        itemConfirmed.setStatus("CONFIRMED");
        itemConfirmed.setResponsibility("OPERATOR");
        itemConfirmed.setWorkerUserId(1001L);
        itemConfirmed.setWorkerName("张工");
        itemConfirmed.setConfirmedByName("领班A");
        itemConfirmed.setConfirmedAt(targetDate.atTime(11, 0, 0));
        itemConfirmed.setNote("断纱");
        itemConfirmed.setCreateTime(targetDate.atTime(10, 0, 0));

        InspectionConfirmationDO itemRevoked = new InspectionConfirmationDO();
        itemRevoked.setId(3L);
        itemRevoked.setRollNo("R03");
        itemRevoked.setGrade("C");
        itemRevoked.setStatus("REVOKED");
        itemRevoked.setCreateTime(targetDate.atTime(14, 0, 0));

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Arrays.asList(itemPending, itemConfirmed, itemRevoked));

        List<InspectionConfirmationPrintViewRespVO> result = inspectionConfirmationService.getPrintView(targetDate);

        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证全部状态均返回
        assertEquals("PENDING", result.get(0).getStatus());
        assertEquals("CONFIRMED", result.get(1).getStatus());
        assertEquals("REVOKED", result.get(2).getStatus());

        // 验证字段映射
        InspectionConfirmationPrintViewRespVO vo1 = result.get(1);
        assertEquals("R02", vo1.getRollNo());
        assertEquals("V02", vo1.getVatNo());
        assertEquals("B", vo1.getGrade());
        assertEquals("INS-001", vo1.getInspectionOrderCode());
        assertEquals("OPERATOR", vo1.getResponsibility());
        assertEquals(1001L, vo1.getWorkerUserId());
        assertEquals("张工", vo1.getWorkerName());
        assertEquals("领班A", vo1.getConfirmedByName());
        assertEquals(targetDate.atTime(11, 0, 0), vo1.getConfirmedAt());
        assertEquals("断纱", vo1.getNote());

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectList(captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("create_time >="), "应包含 create_time >= 起始时间");
        assertTrue(sql.contains("create_time <"), "应包含 create_time < 截止时间");
        assertTrue(sql.contains("ORDER BY create_time ASC"), "应按创建时间正序排序");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(targetDate.atStartOfDay()), "应包含指定日期 00:00:00");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(targetDate.plusDays(1).atStartOfDay()), "应包含次日 00:00:00");
    }


    // ==================== getPayrollSummary ====================

    @Test
    @DisplayName("工资扣款汇总：未传 date 缺省今天，严格按 confirmed_at [今天00:00, 明天00:00) + status=CONFIRMED + responsibility=OPERATOR 过滤")
    void getPayrollSummary_defaultToday_queriesStrictConditions() {
        LocalDate today = LocalDate.now();
        when(inspectionConfirmationMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        InspectionConfirmationPayrollSummaryRespVO summary = inspectionConfirmationService.getPayrollSummary(null);

        assertNotNull(summary);
        assertEquals(today, summary.getDate());
        assertTrue(summary.getGroups().isEmpty());

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectList(captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("status ="), "必须过滤 status=CONFIRMED");
        assertTrue(sql.contains("responsibility ="), "必须过滤 responsibility=OPERATOR");
        assertTrue(sql.contains("confirmed_at >="), "必须过滤 confirmed_at >= 当日起始");
        assertTrue(sql.contains("confirmed_at <"), "必须过滤 confirmed_at < 次日起始");
        assertTrue(sql.contains("ORDER BY worker_user_id ASC"), "应按挡车工排序");

        assertTrue(wrapper.getParamNameValuePairs().containsValue("CONFIRMED"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("OPERATOR"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(today.atStartOfDay()));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(today.plusDays(1).atStartOfDay()));
    }

    @Test
    @DisplayName("工资扣款汇总：按挡车工正确分组聚合并统计 count 与 records 明细")
    void getPayrollSummary_groupsByWorker_countsAndDetailsMatch() {
        LocalDate targetDate = LocalDate.of(2026, 9, 25);

        // 李桂香 2 卷
        InspectionConfirmationDO r1 = new InspectionConfirmationDO();
        r1.setId(8801L);
        r1.setWorkerUserId(1024L);
        r1.setWorkerName("李桂香");
        r1.setRollNo("P0309");
        r1.setVatNo("V12");
        r1.setGrade("C");
        r1.setInspectionOrderCode("INS-20260925-001");
        r1.setProductName("全棉汗布");
        r1.setNote("断纱");
        r1.setSource("INSPECTION");
        r1.setStatus("CONFIRMED");
        r1.setResponsibility("OPERATOR");
        r1.setConfirmedAt(targetDate.atTime(14, 0, 0));

        InspectionConfirmationDO r2 = new InspectionConfirmationDO();
        r2.setId(8802L);
        r2.setWorkerUserId(1024L);
        r2.setWorkerName("李桂香");
        r2.setRollNo("P0310");
        r2.setVatNo("V12");
        r2.setGrade("DEFECTIVE");
        r2.setInspectionOrderCode("INS-20260925-001");
        r2.setProductName("全棉汗布");
        r2.setNote("破洞");
        r2.setSource("INSPECTION");
        r2.setStatus("CONFIRMED");
        r2.setResponsibility("OPERATOR");
        r2.setConfirmedAt(targetDate.atTime(15, 0, 0));

        // 王建国 1 卷
        InspectionConfirmationDO r3 = new InspectionConfirmationDO();
        r3.setId(8803L);
        r3.setWorkerUserId(1025L);
        r3.setWorkerName("王建国");
        r3.setRollNo("P0401");
        r3.setVatNo("V15");
        r3.setGrade("B");
        r3.setInspectionOrderCode("INS-20260925-002");
        r3.setProductName("涤棉斜纹");
        r3.setNote("油污");
        r3.setSource("MANUAL");
        r3.setStatus("CONFIRMED");
        r3.setResponsibility("OPERATOR");
        r3.setConfirmedAt(targetDate.atTime(16, 0, 0));

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class)))
                .thenReturn(Arrays.asList(r1, r2, r3));

        InspectionConfirmationPayrollSummaryRespVO summary = inspectionConfirmationService.getPayrollSummary(targetDate);

        assertNotNull(summary);
        assertEquals(targetDate, summary.getDate());
        assertEquals(2, summary.getGroups().size());

        // 验证第 1 组（李桂香）
        InspectionConfirmationPayrollSummaryRespVO.WorkerGroup g1 = summary.getGroups().get(0);
        assertEquals(1024L, g1.getWorkerUserId());
        assertEquals("李桂香", g1.getWorkerName());
        assertEquals(2, g1.getCount());
        assertEquals(2, g1.getRecords().size());
        assertEquals(8801L, g1.getRecords().get(0).getId());
        assertEquals("P0309", g1.getRecords().get(0).getRollNo());
        assertEquals("V12", g1.getRecords().get(0).getVatNo());
        assertEquals("C", g1.getRecords().get(0).getGrade());
        assertEquals("INS-20260925-001", g1.getRecords().get(0).getInspectionOrderCode());
        assertEquals("断纱", g1.getRecords().get(0).getNote());
        assertEquals("INSPECTION", g1.getRecords().get(0).getSource());
        assertEquals(targetDate.atTime(14, 0, 0), g1.getRecords().get(0).getConfirmedAt());

        assertEquals(8802L, g1.getRecords().get(1).getId());
        assertEquals("P0310", g1.getRecords().get(1).getRollNo());
        assertEquals("DEFECTIVE", g1.getRecords().get(1).getGrade());

        // 验证第 2 组（王建国）
        InspectionConfirmationPayrollSummaryRespVO.WorkerGroup g2 = summary.getGroups().get(1);
        assertEquals(1025L, g2.getWorkerUserId());
        assertEquals("王建国", g2.getWorkerName());
        assertEquals(1, g2.getCount());
        assertEquals(1, g2.getRecords().size());
        assertEquals(8803L, g2.getRecords().get(0).getId());
        assertEquals("P0401", g2.getRecords().get(0).getRollNo());
        assertEquals("MANUAL", g2.getRecords().get(0).getSource());
    }

    @Test
    @DisplayName("工资扣款汇总：严格验证 SQL Wrapper 条件排他性（EQUIPMENT/WAIVED/REVOKED/跨日确认均不入组）")
    void getPayrollSummary_wrapperStrictExclusions_equipmentWaivedRevokedAndCrossDayExcluded() {
        LocalDate targetDate = LocalDate.of(2026, 9, 25);
        when(inspectionConfirmationMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        inspectionConfirmationService.getPayrollSummary(targetDate);

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectList(captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();

        // 验证只查 CONFIRMED，排除 PENDING / REVOKED
        assertTrue(sql.contains("status ="), "Wrapper 必须限制 status");
        assertEquals("CONFIRMED", wrapper.getParamNameValuePairs().values().stream()
                .filter("CONFIRMED"::equals).findFirst().orElse(null));

        // 验证只查 OPERATOR，排除 EQUIPMENT / WAIVED
        assertTrue(sql.contains("responsibility ="), "Wrapper 必须限制 responsibility");
        assertEquals("OPERATOR", wrapper.getParamNameValuePairs().values().stream()
                .filter("OPERATOR"::equals).findFirst().orElse(null));

        // 验证跨日边界排他：必须是 targetDate 当日 00:00:00 至次日 00:00:00 闭开区间
        assertTrue(sql.contains("confirmed_at >="), "Wrapper 必须限制 confirmed_at 下界");
        assertTrue(sql.contains("confirmed_at <"), "Wrapper 必须限制 confirmed_at 上界");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(targetDate.atStartOfDay()), "下界为当日零点");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(targetDate.plusDays(1).atStartOfDay()), "上界为次日零点");
    }

    @Test
    @DisplayName("工资扣款汇总：空结果返回 date 与空 groups 列表")
    void getPayrollSummary_empty_returnsEmptyGroups() {
        LocalDate targetDate = LocalDate.of(2026, 9, 25);
        when(inspectionConfirmationMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        InspectionConfirmationPayrollSummaryRespVO summary = inspectionConfirmationService.getPayrollSummary(targetDate);

        assertNotNull(summary);
        assertEquals(targetDate, summary.getDate());
        assertNotNull(summary.getGroups());
        assertTrue(summary.getGroups().isEmpty());
    }

}


