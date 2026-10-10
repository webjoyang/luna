package com.luna.lunasrv.module.lunaerp.approval.instance;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.biz.ApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigDO;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigNodeDO;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigNodeMapper;
import com.luna.lunasrv.module.lunaerp.approval.config.ApprovalConfigService;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalProcessSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordDO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordMapper;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskDO;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskMapper;
import com.luna.lunasrv.module.system.api.dept.DeptApi;
import com.luna.lunasrv.module.system.api.dept.dto.DeptRespDTO;
import com.luna.lunasrv.module.system.api.permission.PermissionApi;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.luna.lunasrv.module.lunaerp.approval.enums.ApprovalErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ApprovalInstanceServiceImpl} 单元测试
 *
 * 覆盖 Issue 04:
 *  - POST submit 流程存在性校验 (1_030_030_001)
 *  - 审批人来源解析为快照 (USER/POST/ROLE/DEPT_LEADER/SUBMITTER_LEADER)
 *  - 创建 instance + 首节点 task(s) + SUBMIT 记录
 *  - 更新业务单据状态 APPROVING
 *  - PUT withdraw 权限校验 (1_030_030_005)
 *  - PUT withdraw 状态机守卫 (1_030_030_004)
 *  - PUT withdraw 任务关闭、记录写入、单据回退 DRAFT
 */
class ApprovalInstanceServiceImplTest {

    @Mock
    private ApprovalConfigService approvalConfigService;

    @Mock
    private ApprovalConfigNodeMapper approvalConfigNodeMapper;

    @Mock
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Mock
    private ApprovalTaskMapper approvalTaskMapper;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private DeptApi deptApi;

    @Mock
    private PermissionApi permissionApi;

    @Mock
    private ApprovalBizHandler salesContractBizHandler;

    @InjectMocks
    private ApprovalInstanceServiceImpl approvalInstanceService;

    private final Long loginUserId = 100L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(1L);

        // 设置当前登录用户
        LoginUser loginUser = new LoginUser();
        loginUser.setId(loginUserId);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );

        // 初始化 BizHandler
        when(salesContractBizHandler.getBizType()).thenReturn("sales_contract");
        when(salesContractBizHandler.getBizTypeName()).thenReturn("销售合同");
        when(salesContractBizHandler.getBizNo(1001L)).thenReturn("CH20260001");

        List<ApprovalBizHandler> handlers = List.of(salesContractBizHandler);
        ReflectionTestUtils.setField(approvalInstanceService, "bizHandlers", handlers);
        approvalInstanceService.init();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("发起审批成功：解析5种审批人来源为快照，生成实例、首节点待办与SUBMIT记录")
    void testSubmitInstance_Success() {
        // 1. 模拟提交人信息
        AdminUserRespDTO submitter = new AdminUserRespDTO();
        submitter.setId(loginUserId);
        submitter.setNickname("业务员小张");
        submitter.setDeptId(10L);
        when(adminUserApi.getUser(loginUserId)).thenReturn(submitter);

        // 2. 模拟启用的审批流程与全部节点
        ApprovalConfigDO config = ApprovalConfigDO.builder()
                .id(1L).name("销售合同标准审批流").bizType("sales_contract").status("ENABLED")
                .build();
        when(approvalConfigService.getEnabledConfigByBizType("sales_contract")).thenReturn(config);

        // 5 个节点分别测试 5 种审批人来源
        List<ApprovalConfigNodeDO> nodes = List.of(
                ApprovalConfigNodeDO.builder().id(11L).configId(1L).sort(1).name("第一级-指定用户")
                        .nodeType("OR_SIGN").approverType("USER").approverIds("[101, 102]").build(),
                ApprovalConfigNodeDO.builder().id(12L).configId(1L).sort(2).name("第二级-指定岗位")
                        .nodeType("SEQUENTIAL").approverType("POST").approverIds("20").build(),
                ApprovalConfigNodeDO.builder().id(13L).configId(1L).sort(3).name("第三级-指定角色")
                        .nodeType("SEQUENTIAL").approverType("ROLE").approverIds("30").build(),
                ApprovalConfigNodeDO.builder().id(14L).configId(1L).sort(4).name("第四级-部门负责人")
                        .nodeType("SEQUENTIAL").approverType("DEPT_LEADER").build(),
                ApprovalConfigNodeDO.builder().id(15L).configId(1L).sort(5).name("第五级-直属上级")
                        .nodeType("SEQUENTIAL").approverType("SUBMITTER_LEADER").build()
        );
        when(approvalConfigNodeMapper.selectListByConfigId(1L)).thenReturn(nodes);

        // 模拟各类型审批人解析返回
        AdminUserRespDTO postUser = new AdminUserRespDTO();
        postUser.setId(201L);
        when(adminUserApi.getUserListByPostIds(List.of(20L))).thenReturn(List.of(postUser));

        when(permissionApi.getUserRoleIdListByRoleIds(List.of(30L))).thenReturn(Set.of(301L));

        DeptRespDTO submitterDept = new DeptRespDTO();
        submitterDept.setId(10L);
        submitterDept.setLeaderUserId(401L); // 部门负责人不是提交人
        when(deptApi.getDept(10L)).thenReturn(submitterDept);

        // 3. 执行提交
        ApprovalInstanceSubmitReqVO reqVO = new ApprovalInstanceSubmitReqVO();
        reqVO.setBizType("sales_contract");
        reqVO.setBizId(1001L);
        reqVO.setComment("请领导尽快审批");

        doAnswer(invocation -> {
            ApprovalInstanceDO inst = invocation.getArgument(0);
            inst.setId(5001L);
            return 1;
        }).when(approvalInstanceMapper).insert(any(ApprovalInstanceDO.class));

        Long instanceId = approvalInstanceService.submitInstance(reqVO);
        assertEquals(5001L, instanceId);

        // 4. 校验流程
        verify(salesContractBizHandler).validateCanSubmit(1001L);
        verify(salesContractBizHandler).updateApprovalStatus(1001L, "APPROVING");

        // 校验 instance 插入数据
        ArgumentCaptor<ApprovalInstanceDO> instCaptor = ArgumentCaptor.forClass(ApprovalInstanceDO.class);
        verify(approvalInstanceMapper).insert(instCaptor.capture());
        ApprovalInstanceDO savedInst = instCaptor.getValue();
        assertEquals("sales_contract", savedInst.getBizType());
        assertEquals(1001L, savedInst.getBizId());
        assertEquals("CH20260001", savedInst.getBizNo());
        assertEquals("APPROVING", savedInst.getStatus());
        assertEquals(loginUserId, savedInst.getSubmitterId());
        assertEquals(1, savedInst.getCurrentNodeSort());

        // 校验 snapshot 解析
        ApprovalProcessSnapshotDTO snapshot = JsonUtils.parseObject(savedInst.getProcessSnapshot(), ApprovalProcessSnapshotDTO.class);
        assertNotNull(snapshot);
        assertEquals(5, snapshot.getNodes().size());
        assertEquals(List.of(101L, 102L), snapshot.getNodes().get(0).getApproverUserIds());
        assertEquals(List.of(201L), snapshot.getNodes().get(1).getApproverUserIds());
        assertEquals(List.of(301L), snapshot.getNodes().get(2).getApproverUserIds());
        assertEquals(List.of(401L), snapshot.getNodes().get(3).getApproverUserIds());
        assertEquals(List.of(401L), snapshot.getNodes().get(4).getApproverUserIds());

        // 校验首节点待办任务生成（首节点为 USER [101, 102]，应生成 2 条 PENDING task）
        ArgumentCaptor<ApprovalTaskDO> taskCaptor = ArgumentCaptor.forClass(ApprovalTaskDO.class);
        verify(approvalTaskMapper, times(2)).insert(taskCaptor.capture());
        List<ApprovalTaskDO> tasks = taskCaptor.getAllValues();
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().allMatch(t -> "PENDING".equals(t.getStatus())));
        assertTrue(tasks.stream().allMatch(t -> t.getNodeSort() == 1));
        assertTrue(tasks.stream().allMatch(t -> t.getAction() == null));
        assertEquals(Set.of(101L, 102L), Set.of(tasks.get(0).getApproverId(), tasks.get(1).getApproverId()));

        // 校验 SUBMIT 记录生成
        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        ApprovalRecordDO record = recordCaptor.getValue();
        assertEquals(5001L, record.getInstanceId());
        assertEquals(loginUserId, record.getOperatorId());
        assertEquals("SUBMIT", record.getAction());
        assertEquals("请领导尽快审批", record.getComment());
    }

    @Test
    @DisplayName("发起审批失败：无启用的流程配置抛出 1_030_030_001")
    void testSubmitInstance_NoEnabledConfig_Throws() {
        when(approvalConfigService.getEnabledConfigByBizType("sales_contract")).thenReturn(null);

        ApprovalInstanceSubmitReqVO reqVO = new ApprovalInstanceSubmitReqVO();
        reqVO.setBizType("sales_contract");
        reqVO.setBizId(1001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalInstanceService.submitInstance(reqVO));
        assertEquals(APPROVAL_CONFIG_NOT_ENABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("发起审批失败：首节点未解析出有效审批人抛出 1_030_030_001")
    void testSubmitInstance_FirstNodeNoApprovers_Throws() {
        ApprovalConfigDO config = ApprovalConfigDO.builder().id(1L).bizType("sales_contract").status("ENABLED").build();
        when(approvalConfigService.getEnabledConfigByBizType("sales_contract")).thenReturn(config);

        // 空用户的首节点
        List<ApprovalConfigNodeDO> nodes = List.of(
                ApprovalConfigNodeDO.builder().id(11L).configId(1L).sort(1).name("未配置人员节点")
                        .nodeType("SEQUENTIAL").approverType("USER").approverIds("[]").build()
        );
        when(approvalConfigNodeMapper.selectListByConfigId(1L)).thenReturn(nodes);

        ApprovalInstanceSubmitReqVO reqVO = new ApprovalInstanceSubmitReqVO();
        reqVO.setBizType("sales_contract");
        reqVO.setBizId(1001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalInstanceService.submitInstance(reqVO));
        assertEquals(APPROVAL_CONFIG_NOT_ENABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤回审批成功：发起人撤回，实例变 CANCELLED，待办置 DONE+action=null，记录 WITHDRAW，单据回退 DRAFT")
    void testWithdrawInstance_Success() {
        Long instanceId = 5001L;
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(instanceId)
                .bizType("sales_contract")
                .bizId(1001L)
                .status("APPROVING")
                .submitterId(loginUserId)
                .build();
        when(approvalInstanceMapper.selectById(instanceId)).thenReturn(instance);

        List<ApprovalTaskDO> pendingTasks = List.of(
                ApprovalTaskDO.builder().id(201L).instanceId(instanceId).status("PENDING").build(),
                ApprovalTaskDO.builder().id(202L).instanceId(instanceId).status("PENDING").build()
        );
        when(approvalTaskMapper.selectPendingListByInstanceId(instanceId)).thenReturn(pendingTasks);

        ApprovalInstanceWithdrawReqVO withdrawReqVO = new ApprovalInstanceWithdrawReqVO();
        withdrawReqVO.setReason("信息填写错误撤回");

        approvalInstanceService.withdrawInstance(instanceId, withdrawReqVO);

        // 校验实例状态置为 CANCELLED
        assertEquals("CANCELLED", instance.getStatus());
        assertNotNull(instance.getEndTime());
        verify(approvalInstanceMapper).updateById(instance);

        // 校验待办任务置为 DONE 且 action=null
        verify(approvalTaskMapper, times(2)).updateById(any(ApprovalTaskDO.class));
        assertEquals("DONE", pendingTasks.get(0).getStatus());
        assertNull(pendingTasks.get(0).getAction());
        assertEquals("DONE", pendingTasks.get(1).getStatus());
        assertNull(pendingTasks.get(1).getAction());

        // 校验记录写入 WITHDRAW
        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        ApprovalRecordDO record = recordCaptor.getValue();
        assertEquals(instanceId, record.getInstanceId());
        assertEquals(loginUserId, record.getOperatorId());
        assertEquals("WITHDRAW", record.getAction());
        assertEquals("信息填写错误撤回", record.getComment());

        // 校验单据状态回退 DRAFT
        verify(salesContractBizHandler).updateApprovalStatus(1001L, "DRAFT");
    }

    @Test
    @DisplayName("撤回审批失败：实例不存在抛出 1_030_030_002")
    void testWithdrawInstance_NotExists_Throws() {
        when(approvalInstanceMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalInstanceService.withdrawInstance(9999L, null));
        assertEquals(APPROVAL_INSTANCE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤回审批失败：非发起人本人撤回抛出 1_030_030_005")
    void testWithdrawInstance_NotSubmitter_Throws() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(5001L)
                .bizType("sales_contract")
                .bizId(1001L)
                .status("APPROVING")
                .submitterId(999L) // 其他用户发起的
                .build();
        when(approvalInstanceMapper.selectById(5001L)).thenReturn(instance);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalInstanceService.withdrawInstance(5001L, null));
        assertEquals(APPROVAL_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤回审批失败：非 APPROVING 状态抛出 1_030_030_004")
    void testWithdrawInstance_StatusNotApproving_Throws() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(5001L)
                .bizType("sales_contract")
                .bizId(1001L)
                .status("APPROVED") // 已通过终态
                .submitterId(loginUserId)
                .build();
        when(approvalInstanceMapper.selectById(5001L)).thenReturn(instance);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalInstanceService.withdrawInstance(5001L, null));
        assertEquals(APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("查询审批实例详情：聚合 tasks、records 与用户昵称回显")
    void testGetInstance_Success() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(5001L)
                .bizType("sales_contract")
                .bizId(1001L)
                .bizNo("CH20260001")
                .status("APPROVING")
                .submitterId(loginUserId)
                .currentNodeSort(1)
                .startTime(LocalDateTime.now())
                .build();
        when(approvalInstanceMapper.selectById(5001L)).thenReturn(instance);

        List<ApprovalTaskDO> tasks = List.of(
                ApprovalTaskDO.builder().id(201L).instanceId(5001L).approverId(101L).status("PENDING").build()
        );
        when(approvalTaskMapper.selectListByInstanceId(5001L)).thenReturn(tasks);

        List<ApprovalRecordDO> records = List.of(
                ApprovalRecordDO.builder().id(301L).instanceId(5001L).operatorId(loginUserId).action("SUBMIT").build()
        );
        when(approvalRecordMapper.selectListByInstanceId(5001L)).thenReturn(records);

        AdminUserRespDTO user100 = new AdminUserRespDTO();
        user100.setId(100L);
        user100.setNickname("发起人小张");
        AdminUserRespDTO user101 = new AdminUserRespDTO();
        user101.setId(101L);
        user101.setNickname("审批人主管");

        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Map.of(100L, user100, 101L, user101));

        ApprovalInstanceRespVO respVO = approvalInstanceService.getInstance(5001L);
        assertNotNull(respVO);
        assertEquals(5001L, respVO.getId());
        assertEquals("发起人小张", respVO.getSubmitterName());
        assertEquals(1, respVO.getTasks().size());
        assertEquals("审批人主管", respVO.getTasks().get(0).getApproverName());
        assertEquals(1, respVO.getRecords().size());
        assertEquals("发起人小张", respVO.getRecords().get(0).getOperatorName());
    }

}
