package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskSaveRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.service.production.loom.LoomService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link MachineTaskServiceImpl} 机台任务同织机时段冲突硬校验测试
 *
 * 覆盖 Issue 02 / Testing Decisions 1-6 全部场景：
 * 1. 新建任务与同织机 PENDING 任务闭区间相交（含同日）→ 000，消息含冲突任务号；事务回滚；
 * 2. 不相交/不同织机/他任务 COMPLETED/update 排除自身 → 通过；
 * 3. 仅填一个日期：按 coalesce 单日区间判定；两端皆空 → 不校验；
 * 4. 与 HALTED、IN_PROGRESS 任务相交均拦截；
 * 5. update COMPLETED 任务本身仍被既有守卫拦截（102）；
 * 6. 超量软警告仍返回 warning 且不拦截（回归既有行为）。
 */
class MachineTaskConflictTest {

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private LoomService loomService;

    @InjectMocks
    private MachineTaskServiceImpl machineTaskService;

    private WeavingOrderDO defaultOrder;
    private LoomDO defaultLoom;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), MachineTaskDO.class);

        defaultOrder = new WeavingOrderDO();
        defaultOrder.setId(100L);
        defaultOrder.setCode("ZZ-PB0919001-01");
        defaultOrder.setStatus("DRAFT");
        defaultOrder.setPlannedQty(100);

        defaultLoom = new LoomDO();
        defaultLoom.setId(200L);
        defaultLoom.setLoomNo("LM20260001");
        defaultLoom.setStatus("ACTIVE");

        when(weavingOrderMapper.selectById(100L)).thenReturn(defaultOrder);
        when(loomService.validateLoomActive(200L)).thenReturn(defaultLoom);
    }

    // ==================== Decision 1: 同织机 PENDING 闭区间相交拦截 ====================

    @Test
    @DisplayName("D1-1: 新建任务与同织机 PENDING 任务同日相交 → 抛 1_030_036_000 且不插入")
    void testCreate_conflictWithPendingTask_sameDay_throwsConflictError() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));

        MachineTaskDO existingTask = buildTask(1L, "MT-EXIST-01", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-EXIST-01"));
        assertTrue(ex.getMessage().contains("LM20260001"));
        assertTrue(ex.getMessage().contains("2026-10-10"));
        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D1-2: 新建任务与同织机 PENDING 任务边界日期重叠（闭区间相交）→ 拦截")
    void testCreate_conflictWithPendingTask_boundaryOverlap_throwsConflictError() {
        // 新任务 [2026-10-10, 2026-10-15]，老任务 [2026-10-05, 2026-10-10]，在 10-10 重叠
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));

        MachineTaskDO existingTask = buildTask(1L, "MT-EXIST-02", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-EXIST-02"));
        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D1-3: 新建任务落在同织机 PENDING 任务区间内 → 拦截")
    void testCreate_conflictWithPendingTask_internalOverlap_throwsConflictError() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 8));

        MachineTaskDO existingTask = buildTask(1L, "MT-EXIST-03", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-EXIST-03"));
        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    // ==================== Decision 2: 不相交 / 不同织机 / COMPLETED / update 排除自身 ====================

    @Test
    @DisplayName("D2-1: 新建任务与同织机已有任务时段不相交（前后紧邻但不重合）→ 成功")
    void testCreate_noConflict_disjointDates_success() {
        // 新任务 [2026-10-06, 2026-10-14]
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 14));

        MachineTaskDO beforeTask = buildTask(1L, "MT-BEFORE", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        MachineTaskDO afterTask = buildTask(2L, "MT-AFTER", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(beforeTask, afterTask));

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D2-2: 不同织机时段相同 → 不冲突")
    void testCreate_noConflict_differentLoom_success() {
        // 新任务在织机 200L，库内该织机无占用任务
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(Collections.emptyList());

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D2-3: 同织机已完工（COMPLETED）任务时段重叠 → 不拦截新排")
    void testCreate_noConflict_existingTaskCompleted_success() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));

        MachineTaskDO completedTask = buildTask(1L, "MT-DONE", 200L, "LM20260001", "COMPLETED",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(completedTask));

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D2-4: update 任务修改自身信息且时段不变 → 排除自身不误报冲突")
    void testUpdate_excludeSelf_sameDates_success() {
        MachineTaskDO exist = buildTask(10L, "MT-SELF", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectById(10L)).thenReturn(exist);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(exist));

        MachineTaskSaveReqVO updateReq = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        updateReq.setId(10L);
        updateReq.setCode("MT-SELF");
        updateReq.setPlannedQty(60);

        MachineTaskSaveRespVO resp = machineTaskService.updateMachineTask(updateReq);

        assertNotNull(resp);
        verify(machineTaskMapper).updateById(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D2-5: update 任务改期后与同织机他任务冲突 → 拦截且不更新")
    void testUpdate_conflictWithOtherTask_throwsConflictError() {
        MachineTaskDO self = buildTask(10L, "MT-SELF", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        MachineTaskDO other = buildTask(20L, "MT-OTHER", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectById(10L)).thenReturn(self);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(self, other));

        // 将 self 改期到 [10-12, 10-18]，与 other [10-10, 10-15] 冲突
        MachineTaskSaveReqVO updateReq = buildBaseReq(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));
        updateReq.setId(10L);
        updateReq.setCode("MT-SELF");

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.updateMachineTask(updateReq));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-OTHER"));
        verify(machineTaskMapper, never()).updateById(any(MachineTaskDO.class));
    }

    // ==================== Decision 3: coalesce 单端日期与两端皆空 ====================

    @Test
    @DisplayName("D3-1: 本次提交仅填 plannedStart → 按 coalesce 单日判定，与老任务相交拦截")
    void testCreate_singleDateCoalesce_newPlannedStartOnly_conflict() {
        // 新任务仅填 10-10 -> [10-10, 10-10]
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), null);

        MachineTaskDO existingTask = buildTask(1L, "MT-EXIST-04", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-EXIST-04"));
    }

    @Test
    @DisplayName("D3-2: 本次提交仅填 plannedEnd → 按 coalesce 单日判定，与老任务相交拦截")
    void testCreate_singleDateCoalesce_newPlannedEndOnly_conflict() {
        // 新任务仅填 10-10 -> [10-10, 10-10]
        MachineTaskSaveReqVO req = buildBaseReq(null, LocalDate.of(2026, 10, 10));

        MachineTaskDO existingTask = buildTask(1L, "MT-EXIST-05", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-EXIST-05"));
    }

    @Test
    @DisplayName("D3-3: 库内任务仅填一端日期 → coalesce 单日判定，相交拦截")
    void testCreate_singleDateCoalesce_existingTaskSingleDate_conflict() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 11));

        // 老任务仅填 plannedStart 10-10 -> [10-10, 10-10]
        MachineTaskDO existingTask = buildTask(1L, "MT-EXIST-06", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 10), null);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-EXIST-06"));
    }

    @Test
    @DisplayName("D3-4: 本次提交起止皆空 → 跳过校验，允许创建（进待排池）")
    void testCreate_bothDatesNull_skipsCheck_success() {
        MachineTaskSaveReqVO req = buildBaseReq(null, null);

        MachineTaskDO existingTask = buildTask(1L, "MT-OCCUPIED", 200L, "LM20260001", "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(existingTask));

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
        // 两端皆空直接跳过，甚至无需查询 occupied tasks
        verify(machineTaskMapper, never()).selectOccupiedTasksByLoomId(any());
    }

    @Test
    @DisplayName("D3-5: 库内任务起止皆空 → 视为未排时段，不拦截本次有日期的提交")
    void testCreate_existingTaskBothDatesNull_skipped_success() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));

        MachineTaskDO unDatedTask = buildTask(1L, "MT-UNDATED", 200L, "LM20260001", "PENDING",
                null, null);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(unDatedTask));

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    // ==================== Decision 4: HALTED / IN_PROGRESS 拦截 ====================

    @Test
    @DisplayName("D4-1: 与 HALTED（停机）任务相交 → 拦截")
    void testCreate_conflictWithHaltedTask_throwsConflictError() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));

        MachineTaskDO haltedTask = buildTask(1L, "MT-HALTED-01", 200L, "LM20260001", "HALTED",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(haltedTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-HALTED-01"));
        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("D4-2: 与 IN_PROGRESS（生产中）任务相交 → 拦截")
    void testCreate_conflictWithInProgressTask_throwsConflictError() {
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));

        MachineTaskDO inProgressTask = buildTask(1L, "MT-INPROG-01", 200L, "LM20260001", "IN_PROGRESS",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(List.of(inProgressTask));

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_LOOM_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-INPROG-01"));
        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    // ==================== Decision 5: update COMPLETED 任务被既有守卫拦截 ====================

    @Test
    @DisplayName("D5-1: 修改已完工（COMPLETED）任务自身 → 被既有状态守卫 102 拦截")
    void testUpdate_completedTaskItself_blockedByExistingGuard() {
        MachineTaskDO completedTask = buildTask(10L, "MT-COMPLETED", 200L, "LM20260001", "COMPLETED",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        when(machineTaskMapper.selectById(10L)).thenReturn(completedTask);

        MachineTaskSaveReqVO updateReq = buildBaseReq(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        updateReq.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class, () -> machineTaskService.updateMachineTask(updateReq));

        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getCode(), ex.getCode());
        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getMsg(), ex.getMessage());
        verify(machineTaskMapper, never()).selectOccupiedTasksByLoomId(any());
    }

    // ==================== Decision 6: 超量仍为软警告不拦截 ====================

    @Test
    @DisplayName("D6-1: 时段无冲突但计划匹数超量 → 返回 warning 软警告，不拦截保存")
    void testCreate_overPlannedQtyWarningStillReturned_notBlocked() {
        // defaultOrder.plannedQty = 100
        // 织造单内已有任务已排 80 匹
        MachineTaskDO existingActive = new MachineTaskDO();
        existingActive.setId(101L);
        existingActive.setPlannedQty(80);
        when(machineTaskMapper.selectActiveListByWeavingOrderId(100L)).thenReturn(List.of(existingActive));

        // 本次排 30 匹 (80 + 30 = 110 > 100)，时段无冲突
        MachineTaskSaveReqVO req = buildBaseReq(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15));
        req.setPlannedQty(30);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L)).thenReturn(Collections.emptyList());

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        assertNotNull(resp.getWarning());
        assertTrue(resp.getWarning().contains("超出织造单计划匹数"));
        assertTrue(resp.getWarning().contains("110"));
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    // ==================== 辅助方法 ====================

    private MachineTaskSaveReqVO buildBaseReq(LocalDate start, LocalDate end) {
        MachineTaskSaveReqVO req = new MachineTaskSaveReqVO();
        req.setWeavingOrderId(100L);
        req.setLoomId(200L);
        req.setPlannedQty(50);
        req.setPlannedStart(start);
        req.setPlannedEnd(end);
        return req;
    }

    private MachineTaskDO buildTask(Long id, String code, Long loomId, String loomNo, String status,
                                    LocalDate start, LocalDate end) {
        MachineTaskDO task = new MachineTaskDO();
        task.setId(id);
        task.setCode(code);
        task.setWeavingOrderId(100L);
        task.setLoomId(loomId);
        task.setLoomNo(loomNo);
        task.setStatus(status);
        task.setPlannedStart(start);
        task.setPlannedEnd(end);
        task.setPlannedQty(50);
        return task;
    }

}
