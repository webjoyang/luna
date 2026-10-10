package com.luna.lunasrv.module.lunaerp.approval.task;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.biz.ApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.event.ApprovalCompletedEvent;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceMapper;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalNodeSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalProcessSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordDO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordMapper;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.*;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.luna.lunasrv.module.lunaerp.approval.enums.ApprovalErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ApprovalTaskServiceImpl} 单元测试
 *
 * 覆盖 Issue 05:
 *  - 待我审批 todo-page 查询与超时计算
 *  - 逐级通过（顺序流转下一节点 / 终节点通过触发完成与单据更新）
 *  - 或签任一通过（同节点其他待办自动关闭）
 *  - 会签全部通过（部分通过不推进，全员通过推进）
 *  - 会签并发通过 task.version 乐观锁冲突 (1_030_030_004)
 *  - 审批驳回（意见必填校验 1_030_030_006、终止实例、单据回退 REJECTED）
 *  - 审批转交（转交自己校验 1_030_030_009、任务流转与记录写入）
 *  - 终态守卫（实例非 APPROVING 报 1_030_030_004）
 *  - 权限守卫（非审批人本人报 1_030_030_005）
 */
class ApprovalTaskServiceImplTest {

    @Mock
    private ApprovalTaskMapper approvalTaskMapper;

    @Mock
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ApprovalBizHandler salesContractBizHandler;

    @InjectMocks
    private ApprovalTaskServiceImpl approvalTaskService;

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

        when(salesContractBizHandler.getBizType()).thenReturn("sales_contract");
        when(salesContractBizHandler.getBizTypeName()).thenReturn("销售合同");

        List<ApprovalBizHandler> handlers = List.of(salesContractBizHandler);
        ReflectionTestUtils.setField(approvalTaskService, "bizHandlers", handlers);
        approvalTaskService.init();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private ApprovalProcessSnapshotDTO create2NodeSnapshot(String firstNodeType) {
        return ApprovalProcessSnapshotDTO.builder()
                .configId(1L)
                .configName("测试合同流程")
                .bizType("sales_contract")
                .nodes(List.of(
                        ApprovalNodeSnapshotDTO.builder()
                                .sort(1)
                                .name("第1级-" + firstNodeType)
                                .nodeType(firstNodeType)
                                .approverType("USER")
                                .approverUserIds(List.of(100L, 101L))
                                .build(),
                        ApprovalNodeSnapshotDTO.builder()
                                .sort(2)
                                .name("第2级-终审")
                                .nodeType("SEQUENTIAL")
                                .approverType("USER")
                                .approverUserIds(List.of(200L))
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("待我审批分页：查询当前用户 PENDING 任务并组装实例单号与超时信息")
    void testGetTodoPage_Success() {
        ApprovalTaskTodoPageReqVO reqVO = new ApprovalTaskTodoPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("主管审批")
                .nodeType("SEQUENTIAL")
                .approverId(loginUserId)
                .status("PENDING")
                .startTime(LocalDateTime.now().minusHours(2))
                .build();

        PageResult<ApprovalTaskDO> taskPage = new PageResult<>(List.of(task), 1L);
        when(approvalTaskMapper.selectTodoPage(eq(loginUserId), any())).thenReturn(taskPage);

        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .submitterId(300L)
                .status("APPROVING")
                .startTime(LocalDateTime.now().minusHours(3))
                .build();
        when(approvalInstanceMapper.selectBatchIds(List.of(501L))).thenReturn(List.of(instance));

        AdminUserRespDTO approverUser = new AdminUserRespDTO();
        approverUser.setId(loginUserId);
        approverUser.setNickname("主管张三");

        AdminUserRespDTO submitterUser = new AdminUserRespDTO();
        submitterUser.setId(300L);
        submitterUser.setNickname("业务员李四");

        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Map.of(
                loginUserId, approverUser,
                300L, submitterUser
        ));

        PageResult<ApprovalTaskTodoRespVO> result = approvalTaskService.getTodoPage(reqVO);
        assertNotNull(result);
        assertEquals(1, result.getList().size());

        ApprovalTaskTodoRespVO vo = result.getList().get(0);
        assertEquals(101L, vo.getId());
        assertEquals("主管审批", vo.getNodeName());
        assertEquals("主管张三", vo.getApproverName());
        assertEquals("HT20261010001", vo.getBizNo());
        assertEquals("销售合同", vo.getBizTypeName());
        assertEquals("业务员李四", vo.getSubmitterName());
        assertNotNull(vo.getTimeoutDuration());
        assertNotNull(vo.getTimeoutDurationDesc());
        assertTrue(vo.getTimeoutDurationDesc().contains("小时"));
    }

    @Test
    @DisplayName("审批通过-逐级节点流转：第1级逐级通过，推进至第2级节点并生成下一节点待办")
    void testApproveTask_Sequential_AdvanceNextNode() {
        ApprovalProcessSnapshotDTO snapshot = create2NodeSnapshot("SEQUENTIAL");
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .submitterId(300L)
                .status("APPROVING")
                .currentNodeSort(1)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-SEQUENTIAL")
                .nodeType("SEQUENTIAL")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(task);
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        ApprovalTaskApproveReqVO reqVO = new ApprovalTaskApproveReqVO();
        reqVO.setComment("同意签署");

        approvalTaskService.approveTask(101L, reqVO);

        // 校验当前任务标记为 DONE+DONE
        assertEquals("DONE", task.getStatus());
        assertEquals("DONE", task.getAction());
        assertNotNull(task.getEndTime());

        // 校验记录写入 APPROVE
        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        ApprovalRecordDO record = recordCaptor.getValue();
        assertEquals(501L, record.getInstanceId());
        assertEquals(101L, record.getTaskId());
        assertEquals(loginUserId, record.getOperatorId());
        assertEquals("APPROVE", record.getAction());
        assertEquals("同意签署", record.getComment());

        // 校验实例推进至第2级
        assertEquals(2, instance.getCurrentNodeSort());
        assertEquals("APPROVING", instance.getStatus());
        verify(approvalInstanceMapper).updateById(instance);

        // 校验创建第2级待办（第2级审批人为 200L）
        ArgumentCaptor<ApprovalTaskDO> nextTaskCaptor = ArgumentCaptor.forClass(ApprovalTaskDO.class);
        verify(approvalTaskMapper).insert(nextTaskCaptor.capture());
        ApprovalTaskDO nextTask = nextTaskCaptor.getValue();
        assertEquals(501L, nextTask.getInstanceId());
        assertEquals(2, nextTask.getNodeSort());
        assertEquals(200L, nextTask.getApproverId());
        assertEquals("PENDING", nextTask.getStatus());
    }

    @Test
    @DisplayName("审批通过-终节点逐级通过：流程终态 APPROVED，更新单据状态并发布 ApprovalCompletedEvent")
    void testApproveTask_Sequential_LastNode_Approved() {
        ApprovalProcessSnapshotDTO snapshot = create2NodeSnapshot("SEQUENTIAL");
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .submitterId(300L)
                .status("APPROVING")
                .currentNodeSort(2)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(102L)
                .instanceId(501L)
                .nodeSort(2)
                .nodeName("第2级-终审")
                .nodeType("SEQUENTIAL")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(102L)).thenReturn(task);
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        approvalTaskService.approveTask(102L, null);

        // 校验实例标记为 APPROVED, currentNodeSort = -1
        assertEquals("APPROVED", instance.getStatus());
        assertEquals(-1, instance.getCurrentNodeSort());
        assertNotNull(instance.getEndTime());
        verify(approvalInstanceMapper).updateById(instance);

        // 校验业务单据状态更新为 APPROVED
        verify(salesContractBizHandler).updateApprovalStatus(2001L, "APPROVED");

        // 校验事件发布
        verify(applicationEventPublisher).publishEvent(any(ApprovalCompletedEvent.class));
    }

    @Test
    @DisplayName("审批通过-或签节点任一通过：同节点其他 PENDING 待办自动置 DONE+action=null，节点通过")
    void testApproveTask_OrSign_AnyPass() {
        ApprovalProcessSnapshotDTO snapshot = create2NodeSnapshot("OR_SIGN");
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .submitterId(300L)
                .status("APPROVING")
                .currentNodeSort(1)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO myTask = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-OR_SIGN")
                .nodeType("OR_SIGN")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(myTask);

        ApprovalTaskDO otherTask = ApprovalTaskDO.builder()
                .id(102L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-OR_SIGN")
                .nodeType("OR_SIGN")
                .approverId(101L)
                .status("PENDING")
                .version(0)
                .build();

        when(approvalTaskMapper.selectListByInstanceIdAndNodeSort(501L, 1))
                .thenReturn(List.of(myTask, otherTask));
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        approvalTaskService.approveTask(101L, null);

        // 校验本人任务为 DONE+DONE
        assertEquals("DONE", myTask.getStatus());
        assertEquals("DONE", myTask.getAction());

        // 校验同节点其他任务置 DONE+action=null（自动关闭）
        assertEquals("DONE", otherTask.getStatus());
        assertNull(otherTask.getAction());
        assertNotNull(otherTask.getEndTime());

        // 校验推进下一节点
        assertEquals(2, instance.getCurrentNodeSort());
        verify(approvalInstanceMapper).updateById(instance);
    }

    @Test
    @DisplayName("审批通过-会签节点部分通过：一人通过但仍有他人 PENDING，节点不推进")
    void testApproveTask_AndSign_PartialDone_NotAdvance() {
        ApprovalProcessSnapshotDTO snapshot = create2NodeSnapshot("AND_SIGN");
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .submitterId(300L)
                .status("APPROVING")
                .currentNodeSort(1)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO myTask = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-AND_SIGN")
                .nodeType("AND_SIGN")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(myTask);

        ApprovalTaskDO otherTask = ApprovalTaskDO.builder()
                .id(102L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-AND_SIGN")
                .nodeType("AND_SIGN")
                .approverId(101L)
                .status("PENDING") // 仍有未完成的审批人
                .version(0)
                .build();

        // 模拟当前任务更新后，再次查询该节点全部任务
        when(approvalTaskMapper.selectListByInstanceIdAndNodeSort(501L, 1))
                .thenReturn(List.of(myTask, otherTask));
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        approvalTaskService.approveTask(101L, null);

        // 校验本人任务为 DONE+DONE
        assertEquals("DONE", myTask.getStatus());
        assertEquals("DONE", myTask.getAction());

        // 校验实例依然停留在节点 1，未推进
        assertEquals(1, instance.getCurrentNodeSort());
        assertEquals("APPROVING", instance.getStatus());
        verify(approvalInstanceMapper, never()).updateById(instance);
        verify(approvalTaskMapper, never()).insert(any(ApprovalTaskDO.class));
    }

    @Test
    @DisplayName("审批通过-会签节点全部通过：全员 DONE+DONE 后节点通过，推进下一节点")
    void testApproveTask_AndSign_AllDone_Advance() {
        ApprovalProcessSnapshotDTO snapshot = create2NodeSnapshot("AND_SIGN");
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .submitterId(300L)
                .status("APPROVING")
                .currentNodeSort(1)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO myTask = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-AND_SIGN")
                .nodeType("AND_SIGN")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(myTask);

        ApprovalTaskDO otherTask = ApprovalTaskDO.builder()
                .id(102L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("第1级-AND_SIGN")
                .nodeType("AND_SIGN")
                .approverId(101L)
                .status("DONE")
                .action("DONE")
                .build();

        when(approvalTaskMapper.selectListByInstanceIdAndNodeSort(501L, 1))
                .thenReturn(List.of(myTask, otherTask));
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        approvalTaskService.approveTask(101L, null);

        // 校验全员通过后，实例推进至节点 2
        assertEquals(2, instance.getCurrentNodeSort());
        verify(approvalInstanceMapper).updateById(instance);
        verify(approvalTaskMapper).insert(any(ApprovalTaskDO.class));
    }

    @Test
    @DisplayName("会签并发乐观锁冲突：task.version 不匹配导致更新行数为 0，抛出 1_030_030_004")
    void testApproveTask_OptimisticLockConflict_Throws() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .status("APPROVING")
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(task);

        // 模拟 MyBatis-Plus 乐观锁拦截器因并发版本号冲突返回 0
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalTaskService.approveTask(101L, null));
        assertEquals(APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("终态守卫：实例状态为 APPROVED 时不允许执行任务审批，报 1_030_030_004")
    void testApproveTask_InstanceNotApproving_Throws() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .status("APPROVED") // 已终态
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .approverId(loginUserId)
                .status("PENDING")
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalTaskService.approveTask(101L, null));
        assertEquals(APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("权限守卫：非当前任务 approver_id 用户操作，报 1_030_030_005")
    void testApproveTask_NotApprover_Throws() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .status("APPROVING")
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .approverId(999L) // 其他用户任务
                .status("PENDING")
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalTaskService.approveTask(101L, null));
        assertEquals(APPROVAL_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("任务状态守卫：当前任务已是 DONE 时重复审批，报 1_030_030_004")
    void testApproveTask_TaskAlreadyDone_Throws() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .status("APPROVING")
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .approverId(loginUserId)
                .status("DONE") // 已完成
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalTaskService.approveTask(101L, null));
        assertEquals(APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审批驳回成功：当前任务置 REJECTED，同节点其他待办自动关闭，实例 REJECTED，单据回退 REJECTED")
    void testRejectTask_Success() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .submitterId(300L)
                .status("APPROVING")
                .currentNodeSort(1)
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO myTask = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(myTask);

        ApprovalTaskDO otherTask = ApprovalTaskDO.builder()
                .id(102L)
                .instanceId(501L)
                .nodeSort(1)
                .approverId(101L)
                .status("PENDING")
                .version(0)
                .build();

        when(approvalTaskMapper.selectListByInstanceIdAndNodeSort(501L, 1))
                .thenReturn(List.of(myTask, otherTask));
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        ApprovalTaskRejectReqVO reqVO = new ApprovalTaskRejectReqVO();
        reqVO.setComment("价格不符合毛利标准，打回重议");

        approvalTaskService.rejectTask(101L, reqVO);

        // 校验当前任务 DONE+REJECTED
        assertEquals("DONE", myTask.getStatus());
        assertEquals("REJECTED", myTask.getAction());

        // 校验其他任务 DONE+null
        assertEquals("DONE", otherTask.getStatus());
        assertNull(otherTask.getAction());

        // 校验实例终止为 REJECTED
        assertEquals("REJECTED", instance.getStatus());
        assertEquals(-1, instance.getCurrentNodeSort());
        verify(approvalInstanceMapper).updateById(instance);

        // 校验业务单据回退为 REJECTED
        verify(salesContractBizHandler).updateApprovalStatus(2001L, "REJECTED");

        // 校验记录写入 REJECT
        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        ApprovalRecordDO record = recordCaptor.getValue();
        assertEquals("REJECT", record.getAction());
        assertEquals("价格不符合毛利标准，打回重议", record.getComment());
    }

    @Test
    @DisplayName("审批驳回失败：驳回意见为空报 1_030_030_006")
    void testRejectTask_BlankComment_Throws() {
        ApprovalTaskRejectReqVO reqVO = new ApprovalTaskRejectReqVO();
        reqVO.setComment("   ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalTaskService.rejectTask(101L, reqVO));
        assertEquals(APPROVAL_REJECT_COMMENT_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审批转交成功：当前任务置 TRANSFERRED，新建接收人待办任务，记录 TRANSFER 并保存 transfer_to_id")
    void testTransferTask_Success() {
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .status("APPROVING")
                .currentNodeSort(1)
                .build();
        when(approvalInstanceMapper.selectById(501L)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeSort(1)
                .nodeName("部门主管审批")
                .nodeType("SEQUENTIAL")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(101L)).thenReturn(task);
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);

        ApprovalTaskTransferReqVO reqVO = new ApprovalTaskTransferReqVO();
        reqVO.setToUserId(205L);
        reqVO.setComment("外出公干，转交李组长处理");

        approvalTaskService.transferTask(101L, reqVO);

        // 校验当前任务置 DONE+TRANSFERRED
        assertEquals("DONE", task.getStatus());
        assertEquals("TRANSFERRED", task.getAction());

        // 校验新建待办给接收人 205L
        ArgumentCaptor<ApprovalTaskDO> newTaskCaptor = ArgumentCaptor.forClass(ApprovalTaskDO.class);
        verify(approvalTaskMapper).insert(newTaskCaptor.capture());
        ApprovalTaskDO newTask = newTaskCaptor.getValue();
        assertEquals(501L, newTask.getInstanceId());
        assertEquals(1, newTask.getNodeSort());
        assertEquals(205L, newTask.getApproverId());
        assertEquals("PENDING", newTask.getStatus());
        assertNull(newTask.getAction());

        // 校验记录写入 TRANSFER 与 transfer_to_id
        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        ApprovalRecordDO record = recordCaptor.getValue();
        assertEquals("TRANSFER", record.getAction());
        assertEquals(205L, record.getTransferToId());
        assertEquals("外出公干，转交李组长处理", record.getComment());
    }

    @Test
    @DisplayName("审批转交失败：转交给本人抛出 1_030_030_009")
    void testTransferTask_ToSelf_Throws() {
        ApprovalTaskTransferReqVO reqVO = new ApprovalTaskTransferReqVO();
        reqVO.setToUserId(loginUserId); // 接收人是当前登录人自己

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalTaskService.transferTask(101L, reqVO));
        assertEquals(APPROVAL_TRANSFER_TO_SELF_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("我已审批分页：成功聚合单据、操作记录、操作类型与用户昵称")
    void testGetDonePage_Success() {
        ApprovalTaskDonePageReqVO reqVO = new ApprovalTaskDonePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setAction("APPROVE");

        ApprovalRecordDO record = ApprovalRecordDO.builder()
                .id(301L)
                .instanceId(501L)
                .taskId(101L)
                .operatorId(loginUserId)
                .action("APPROVE")
                .comment("同意通过合同")
                .transferToId(null)
                .operateTime(LocalDateTime.now())
                .build();
        when(approvalRecordMapper.selectDonePage(eq(loginUserId), any()))
                .thenReturn(new PageResult<>(List.of(record), 1L));

        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(501L)
                .bizType("sales_contract")
                .bizId(2001L)
                .bizNo("HT20261010001")
                .status("APPROVED")
                .submitterId(888L)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now())
                .build();
        when(approvalInstanceMapper.selectBatchIds(any())).thenReturn(List.of(instance));

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(101L)
                .instanceId(501L)
                .nodeName("财务审核")
                .approverId(loginUserId)
                .status("DONE")
                .action("DONE")
                .build();
        when(approvalTaskMapper.selectBatchIds(any())).thenReturn(List.of(task));

        AdminUserRespDTO opUser = new AdminUserRespDTO();
        opUser.setId(loginUserId);
        opUser.setNickname("审批财务专员");
        AdminUserRespDTO subUser = new AdminUserRespDTO();
        subUser.setId(888L);
        subUser.setNickname("销售小李");
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Map.of(loginUserId, opUser, 888L, subUser));

        PageResult<ApprovalTaskDoneRespVO> result = approvalTaskService.getDonePage(reqVO);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        ApprovalTaskDoneRespVO respVO = result.getList().get(0);
        assertEquals(301L, respVO.getId());
        assertEquals(501L, respVO.getInstanceId());
        assertEquals(101L, respVO.getTaskId());
        assertEquals("sales_contract", respVO.getBizType());
        assertEquals("销售合同", respVO.getBizTypeName());
        assertEquals("HT20261010001", respVO.getBizNo());
        assertEquals("APPROVED", respVO.getInstanceStatus());
        assertEquals("销售小李", respVO.getSubmitterName());
        assertEquals("审批财务专员", respVO.getOperatorName());
        assertEquals("APPROVE", respVO.getAction());
        assertEquals("通过", respVO.getActionName());
        assertEquals("同意通过合同", respVO.getComment());
        assertEquals("财务审核", respVO.getNodeName());
    }

    @Test
    @DisplayName("我已审批分页：未登录拦截抛 1_030_030_005")
    void testGetDonePage_NotLoggedIn_Throws() {
        SecurityContextHolder.clearContext();
        ApprovalTaskDonePageReqVO reqVO = new ApprovalTaskDonePageReqVO();
        ServiceException ex = assertThrows(ServiceException.class, () -> approvalTaskService.getDonePage(reqVO));
        assertEquals(APPROVAL_NO_PERMISSION.getCode(), ex.getCode());
    }

}
