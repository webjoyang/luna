package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - 机台任务 Service 实现类
 *
 * 核心规则：
 * 1. 编号派生：MT-{织造单号去符号}-{单内 2 位序号}，全量唯一含软删行，并发冲突顺延重试（ADR-0023）；
 * 2. 织机校验：仅启用（ACTIVE）状态织机可选，机台号快照留痕；
 * 3. 排期与数量校验：计划匹数正整数，计划结束日期 end ≥ start；
 * 4. 软校验：单内机台任务计划匹数合计超出织造单计划匹数时返回软警告，不拦截保存；
 * 5. 状态守卫：仅待开始（PENDING）允许删除，已完工任务禁止修改与停机；
 * 6. 停机与恢复：停机需必填原因、记录 haltAt，恢复重置为 PENDING。
 *
 * @author luna
 */
@Service
@Validated
public class MachineTaskServiceImpl implements MachineTaskService {

    /**
     * 自动生成编号最大重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;

    /**
     * 任务初始状态
     */
    private static final String DEFAULT_STATUS = "PENDING";
    /**
     * 进行中状态
     */
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    /**
     * 停机状态
     */
    private static final String STATUS_HALTED = "HALTED";
    /**
     * 已完工状态
     */
    private static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * 占用织机时段的任务状态集合（ADR-0041）
     */
    private static final List<String> OCCUPIED_STATUSES = List.of(DEFAULT_STATUS, STATUS_IN_PROGRESS, STATUS_HALTED);

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private LoomService loomService;

    @org.springframework.context.annotation.Lazy
    @Resource
    private com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService weavingTicketService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MachineTaskSaveRespVO createMachineTask(MachineTaskSaveReqVO createReqVO) {
        // 1. 织造单校验
        WeavingOrderDO weavingOrder = validateWeavingOrderForScheduling(createReqVO.getWeavingOrderId());

        // 2. 织机校验（仅 ACTIVE 织机可选，未启用抛 LOOM_NOT_EXISTS）
        LoomDO loom = loomService.validateLoomActive(createReqVO.getLoomId());

        // 3. 计划匹数校验（正整数）
        validatePlannedQty(createReqVO.getPlannedQty());

        // 4. 排期日期校验（end ≥ start）
        validateDateRange(createReqVO.getPlannedStart(), createReqVO.getPlannedEnd());

        // 4.1 同织机计划时段冲突校验（ADR-0041）
        validateLoomTimeConflict(null, loom, createReqVO.getPlannedStart(), createReqVO.getPlannedEnd());

        // 5. 组装实体
        MachineTaskDO task = BeanUtils.toBean(createReqVO, MachineTaskDO.class);
        task.setLoomNo(loom.getLoomNo());
        task.setStatus(DEFAULT_STATUS);
        task.setActualQty(0);

        // 6. 编号处理：手填查重（含已删除行）；留空自动派生
        if (StringUtils.hasText(task.getCode())) {
            validateCodeUnique(task.getCode().trim(), null);
            task.setCode(task.getCode().trim());
        } else {
            task.setCode(generateCodeWithRetry(weavingOrder.getCode()));
        }

        // 7. 挡车工姓名快照整理
        if (task.getWorkerUserId() == null) {
            task.setWorkerName(null);
        } else if (StringUtils.hasText(task.getWorkerName())) {
            task.setWorkerName(task.getWorkerName().trim());
        }

        // 8. 计算超量软警告（在插入前基于现有任务计划匹数与本次新增匹数合计进行判断，避免重复累加）
        String warning = calculateWarning(weavingOrder, null, task.getPlannedQty());

        // 9. 插入机台任务
        machineTaskMapper.insert(task);

        return new MachineTaskSaveRespVO(task.getId(), task.getCode(), warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MachineTaskSaveRespVO updateMachineTask(MachineTaskSaveReqVO updateReqVO) {
        // 1. 任务存在性与状态守卫
        MachineTaskDO exist = validateMachineTaskExists(updateReqVO.getId());
        if (STATUS_COMPLETED.equals(exist.getStatus())) {
            throw exception(MACHINE_TASK_STATUS_NOT_ALLOW, "已完工的机台任务不允许修改");
        }

        // 2. 织造单存在性校验
        WeavingOrderDO weavingOrder = validateWeavingOrderForScheduling(exist.getWeavingOrderId());

        // 3. 织机校验（若更换或重新校验织机）
        LoomDO loom = loomService.validateLoomActive(updateReqVO.getLoomId());

        // 4. 计划匹数与排期校验
        validatePlannedQty(updateReqVO.getPlannedQty());
        validateDateRange(updateReqVO.getPlannedStart(), updateReqVO.getPlannedEnd());

        // 4.1 同织机计划时段冲突校验（ADR-0041）
        validateLoomTimeConflict(exist.getId(), loom, updateReqVO.getPlannedStart(), updateReqVO.getPlannedEnd());

        // 5. 任务编号变更查重（若手填修改）
        if (StringUtils.hasText(updateReqVO.getCode()) && !updateReqVO.getCode().trim().equals(exist.getCode())) {
            validateCodeUnique(updateReqVO.getCode().trim(), exist.getId());
            exist.setCode(updateReqVO.getCode().trim());
        }

        // 6. 更新字段
        exist.setLoomId(loom.getId());
        exist.setLoomNo(loom.getLoomNo());
        exist.setPlannedQty(updateReqVO.getPlannedQty());
        exist.setPlannedStart(updateReqVO.getPlannedStart());
        exist.setPlannedEnd(updateReqVO.getPlannedEnd());
        exist.setRemark(updateReqVO.getRemark());

        if (updateReqVO.getWorkerUserId() == null) {
            exist.setWorkerUserId(null);
            exist.setWorkerName(null);
        } else {
            exist.setWorkerUserId(updateReqVO.getWorkerUserId());
            exist.setWorkerName(StringUtils.hasText(updateReqVO.getWorkerName()) ? updateReqVO.getWorkerName().trim() : null);
        }

        machineTaskMapper.updateById(exist);

        // 7. 计算超量软警告
        String warning = calculateWarning(weavingOrder, exist.getId(), exist.getPlannedQty());

        return new MachineTaskSaveRespVO(exist.getId(), exist.getCode(), warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMachineTask(Long id) {
        MachineTaskDO exist = validateMachineTaskExists(id);

        // 状态守卫：仅待开始 PENDING 态允许删除
        if (!DEFAULT_STATUS.equals(exist.getStatus())) {
            throw exception(MACHINE_TASK_STATUS_NOT_ALLOW, "仅待开始（PENDING）状态的机台任务允许删除");
        }

        machineTaskMapper.deleteById(id);
        if (weavingTicketService != null) {
            weavingTicketService.deleteByMachineTaskId(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void haltMachineTask(MachineTaskHaltReqVO haltReqVO) {
        MachineTaskDO exist = validateMachineTaskExists(haltReqVO.getId());

        // 状态守卫：已完工任务不能标停机（ADR-0024）
        if (STATUS_COMPLETED.equals(exist.getStatus())) {
            throw exception(MACHINE_TASK_COMPLETED_CANNOT_HALT);
        }

        if (!StringUtils.hasText(haltReqVO.getHaltReason())) {
            throw exception(MACHINE_TASK_HALT_REASON_EMPTY);
        }

        MachineTaskDO updateObj = new MachineTaskDO();
        updateObj.setId(exist.getId());
        updateObj.setStatus(STATUS_HALTED);
        updateObj.setHaltReason(haltReqVO.getHaltReason().trim());
        updateObj.setHaltAt(LocalDateTime.now());
        machineTaskMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeMachineTask(Long id) {
        MachineTaskDO exist = validateMachineTaskExists(id);

        // 状态守卫：只有 HALTED 状态可以恢复
        if (!STATUS_HALTED.equals(exist.getStatus())) {
            throw exception(MACHINE_TASK_STATUS_NOT_ALLOW, "只有暂停异常（HALTED）状态的机台任务可以恢复");
        }

        machineTaskMapper.update(null, new LambdaUpdateWrapper<MachineTaskDO>()
                .eq(MachineTaskDO::getId, id)
                .set(MachineTaskDO::getStatus, DEFAULT_STATUS)
                .set(MachineTaskDO::getHaltReason, null)
                .set(MachineTaskDO::getHaltAt, null));

        if (weavingTicketService != null) {
            weavingTicketService.deriveState(id, exist.getWeavingOrderId());
        }
    }

    @Override
    public MachineTaskRespVO getMachineTask(Long id) {
        MachineTaskDO exist = validateMachineTaskExists(id);
        MachineTaskRespVO resp = BeanUtils.toBean(exist, MachineTaskRespVO.class);

        // 附带超量警告计算
        WeavingOrderDO weavingOrder = weavingOrderMapper.selectById(exist.getWeavingOrderId());
        if (weavingOrder != null) {
            resp.setWarning(calculateWarning(weavingOrder, exist.getId(), exist.getPlannedQty()));
        }

        return resp;
    }

    @Override
    public PageResult<MachineTaskRespVO> getMachineTaskPage(MachineTaskPageReqVO pageReqVO) {
        PageResult<MachineTaskDO> pageResult = machineTaskMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, MachineTaskRespVO.class);
    }

    @Override
    public String getNextMachineTaskCode(Long weavingOrderId) {
        WeavingOrderDO weavingOrder = validateWeavingOrderForScheduling(weavingOrderId);
        return generateCode(weavingOrder.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByWeavingOrderId(Long weavingOrderId) {
        if (weavingOrderId == null) {
            return;
        }
        machineTaskMapper.deleteByWeavingOrderId(weavingOrderId);
    }

    // ==================== 内部私有方法 ====================

    private MachineTaskDO validateMachineTaskExists(Long id) {
        if (id == null) {
            throw exception(MACHINE_TASK_NOT_EXISTS);
        }
        MachineTaskDO task = machineTaskMapper.selectById(id);
        if (task == null) {
            throw exception(MACHINE_TASK_NOT_EXISTS);
        }
        return task;
    }

    private WeavingOrderDO validateWeavingOrderForScheduling(Long weavingOrderId) {
        if (weavingOrderId == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        WeavingOrderDO order = weavingOrderMapper.selectById(weavingOrderId);
        if (order == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        if ("CANCELLED".equals(order.getStatus()) || STATUS_COMPLETED.equals(order.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "织造单已完工或已取消，不允许排产机台任务");
        }
        return order;
    }

    private void validatePlannedQty(Integer plannedQty) {
        if (plannedQty == null || plannedQty <= 0) {
            throw exception(MACHINE_TASK_PLANNED_QTY_INVALID);
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw exception(MACHINE_TASK_DATE_RANGE_INVALID);
        }
    }

    /**
     * 校验同织机计划时段冲突（ADR-0041）
     *
     * 规则：
     * 1. 本次提交有效区间：effStart = coalesce(plannedStart, plannedEnd)，effEnd = coalesce(plannedEnd, plannedStart)；
     *    两端皆空则跳过冲突校验（进待排池，不占时段）。
     * 2. 参与冲突任务：同租户、未删、同织机、状态 ∈ PENDING/IN_PROGRESS/HALTED、起止日期至少一个非空；update 排除自身。
     * 3. 闭区间相交判定（同日即冲突）：existing.effStart ≤ effEnd AND existing.effEnd ≥ effStart。
     * 4. 命中首条冲突即抛 1_030_036_000，消息含织机编号、冲突任务起止日期、冲突任务编号。
     *
     * @param excludeTaskId 排除的任务ID（更新时传当前任务ID，创建时传 null）
     * @param loom 织机DO
     * @param plannedStart 计划开始日期
     * @param plannedEnd 计划结束日期
     */
    private void validateLoomTimeConflict(Long excludeTaskId, LoomDO loom, LocalDate plannedStart, LocalDate plannedEnd) {
        LocalDate effStart = plannedStart != null ? plannedStart : plannedEnd;
        LocalDate effEnd = plannedEnd != null ? plannedEnd : plannedStart;
        // 两端皆空跳过冲突校验（进待排池，不占时段）
        if (effStart == null && effEnd == null) {
            return;
        }

        List<MachineTaskDO> candidates = machineTaskMapper.selectOccupiedTasksByLoomId(loom.getId());
        if (CollUtil.isEmpty(candidates)) {
            return;
        }

        for (MachineTaskDO existTask : candidates) {
            // update 时排除自身
            if (excludeTaskId != null && excludeTaskId.equals(existTask.getId())) {
                continue;
            }
            // 过滤状态（仅 PENDING / IN_PROGRESS / HALTED 参与）
            if (!OCCUPIED_STATUSES.contains(existTask.getStatus())) {
                continue;
            }
            // 库内任务有效区间（coalesce 单端日期，两端皆空跳过）
            LocalDate existEffStart = existTask.getPlannedStart() != null ? existTask.getPlannedStart() : existTask.getPlannedEnd();
            LocalDate existEffEnd = existTask.getPlannedEnd() != null ? existTask.getPlannedEnd() : existTask.getPlannedStart();
            if (existEffStart == null && existEffEnd == null) {
                continue;
            }

            // 闭区间相交判定：existEffStart <= effEnd && existEffEnd >= effStart（同日即冲突）
            if (!existEffStart.isAfter(effEnd) && !existEffEnd.isBefore(effStart)) {
                String loomNo = StringUtils.hasText(loom.getLoomNo()) ? loom.getLoomNo() : existTask.getLoomNo();
                throw exception(MACHINE_TASK_LOOM_TIME_CONFLICT, loomNo, existEffStart, existEffEnd, existTask.getCode());
            }
        }
    }

    private void validateCodeUnique(String code, Long excludeId) {
        MachineTaskDO duplicate = machineTaskMapper.selectByCodeIncludingDeleted(code);
        if (duplicate != null && (excludeId == null || !duplicate.getId().equals(excludeId))) {
            throw exception(MACHINE_TASK_CODE_DUPLICATE, code);
        }
    }

    private String buildTaskCodePrefix(String weavingOrderCode) {
        String cleanCode = weavingOrderCode == null ? "" : weavingOrderCode.replaceAll("[^a-zA-Z0-9]", "");
        return "MT-" + cleanCode + "-";
    }

    private int getMaxSequence(String prefix) {
        List<MachineTaskDO> candidates = machineTaskMapper.selectListByCodePrefixIncludingDeleted(prefix);
        int maxSeq = 0;
        if (CollUtil.isNotEmpty(candidates)) {
            for (MachineTaskDO candidate : candidates) {
                String code = candidate.getCode();
                if (code != null && code.startsWith(prefix)) {
                    String seqStr = code.substring(prefix.length());
                    try {
                        int seq = Integer.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略非数字后缀
                    }
                }
            }
        }
        return maxSeq;
    }

    private String generateCode(String weavingOrderCode) {
        String prefix = buildTaskCodePrefix(weavingOrderCode);
        int nextSeq = getMaxSequence(prefix) + 1;
        return String.format("%s%02d", prefix, nextSeq);
    }

    private String generateCodeWithRetry(String weavingOrderCode) {
        String prefix = buildTaskCodePrefix(weavingOrderCode);
        int maxSeq = getMaxSequence(prefix);
        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            int seq = maxSeq + i;
            String candidate = String.format("%s%02d", prefix, seq);
            if (machineTaskMapper.selectByCodeIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(MACHINE_TASK_CODE_DUPLICATE, "生成机台任务编号并发冲突，请稍后重试");
    }

    private String calculateWarning(WeavingOrderDO weavingOrder, Long excludeTaskId, int newPlannedQty) {
        if (weavingOrder == null || weavingOrder.getPlannedQty() == null) {
            return null;
        }
        List<MachineTaskDO> activeTasks = machineTaskMapper.selectActiveListByWeavingOrderId(weavingOrder.getId());
        int existingSum = 0;
        if (CollUtil.isNotEmpty(activeTasks)) {
            for (MachineTaskDO task : activeTasks) {
                if (excludeTaskId != null && task.getId().equals(excludeTaskId)) {
                    continue;
                }
                if (task.getPlannedQty() != null) {
                    existingSum += task.getPlannedQty();
                }
            }
        }
        int totalPlanned = existingSum + newPlannedQty;
        if (totalPlanned > weavingOrder.getPlannedQty()) {
            return String.format("单内机台任务计划匹数合计(%d匹)超出织造单计划匹数(%d匹)", totalPlanned, weavingOrder.getPlannedQty());
        }
        return null;
    }

}
