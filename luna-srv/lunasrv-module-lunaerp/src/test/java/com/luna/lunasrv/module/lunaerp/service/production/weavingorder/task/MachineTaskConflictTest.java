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

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.MACHINE_TASK_STATUS_NOT_ALLOW;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.MACHINE_TASK_TIME_CONFLICT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 生产管理 - 机台任务时段冲突硬校验测试（Issue 02）
 *
 * 覆盖场景：
 * 1. 新建任务与同织机 PENDING 任务闭区间相交（含同日/多日相交）报错且不保存，消息含冲突任务号；
 * 2. 单端日期 coalesce 判定，两端皆空跳过校验；
 * 3. 不相交/不同织机/他任务 COMPLETED 通过；
 * 4. update 排除自身通过，与他任务冲突拦截；
 * 5. 与 HALTED、IN_PROGRESS 任务相交均拦截；
 * 6. COMPLETED 任务自身修改仍被既有守卫拦截；
 * 7. 超量软警告仍返回 warning 且不拦截（回归既有行为）。
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
        defaultOrder.setCode("ZZ2026101401");
        defaultOrder.setStatus("ACTIVE");
        defaultOrder.setPlannedQty(100);

        defaultLoom = new LoomDO();
        defaultLoom.setId(200L);
        defaultLoom.setLoomNo("LM-01");
        defaultLoom.setStatus("ACTIVE");

        when(weavingOrderMapper.selectById(100L)).thenReturn(defaultOrder);
        when(loomService.validateLoomActive(200L)).thenReturn(defaultLoom);
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(machineTaskMapper.selectByCodeIncludingDeleted(any())).thenReturn(null);
    }

    // ==================== 1. 新建冲突拦截场景 ====================

    @Test
    @DisplayName("新建任务：与同织机 PENDING 任务同日闭区间相交 -> 抛 1_030_036_000 错误，消息含织机、冲突任务号与起止")
    void testCreate_conflictWithPendingTask_sameDay_throwsConflict() {
        MachineTaskDO existingTask = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));

        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(existingTask));

        MachineTaskSaveReqVO req = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));

        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-ZZ01-01"), "消息应包含冲突任务号");
        assertTrue(ex.getMessage().contains("LM-01"), "消息应包含织机号");
        assertTrue(ex.getMessage().contains("2026-10-10"), "消息应包含冲突起止日期");
        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("新建任务：与已有任务起止边界相交（含首尾同日） -> 抛 1_030_036_000 错误")
    void testCreate_conflictWithBoundaryOverlap_throwsConflict() {
        // 库内任务 10-08 ~ 10-12
        MachineTaskDO existingTask = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));

        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(existingTask));

        // 新建任务 10-12 ~ 10-15（右端点重合）
        MachineTaskSaveReqVO req1 = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 15));
        ServiceException ex1 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req1));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex1.getCode());

        // 新建任务 10-05 ~ 10-08（左端点重合）
        MachineTaskSaveReqVO req2 = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8));
        ServiceException ex2 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req2));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex2.getCode());

        // 新建任务 10-09 ~ 10-11（完全落在库内任务区间内）
        MachineTaskSaveReqVO req3 = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 11));
        ServiceException ex3 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(req3));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex3.getCode());

        verify(machineTaskMapper, never()).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("新建任务：与 HALTED 或 IN_PROGRESS 任务相交 -> 均拦截抛 1_030_036_000")
    void testCreate_conflictWithHaltedAndInProgressTasks_throwsConflict() {
        MachineTaskDO haltedTask = buildTask(10L, "MT-ZZ01-HALTED", 200L, "LM-01", "HALTED",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(haltedTask));

        MachineTaskSaveReqVO reqHalted = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14));
        ServiceException ex1 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(reqHalted));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex1.getCode());
        assertTrue(ex1.getMessage().contains("MT-ZZ01-HALTED"));

        MachineTaskDO inProgressTask = buildTask(11L, "MT-ZZ01-INPROG", 200L, "LM-01", "IN_PROGRESS",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(inProgressTask));

        MachineTaskSaveReqVO reqInProg = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14));
        ServiceException ex2 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(reqInProg));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex2.getCode());
        assertTrue(ex2.getMessage().contains("MT-ZZ01-INPROG"));
    }

    // ==================== 2. 日期 coalesce 与两端皆空跳过 ====================

    @Test
    @DisplayName("日期 coalesce：仅一端填日期按单日闭区间判定冲突")
    void testCreate_coalesceSingleDate_conflicts() {
        MachineTaskDO existingTask = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(existingTask));

        // 本次提交仅填 plannedStart=10-10，plannedEnd 为空 -> eff=[10-10, 10-10]，与 10-08~10-12 相交
        MachineTaskSaveReqVO reqStartOnly = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 10), null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(reqStartOnly));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex1.getCode());

        // 本次提交仅填 plannedEnd=10-10，plannedStart 为空 -> eff=[10-10, 10-10]，与 10-08~10-12 相交
        MachineTaskSaveReqVO reqEndOnly = buildCreateReq(200L, 20, null, LocalDate.of(2026, 10, 10));
        ServiceException ex2 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(reqEndOnly));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex2.getCode());

        // 库内任务仅填 plannedStart=10-10，本次提交 10-08~10-12 -> 同样相交冲突
        MachineTaskDO existStartOnly = buildTask(11L, "MT-ZZ01-02", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 10), null);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(existStartOnly));

        MachineTaskSaveReqVO reqFull = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        ServiceException ex3 = assertThrows(ServiceException.class, () -> machineTaskService.createMachineTask(reqFull));
        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex3.getCode());
    }

    @Test
    @DisplayName("两端皆空：plannedStart 与 plannedEnd 均为空 -> 跳过冲突校验成功创建")
    void testCreate_bothDatesNull_skipsConflictCheck_success() {
        // 库内即使有占用态任务
        MachineTaskDO existingTask = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(existingTask));

        MachineTaskSaveReqVO req = buildCreateReq(200L, 20, null, null);
        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    // ==================== 3. 正常通过场景（不相交/不同织机/COMPLETED） ====================

    @Test
    @DisplayName("正常通过：时段不相交 -> 成功创建")
    void testCreate_nonOverlapping_success() {
        MachineTaskDO existingTask = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(existingTask));

        // 10-13 ~ 10-16 紧随其后不重叠
        MachineTaskSaveReqVO req = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 16));
        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("正常通过：不同织机 -> 成功创建")
    void testCreate_differentLoom_success() {
        // 织机 201
        LoomDO loom201 = new LoomDO();
        loom201.setId(201L);
        loom201.setLoomNo("LM-02");
        loom201.setStatus("ACTIVE");
        when(loomService.validateLoomActive(201L)).thenReturn(loom201);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(201L, null)).thenReturn(Collections.emptyList());

        MachineTaskSaveReqVO req = buildCreateReq(201L, 20, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("正常通过：库内同织机任务为 COMPLETED -> 不拦截新排")
    void testCreate_completedTaskOnSameLoom_doesNotBlock() {
        MachineTaskDO completedTask = buildTask(10L, "MT-ZZ01-COMPLETED", 200L, "LM-01", "COMPLETED",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null))
                .thenReturn(List.of(completedTask));

        MachineTaskSaveReqVO req = buildCreateReq(200L, 20, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    // ==================== 4. 更新操作冲突守卫 ====================

    @Test
    @DisplayName("更新任务：修改自身信息，日期与自身相交 -> 排除自身，通过")
    void testUpdate_excludeSelf_success() {
        MachineTaskDO exist = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectById(10L)).thenReturn(exist);
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, 10L)).thenReturn(Collections.emptyList());

        MachineTaskSaveReqVO updateReq = new MachineTaskSaveReqVO();
        updateReq.setId(10L);
        updateReq.setWeavingOrderId(100L);
        updateReq.setLoomId(200L);
        updateReq.setPlannedQty(30);
        updateReq.setPlannedStart(LocalDate.of(2026, 10, 8));
        updateReq.setPlannedEnd(LocalDate.of(2026, 10, 14)); // 改动结束日期

        MachineTaskSaveRespVO resp = machineTaskService.updateMachineTask(updateReq);

        assertNotNull(resp);
        verify(machineTaskMapper).updateById(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("更新任务：改期后与同织机另一任务冲突 -> 抛 1_030_036_000 拦截")
    void testUpdate_conflictWithOtherTask_throwsConflict() {
        MachineTaskDO exist = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        when(machineTaskMapper.selectById(10L)).thenReturn(exist);

        MachineTaskDO otherTask = buildTask(20L, "MT-ZZ01-02", 200L, "LM-01", "PENDING",
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, 10L)).thenReturn(List.of(otherTask));

        MachineTaskSaveReqVO updateReq = new MachineTaskSaveReqVO();
        updateReq.setId(10L);
        updateReq.setWeavingOrderId(100L);
        updateReq.setLoomId(200L);
        updateReq.setPlannedQty(30);
        updateReq.setPlannedStart(LocalDate.of(2026, 10, 8)); // 改到与任务20重叠
        updateReq.setPlannedEnd(LocalDate.of(2026, 10, 10));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.updateMachineTask(updateReq));

        assertEquals(MACHINE_TASK_TIME_CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("MT-ZZ01-02"));
        verify(machineTaskMapper, never()).updateById(any(MachineTaskDO.class));
    }

    @Test
    @DisplayName("更新任务：COMPLETED 任务自身修改仍被既有守卫拦截")
    void testUpdate_completedTaskSelf_blockedByExistingGuard() {
        MachineTaskDO exist = buildTask(10L, "MT-ZZ01-01", 200L, "LM-01", "COMPLETED",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        when(machineTaskMapper.selectById(10L)).thenReturn(exist);

        MachineTaskSaveReqVO updateReq = new MachineTaskSaveReqVO();
        updateReq.setId(10L);
        updateReq.setWeavingOrderId(100L);
        updateReq.setLoomId(200L);
        updateReq.setPlannedQty(30);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.updateMachineTask(updateReq));

        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 5. 超量软警告回归 ====================

    @Test
    @DisplayName("超量软警告回归：未冲突但单内总计划匹数超织造单 -> 返回 warning 且不拦截")
    void testCreate_softWarning_returnsWarning_notBlocked() {
        when(machineTaskMapper.selectOccupiedTasksByLoomId(200L, null)).thenReturn(Collections.emptyList());

        // 织造单 plannedQty = 100，已有任务 60 匹，新增 50 匹 -> 合计 110 匹
        MachineTaskDO existingInOrder = new MachineTaskDO();
        existingInOrder.setId(1L);
        existingInOrder.setPlannedQty(60);
        when(machineTaskMapper.selectActiveListByWeavingOrderId(100L))
                .thenReturn(List.of(existingInOrder));

        MachineTaskSaveReqVO req = buildCreateReq(200L, 50, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12));
        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        assertNotNull(resp.getWarning(), "应返回超量软警告");
        assertTrue(resp.getWarning().contains("110"));
        assertTrue(resp.getWarning().contains("100"));
        verify(machineTaskMapper).insert(any(MachineTaskDO.class));
    }

    // ==================== 辅助方法 ====================

    private MachineTaskDO buildTask(Long id, String code, Long loomId, String loomNo, String status,
                                    LocalDate start, LocalDate end) {
        MachineTaskDO task = new MachineTaskDO();
        task.setId(id);
        task.setCode(code);
        task.setWeavingOrderId(100L);
        task.setLoomId(loomId);
        task.setLoomNo(loomNo);
        task.setStatus(status);
        task.setPlannedQty(20);
        task.setPlannedStart(start);
        task.setPlannedEnd(end);
        return task;
    }

    private MachineTaskSaveReqVO buildCreateReq(Long loomId, int plannedQty, LocalDate start, LocalDate end) {
        MachineTaskSaveReqVO req = new MachineTaskSaveReqVO();
        req.setWeavingOrderId(100L);
        req.setLoomId(loomId);
        req.setPlannedQty(plannedQty);
        req.setPlannedStart(start);
        req.setPlannedEnd(end);
        return req;
    }

}
