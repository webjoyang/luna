package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskHaltReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MachineTaskServiceImpl} 单元测试
 *
 * 覆盖 Issue 04 核心规格：
 * 1. 编号派生（MT-{织造单号去符号}-{单内 2 位序号}）、并发重试与手填查重；
 * 2. 关联织造单校验（存在性、未取消、未完工）；
 * 3. 关联织机校验（存在且仅 ACTIVE 可选）与机台号快照；
 * 4. 计划匹数正整数校验与排期（end ≥ start）校验；
 * 5. 单内机台任务 plannedQty 超量软警告；
 * 6. 更新操作（排期/挡车工/匹数/织机）与状态守卫；
 * 7. 停机（haltReason 必填、记录 haltAt、已完工禁停）与恢复（恢复为 PENDING）；
 * 8. 删除守卫（仅 PENDING 态允许）；
 * 9. 协同：LoomTaskReferenceProvider 与 WeavingOrderCascadeDeleteListener；
 * 10. 分页与详情查询。
 *
 * @author luna
 */
class MachineTaskServiceImplTest {

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private LoomService loomService;

    @InjectMocks
    private MachineTaskServiceImpl machineTaskService;

    @InjectMocks
    private MachineTaskLoomReferenceProviderImpl loomReferenceProvider;

    @InjectMocks
    private MachineTaskCascadeDeleteListenerImpl cascadeDeleteListener;

    private WeavingOrderDO defaultOrder;
    private LoomDO defaultLoom;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), MachineTaskDO.class);

        ReflectionTestUtils.setField(loomReferenceProvider, "machineTaskMapper", machineTaskMapper);
        ReflectionTestUtils.setField(cascadeDeleteListener, "machineTaskService", machineTaskService);

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

    // ==================== 1. 编号派生与重试 ====================

    @Test
    void testCreate_autoGenerateCode_firstTask() {
        MachineTaskSaveReqVO req = buildBaseReq();
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted("MT-ZZPB091900101-"))
                .thenReturn(Collections.emptyList());
        when(machineTaskMapper.selectByCodeIncludingDeleted("MT-ZZPB091900101-01")).thenReturn(null);

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertEquals("MT-ZZPB091900101-01", resp.getCode());
        ArgumentCaptor<MachineTaskDO> captor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).insert(captor.capture());
        MachineTaskDO inserted = captor.getValue();
        assertEquals("MT-ZZPB091900101-01", inserted.getCode());
        assertEquals("LM20260001", inserted.getLoomNo());
        assertEquals("PENDING", inserted.getStatus());
        assertEquals(0, inserted.getActualQty());
    }

    @Test
    void testCreate_autoGenerateCode_increment() {
        MachineTaskSaveReqVO req = buildBaseReq();
        MachineTaskDO existing = new MachineTaskDO();
        existing.setCode("MT-ZZPB091900101-01");
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted("MT-ZZPB091900101-"))
                .thenReturn(Collections.singletonList(existing));
        when(machineTaskMapper.selectByCodeIncludingDeleted("MT-ZZPB091900101-02")).thenReturn(null);

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertEquals("MT-ZZPB091900101-02", resp.getCode());
    }

    @Test
    void testCreate_autoGenerateCode_retryOnCollision() {
        MachineTaskSaveReqVO req = buildBaseReq();
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted("MT-ZZPB091900101-"))
                .thenReturn(Collections.emptyList());
        // 第一次顺延 01 撞号，第二次 02 成功
        MachineTaskDO collision = new MachineTaskDO();
        collision.setId(999L);
        when(machineTaskMapper.selectByCodeIncludingDeleted("MT-ZZPB091900101-01")).thenReturn(collision);
        when(machineTaskMapper.selectByCodeIncludingDeleted("MT-ZZPB091900101-02")).thenReturn(null);

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertEquals("MT-ZZPB091900101-02", resp.getCode());
    }

    @Test
    void testCreate_autoGenerateCode_maxRetryExceeded_throws() {
        MachineTaskSaveReqVO req = buildBaseReq();
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted("MT-ZZPB091900101-"))
                .thenReturn(Collections.emptyList());
        MachineTaskDO collision = new MachineTaskDO();
        collision.setId(999L);
        when(machineTaskMapper.selectByCodeIncludingDeleted(any())).thenReturn(collision);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(MACHINE_TASK_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void testCreate_manualCode_duplicate_throws() {
        MachineTaskSaveReqVO req = buildBaseReq();
        req.setCode("MT-CUSTOM-01");
        MachineTaskDO duplicate = new MachineTaskDO();
        duplicate.setId(888L);
        when(machineTaskMapper.selectByCodeIncludingDeleted("MT-CUSTOM-01")).thenReturn(duplicate);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(MACHINE_TASK_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void testGetNextMachineTaskCode() {
        MachineTaskDO existing = new MachineTaskDO();
        existing.setCode("MT-ZZPB091900101-02");
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted("MT-ZZPB091900101-"))
                .thenReturn(Collections.singletonList(existing));

        String nextCode = machineTaskService.getNextMachineTaskCode(100L);
        assertEquals("MT-ZZPB091900101-03", nextCode);
    }

    // ==================== 2. 创建校验：织造单、织机、匹数、排期 ====================

    @Test
    void testCreate_weavingOrderNotExists_throws() {
        MachineTaskSaveReqVO req = buildBaseReq();
        when(weavingOrderMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(WEAVING_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testCreate_weavingOrderCancelled_throws() {
        defaultOrder.setStatus("CANCELLED");
        MachineTaskSaveReqVO req = buildBaseReq();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCreate_weavingOrderCompleted_throws() {
        defaultOrder.setStatus("COMPLETED");
        MachineTaskSaveReqVO req = buildBaseReq();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCreate_loomNotActive_throws() {
        when(loomService.validateLoomActive(200L))
                .thenThrow(new ServiceException(LOOM_NOT_EXISTS.getCode(), LOOM_NOT_EXISTS.getMsg()));
        MachineTaskSaveReqVO req = buildBaseReq();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(LOOM_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testCreate_plannedQtyZeroOrNegative_throws() {
        MachineTaskSaveReqVO req = buildBaseReq();
        req.setPlannedQty(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(MACHINE_TASK_PLANNED_QTY_INVALID.getCode(), ex.getCode());
    }

    @Test
    void testCreate_dateRangeEndBeforeStart_throws() {
        MachineTaskSaveReqVO req = buildBaseReq();
        req.setPlannedStart(LocalDate.of(2026, 9, 28));
        req.setPlannedEnd(LocalDate.of(2026, 9, 26));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.createMachineTask(req));
        assertEquals(MACHINE_TASK_DATE_RANGE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void testCreate_success_withWorkerAndLoomSnapshot() {
        MachineTaskSaveReqVO req = buildBaseReq();
        req.setWorkerUserId(10L);
        req.setWorkerName("李师傅");
        req.setPlannedStart(LocalDate.of(2026, 9, 26));
        req.setPlannedEnd(LocalDate.of(2026, 9, 28));
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(machineTaskMapper.selectByCodeIncludingDeleted(any())).thenReturn(null);

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp);
        ArgumentCaptor<MachineTaskDO> captor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).insert(captor.capture());
        MachineTaskDO val = captor.getValue();
        assertEquals(10L, val.getWorkerUserId());
        assertEquals("李师傅", val.getWorkerName());
        assertEquals("LM20260001", val.getLoomNo());
        assertEquals(50, val.getPlannedQty());
        assertEquals("PENDING", val.getStatus());
        assertEquals(0, val.getActualQty());
    }

    // ==================== 3. 软警告：单内任务计划匹数超织造单 ====================

    @Test
    void testCreate_softWarning_whenTotalPlannedQtyExceedsOrder() {
        MachineTaskSaveReqVO req = buildBaseReq();
        req.setPlannedQty(60);

        MachineTaskDO existingTask = new MachineTaskDO();
        existingTask.setId(1L);
        existingTask.setPlannedQty(50);
        when(machineTaskMapper.selectActiveListByWeavingOrderId(100L))
                .thenReturn(Collections.singletonList(existingTask));
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(machineTaskMapper.selectByCodeIncludingDeleted(any())).thenReturn(null);

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNotNull(resp.getWarning());
        assertTrue(resp.getWarning().contains("110"));
        assertTrue(resp.getWarning().contains("100"));
    }

    @Test
    void testCreate_noWarning_whenWithinLimit() {
        MachineTaskSaveReqVO req = buildBaseReq();
        req.setPlannedQty(40);

        MachineTaskDO existingTask = new MachineTaskDO();
        existingTask.setId(1L);
        existingTask.setPlannedQty(50);
        when(machineTaskMapper.selectActiveListByWeavingOrderId(100L))
                .thenReturn(Collections.singletonList(existingTask));
        when(machineTaskMapper.selectListByCodePrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(machineTaskMapper.selectByCodeIncludingDeleted(any())).thenReturn(null);

        MachineTaskSaveRespVO resp = machineTaskService.createMachineTask(req);

        assertNull(resp.getWarning());
    }

    // ==================== 4. 修改与状态守卫 ====================

    @Test
    void testUpdate_success() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setCode("MT-ZZPB091900101-01");
        exist.setWeavingOrderId(100L);
        exist.setLoomId(200L);
        exist.setLoomNo("LM20260001");
        exist.setStatus("PENDING");
        exist.setPlannedQty(50);
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        LoomDO newLoom = new LoomDO();
        newLoom.setId(201L);
        newLoom.setLoomNo("LM20260002");
        newLoom.setStatus("ACTIVE");
        when(loomService.validateLoomActive(201L)).thenReturn(newLoom);

        MachineTaskSaveReqVO updateReq = new MachineTaskSaveReqVO();
        updateReq.setId(1L);
        updateReq.setWeavingOrderId(100L);
        updateReq.setLoomId(201L);
        updateReq.setPlannedQty(80);
        updateReq.setPlannedStart(LocalDate.of(2026, 9, 27));
        updateReq.setPlannedEnd(LocalDate.of(2026, 9, 29));
        updateReq.setWorkerUserId(12L);
        updateReq.setWorkerName("王五");
        updateReq.setRemark("改用2号机");

        MachineTaskSaveRespVO resp = machineTaskService.updateMachineTask(updateReq);

        assertNotNull(resp);
        assertEquals("MT-ZZPB091900101-01", resp.getCode());
        ArgumentCaptor<MachineTaskDO> captor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(captor.capture());
        MachineTaskDO updated = captor.getValue();
        assertEquals("LM20260002", updated.getLoomNo());
        assertEquals(80, updated.getPlannedQty());
        assertEquals("王五", updated.getWorkerName());
        assertEquals(12L, updated.getWorkerUserId());
    }

    @Test
    void testUpdate_completedTask_throws() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("COMPLETED");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        MachineTaskSaveReqVO req = buildBaseReq();
        req.setId(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.updateMachineTask(req));
        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testUpdate_dateRangeInvalid_throws() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("PENDING");
        exist.setWeavingOrderId(100L);
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        MachineTaskSaveReqVO req = buildBaseReq();
        req.setId(1L);
        req.setPlannedStart(LocalDate.of(2026, 9, 30));
        req.setPlannedEnd(LocalDate.of(2026, 9, 20));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.updateMachineTask(req));
        assertEquals(MACHINE_TASK_DATE_RANGE_INVALID.getCode(), ex.getCode());
    }

    // ==================== 5. 停机与恢复 ====================

    @Test
    void testHalt_success() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("IN_PROGRESS");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        MachineTaskHaltReqVO haltReq = new MachineTaskHaltReqVO(1L, "电机过热待修");
        machineTaskService.haltMachineTask(haltReq);

        ArgumentCaptor<MachineTaskDO> captor = ArgumentCaptor.forClass(MachineTaskDO.class);
        verify(machineTaskMapper).updateById(captor.capture());
        MachineTaskDO updated = captor.getValue();
        assertEquals("HALTED", updated.getStatus());
        assertEquals("电机过热待修", updated.getHaltReason());
        assertNotNull(updated.getHaltAt());
    }

    @Test
    void testHalt_completedTask_throws() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("COMPLETED");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        MachineTaskHaltReqVO haltReq = new MachineTaskHaltReqVO(1L, "电机过热待修");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.haltMachineTask(haltReq));
        assertEquals(MACHINE_TASK_COMPLETED_CANNOT_HALT.getCode(), ex.getCode());
    }

    @Test
    void testHalt_emptyReason_throws() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("IN_PROGRESS");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        MachineTaskHaltReqVO haltReq = new MachineTaskHaltReqVO(1L, "   ");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.haltMachineTask(haltReq));
        assertEquals(MACHINE_TASK_HALT_REASON_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void testResume_success() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("HALTED");
        exist.setHaltReason("故障");
        exist.setHaltAt(LocalDateTime.now().minusHours(2));
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        machineTaskService.resumeMachineTask(1L);

        verify(machineTaskMapper).update(isNull(), any());
    }

    @Test
    void testResume_notHalted_throws() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("IN_PROGRESS");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.resumeMachineTask(1L));
        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 6. 删除守卫与协作提供者 ====================

    @Test
    void testDelete_success_whenPending() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("PENDING");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        machineTaskService.deleteMachineTask(1L);

        verify(machineTaskMapper).deleteById(1L);
    }

    @Test
    void testDelete_notPending_throws() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setStatus("IN_PROGRESS");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> machineTaskService.deleteMachineTask(1L));
        assertEquals(MACHINE_TASK_STATUS_NOT_ALLOW.getCode(), ex.getCode());
        verify(machineTaskMapper, never()).deleteById(any());
    }

    @Test
    void testLoomTaskReferenceProvider_isLoomReferencedByTask() {
        when(machineTaskMapper.selectCountByLoomId(200L)).thenReturn(2L);
        when(machineTaskMapper.selectCountByLoomId(201L)).thenReturn(0L);

        assertTrue(loomReferenceProvider.isLoomReferencedByTask(200L));
        assertFalse(loomReferenceProvider.isLoomReferencedByTask(201L));
        assertFalse(loomReferenceProvider.isLoomReferencedByTask(null));
    }

    @Test
    void testWeavingOrderCascadeDeleteListener_onWeavingOrderDeleted() {
        cascadeDeleteListener.onWeavingOrderDeleted(100L);

        verify(machineTaskMapper).deleteByWeavingOrderId(100L);
    }

    // ==================== 7. 详情与分页查询 ====================

    @Test
    void testGetMachineTask_success() {
        MachineTaskDO exist = new MachineTaskDO();
        exist.setId(1L);
        exist.setCode("MT-ZZPB091900101-01");
        exist.setWeavingOrderId(100L);
        exist.setLoomId(200L);
        exist.setLoomNo("LM20260001");
        exist.setPlannedQty(60);
        exist.setStatus("PENDING");
        when(machineTaskMapper.selectById(1L)).thenReturn(exist);

        MachineTaskDO other = new MachineTaskDO();
        other.setId(2L);
        other.setPlannedQty(50);
        when(machineTaskMapper.selectActiveListByWeavingOrderId(100L))
                .thenReturn(List.of(exist, other));

        MachineTaskRespVO resp = machineTaskService.getMachineTask(1L);

        assertNotNull(resp);
        assertEquals("MT-ZZPB091900101-01", resp.getCode());
        assertEquals("LM20260001", resp.getLoomNo());
        assertNotNull(resp.getWarning());
        assertTrue(resp.getWarning().contains("110"));
    }

    @Test
    void testGetMachineTaskPage() {
        MachineTaskPageReqVO pageReq = new MachineTaskPageReqVO();
        pageReq.setWeavingOrderId(100L);
        MachineTaskDO task = new MachineTaskDO();
        task.setId(1L);
        task.setCode("MT-ZZPB091900101-01");
        PageResult<MachineTaskDO> mockPage = new PageResult<>(List.of(task), 1L);
        when(machineTaskMapper.selectPage(pageReq)).thenReturn(mockPage);

        PageResult<MachineTaskRespVO> result = machineTaskService.getMachineTaskPage(pageReq);

        assertEquals(1L, result.getTotal());
        assertEquals("MT-ZZPB091900101-01", result.getList().get(0).getCode());
    }

    // ==================== 辅助方法 ====================

    private MachineTaskSaveReqVO buildBaseReq() {
        MachineTaskSaveReqVO req = new MachineTaskSaveReqVO();
        req.setWeavingOrderId(100L);
        req.setLoomId(200L);
        req.setPlannedQty(50);
        req.setPlannedStart(LocalDate.of(2026, 9, 26));
        req.setPlannedEnd(LocalDate.of(2026, 9, 28));
        return req;
    }

}
