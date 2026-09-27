package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - 织造布票 Service 实现类
 *
 * 核心机制：
 * 1. 编号规则：BT-{机台任务号去符号}-{4 位流水}，单内流水计数，全量唯一含软删行，作废占号不重用，撞键重试。
 * 2. 余量校验与行锁：在事务内通过 FOR UPDATE 锁定机台任务行，强校验不超计划匹数。
 * 3. 扫码白名单守卫：开工（GENERATED/STARTED/EXCEPTION）、完工（STARTED）、异常（GENERATED/STARTED）、作废（人工终态）。
 * 4. 三层状态同事务推导：每次布票变更同事务推导机台任务 actualQty/状态与织造单状态；HALTED 期间只更新 actualQty 不覆盖状态。
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class WeavingTicketServiceImpl implements WeavingTicketService {

    public static final String STATUS_GENERATED = "GENERATED";
    public static final String STATUS_STARTED = "STARTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_INSPECTED = "INSPECTED";
    public static final String STATUS_STOCKED = "STOCKED";
    public static final String STATUS_EXCEPTION = "EXCEPTION";
    public static final String STATUS_VOID = "VOID";

    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_STATUS_HALTED = "HALTED";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";

    public static final String ORDER_STATUS_DRAFT = "DRAFT";
    public static final String ORDER_STATUS_ACTIVE = "ACTIVE";
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    private static final Set<String> ALLOWED_START_SOURCE_STATUSES = Set.of(STATUS_GENERATED, STATUS_STARTED, STATUS_EXCEPTION);
    private static final Set<String> ALLOWED_COMPLETE_SOURCE_STATUSES = Set.of(STATUS_STARTED);
    private static final Set<String> ALLOWED_EXCEPTION_SOURCE_STATUSES = Set.of(STATUS_GENERATED, STATUS_STARTED);
    private static final Set<String> ALLOWED_VOID_SOURCE_STATUSES = Set.of(STATUS_GENERATED, STATUS_STARTED, STATUS_EXCEPTION);

    private static final Set<String> VALID_EXCEPTION_TYPES = Set.of(
            "YARN_BREAK", "CLOTH_DAMAGE", "YARN_SHORTAGE", "MACHINE_CHANGE", "WORKER_CHANGE"
    );

    @Resource
    private WeavingTicketMapper weavingTicketMapper;

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WeavingTicketRespVO> createTickets(WeavingTicketCreateReqVO createReqVO) {
        if (createReqVO.getCount() == null || createReqVO.getCount() <= 0) {
            throw exception(WEAVING_TICKET_EXCEED_PLANNED_QTY, "生成布票数量必须为正整数");
        }

        // 1. 事务内行级锁机台任务行（FOR UPDATE）
        MachineTaskDO task = machineTaskMapper.selectOneForUpdate(
                new LambdaQueryWrapper<MachineTaskDO>().eq(MachineTaskDO::getId, createReqVO.getMachineTaskId()));
        if (task == null) {
            throw exception(MACHINE_TASK_NOT_EXISTS);
        }
        if (TASK_STATUS_COMPLETED.equals(task.getStatus())) {
            throw exception(MACHINE_TASK_STATUS_NOT_ALLOW, "机台任务已完工，不允许再生成布票");
        }

        // 2. 校验关联织造单
        WeavingOrderDO weavingOrder = weavingOrderMapper.selectById(task.getWeavingOrderId());
        if (weavingOrder == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        if (ORDER_STATUS_CANCELLED.equals(weavingOrder.getStatus()) || ORDER_STATUS_COMPLETED.equals(weavingOrder.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "织造单已完工或已取消，不允许生成布票");
        }

        // 3. 余量校验（超 plannedQty 强拒绝）
        long existingActiveCount = weavingTicketMapper.selectCount(new LambdaQueryWrapperX<WeavingTicketDO>()
                .eq(WeavingTicketDO::getMachineTaskId, task.getId())
                .ne(WeavingTicketDO::getStatus, STATUS_VOID));
        int remaining = task.getPlannedQty() - (int) existingActiveCount;
        if (createReqVO.getCount() > remaining) {
            throw exception(WEAVING_TICKET_EXCEED_PLANNED_QTY,
                    String.format("生成数量(%d)超出机台任务计划余量(%d)", createReqVO.getCount(), Math.max(0, remaining)));
        }

        // 4. 单匹计划重量（织造单总重 ÷ 计划匹数）
        BigDecimal plannedWeight = null;
        if (weavingOrder.getPlannedWeight() != null && weavingOrder.getPlannedQty() != null && weavingOrder.getPlannedQty() > 0) {
            plannedWeight = weavingOrder.getPlannedWeight().divide(
                    BigDecimal.valueOf(weavingOrder.getPlannedQty()), 2, RoundingMode.HALF_UP);
        }

        // 5. 编号前缀（BT-{机台任务号去符号}-）与单内4位流水计数
        String cleanTaskCode = task.getCode() == null ? "" : task.getCode().replaceAll("[^a-zA-Z0-9]", "");
        String codePrefix = "BT-" + cleanTaskCode + "-";

        List<String> allCodes = weavingTicketMapper.selectCodesByWeavingOrderIdIncludingDeleted(weavingOrder.getId());
        int maxSeq = 0;
        if (CollUtil.isNotEmpty(allCodes)) {
            for (String c : allCodes) {
                if (c != null && c.contains("-")) {
                    String seqStr = c.substring(c.lastIndexOf('-') + 1);
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

        // 6. 批量生成并持久化（撞键重试）
        List<WeavingTicketDO> createdTickets = new ArrayList<>(createReqVO.getCount());
        int currentSeq = maxSeq;
        for (int i = 1; i <= createReqVO.getCount(); i++) {
            currentSeq++;
            String codeCandidate = String.format("%s%04d", codePrefix, currentSeq);
            while (weavingTicketMapper.selectByCodeIncludingDeleted(codeCandidate) != null) {
                currentSeq++;
                codeCandidate = String.format("%s%04d", codePrefix, currentSeq);
            }

            WeavingTicketDO ticket = WeavingTicketDO.builder()
                    .code(codeCandidate)
                    .weavingOrderId(weavingOrder.getId())
                    .machineTaskId(task.getId())
                    .plannedMachine(task.getLoomNo())
                    .plannedWeight(plannedWeight)
                    .workerUserId(createReqVO.getWorkerUserId() != null ? createReqVO.getWorkerUserId() : task.getWorkerUserId())
                    .workerName(StringUtils.hasText(createReqVO.getWorkerName()) ? createReqVO.getWorkerName().trim() : task.getWorkerName())
                    .materialBatchNo(StringUtils.hasText(createReqVO.getMaterialBatchNo()) ? createReqVO.getMaterialBatchNo().trim() : null)
                    .status(STATUS_GENERATED)
                    .build();

            weavingTicketMapper.insert(ticket);
            createdTickets.add(ticket);
        }

        // 7. 同事务触发状态推导
        deriveState(task.getId(), weavingOrder.getId());

        return BeanUtils.toBean(createdTickets, WeavingTicketRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTicket(WeavingTicketStartReqVO startReqVO) {
        List<Long> targetIds = resolveTicketIds(startReqVO.getId(), startReqVO.getIds());
        Set<Long> affectedTaskIds = new HashSet<>();
        Set<Long> affectedOrderIds = new HashSet<>();

        LocalDateTime startedAt = startReqVO.getStartedAt() != null ? startReqVO.getStartedAt() : LocalDateTime.now();

        for (Long id : targetIds) {
            WeavingTicketDO ticket = validateTicketExists(id);

            // 状态守卫
            if (STATUS_VOID.equals(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_ALREADY_VOIDED);
            }
            if (!ALLOWED_START_SOURCE_STATUSES.contains(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_STATUS_NOT_ALLOW,
                        String.format("布票当前状态【%s】不允许开工操作", ticket.getStatus()));
            }

            WeavingTicketDO updateTicket = new WeavingTicketDO();
            updateTicket.setId(ticket.getId());
            updateTicket.setStatus(STATUS_STARTED);

            // 实际上机机台号（录入值 > 原实际值 > 预排机台号）
            if (StringUtils.hasText(startReqVO.getActualMachine())) {
                updateTicket.setActualMachine(startReqVO.getActualMachine().trim());
            } else if (!StringUtils.hasText(ticket.getActualMachine())) {
                updateTicket.setActualMachine(ticket.getPlannedMachine());
            }

            // 挡车工信息
            if (startReqVO.getWorkerUserId() != null) {
                updateTicket.setWorkerUserId(startReqVO.getWorkerUserId());
                updateTicket.setWorkerName(StringUtils.hasText(startReqVO.getWorkerName()) ? startReqVO.getWorkerName().trim() : null);
            }

            // 开工时间（若未曾开工则记录）
            if (ticket.getStartedAt() == null || startReqVO.getStartedAt() != null) {
                updateTicket.setStartedAt(startedAt);
            }

            // 若由 EXCEPTION 恢复开工，清除异常信息
            if (STATUS_EXCEPTION.equals(ticket.getStatus())) {
                updateTicket.setExceptionType(null);
                updateTicket.setExceptionNote(null);
            }

            weavingTicketMapper.updateById(updateTicket);
            affectedTaskIds.add(ticket.getMachineTaskId());
            affectedOrderIds.add(ticket.getWeavingOrderId());
        }

        // 同事务推导受影响任务与单据状态
        triggerBatchDerive(affectedTaskIds, affectedOrderIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTicket(WeavingTicketCompleteReqVO completeReqVO) {
        List<Long> targetIds = resolveTicketIds(completeReqVO.getId(), completeReqVO.getIds());
        Set<Long> affectedTaskIds = new HashSet<>();
        Set<Long> affectedOrderIds = new HashSet<>();

        LocalDateTime completedAt = completeReqVO.getCompletedAt() != null ? completeReqVO.getCompletedAt() : LocalDateTime.now();

        for (Long id : targetIds) {
            WeavingTicketDO ticket = validateTicketExists(id);

            // 状态守卫：仅 STARTED 允许完工
            if (STATUS_VOID.equals(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_ALREADY_VOIDED);
            }
            if (!ALLOWED_COMPLETE_SOURCE_STATUSES.contains(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_STATUS_NOT_ALLOW,
                        String.format("布票当前状态【%s】不允许完工操作，仅已开工（STARTED）允许完工", ticket.getStatus()));
            }

            WeavingTicketDO updateTicket = new WeavingTicketDO();
            updateTicket.setId(ticket.getId());
            updateTicket.setStatus(STATUS_COMPLETED);
            updateTicket.setCompletedAt(completedAt);

            weavingTicketMapper.updateById(updateTicket);
            affectedTaskIds.add(ticket.getMachineTaskId());
            affectedOrderIds.add(ticket.getWeavingOrderId());
        }

        triggerBatchDerive(affectedTaskIds, affectedOrderIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exceptionTicket(WeavingTicketExceptionReqVO exceptionReqVO) {
        List<Long> targetIds = resolveTicketIds(exceptionReqVO.getId(), exceptionReqVO.getIds());
        Set<Long> affectedTaskIds = new HashSet<>();
        Set<Long> affectedOrderIds = new HashSet<>();

        // 校验异常类型合法性（113）
        String exType = exceptionReqVO.getExceptionType();
        if (!StringUtils.hasText(exType) || !VALID_EXCEPTION_TYPES.contains(exType.trim())) {
            throw exception(WEAVING_TICKET_EXCEPTION_TYPE_INVALID);
        }
        String cleanExType = exType.trim();

        for (Long id : targetIds) {
            WeavingTicketDO ticket = validateTicketExists(id);

            // 状态守卫：仅 GENERATED/STARTED 允许报异常
            if (STATUS_VOID.equals(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_ALREADY_VOIDED);
            }
            if (!ALLOWED_EXCEPTION_SOURCE_STATUSES.contains(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_STATUS_NOT_ALLOW,
                        String.format("布票当前状态【%s】不允许登记异常", ticket.getStatus()));
            }

            WeavingTicketDO updateTicket = new WeavingTicketDO();
            updateTicket.setId(ticket.getId());
            updateTicket.setStatus(STATUS_EXCEPTION);
            updateTicket.setExceptionType(cleanExType);
            updateTicket.setExceptionNote(StringUtils.hasText(exceptionReqVO.getExceptionNote()) ? exceptionReqVO.getExceptionNote().trim() : null);

            weavingTicketMapper.updateById(updateTicket);
            affectedTaskIds.add(ticket.getMachineTaskId());
            affectedOrderIds.add(ticket.getWeavingOrderId());
        }

        triggerBatchDerive(affectedTaskIds, affectedOrderIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidTicket(WeavingTicketVoidReqVO voidReqVO) {
        List<Long> targetIds = resolveTicketIds(voidReqVO.getId(), voidReqVO.getIds());
        Set<Long> affectedTaskIds = new HashSet<>();
        Set<Long> affectedOrderIds = new HashSet<>();

        if (!StringUtils.hasText(voidReqVO.getVoidReason())) {
            throw exception(WEAVING_TICKET_VOID_REASON_EMPTY);
        }
        String voidReason = voidReqVO.getVoidReason().trim();
        LocalDateTime voidedAt = LocalDateTime.now();

        for (Long id : targetIds) {
            WeavingTicketDO ticket = validateTicketExists(id);

            // 状态守卫：仅 GENERATED/STARTED/EXCEPTION 允许作废，作废为终态不可二次作废
            if (STATUS_VOID.equals(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_ALREADY_VOIDED);
            }
            if (!ALLOWED_VOID_SOURCE_STATUSES.contains(ticket.getStatus())) {
                throw exception(WEAVING_TICKET_STATUS_NOT_ALLOW,
                        String.format("布票当前状态【%s】不允许作废操作", ticket.getStatus()));
            }

            WeavingTicketDO updateTicket = new WeavingTicketDO();
            updateTicket.setId(ticket.getId());
            updateTicket.setStatus(STATUS_VOID);
            updateTicket.setVoidReason(voidReason);
            updateTicket.setVoidedAt(voidedAt);

            weavingTicketMapper.updateById(updateTicket);
            affectedTaskIds.add(ticket.getMachineTaskId());
            affectedOrderIds.add(ticket.getWeavingOrderId());
        }

        triggerBatchDerive(affectedTaskIds, affectedOrderIds);
    }

    @Override
    public WeavingTicketRespVO getTicket(Long id) {
        WeavingTicketDO ticket = validateTicketExists(id);
        return BeanUtils.toBean(ticket, WeavingTicketRespVO.class);
    }

    @Override
    public WeavingTicketRespVO getTicketByCode(String code) {
        WeavingTicketDO ticket = weavingTicketMapper.selectByCode(code);
        if (ticket == null) {
            throw exception(WEAVING_TICKET_NOT_EXISTS);
        }
        return BeanUtils.toBean(ticket, WeavingTicketRespVO.class);
    }

    @Override
    public PageResult<WeavingTicketRespVO> getTicketPage(WeavingTicketPageReqVO pageReqVO) {
        PageResult<WeavingTicketDO> pageResult = weavingTicketMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, WeavingTicketRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByWeavingOrderId(Long weavingOrderId) {
        weavingTicketMapper.deleteByWeavingOrderId(weavingOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByMachineTaskId(Long machineTaskId) {
        weavingTicketMapper.deleteByMachineTaskId(machineTaskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deriveState(Long machineTaskId, Long weavingOrderId) {
        // 1. 推导机台任务状态与 actualQty
        if (machineTaskId != null) {
            MachineTaskDO task = machineTaskMapper.selectById(machineTaskId);
            if (task != null) {
                List<WeavingTicketDO> taskTickets = weavingTicketMapper.selectListByMachineTaskId(machineTaskId);
                List<WeavingTicketDO> validTickets = taskTickets == null ? Collections.emptyList()
                        : taskTickets.stream().filter(t -> !STATUS_VOID.equals(t.getStatus())).toList();

                long completedCount = validTickets.stream()
                        .filter(t -> STATUS_COMPLETED.equals(t.getStatus()) || STATUS_INSPECTED.equals(t.getStatus()) || STATUS_STOCKED.equals(t.getStatus()))
                        .count();
                long inProgressCount = validTickets.stream()
                        .filter(t -> STATUS_STARTED.equals(t.getStatus()) || STATUS_COMPLETED.equals(t.getStatus())
                                || STATUS_INSPECTED.equals(t.getStatus()) || STATUS_STOCKED.equals(t.getStatus()) || STATUS_EXCEPTION.equals(t.getStatus()))
                        .count();

                int newActualQty = (int) completedCount;

                // 特例：任务处于 HALTED 停机终态期间，自动推导只更新 actualQty，不覆盖 HALTED 状态
                if (TASK_STATUS_HALTED.equals(task.getStatus())) {
                    if (!Objects.equals(task.getActualQty(), newActualQty)) {
                        MachineTaskDO updateTask = new MachineTaskDO();
                        updateTask.setId(task.getId());
                        updateTask.setActualQty(newActualQty);
                        machineTaskMapper.updateById(updateTask);
                    }
                } else {
                    String newStatus;
                    int totalGenerated = taskTickets == null ? 0 : taskTickets.size();
                    if (!validTickets.isEmpty() && completedCount == validTickets.size()
                            && (task.getPlannedQty() == null || totalGenerated >= task.getPlannedQty() || completedCount >= task.getPlannedQty())) {
                        newStatus = TASK_STATUS_COMPLETED;
                    } else if (inProgressCount > 0) {
                        newStatus = TASK_STATUS_IN_PROGRESS;
                    } else {
                        newStatus = TASK_STATUS_PENDING;
                    }

                    if (!Objects.equals(task.getActualQty(), newActualQty) || !Objects.equals(task.getStatus(), newStatus)) {
                        MachineTaskDO updateTask = new MachineTaskDO();
                        updateTask.setId(task.getId());
                        updateTask.setActualQty(newActualQty);
                        updateTask.setStatus(newStatus);
                        machineTaskMapper.updateById(updateTask);
                    }
                }
            }
        }

        // 2. 推导织造单状态
        if (weavingOrderId != null) {
            WeavingOrderDO order = weavingOrderMapper.selectById(weavingOrderId);
            if (order != null && !ORDER_STATUS_CANCELLED.equals(order.getStatus())) {
                List<WeavingTicketDO> orderTickets = weavingTicketMapper.selectListByWeavingOrderId(weavingOrderId);
                List<WeavingTicketDO> validOrderTickets = orderTickets == null ? Collections.emptyList()
                        : orderTickets.stream().filter(t -> !STATUS_VOID.equals(t.getStatus())).toList();

                long completedCount = validOrderTickets.stream()
                        .filter(t -> STATUS_COMPLETED.equals(t.getStatus()) || STATUS_INSPECTED.equals(t.getStatus()) || STATUS_STOCKED.equals(t.getStatus()))
                        .count();
                long inProgressCount = validOrderTickets.stream()
                        .filter(t -> STATUS_STARTED.equals(t.getStatus()) || STATUS_COMPLETED.equals(t.getStatus())
                                || STATUS_INSPECTED.equals(t.getStatus()) || STATUS_STOCKED.equals(t.getStatus()) || STATUS_EXCEPTION.equals(t.getStatus()))
                        .count();

                List<MachineTaskDO> tasks = machineTaskMapper.selectListByWeavingOrderId(weavingOrderId);
                boolean allTasksCompleted = CollUtil.isNotEmpty(tasks) && tasks.stream()
                        .allMatch(t -> TASK_STATUS_COMPLETED.equals(t.getStatus()));

                String newOrderStatus;
                if (!validOrderTickets.isEmpty() && completedCount == validOrderTickets.size() && allTasksCompleted) {
                    newOrderStatus = ORDER_STATUS_COMPLETED;
                } else if (inProgressCount > 0) {
                    newOrderStatus = ORDER_STATUS_ACTIVE;
                } else {
                    newOrderStatus = ORDER_STATUS_DRAFT;
                }

                if (!Objects.equals(order.getStatus(), newOrderStatus)) {
                    WeavingOrderDO updateOrder = new WeavingOrderDO();
                    updateOrder.setId(order.getId());
                    updateOrder.setStatus(newOrderStatus);
                    weavingOrderMapper.updateById(updateOrder);
                }
            }
        }
    }

    // ==================== 内部私有方法 ====================

    private WeavingTicketDO validateTicketExists(Long id) {
        if (id == null) {
            throw exception(WEAVING_TICKET_NOT_EXISTS);
        }
        WeavingTicketDO ticket = weavingTicketMapper.selectById(id);
        if (ticket == null) {
            throw exception(WEAVING_TICKET_NOT_EXISTS);
        }
        return ticket;
    }

    private List<Long> resolveTicketIds(Long singleId, List<Long> ids) {
        List<Long> result = new ArrayList<>();
        if (singleId != null) {
            result.add(singleId);
        }
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                if (id != null && !result.contains(id)) {
                    result.add(id);
                }
            }
        }
        if (result.isEmpty()) {
            throw exception(WEAVING_TICKET_NOT_EXISTS, "请指定至少一张布票");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markInspected(Long ticketId) {
        if (ticketId == null) {
            return;
        }
        WeavingTicketDO ticket = weavingTicketMapper.selectById(ticketId);
        if (ticket == null || STATUS_VOID.equals(ticket.getStatus())) {
            return;
        }
        // 若已经是 INSPECTED 或 STOCKED，保持幂等
        if (STATUS_INSPECTED.equals(ticket.getStatus()) || STATUS_STOCKED.equals(ticket.getStatus())) {
            return;
        }
        // ADR-0024 白名单约束：只有完工票（COMPLETED）允许跃迁至 INSPECTED
        if (!STATUS_COMPLETED.equals(ticket.getStatus())) {
            return;
        }
        WeavingTicketDO update = new WeavingTicketDO();
        update.setId(ticket.getId());
        update.setStatus(STATUS_INSPECTED);
        weavingTicketMapper.updateById(update);

        deriveState(ticket.getMachineTaskId(), ticket.getWeavingOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackInspected(Long ticketId) {
        if (ticketId == null) {
            return;
        }
        WeavingTicketDO ticket = weavingTicketMapper.selectById(ticketId);
        if (ticket == null || !STATUS_INSPECTED.equals(ticket.getStatus())) {
            return;
        }
        // 回退为 COMPLETED
        WeavingTicketDO update = new WeavingTicketDO();
        update.setId(ticket.getId());
        update.setStatus(STATUS_COMPLETED);
        weavingTicketMapper.updateById(update);

        deriveState(ticket.getMachineTaskId(), ticket.getWeavingOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markStocked(java.util.Collection<Long> ticketIds) {
        if (CollUtil.isEmpty(ticketIds)) {
            return;
        }
        Set<Long> affectedTaskIds = new HashSet<>();
        Set<Long> affectedOrderIds = new HashSet<>();

        for (Long id : ticketIds) {
            if (id == null) {
                continue;
            }
            WeavingTicketDO ticket = weavingTicketMapper.selectById(id);
            if (ticket == null || STATUS_VOID.equals(ticket.getStatus()) || STATUS_STOCKED.equals(ticket.getStatus())) {
                continue;
            }
            WeavingTicketDO update = new WeavingTicketDO();
            update.setId(ticket.getId());
            update.setStatus(STATUS_STOCKED);
            weavingTicketMapper.updateById(update);

            affectedTaskIds.add(ticket.getMachineTaskId());
            affectedOrderIds.add(ticket.getWeavingOrderId());
        }

        triggerBatchDerive(affectedTaskIds, affectedOrderIds);
    }

    private void triggerBatchDerive(Set<Long> affectedTaskIds, Set<Long> affectedOrderIds) {
        for (Long taskId : affectedTaskIds) {
            deriveState(taskId, null);
        }
        for (Long orderId : affectedOrderIds) {
            deriveState(null, orderId);
        }
    }

}
