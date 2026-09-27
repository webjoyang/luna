package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationConstants;
import com.luna.lunasrv.module.system.api.dept.PostApi;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class InspectionConfirmationServiceImplTest {

    @Mock
    private InspectionConfirmationMapper inspectionConfirmationMapper;

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

}
