package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStatusCountsVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketScanRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.item.InspectionOrderItemService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color.GreigeOrderColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color.GreigeOrderColorMapper;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.WeavingOrderService;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderHandoffInspectionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.WorkshopH5ErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 车间移动 H5 接入层编排服务实现类
 *
 * 规范守卫：
 * 1. 强制数据范围：基于当前登录用户 ID 硬过滤，严禁信任或采用客户端传入的 workerId；
 * 2. 成功出参使用 H5 精简 VO，严格过滤成本、单价、内部备注等非现场信息；
 * 3. 读链路批量抽取快照（防 N+1，batch* 前缀方法封装），写链路一律委托生产域核心 Service。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class WorkshopH5ServiceImpl implements WorkshopH5Service {

    private static final List<String> DEFAULT_ACTIVE_STATUSES = List.of("PENDING", "IN_PROGRESS", "HALTED");
    private static final List<String> DEFAULT_INSPECTION_STATUSES = List.of("DRAFT", "ACTIVE");

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private WeavingTicketMapper weavingTicketMapper;

    @Resource
    private WeavingTicketService weavingTicketService;

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Resource
    private WeavingOrderService weavingOrderService;

    @Resource
    private InspectionOrderItemService inspectionOrderItemService;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private GreigeOrderColorMapper greigeOrderColorMapper;

    @Override
    public PageResult<WorkshopMyTaskRespVO> getMyTasksPage(WorkshopMyTasksPageReqVO reqVO) {
        // 1. 强制取登录人用户ID，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 解析状态过滤条件（缺省默认 PENDING, IN_PROGRESS, HALTED）
        List<String> statuses;
        if (StrUtil.isNotBlank(reqVO.getStatus())) {
            statuses = Arrays.stream(reqVO.getStatus().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
            if (statuses.isEmpty()) {
                statuses = DEFAULT_ACTIVE_STATUSES;
            }
        } else {
            statuses = DEFAULT_ACTIVE_STATUSES;
        }

        // 3. 硬过滤 worker_user_id = 登录人 分页查询机台任务
        LambdaQueryWrapperX<MachineTaskDO> taskQuery = new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getWorkerUserId, loginUserId)
                .in(MachineTaskDO::getStatus, statuses)
                .orderByDesc(MachineTaskDO::getId);
        PageResult<MachineTaskDO> pageResult = machineTaskMapper.selectPage(reqVO, taskQuery);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }

        // 4. 批量查询织造单快照（防 N+1，符合 coding-standards.md §2 batch* 命名规范）
        Set<Long> orderIds = pageResult.getList().stream()
                .map(MachineTaskDO::getWeavingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeavingOrderDO> orderMap = batchLoadWeavingOrders(orderIds);

        // 5. 批量查询关联布票各状态计数（防 N+1，仅投影 machineTaskId 与 status）
        List<Long> taskIds = pageResult.getList().stream()
                .map(MachineTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Map<String, Integer>> taskStatusCountsMap = batchLoadTicketStatusCounts(taskIds);

        // 6. 组装 H5 精简 VO
        List<WorkshopMyTaskRespVO> voList = pageResult.getList().stream().map(task -> {
            WeavingOrderDO order = orderMap.get(task.getWeavingOrderId());
            Map<String, Integer> counts = taskStatusCountsMap.getOrDefault(task.getId(), Collections.emptyMap());
            WorkshopTicketStatusCountsVO ticketCounts = WorkshopTicketStatusCountsVO.fromStatusCounts(counts);

            return WorkshopMyTaskRespVO.builder()
                    .id(task.getId())
                    .code(task.getCode())
                    .weavingOrderId(task.getWeavingOrderId())
                    .weavingOrderCode(order != null ? order.getCode() : null)
                    .productName(order != null ? order.getProductName() : null)
                    .productCode(order != null ? order.getProductCode() : null)
                    .loomId(task.getLoomId())
                    .loomNo(task.getLoomNo())
                    .workerUserId(task.getWorkerUserId())
                    .workerName(task.getWorkerName())
                    .plannedQty(task.getPlannedQty() != null ? task.getPlannedQty() : 0)
                    .actualQty(task.getActualQty() != null ? task.getActualQty() : 0)
                    .status(task.getStatus())
                    .plannedStart(task.getPlannedStart())
                    .plannedEnd(task.getPlannedEnd())
                    .ticketCounts(ticketCounts)
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public WorkshopTicketRespVO getTicketByCode(String code) {
        // 1. 强制取登录人用户ID，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 校验参数
        if (StrUtil.isBlank(code)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 3. 按布票编号查询有效布票（未删除）
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, code.trim())
        );
        if (ticket == null || "VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 4. 查询关联机台任务并进行挡车工权限与自助认领守卫
        MachineTaskDO task = ticket.getMachineTaskId() != null
                ? machineTaskMapper.selectById(ticket.getMachineTaskId())
                : null;
        if (task != null && task.getWorkerUserId() != null && !Objects.equals(task.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }
        if (task == null && ticket.getWorkerUserId() != null && !Objects.equals(ticket.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }

        // 5. 查询关联织造单快照
        WeavingOrderDO order = ticket.getWeavingOrderId() != null
                ? weavingOrderMapper.selectById(ticket.getWeavingOrderId())
                : null;

        // 6. 确定挡车工归属与认领标志（任务 worker 为空或=登录人，claimable=true）
        Long workerUserId = task != null && task.getWorkerUserId() != null ? task.getWorkerUserId() : ticket.getWorkerUserId();
        String workerName = task != null && task.getWorkerName() != null ? task.getWorkerName() : ticket.getWorkerName();

        // 7. 组装并返回 H5 精简布票卡 VO
        return WorkshopTicketRespVO.builder()
                .id(ticket.getId())
                .code(ticket.getCode())
                .weavingOrderId(ticket.getWeavingOrderId())
                .weavingOrderCode(order != null ? order.getCode() : null)
                .machineTaskId(ticket.getMachineTaskId())
                .machineTaskCode(task != null ? task.getCode() : null)
                .loomId(task != null ? task.getLoomId() : null)
                .loomNo(task != null ? task.getLoomNo() : ticket.getPlannedMachine())
                .plannedMachine(ticket.getPlannedMachine())
                .actualMachine(ticket.getActualMachine())
                .productName(order != null ? order.getProductName() : null)
                .productCode(order != null ? order.getProductCode() : null)
                .composition(order != null ? order.getComposition() : null)
                .gsm(order != null ? order.getGsm() : null)
                .width(order != null ? order.getWidth() : null)
                .plannedWeight(ticket.getPlannedWeight())
                .workerUserId(workerUserId)
                .workerName(workerName)
                .materialBatchNo(ticket.getMaterialBatchNo())
                .status(ticket.getStatus())
                .exceptionType(ticket.getExceptionType())
                .exceptionNote(ticket.getExceptionNote())
                .exceptionPhotos(ticket.getExceptionPhotos())
                .startedAt(ticket.getStartedAt())
                .completedAt(ticket.getCompletedAt())
                .claimable(Boolean.TRUE)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "车间移动H5", subType = "布票开工", bizNo = "{{#reqVO.code}}", success = "布票【{{#reqVO.code}}】开工成功")
    public void startTicket(WorkshopTicketStartReqVO reqVO) {
        // 1. 强制取登录人用户ID与昵称，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        String loginNickname = SecurityFrameworkUtils.getLoginUserNickname();

        // 2. 校验参数
        if (reqVO == null || StrUtil.isBlank(reqVO.getCode())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 3. 按布票编号查询有效布票（非 VOID）
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, reqVO.getCode().trim())
        );
        if (ticket == null || "VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 4. 查询关联机台任务并进行挡车工权限与自助认领守卫
        MachineTaskDO task = ticket.getMachineTaskId() != null
                ? machineTaskMapper.selectById(ticket.getMachineTaskId())
                : null;
        if (task != null) {
            if (task.getWorkerUserId() != null && !Objects.equals(task.getWorkerUserId(), loginUserId)) {
                // 已派给其他挡车工，横向越权拦截
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
            }
            if (task.getWorkerUserId() == null) {
                // 未派工：自助认领，原子条件更新 WHERE worker_user_id IS NULL
                MachineTaskDO updateTask = new MachineTaskDO();
                updateTask.setWorkerUserId(loginUserId);
                updateTask.setWorkerName(loginNickname);
                int updated = machineTaskMapper.update(updateTask,
                        new LambdaUpdateWrapper<MachineTaskDO>()
                                .eq(MachineTaskDO::getId, task.getId())
                                .isNull(MachineTaskDO::getWorkerUserId)
                );
                if (updated == 0) {
                    // 并发落败：被其他工人抢先认领
                    throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
                }
            }
        } else if (ticket.getWorkerUserId() != null && !Objects.equals(ticket.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }

        // 5. 委托生产域核心 Service 执行开工，workerUserId/workerName 强制绑定当前登录人
        WeavingTicketStartReqVO startReqVO = WeavingTicketStartReqVO.builder()
                .id(ticket.getId())
                .actualMachine(StrUtil.isNotBlank(reqVO.getActualMachine()) ? reqVO.getActualMachine().trim() : null)
                .workerUserId(loginUserId)
                .workerName(loginNickname)
                .build();
        weavingTicketService.startTicket(startReqVO);
    }

    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "^(https?://|/).*\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?.*)?$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "车间移动H5", subType = "布票完工", bizNo = "{{#reqVO.code}}", success = "布票【{{#reqVO.code}}】完工成功")
    public WorkshopTicketCompleteRespVO completeTicket(WorkshopTicketCompleteReqVO reqVO) {
        // 1. 强制取登录人用户ID，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 校验参数
        if (reqVO == null || StrUtil.isBlank(reqVO.getCode())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 3. 按布票编号查询有效布票（非 VOID）
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, reqVO.getCode().trim())
        );
        if (ticket == null) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }
        if ("VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.WEAVING_TICKET_ALREADY_VOIDED);
        }

        // 4. 查询关联机台任务并进行挡车工权限守卫（横向越权拦截）
        MachineTaskDO task = ticket.getMachineTaskId() != null
                ? machineTaskMapper.selectById(ticket.getMachineTaskId())
                : null;
        if (task != null) {
            if (task.getWorkerUserId() != null && !Objects.equals(task.getWorkerUserId(), loginUserId)) {
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
            }
        } else if (ticket.getWorkerUserId() != null && !Objects.equals(ticket.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }

        // 5. 幂等友好：票已 COMPLETED/INSPECTED/STOCKED → 返回成功+alreadyCompleted=true（不报错），供弱网重试
        if ("COMPLETED".equals(ticket.getStatus()) || "INSPECTED".equals(ticket.getStatus()) || "STOCKED".equals(ticket.getStatus())) {
            return WorkshopTicketCompleteRespVO.builder()
                    .alreadyCompleted(Boolean.TRUE)
                    .build();
        }

        // 6. 状态守卫：非 STARTED/已完成态抛既有 015-111
        if (!"STARTED".equals(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.WEAVING_TICKET_STATUS_NOT_ALLOW,
                    String.format("布票当前状态【%s】不允许完工操作，仅已开工（STARTED）允许完工", ticket.getStatus()));
        }

        // 7. 委托生产域核心 Service 执行完工
        WeavingTicketCompleteReqVO completeReqVO = WeavingTicketCompleteReqVO.builder()
                .id(ticket.getId())
                .completedAt(LocalDateTime.now())
                .build();
        weavingTicketService.completeTicket(completeReqVO);

        return WorkshopTicketCompleteRespVO.builder()
                .alreadyCompleted(Boolean.FALSE)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "车间移动H5", subType = "布票异常登记", bizNo = "{{#reqVO.code}}", success = "布票【{{#reqVO.code}}】异常登记成功：{{#reqVO.exceptionType}}")
    public void exceptionTicket(WorkshopTicketExceptionReqVO reqVO) {
        // 1. 强制取登录人用户ID，不信任前端传参
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 校验参数
        if (reqVO == null || StrUtil.isBlank(reqVO.getCode())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 3. 按布票编号查询有效布票（非 VOID）
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, reqVO.getCode().trim())
        );
        if (ticket == null) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }
        if ("VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.WEAVING_TICKET_ALREADY_VOIDED);
        }

        // 4. 查询关联机台任务并进行挡车工权限守卫（横向越权拦截）
        MachineTaskDO task = ticket.getMachineTaskId() != null
                ? machineTaskMapper.selectById(ticket.getMachineTaskId())
                : null;
        if (task != null) {
            if (task.getWorkerUserId() != null && !Objects.equals(task.getWorkerUserId(), loginUserId)) {
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
            }
        } else if (ticket.getWorkerUserId() != null && !Objects.equals(ticket.getWorkerUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER);
        }

        // 5. 照片守卫：≤6 张且仅允许图片 URL，超限抛 039-008
        validateExceptionPhotos(reqVO.getExceptionPhotos());

        // 6. 委托生产域核心 Service 执行异常登记（内部校验异常类型字典 015-113 与来源态 GENERATED/STARTED 015-111，写入 exception_photos jsonb 并覆盖前次异常）
        WeavingTicketExceptionReqVO exceptionReqVO = WeavingTicketExceptionReqVO.builder()
                .id(ticket.getId())
                .exceptionType(reqVO.getExceptionType())
                .exceptionNote(reqVO.getExceptionNote())
                .exceptionPhotos(reqVO.getExceptionPhotos())
                .build();
        weavingTicketService.exceptionTicket(exceptionReqVO);
    }

    private void validateExceptionPhotos(List<String> photos) {
        if (CollUtil.isEmpty(photos)) {
            return;
        }
        if (photos.size() > 6) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID);
        }
        for (String photo : photos) {
            if (StrUtil.isBlank(photo) || !IMAGE_URL_PATTERN.matcher(photo.trim()).matches()) {
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID);
            }
        }
    }

    /**
     * 批量加载织造单快照（coding-standards.md §2 batch* 规范）
     */
    private Map<Long, WeavingOrderDO> batchLoadWeavingOrders(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        return weavingOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(WeavingOrderDO::getId, o -> o, (a, b) -> a));
    }

    /**
     * 批量查询机台任务的布票各状态计数（coding-standards.md §2 batch* 规范）
     */
    private Map<Long, Map<String, Integer>> batchLoadTicketStatusCounts(Collection<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        List<WeavingTicketDO> tickets = weavingTicketMapper.selectList(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .in(WeavingTicketDO::getMachineTaskId, taskIds)
                        .select(WeavingTicketDO::getMachineTaskId, WeavingTicketDO::getStatus)
        );
        Map<Long, Map<String, Integer>> resultMap = new HashMap<>();
        for (WeavingTicketDO ticket : tickets) {
            if (ticket.getMachineTaskId() == null || ticket.getStatus() == null) {
                continue;
            }
            resultMap
                    .computeIfAbsent(ticket.getMachineTaskId(), k -> new HashMap<>())
                    .merge(ticket.getStatus(), 1, Integer::sum);
        }
        return resultMap;
    }

    @Override
    public PageResult<WorkshopInspectionRespVO> getInspectionPage(WorkshopInspectionPageReqVO reqVO) {
        // 1. 强制登录校验
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 解析状态过滤条件（缺省默认 DRAFT, ACTIVE）
        List<String> statuses;
        if (StrUtil.isNotBlank(reqVO.getStatus())) {
            statuses = Arrays.stream(reqVO.getStatus().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
            if (statuses.isEmpty()) {
                statuses = DEFAULT_INSPECTION_STATUSES;
            }
        } else {
            statuses = DEFAULT_INSPECTION_STATUSES;
        }

        // 3. 构建查询（租户隔离由框架自动追加，不按机台过滤）
        LambdaQueryWrapperX<InspectionOrderDO> query = new LambdaQueryWrapperX<InspectionOrderDO>()
                .in(InspectionOrderDO::getStatus, statuses)
                .eqIfPresent(InspectionOrderDO::getType, StrUtil.isNotBlank(reqVO.getType()) ? reqVO.getType().trim() : null)
                .orderByDesc(InspectionOrderDO::getOrderDate)
                .orderByDesc(InspectionOrderDO::getId);
        PageResult<InspectionOrderDO> pageResult = inspectionOrderMapper.selectPage(reqVO, query);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }

        // 4. 批量统计已录卷数/件数（防 N+1，coding-standards.md §2 batch* 规范）
        List<Long> orderIds = pageResult.getList().stream()
                .map(InspectionOrderDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Integer> countMap = inspectionOrderItemMapper.selectCountMapByInspectionOrderIds(orderIds);

        // 5. 组装 H5 精简 VO
        List<WorkshopInspectionRespVO> voList = pageResult.getList().stream().map(order -> WorkshopInspectionRespVO.builder()
                .id(order.getId())
                .code(order.getCode())
                .type(order.getType())
                .packageMode(order.getPackageMode())
                .status(order.getStatus())
                .productId(order.getProductId())
                .productCode(order.getProductCode())
                .productName(order.getProductName())
                .greigeOrderId(order.getGreigeOrderId())
                .dyeingOrderId(order.getDyeingOrderId())
                .inspectionFactoryId(order.getInspectionFactoryId())
                .inspectionFactoryName(order.getInspectionFactoryName())
                .salesContractId(order.getSalesContractId())
                .contractNo(order.getContractNo())
                .targetWeight(order.getTargetWeight())
                .orderDate(order.getOrderDate())
                .remark(order.getRemark())
                .rollCount(countMap.getOrDefault(order.getId(), 0))
                .createTime(order.getCreateTime())
                .build()).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public WorkshopInspectionDetailRespVO getInspection(Long id) {
        // 1. 强制登录校验
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 校验参数与单据存在性（租户隔离自动生效，跨租户构造 id 查无）
        if (id == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS);
        }
        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        if (order == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS);
        }

        // 3. 状态校验：仅 DRAFT/ACTIVE 可进，COMPLETED 只读；CANCELLED 抛 010-002
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED);
        }
        boolean readonly = "COMPLETED".equalsIgnoreCase(order.getStatus());

        // 4. 查询录检明细（按 id 倒序展示最新录入）
        List<InspectionOrderItemDO> itemDOList = inspectionOrderItemMapper.selectList(
                new LambdaQueryWrapperX<InspectionOrderItemDO>()
                        .eq(InspectionOrderItemDO::getInspectionOrderId, id)
                        .orderByDesc(InspectionOrderItemDO::getId)
        );

        // 5. 组装明细 VO（含 photos 照片列表）
        List<WorkshopInspectionItemRespVO> itemVOList = CollUtil.isEmpty(itemDOList)
                ? Collections.emptyList()
                : itemDOList.stream().map(this::buildInspectionItemRespVO).collect(Collectors.toList());

        // 6. 组装单头+明细详情 VO
        return WorkshopInspectionDetailRespVO.builder()
                .id(order.getId())
                .code(order.getCode())
                .type(order.getType())
                .packageMode(order.getPackageMode())
                .status(order.getStatus())
                .productId(order.getProductId())
                .productCode(order.getProductCode())
                .productName(order.getProductName())
                .greigeOrderId(order.getGreigeOrderId())
                .dyeingOrderId(order.getDyeingOrderId())
                .inspectionFactoryId(order.getInspectionFactoryId())
                .inspectionFactoryName(order.getInspectionFactoryName())
                .salesContractId(order.getSalesContractId())
                .contractNo(order.getContractNo())
                .targetWeight(order.getTargetWeight())
                .orderDate(order.getOrderDate())
                .remark(order.getRemark())
                .rollCount(itemVOList.size())
                .readonly(readonly)
                .items(itemVOList)
                .createTime(order.getCreateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkshopTicketScanRespVO ticketScan(String code) {
        // 1. 强制登录校验
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 规则 ① 按 code 查布票（不存在/VOID → 039-001）
        if (StrUtil.isBlank(code)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }
        WeavingTicketDO ticket = weavingTicketMapper.selectOne(
                new LambdaQueryWrapperX<WeavingTicketDO>()
                        .eq(WeavingTicketDO::getCode, code.trim())
        );
        if (ticket == null || "VOID".equalsIgnoreCase(ticket.getStatus())) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
        }

        // 状态必须 COMPLETED/INSPECTED/STOCKED 之一，未完工 → 039-003，其余不可录态 → 039-004
        String status = ticket.getStatus();
        if ("GENERATED".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_TICKET_NOT_COMPLETED);
        }
        if (!"COMPLETED".equalsIgnoreCase(status) && !"INSPECTED".equalsIgnoreCase(status) && !"STOCKED".equalsIgnoreCase(status)) {
            throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_TICKET_STATUS_NOT_ALLOW_INSPECTION);
        }

        // 3. 规则 ② 内部直接调 weavingOrderService.handoffInspection(ticket.weavingOrderId)
        // 幂等：已交接复用，按胚布单归一 find-or-create 仅建检验单表头，ADR-0025
        // H5 Service 调用绕过 Web 权限点，不要求检验员具备 :handoff-inspection
        WeavingOrderHandoffInspectionRespVO handoffResp = weavingOrderService.handoffInspection(ticket.getWeavingOrderId());
        Long inspectionOrderId = handoffResp.getInspectionOrderId();

        // 4. 规则 ④ 该检验单已 COMPLETED/CANCELLED → 010-002
        InspectionOrderDO inspectionOrder = inspectionOrderMapper.selectById(inspectionOrderId);
        if (inspectionOrder != null && ("COMPLETED".equalsIgnoreCase(inspectionOrder.getStatus()) || "CANCELLED".equalsIgnoreCase(inspectionOrder.getStatus()))) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED);
        }

        // 5. 规则 ③ 按 weavingTicketId 查录检行：存在 → 返回 {inspectionOrderId, item} 供改录；不存在 → 返回 {inspectionOrderId, item:null, suggestedPieceNo: 布票code}
        List<InspectionOrderItemDO> items = inspectionOrderItemMapper.selectList(
                new LambdaQueryWrapperX<InspectionOrderItemDO>()
                        .eq(InspectionOrderItemDO::getInspectionOrderId, inspectionOrderId)
                        .eq(InspectionOrderItemDO::getWeavingTicketId, ticket.getId())
                        .orderByDesc(InspectionOrderItemDO::getId)
        );

        if (CollUtil.isNotEmpty(items)) {
            InspectionOrderItemDO item = items.get(0);
            WorkshopInspectionItemRespVO itemVO = buildInspectionItemRespVO(item);
            return WorkshopTicketScanRespVO.builder()
                    .inspectionOrderId(inspectionOrderId)
                    .weavingTicketId(ticket.getId())
                    .suggestedPieceNo(null)
                    .item(itemVO)
                    .build();
        }

        return WorkshopTicketScanRespVO.builder()
                .inspectionOrderId(inspectionOrderId)
                .weavingTicketId(ticket.getId())
                .suggestedPieceNo(ticket.getCode())
                .item(null)
                .build();
    }

    private WorkshopInspectionItemRespVO buildInspectionItemRespVO(InspectionOrderItemDO item) {
        if (item == null) {
            return null;
        }
        return WorkshopInspectionItemRespVO.builder()
                .id(item.getId())
                .inspectionOrderId(item.getInspectionOrderId())
                .rollNo(item.getRollNo())
                .pieceNo(item.getPieceNo())
                .weavingTicketId(item.getWeavingTicketId())
                .vatNo(item.getVatNo())
                .measuredWeight(item.getMeasuredWeight())
                .lengthM(item.getLengthM())
                .machineNo(item.getMachineNo())
                .grade(item.getGrade())
                .colorName(item.getColorName())
                .colorNo(item.getColorNo())
                .styleNo(item.getStyleNo())
                .productCode(item.getProductCode())
                .productName(item.getProductName())
                .pieceCount(item.getPieceCount())
                .remark(item.getRemark())
                .photos(item.getPhotos() != null ? item.getPhotos() : Collections.emptyList())
                .createTime(item.getCreateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "车间移动H5", subType = "录检行保存", bizNo = "{{#reqVO.inspectionOrderId}}", success = "保存检验单【{{#reqVO.inspectionOrderId}}】录检明细成功")
    public Long saveInspectionItem(WorkshopInspectionItemSaveReqVO reqVO) {
        // 1. 登录校验
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }

        // 2. 校验所属检验单存在且状态可录（仅 DRAFT/ACTIVE 允许，COMPLETED/CANCELLED 抛 010-002）
        if (reqVO.getInspectionOrderId() == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS);
        }
        InspectionOrderDO inspectionOrder = inspectionOrderMapper.selectById(reqVO.getInspectionOrderId());
        if (inspectionOrder == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS);
        }
        if (!"DRAFT".equalsIgnoreCase(inspectionOrder.getStatus()) && !"ACTIVE".equalsIgnoreCase(inspectionOrder.getStatus())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_STATUS_NOT_ALLOWED);
        }

        // 3. 照片校验守卫（≤6 张，非法或超量抛 039-008）
        validateExceptionPhotos(reqVO.getPhotos());

        // 4. 构建生产域保存 VO 并根据包装模式分化处理
        boolean isRollMode = "ROLL".equalsIgnoreCase(inspectionOrder.getPackageMode())
                || ("FINISHED".equalsIgnoreCase(inspectionOrder.getType()) && !"PIECE".equalsIgnoreCase(inspectionOrder.getPackageMode()));
        boolean isPieceMode = !isRollMode;
        InspectionOrderItemSaveReqVO saveReqVO = new InspectionOrderItemSaveReqVO();
        saveReqVO.setId(reqVO.getId());
        saveReqVO.setInspectionOrderId(reqVO.getInspectionOrderId());
        saveReqVO.setGrade(reqVO.getGrade());
        saveReqVO.setMeasuredWeight(reqVO.getMeasuredWeight());
        saveReqVO.setLengthM(reqVO.getLengthM());
        saveReqVO.setRemark(reqVO.getRemark());
        saveReqVO.setPhotos(reqVO.getPhotos());

        if (isPieceMode) {
            // 胚布 PIECE 模式：weavingTicketId 必填带回
            if (reqVO.getWeavingTicketId() == null) {
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
            }
            WeavingTicketDO ticket = weavingTicketMapper.selectById(reqVO.getWeavingTicketId());
            if (ticket == null || "VOID".equalsIgnoreCase(ticket.getStatus())) {
                throw ServiceExceptionUtil.exception(WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS);
            }

            // pieceNo 缺省=布票完整 code（可改，单内 (vatNo,pieceNo) 唯一性由既有 service 校验）
            String pieceNo = StrUtil.isNotBlank(reqVO.getPieceNo()) ? reqVO.getPieceNo().trim() : ticket.getCode();
            saveReqVO.setPieceNo(pieceNo);
            saveReqVO.setWeavingTicketId(ticket.getId());
            saveReqVO.setVatNo(reqVO.getVatNo());
            saveReqVO.setRollNo(reqVO.getRollNo());

            // 机台号/产品编码品名/颜色快照由服务端从布票→织造单/胚布单补齐，不信任也不采用客户端快照
            populateItemSnapshots(saveReqVO, ticket);

            // 改录判定：已传 id 直接改录；若未传 id 但单内已存在同 weavingTicketId 的明细，自动定位改录原行
            if (saveReqVO.getId() != null) {
                inspectionOrderItemService.updateInspectionOrderItem(saveReqVO);
                return saveReqVO.getId();
            }

            List<InspectionOrderItemDO> existItems = inspectionOrderItemMapper.selectList(
                    new LambdaQueryWrapperX<InspectionOrderItemDO>()
                            .eq(InspectionOrderItemDO::getInspectionOrderId, reqVO.getInspectionOrderId())
                            .eq(InspectionOrderItemDO::getWeavingTicketId, ticket.getId())
            );
            if (CollUtil.isNotEmpty(existItems)) {
                saveReqVO.setId(existItems.get(0).getId());
                inspectionOrderItemService.updateInspectionOrderItem(saveReqVO);
                return saveReqVO.getId();
            }

            return inspectionOrderItemService.createInspectionOrderItem(saveReqVO);
        } else {
            // 成品 ROLL 模式（来自检验单内逐卷录）：vatNo、rollNo 必填
            String vatNo = StrUtil.isNotBlank(reqVO.getVatNo()) ? reqVO.getVatNo().trim() : null;
            String rollNo = StrUtil.isNotBlank(reqVO.getRollNo()) ? reqVO.getRollNo().trim() : null;
            if (StrUtil.isBlank(vatNo)) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_VAT_NO_REQUIRED);
            }
            if (StrUtil.isBlank(rollNo)) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INSPECTION_ORDER_ROLL_NO_REQUIRED);
            }

            // 保存前按 (inspectionOrderId, vatNo, rollNo) 查重，命中已存在行 → 不覆盖，返回 039-005 + 该行数据
            InspectionOrderItemDO exist = inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(
                    reqVO.getInspectionOrderId(), vatNo, rollNo
            );
            if (exist != null) {
                // 若为新建（id==null）或改录了其他行的缸号卷号（id!=exist.getId()），触发 039-005 查重拦截并携带该行数据
                if (reqVO.getId() == null || !exist.getId().equals(reqVO.getId())) {
                    throw ServiceExceptionUtil.exceptionWithData(
                            WorkshopH5ErrorCodeConstants.H5_INSPECTION_ITEM_DUPLICATE,
                            buildInspectionItemRespVO(exist)
                    );
                }
            }

            saveReqVO.setVatNo(vatNo);
            saveReqVO.setRollNo(rollNo);
            saveReqVO.setPieceNo(null);
            if (StrUtil.isBlank(saveReqVO.getProductCode())) {
                saveReqVO.setProductCode(inspectionOrder.getProductCode());
            }
            if (StrUtil.isBlank(saveReqVO.getProductName())) {
                saveReqVO.setProductName(inspectionOrder.getProductName());
            }

            if (saveReqVO.getId() != null) {
                inspectionOrderItemService.updateInspectionOrderItem(saveReqVO);
                return saveReqVO.getId();
            }
            return inspectionOrderItemService.createInspectionOrderItem(saveReqVO);
        }
    }

    private void populateItemSnapshots(InspectionOrderItemSaveReqVO saveReqVO, WeavingTicketDO ticket) {
        String machineNo = StrUtil.isNotBlank(ticket.getActualMachine()) ? ticket.getActualMachine() : ticket.getPlannedMachine();
        saveReqVO.setMachineNo(machineNo);

        if (ticket.getWeavingOrderId() == null) {
            return;
        }
        WeavingOrderDO weavingOrder = weavingOrderMapper.selectById(ticket.getWeavingOrderId());
        if (weavingOrder == null) {
            return;
        }

        String productCode = weavingOrder.getProductCode();
        String productName = weavingOrder.getProductName();
        String colorName = null;

        if (weavingOrder.getGreigeOrderId() != null) {
            GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(weavingOrder.getGreigeOrderId());
            if (greigeOrder != null) {
                if (StrUtil.isBlank(productCode)) {
                    productCode = greigeOrder.getProductCode();
                }
                if (StrUtil.isBlank(productName)) {
                    productName = greigeOrder.getProductName();
                }
            }
            List<GreigeOrderColorDO> colors = greigeOrderColorMapper.selectList(
                    new LambdaQueryWrapperX<GreigeOrderColorDO>()
                            .eq(GreigeOrderColorDO::getOrderId, weavingOrder.getGreigeOrderId())
                            .orderByAsc(GreigeOrderColorDO::getLineNo)
            );
            if (CollUtil.isNotEmpty(colors)) {
                colorName = colors.get(0).getColor();
            }
        }

        saveReqVO.setProductCode(productCode);
        saveReqVO.setProductName(productName);
        saveReqVO.setColorName(colorName);
    }

}
