package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackColorSectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackStatisticsRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackVatRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note.ProgressFeedbackNoteDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.note.ProgressFeedbackNoteMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.progressfeedback.ProgressFeedbackStatusEnum;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackAdvanceReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarVatVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackUpdateReqVO;
import jakarta.annotation.Resource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_DYEING_COLOR_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_STATUS_ADVANCE_NOT_ALLOWED;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_STATUS_ROLLBACK_NOT_ALLOWED;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_VAT_NO_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_VAT_NOT_EXISTS;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 生产管理-进度反馈 Service 实现类
 *
 * 核心读模型（ADR-0031）：
 * 1. 骨架查询：以染色颜色明细行为骨架，仅聚合 ACTIVE 与 COMPLETED 染色单（DRAFT/CANCELLED 排除）。
 * 2. 零快照：合同号/客户/染厂/产品/颜色等展示字段实时关联取出。
 * 3. 挂载聚合：每节 LEFT JOIN 挂载反馈缸，派生计算延期标记、排产覆盖率、超排标记。
 * 4. 零缸虚拟节：没有反馈缸的颜色行渲染空缸列表供前端渲染虚线 "+ 建缸" 占位卡。
 * 5. 统计条：未排缸颜色行数、进行中缸数、已完成缸数、延期缸数与当前筛选同口径联动。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ProgressFeedbackServiceImpl implements ProgressFeedbackService {

    @Resource
    private ProgressFeedbackMapper progressFeedbackMapper;

    @Resource
    private ProgressFeedbackNoteMapper progressFeedbackNoteMapper;

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Resource
    private DyeingColorMapper dyeingColorMapper;

    /**
     * 当前日期提供器（默认 LocalDate::now，支持单测注入固定时钟）
     */
    @Setter
    private Supplier<LocalDate> todayProvider = LocalDate::now;

    @Override
    public PageResult<ProgressFeedbackColorSectionRespVO> getProgressFeedbackPage(ProgressFeedbackPageReqVO reqVO) {
        LocalDate today = todayProvider.get();

        // 1. 查询符合条件的染色单（仅 ACTIVE 与 COMPLETED，默认排除软删）
        List<DyeingOrderDO> eligibleOrders = getEligibleOrders(reqVO);
        if (CollUtil.isEmpty(eligibleOrders)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        Map<Long, DyeingOrderDO> orderMap = eligibleOrders.stream()
                .collect(Collectors.toMap(DyeingOrderDO::getId, o -> o, (a, b) -> a));
        List<Long> orderIds = eligibleOrders.stream().map(DyeingOrderDO::getId).collect(Collectors.toList());

        // 2. 查询所属颜色明细行，保持单据顺序及行号 lineNo 升序
        boolean includeDeleted = Boolean.TRUE.equals(reqVO.getIncludeDeleted());
        List<DyeingColorDO> colors = getOrderedColors(orderIds, eligibleOrders, includeDeleted);
        if (CollUtil.isEmpty(colors)) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 3. 批量查询反馈缸与未决问题
        Set<Long> colorIds = colors.stream().map(DyeingColorDO::getId).collect(Collectors.toSet());
        List<ProgressFeedbackDO> allVats = progressFeedbackMapper.selectListByColorIds(colorIds);
        Map<Long, List<ProgressFeedbackDO>> vatsByColor = allVats.stream()
                .filter(v -> v.getDeleted() == null || !v.getDeleted())
                .collect(Collectors.groupingBy(ProgressFeedbackDO::getDyeingColorId));

        Map<Long, Integer> unresolvedCountMap = getUnresolvedCountMap(allVats);

        // 4. 骨架筛选下推（状态、延期、未决问题、含已完成、综合关键词）
        List<DyeingColorDO> matchedColors = filterColors(colors, orderMap, vatsByColor, unresolvedCountMap, reqVO, today);

        long total = matchedColors.size();
        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 5. 颜色行骨架分页切片
        int pageNo = reqVO.getPageNo() != null && reqVO.getPageNo() > 0 ? reqVO.getPageNo() : 1;
        int pageSize = reqVO.getPageSize() != null && reqVO.getPageSize() > 0 ? reqVO.getPageSize() : 10;
        int fromIndex = (pageNo - 1) * pageSize;
        if (fromIndex >= total) {
            return new PageResult<>(Collections.emptyList(), total);
        }
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<DyeingColorDO> pagedColors = matchedColors.subList(fromIndex, toIndex);

        // 6. 组装响应节（挂载反馈缸与派生计算）
        List<ProgressFeedbackColorSectionRespVO> sections = new ArrayList<>(pagedColors.size());
        for (DyeingColorDO color : pagedColors) {
            DyeingOrderDO order = orderMap.get(color.getDyeingOrderId());
            List<ProgressFeedbackDO> vats = vatsByColor.getOrDefault(color.getId(), Collections.emptyList());
            sections.add(buildColorSection(order, color, vats, unresolvedCountMap, reqVO, today));
        }

        return new PageResult<>(sections, total);
    }

    @Override
    public List<ProgressFeedbackColorSectionRespVO> getProgressFeedbackListByOrder(Long dyeingOrderId) {
        if (dyeingOrderId == null) {
            return Collections.emptyList();
        }
        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        reqVO.setDyeingOrderId(dyeingOrderId);
        reqVO.setPageNo(1);
        reqVO.setPageSize(1000); // 染色单工作台页签不分页，取全量颜色行

        PageResult<ProgressFeedbackColorSectionRespVO> pageResult = getProgressFeedbackPage(reqVO);
        return pageResult.getList();
    }

    @Override
    public ProgressFeedbackStatisticsRespVO getProgressFeedbackStatistics(ProgressFeedbackPageReqVO reqVO) {
        LocalDate today = todayProvider.get();

        // 1. 同口径查询符合基础筛选条件的染色单
        List<DyeingOrderDO> eligibleOrders = getEligibleOrders(reqVO);
        if (CollUtil.isEmpty(eligibleOrders)) {
            return ProgressFeedbackStatisticsRespVO.builder()
                    .unscheduledColorCount(0L)
                    .inProgressVatCount(0L)
                    .completedVatCount(0L)
                    .delayedVatCount(0L)
                    .build();
        }

        Map<Long, DyeingOrderDO> orderMap = eligibleOrders.stream()
                .collect(Collectors.toMap(DyeingOrderDO::getId, o -> o, (a, b) -> a));
        List<Long> orderIds = eligibleOrders.stream().map(DyeingOrderDO::getId).collect(Collectors.toList());

        // 2. 查询所属颜色明细行
        boolean includeDeleted = Boolean.TRUE.equals(reqVO.getIncludeDeleted());
        List<DyeingColorDO> colors = getOrderedColors(orderIds, eligibleOrders, includeDeleted);
        if (CollUtil.isEmpty(colors)) {
            return ProgressFeedbackStatisticsRespVO.builder()
                    .unscheduledColorCount(0L)
                    .inProgressVatCount(0L)
                    .completedVatCount(0L)
                    .delayedVatCount(0L)
                    .unresolvedVatCount(0L)
                    .build();
        }

        // 3. 批量查询反馈缸与未决问题
        Set<Long> colorIds = colors.stream().map(DyeingColorDO::getId).collect(Collectors.toSet());
        List<ProgressFeedbackDO> allVats = progressFeedbackMapper.selectListByColorIds(colorIds);
        Map<Long, List<ProgressFeedbackDO>> vatsByColor = allVats.stream()
                .filter(v -> v.getDeleted() == null || !v.getDeleted())
                .collect(Collectors.groupingBy(ProgressFeedbackDO::getDyeingColorId));

        Map<Long, Integer> unresolvedCountMap = getUnresolvedCountMap(allVats);

        // 4. 若传入综合关键词，对颜色行进行关键词过滤，保持统计与筛选同口径
        if (StringUtils.hasText(reqVO.getKeyword())) {
            colors = colors.stream().filter(color -> {
                List<ProgressFeedbackDO> vats = vatsByColor.getOrDefault(color.getId(), Collections.emptyList());
                return matchesKeyword(color, vats, reqVO.getKeyword());
            }).collect(Collectors.toList());
        }

        // 5. 计算统计条指标
        // 未排缸颜色行数 = 零缸节数
        long unscheduledColorCount = colors.stream()
                .filter(c -> CollUtil.isEmpty(vatsByColor.get(c.getId())))
                .count();

        long inProgressVatCount = 0L;
        long completedVatCount = 0L;
        long delayedVatCount = 0L;
        long unresolvedVatCount = 0L;

        Set<Long> matchedColorIds = colors.stream().map(DyeingColorDO::getId).collect(Collectors.toSet());
        for (ProgressFeedbackDO vat : allVats) {
            if (Boolean.TRUE.equals(vat.getDeleted()) || !matchedColorIds.contains(vat.getDyeingColorId())) {
                continue;
            }
            String status = vat.getStatus();
            if (ProgressFeedbackStatusEnum.IN_PROGRESS_STATUSES.contains(status)) {
                inProgressVatCount++;
            } else if (ProgressFeedbackStatusEnum.COMPLETED.getValue().equals(status)) {
                completedVatCount++;
            }

            DyeingOrderDO order = orderMap.get(vat.getDyeingOrderId());
            if (order != null && isDelayed(order.getDeliveryDate(), status, today)) {
                delayedVatCount++;
            }

            if (unresolvedCountMap.getOrDefault(vat.getId(), 0) > 0) {
                unresolvedVatCount++;
            }
        }

        return ProgressFeedbackStatisticsRespVO.builder()
                .unscheduledColorCount(unscheduledColorCount)
                .inProgressVatCount(inProgressVatCount)
                .completedVatCount(completedVatCount)
                .delayedVatCount(delayedVatCount)
                .unresolvedVatCount(unresolvedVatCount)
                .build();
    }

    @Override
    public ProgressFeedbackCalendarRespVO getCalendar(ProgressFeedbackCalendarReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new ProgressFeedbackCalendarReqVO();
        }

        // 1. 计算日期区间 [startDate, endDate]
        LocalDate startDate;
        LocalDate endDate;
        if (reqVO.getFrom() != null && reqVO.getTo() != null) {
            startDate = reqVO.getFrom();
            endDate = reqVO.getTo();
        } else if (StringUtils.hasText(reqVO.getMonth())) {
            try {
                YearMonth ym = YearMonth.parse(reqVO.getMonth().trim());
                startDate = ym.atDay(1);
                endDate = ym.atEndOfMonth();
            } catch (DateTimeParseException e) {
                throw invalidParamException("月份格式不正确，应为 yyyy-MM");
            }
        } else if (reqVO.getFrom() != null) {
            startDate = reqVO.getFrom();
            endDate = reqVO.getFrom();
        } else if (reqVO.getTo() != null) {
            startDate = reqVO.getTo();
            endDate = reqVO.getTo();
        } else {
            YearMonth ym = YearMonth.from(todayProvider.get());
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }

        if (startDate.isAfter(endDate)) {
            return new ProgressFeedbackCalendarRespVO(Collections.emptyList());
        }

        // 2. 查询落在区间内的反馈缸（支持 status 过滤）
        List<ProgressFeedbackDO> vats = progressFeedbackMapper.selectCalendarList(startDate, endDate, reqVO.getStatus());
        if (CollUtil.isEmpty(vats)) {
            return new ProgressFeedbackCalendarRespVO(Collections.emptyList());
        }

        // 内存双重防御：确保区间、状态与软删除过滤在 Mock 单测中依然生效
        LocalDate finalStart = startDate;
        LocalDate finalEnd = endDate;
        String reqStatus = reqVO.getStatus();
        vats = vats.stream()
                .filter(v -> v.getDeleted() == null || !v.getDeleted())
                .filter(v -> v.getScheduledDate() != null)
                .filter(v -> !v.getScheduledDate().isBefore(finalStart) && !v.getScheduledDate().isAfter(finalEnd))
                .filter(v -> !StringUtils.hasText(reqStatus) || Objects.equals(v.getStatus(), reqStatus))
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(vats)) {
            return new ProgressFeedbackCalendarRespVO(Collections.emptyList());
        }

        // 3. 批量查询关联染色单与颜色明细（零展示快照）
        Set<Long> orderIds = vats.stream()
                .map(ProgressFeedbackDO::getDyeingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> colorIds = vats.stream()
                .map(ProgressFeedbackDO::getDyeingColorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (orderIds.isEmpty()) {
            return new ProgressFeedbackCalendarRespVO(Collections.emptyList());
        }

        List<DyeingOrderDO> orders = dyeingOrderMapper.selectListByIds(orderIds);
        // 仅 ACTIVE 与 COMPLETED 染色单（CANCELLED/DRAFT 等排除），未软删，且匹配可选承染厂过滤
        Long factoryFilter = reqVO.getDyeingFactoryId();
        Map<Long, DyeingOrderDO> orderMap = (orders != null ? orders : Collections.<DyeingOrderDO>emptyList()).stream()
                .filter(o -> o.getDeleted() == null || !o.getDeleted())
                .filter(o -> "ACTIVE".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus()))
                .filter(o -> factoryFilter == null || Objects.equals(o.getDyeingFactoryId(), factoryFilter))
                .collect(Collectors.toMap(DyeingOrderDO::getId, o -> o, (a, b) -> a));

        if (orderMap.isEmpty()) {
            return new ProgressFeedbackCalendarRespVO(Collections.emptyList());
        }

        List<DyeingColorDO> colors = colorIds.isEmpty() ? Collections.emptyList() : dyeingColorMapper.selectListByIds(colorIds);
        Map<Long, DyeingColorDO> colorMap = (colors != null ? colors : Collections.<DyeingColorDO>emptyList()).stream()
                .filter(c -> c.getDeleted() == null || !c.getDeleted())
                .collect(Collectors.toMap(DyeingColorDO::getId, c -> c, (a, b) -> a));

        // 4. 组装扁平缸列表
        List<ProgressFeedbackCalendarVatVO> vatVOList = new ArrayList<>();
        for (ProgressFeedbackDO vat : vats) {
            DyeingOrderDO order = orderMap.get(vat.getDyeingOrderId());
            if (order == null) {
                // 染色单不满足条件（非 ACTIVE/COMPLETED、已软删、承染厂不匹配等）
                continue;
            }
            DyeingColorDO color = colorMap.get(vat.getDyeingColorId());

            ProgressFeedbackCalendarVatVO vo = new ProgressFeedbackCalendarVatVO();
            vo.setFeedbackId(vat.getId());
            vo.setDyeingOrderId(vat.getDyeingOrderId());
            vo.setDyeingColorId(vat.getDyeingColorId());
            vo.setVatNo(vat.getVatNo());
            vo.setScheduledDate(vat.getScheduledDate());
            vo.setStatus(vat.getStatus());
            vo.setQuantity(vat.getQuantity());
            vo.setPieces(vat.getPieces());

            if (color != null) {
                vo.setUnit(color.getDyeingInputUnit());
                vo.setCustomerColorNo(color.getCustomerColorNo());
                vo.setColorName(color.getColorName());
            }

            vo.setContractNo(order.getContractNo());
            vo.setDyeingFactoryId(order.getDyeingFactoryId());
            vo.setDyeingFactoryName(order.getDyeingFactoryName());

            vatVOList.add(vo);
        }

        // 5. 排序：按 scheduledDate 升序、vatNo 升序、feedbackId 升序
        vatVOList.sort(Comparator.comparing(ProgressFeedbackCalendarVatVO::getScheduledDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProgressFeedbackCalendarVatVO::getVatNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProgressFeedbackCalendarVatVO::getFeedbackId, Comparator.nullsLast(Comparator.naturalOrder())));

        return new ProgressFeedbackCalendarRespVO(vatVOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProgressFeedback(ProgressFeedbackCreateReqVO reqVO) {
        // 1. 校验数量 > 0
        if (reqVO.getQuantity() == null || reqVO.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("本缸数量必须大于0");
        }

        // 2. 校验染色单存在且状态为 ACTIVE 或 COMPLETED，且未软删（否则 1-030-017-002）
        DyeingOrderDO order = dyeingOrderMapper.selectById(reqVO.getDyeingOrderId());
        if (order == null || Boolean.TRUE.equals(order.getDeleted())
                || (!"ACTIVE".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus()))) {
            throw exception(PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED);
        }

        // 3. 校验染色颜色行存在且属于该单，且未软删（否则 1-030-017-003）
        DyeingColorDO color = dyeingColorMapper.selectById(reqVO.getDyeingColorId());
        if (color == null || Boolean.TRUE.equals(color.getDeleted())
                || !Objects.equals(color.getDyeingOrderId(), reqVO.getDyeingOrderId())) {
            throw exception(PROGRESS_FEEDBACK_DYEING_COLOR_NOT_EXISTS);
        }

        // 4. 校验同一染色单同一颜色行下缸号不重复（否则 1-030-017-001）
        String vatNo = reqVO.getVatNo() != null ? reqVO.getVatNo().trim() : "";
        ProgressFeedbackDO existing = progressFeedbackMapper.selectByColorIdAndVatNo(reqVO.getDyeingColorId(), vatNo);
        if (existing != null) {
            throw exception(PROGRESS_FEEDBACK_VAT_NO_DUPLICATE, vatNo);
        }

        // 5. 组装并插入新缸（初始状态为 SCHEDULED，排缸日期缺省当天）
        LocalDate scheduledDate = reqVO.getScheduledDate() != null ? reqVO.getScheduledDate() : todayProvider.get();
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setDyeingOrderId(reqVO.getDyeingOrderId());
        vat.setDyeingColorId(reqVO.getDyeingColorId());
        vat.setVatNo(vatNo);
        vat.setQuantity(reqVO.getQuantity());
        vat.setPieces(reqVO.getPieces());
        vat.setStatus(ProgressFeedbackStatusEnum.SCHEDULED.getValue());
        vat.setScheduledDate(scheduledDate);
        vat.setRemark(reqVO.getRemark());

        progressFeedbackMapper.insert(vat);
        return vat.getId();
    }

    @Override
    public ProgressFeedbackDetailRespVO getProgressFeedback(Long id) {
        // 1. 查询缸
        ProgressFeedbackDO vat = progressFeedbackMapper.selectById(id);
        if (vat == null || Boolean.TRUE.equals(vat.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_VAT_NOT_EXISTS);
        }

        // 2. 查询染色单与颜色行实时信息（零快照）
        DyeingOrderDO order = dyeingOrderMapper.selectById(vat.getDyeingOrderId());
        DyeingColorDO color = dyeingColorMapper.selectById(vat.getDyeingColorId());

        // 3. 查询同颜色行下的所有缸计算覆盖率与总数量
        List<ProgressFeedbackDO> colorVats = progressFeedbackMapper.selectListByColorIds(Set.of(vat.getDyeingColorId()));
        if (colorVats != null) {
            colorVats = colorVats.stream()
                    .filter(v -> v.getDeleted() == null || !v.getDeleted())
                    .collect(Collectors.toList());
        } else {
            colorVats = Collections.emptyList();
        }

        BigDecimal totalVatQty = BigDecimal.ZERO;
        int totalVatPieces = 0;
        for (ProgressFeedbackDO v : colorVats) {
            if (v.getQuantity() != null) {
                totalVatQty = totalVatQty.add(v.getQuantity());
            }
            if (v.getPieces() != null) {
                totalVatPieces += v.getPieces();
            }
        }

        // 4. 查询未决问题数
        Map<Long, Integer> unresolvedMap = getUnresolvedCountMap(List.of(vat));

        // 5. 组装 DetailRespVO
        LocalDate today = todayProvider.get();
        ProgressFeedbackDetailRespVO respVO = BeanUtils.toBean(vat, ProgressFeedbackDetailRespVO.class);
        respVO.setUnresolvedIssueCount(unresolvedMap.getOrDefault(vat.getId(), 0));

        if (order != null) {
            respVO.setSalesContractId(order.getSalesContractId());
            respVO.setContractNo(order.getContractNo());
            respVO.setCustomerId(order.getCustomerId());
            respVO.setClientName(order.getClientName());
            respVO.setDyeingFactoryId(order.getDyeingFactoryId());
            respVO.setDyeingFactoryName(order.getDyeingFactoryName());
            respVO.setDeliveryDate(order.getDeliveryDate());
            respVO.setProductCode(order.getProductCode());
            respVO.setProductName(order.getProductName());
            respVO.setDyeingOrderStatus(order.getStatus());
            respVO.setDelayed(isDelayed(order.getDeliveryDate(), vat.getStatus(), today));
        } else {
            respVO.setDelayed(false);
        }

        if (color != null) {
            respVO.setLineNo(color.getLineNo());
            respVO.setCustomerColorNo(color.getCustomerColorNo());
            respVO.setColorName(color.getColorName());
            BigDecimal colorQty = color.getQuantity() != null ? color.getQuantity() : BigDecimal.ZERO;
            respVO.setColorQuantity(colorQty);
            respVO.setColorPieces(color.getPieces());
            respVO.setUnit(color.getDyeingInputUnit());

            // 覆盖率与超排
            if (color.getQuantity() == null || color.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.setCoveragePercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                respVO.setOverScheduled(totalVatQty.compareTo(BigDecimal.ZERO) > 0);
            } else {
                BigDecimal cov = totalVatQty.divide(color.getQuantity(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                respVO.setCoveragePercent(cov);
                respVO.setOverScheduled(totalVatQty.compareTo(color.getQuantity()) > 0);
            }
        }

        respVO.setTotalVatQuantity(totalVatQty.setScale(2, RoundingMode.HALF_UP));
        respVO.setTotalVatPieces(totalVatPieces);

        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgressFeedback(ProgressFeedbackUpdateReqVO reqVO) {
        // 1. 查缸存在性
        ProgressFeedbackDO vat = progressFeedbackMapper.selectById(reqVO.getId());
        if (vat == null || Boolean.TRUE.equals(vat.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_VAT_NOT_EXISTS);
        }

        // 2. 校验数量 > 0
        if (reqVO.getQuantity() == null || reqVO.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("本缸数量必须大于0");
        }

        // 3. 同单同色缸号唯一性校验（若修改缸号，不能与同色下其它缸撞重）
        String newVatNo = reqVO.getVatNo() != null ? reqVO.getVatNo().trim() : "";
        ProgressFeedbackDO existing = progressFeedbackMapper.selectByColorIdAndVatNo(vat.getDyeingColorId(), newVatNo);
        if (existing != null && !Objects.equals(existing.getId(), vat.getId())) {
            throw exception(PROGRESS_FEEDBACK_VAT_NO_DUPLICATE, newVatNo);
        }

        // 4. 状态变更校验（仅允许保持当前状态，或回退到上一工序）
        if (StringUtils.hasText(reqVO.getStatus()) && !Objects.equals(reqVO.getStatus().trim(), vat.getStatus())) {
            String targetStatus = reqVO.getStatus().trim();
            String prevStatus = getPreviousStatus(vat.getStatus());
            if (prevStatus == null || !prevStatus.equals(targetStatus)) {
                throw exception(PROGRESS_FEEDBACK_STATUS_ROLLBACK_NOT_ALLOWED);
            }
            // 回退放行（回退不强制清日期，日期以人工修正为准）
            vat.setStatus(prevStatus);
        }

        // 5. 更新基本信息与五个阶段日期（COMPLETED 缸仍可修正日期与备注）
        vat.setVatNo(newVatNo);
        vat.setQuantity(reqVO.getQuantity());
        vat.setPieces(reqVO.getPieces());
        vat.setScheduledDate(reqVO.getScheduledDate());
        vat.setDyeingStartedDate(reqVO.getDyeingStartedDate());
        vat.setFixingStartedDate(reqVO.getFixingStartedDate());
        vat.setQualityCheckDate(reqVO.getQualityCheckDate());
        vat.setCompletedDate(reqVO.getCompletedDate());
        vat.setDelayReason(reqVO.getDelayReason());
        vat.setRemark(reqVO.getRemark());

        progressFeedbackMapper.updateById(vat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceProgressFeedback(ProgressFeedbackAdvanceReqVO reqVO) {
        // 1. 查缸存在性
        ProgressFeedbackDO vat = progressFeedbackMapper.selectById(reqVO.getId());
        if (vat == null || Boolean.TRUE.equals(vat.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_VAT_NOT_EXISTS);
        }

        // 2. 状态机推进：严格顺序逐格，不允许跳格；COMPLETED 无后继
        String currentStatus = vat.getStatus();
        String nextStatus = getNextStatus(currentStatus);
        if (nextStatus == null) {
            throw exception(PROGRESS_FEEDBACK_STATUS_ADVANCE_NOT_ALLOWED);
        }

        // 若传入了目标状态，必须与下一格严格一致，不允许跳格
        if (StringUtils.hasText(reqVO.getTargetStatus())) {
            if (!nextStatus.equals(reqVO.getTargetStatus().trim())) {
                throw exception(PROGRESS_FEEDBACK_STATUS_ADVANCE_NOT_ALLOWED);
            }
        }

        // 3. 写入目标状态与对应阶段当天日期戳
        LocalDate today = todayProvider.get();
        vat.setStatus(nextStatus);
        if (ProgressFeedbackStatusEnum.DYEING.getValue().equals(nextStatus)) {
            vat.setDyeingStartedDate(today);
        } else if (ProgressFeedbackStatusEnum.FIXING.getValue().equals(nextStatus)) {
            vat.setFixingStartedDate(today);
        } else if (ProgressFeedbackStatusEnum.QUALITY_CHECK.getValue().equals(nextStatus)) {
            vat.setQualityCheckDate(today);
        } else if (ProgressFeedbackStatusEnum.COMPLETED.getValue().equals(nextStatus)) {
            vat.setCompletedDate(today);
        }

        progressFeedbackMapper.updateById(vat);
    }

    /**
     * 严格顺序工序链：SCHEDULED -> DYEING -> FIXING -> QUALITY_CHECK -> COMPLETED
     */
    private static final List<String> STATUS_SEQUENCE = List.of(
            ProgressFeedbackStatusEnum.SCHEDULED.getValue(),
            ProgressFeedbackStatusEnum.DYEING.getValue(),
            ProgressFeedbackStatusEnum.FIXING.getValue(),
            ProgressFeedbackStatusEnum.QUALITY_CHECK.getValue(),
            ProgressFeedbackStatusEnum.COMPLETED.getValue()
    );

    private String getNextStatus(String currentStatus) {
        if (currentStatus == null) {
            return null;
        }
        int index = STATUS_SEQUENCE.indexOf(currentStatus);
        if (index >= 0 && index < STATUS_SEQUENCE.size() - 1) {
            return STATUS_SEQUENCE.get(index + 1);
        }
        return null;
    }

    private String getPreviousStatus(String currentStatus) {
        if (currentStatus == null) {
            return null;
        }
        int index = STATUS_SEQUENCE.indexOf(currentStatus);
        if (index > 0) {
            return STATUS_SEQUENCE.get(index - 1);
        }
        return null;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查询符合骨架要求的染色单（仅 ACTIVE/COMPLETED，软删默认排除；含已失效开关时绕过逻辑删除查全量）
     */
    private List<DyeingOrderDO> getEligibleOrders(ProgressFeedbackPageReqVO reqVO) {
        List<DyeingOrderDO> orders;
        if (Boolean.TRUE.equals(reqVO.getIncludeDeleted())) {
            orders = dyeingOrderMapper.selectListIncludingDeleted(
                    reqVO.getDyeingOrderId(),
                    reqVO.getContractNo(),
                    reqVO.getDyeingFactoryId()
            );
        } else {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DyeingOrderDO> orderWrapper = new LambdaQueryWrapperX<DyeingOrderDO>()
                    .eqIfPresent(DyeingOrderDO::getId, reqVO.getDyeingOrderId())
                    .likeIfPresent(DyeingOrderDO::getContractNo, reqVO.getContractNo())
                    .eqIfPresent(DyeingOrderDO::getDyeingFactoryId, reqVO.getDyeingFactoryId())
                    .in(DyeingOrderDO::getStatus, "ACTIVE", "COMPLETED")
                    .orderByDesc(DyeingOrderDO::getOrderDate).orderByDesc(DyeingOrderDO::getId);
            orders = dyeingOrderMapper.selectList(orderWrapper);
        }
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyList();
        }

        // 内存双重防御：确保状态与软删除过滤在 Mock 单测中依然生效
        return orders.stream()
                .filter(o -> "ACTIVE".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus()))
                .filter(o -> Boolean.TRUE.equals(reqVO.getIncludeDeleted()) || (o.getDeleted() == null || !o.getDeleted()))
                .collect(Collectors.toList());
    }

    /**
     * 按染色单列表顺序与颜色行号 lineNo 排序获取明细行
     */
    private List<DyeingColorDO> getOrderedColors(List<Long> orderIds, List<DyeingOrderDO> orders, boolean includeDeleted) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        List<DyeingColorDO> colors;
        if (includeDeleted) {
            colors = dyeingColorMapper.selectListByOrderIdsIncludingDeleted(orderIds);
        } else {
            colors = dyeingColorMapper.selectList(new LambdaQueryWrapperX<DyeingColorDO>()
                    .in(DyeingColorDO::getDyeingOrderId, orderIds)
                    .orderByAsc(DyeingColorDO::getLineNo));
        }
        if (CollUtil.isEmpty(colors)) {
            return Collections.emptyList();
        }

        // 内存双重防御
        colors = colors.stream()
                .filter(c -> includeDeleted || (c.getDeleted() == null || !c.getDeleted()))
                .collect(Collectors.toList());

        // 建立染色单顺序索引
        Map<Long, Integer> orderIndexMap = new HashMap<>(orders.size());
        for (int i = 0; i < orders.size(); i++) {
            orderIndexMap.put(orders.get(i).getId(), i);
        }

        List<DyeingColorDO> sortedColors = new ArrayList<>(colors);
        sortedColors.sort((c1, c2) -> {
            int cmp = Integer.compare(
                    orderIndexMap.getOrDefault(c1.getDyeingOrderId(), 0),
                    orderIndexMap.getOrDefault(c2.getDyeingOrderId(), 0)
            );
            if (cmp != 0) {
                return cmp;
            }
            int line1 = c1.getLineNo() != null ? c1.getLineNo() : 0;
            int line2 = c2.getLineNo() != null ? c2.getLineNo() : 0;
            return Integer.compare(line1, line2);
        });

        return sortedColors;
    }

    /**
     * 批量查询未决问题数
     */
    private Map<Long, Integer> getUnresolvedCountMap(List<ProgressFeedbackDO> vats) {
        if (CollUtil.isEmpty(vats)) {
            return Collections.emptyMap();
        }
        Set<Long> vatIds = vats.stream().map(ProgressFeedbackDO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (vatIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ProgressFeedbackNoteDO> notes = progressFeedbackNoteMapper.selectUnresolvedListByFeedbackIds(vatIds);
        if (CollUtil.isEmpty(notes)) {
            return Collections.emptyMap();
        }

        return notes.stream()
                .filter(n -> n.getDeleted() == null || !n.getDeleted())
                .collect(Collectors.groupingBy(ProgressFeedbackNoteDO::getFeedbackId, Collectors.summingInt(n -> 1)));
    }

    /**
     * 骨架颜色行筛选下推
     */
     private List<DyeingColorDO> filterColors(List<DyeingColorDO> colors,
                                              Map<Long, DyeingOrderDO> orderMap,
                                              Map<Long, List<ProgressFeedbackDO>> vatsByColor,
                                              Map<Long, Integer> unresolvedCountMap,
                                              ProgressFeedbackPageReqVO reqVO,
                                              LocalDate today) {
        return colors.stream().filter(color -> {
            DyeingOrderDO order = orderMap.get(color.getDyeingOrderId());
            if (order == null) {
                return false;
            }
            List<ProgressFeedbackDO> vats = vatsByColor.getOrDefault(color.getId(), Collections.emptyList());

            // 1. 状态筛选（支持虚拟态 NOT_SCHEDULED、进行中 IN_PROGRESS 与实体工序状态）
            if (StringUtils.hasText(reqVO.getStatus())) {
                String filterStatus = reqVO.getStatus().trim();
                if (ProgressFeedbackStatusEnum.NOT_SCHEDULED.getValue().equals(filterStatus)) {
                    if (!vats.isEmpty()) {
                        return false;
                    }
                } else if ("IN_PROGRESS".equals(filterStatus)) {
                    boolean hasInProgress = vats.stream().anyMatch(v -> ProgressFeedbackStatusEnum.IN_PROGRESS_STATUSES.contains(v.getStatus()));
                    if (!hasInProgress) {
                        return false;
                    }
                } else {
                    boolean hasStatus = vats.stream().anyMatch(v -> filterStatus.equals(v.getStatus()));
                    if (!hasStatus) {
                        return false;
                    }
                }
            }

            // 2. 仅看延期筛选（染色单交货日期 < 今天 且 缸非 COMPLETED）
            if (Boolean.TRUE.equals(reqVO.getDelayedOnly())) {
                boolean hasDelayed = vats.stream().anyMatch(v -> isDelayed(order.getDeliveryDate(), v.getStatus(), today));
                if (!hasDelayed) {
                    return false;
                }
            }

            // 3. 仅看未决问题筛选（存在未闭环反馈记录的缸）
            if (Boolean.TRUE.equals(reqVO.getUnresolvedOnly())) {
                boolean hasUnresolved = vats.stream().anyMatch(v -> unresolvedCountMap.getOrDefault(v.getId(), 0) > 0);
                if (!hasUnresolved) {
                    return false;
                }
            }

            // 4. 含已完成开关：默认进行中视图（全缸 COMPLETED 的节隐藏），开启后显示；若筛选 status=COMPLETED 时放行
            if (!Boolean.TRUE.equals(reqVO.getIncludeCompleted())
                    && !ProgressFeedbackStatusEnum.COMPLETED.getValue().equals(reqVO.getStatus())) {
                boolean isAllCompleted = !vats.isEmpty() && vats.stream()
                        .allMatch(v -> ProgressFeedbackStatusEnum.COMPLETED.getValue().equals(v.getStatus()));
                if (isAllCompleted) {
                    return false;
                }
            }

            // 5. 关键词筛选（匹配颜色名称、客户色号、缸号，忽略大小写）
            if (StringUtils.hasText(reqVO.getKeyword())) {
                if (!matchesKeyword(color, vats, reqVO.getKeyword())) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }

    /**
     * 关键词匹配检查（匹配颜色名称、客户色号、缸号，忽略大小写）
     */
    private boolean matchesKeyword(DyeingColorDO color, List<ProgressFeedbackDO> vats, String kw) {
        if (!StringUtils.hasText(kw)) {
            return true;
        }
        String lowerKw = kw.trim().toLowerCase();
        boolean matchColor = (color.getCustomerColorNo() != null && color.getCustomerColorNo().toLowerCase().contains(lowerKw))
                || (color.getColorName() != null && color.getColorName().toLowerCase().contains(lowerKw));
        boolean matchVat = CollUtil.isNotEmpty(vats) && vats.stream()
                .anyMatch(v -> v.getVatNo() != null && v.getVatNo().toLowerCase().contains(lowerKw));
        return matchColor || matchVat;
    }

    /**
     * 组装单个颜色行节 Response VO（零快照实时取出 + 缸卡列表 + 派生字段）
     */
    private ProgressFeedbackColorSectionRespVO buildColorSection(DyeingOrderDO order,
                                                                 DyeingColorDO color,
                                                                 List<ProgressFeedbackDO> vats,
                                                                 Map<Long, Integer> unresolvedCountMap,
                                                                 ProgressFeedbackPageReqVO reqVO,
                                                                 LocalDate today) {
        ProgressFeedbackColorSectionRespVO section = new ProgressFeedbackColorSectionRespVO();
        // 染色单实时字段（零快照）
        section.setDyeingOrderId(order.getId());
        section.setSalesContractId(order.getSalesContractId());
        section.setContractNo(order.getContractNo());
        section.setCustomerId(order.getCustomerId());
        section.setClientName(order.getClientName());
        section.setDyeingFactoryId(order.getDyeingFactoryId());
        section.setDyeingFactoryName(order.getDyeingFactoryName());
        section.setDeliveryDate(order.getDeliveryDate());
        section.setProductCode(order.getProductCode());
        section.setProductName(order.getProductName());
        section.setDyeingOrderStatus(order.getStatus());

        // 颜色行实时字段
        section.setDyeingColorId(color.getId());
        section.setLineNo(color.getLineNo());
        section.setCustomerColorNo(color.getCustomerColorNo());
        section.setColorName(color.getColorName());
        BigDecimal colorQty = color.getQuantity() != null ? color.getQuantity() : BigDecimal.ZERO;
        section.setColorQuantity(colorQty);
        section.setColorPieces(color.getPieces());
        section.setUnit(color.getDyeingInputUnit());

        // 挂载缸卡与计算合计
        BigDecimal totalVatQty = BigDecimal.ZERO;
        int totalVatPieces = 0;
        List<ProgressFeedbackVatRespVO> vatVOList = new ArrayList<>(vats.size());

        for (ProgressFeedbackDO vat : vats) {
            boolean delayed = isDelayed(order.getDeliveryDate(), vat.getStatus(), today);
            int unresolvedCount = unresolvedCountMap.getOrDefault(vat.getId(), 0);

            // 缸级筛选过滤（仅看延期、仅看未决问题、状态筛选与统计条完全对齐）
            if (reqVO != null) {
                if (Boolean.TRUE.equals(reqVO.getDelayedOnly()) && !delayed) {
                    continue;
                }
                if (Boolean.TRUE.equals(reqVO.getUnresolvedOnly()) && unresolvedCount <= 0) {
                    continue;
                }
                if (StringUtils.hasText(reqVO.getStatus()) && !"NOT_SCHEDULED".equals(reqVO.getStatus())) {
                    String filterStatus = reqVO.getStatus();
                    if ("IN_PROGRESS".equals(filterStatus)) {
                        if (ProgressFeedbackStatusEnum.COMPLETED.getValue().equals(vat.getStatus())) {
                            continue;
                        }
                    } else if (!filterStatus.equals(vat.getStatus())) {
                        continue;
                    }
                }
            }

            ProgressFeedbackVatRespVO vatVO = BeanUtils.toBean(vat, ProgressFeedbackVatRespVO.class);
            vatVO.setDelayed(delayed);
            vatVO.setUnresolvedIssueCount(unresolvedCount);
            vatVOList.add(vatVO);

            if (vat.getQuantity() != null) {
                totalVatQty = totalVatQty.add(vat.getQuantity());
            }
            if (vat.getPieces() != null) {
                totalVatPieces += vat.getPieces();
            }
        }

        section.setVats(vatVOList);
        section.setTotalVatQuantity(totalVatQty.setScale(2, RoundingMode.HALF_UP));
        section.setTotalVatPieces(totalVatPieces);

        // 状态分组计数（基于匹配展示的缸）
        Map<String, Integer> statusCountMap = vatVOList.stream()
                .filter(v -> v.getStatus() != null)
                .collect(Collectors.groupingBy(ProgressFeedbackVatRespVO::getStatus, Collectors.summingInt(v -> 1)));
        section.setStatusCountMap(statusCountMap);

        // 派生计算：覆盖率与超排标记（按全量缸累计）
        BigDecimal allVatQty = vats.stream()
                .map(v -> v.getQuantity() != null ? v.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (color.getQuantity() == null || color.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            // 零/空分母按 0% 处理
            section.setCoveragePercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            section.setOverScheduled(allVatQty.compareTo(BigDecimal.ZERO) > 0);
        } else {
            BigDecimal coverage = allVatQty.divide(color.getQuantity(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            section.setCoveragePercent(coverage);
            section.setOverScheduled(allVatQty.compareTo(color.getQuantity()) > 0);
        }

        return section;
    }

    /**
     * 延期判定：染色单交货日期 < 今天 且 缸非 COMPLETED
     */
    private boolean isDelayed(LocalDate deliveryDate, String status, LocalDate today) {
        if (deliveryDate == null) {
            return false;
        }
        if (ProgressFeedbackStatusEnum.COMPLETED.getValue().equals(status)) {
            return false;
        }
        return deliveryDate.isBefore(today);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProgressFeedback(Long id) {
        // 1. 校验反馈缸是否存在且未软删
        ProgressFeedbackDO vat = progressFeedbackMapper.selectById(id);
        if (vat == null || Boolean.TRUE.equals(vat.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_VAT_NOT_EXISTS);
        }

        // 2. 级联逻辑删除其下所有反馈记录
        progressFeedbackNoteMapper.delete(new LambdaQueryWrapperX<ProgressFeedbackNoteDO>()
                .eq(ProgressFeedbackNoteDO::getFeedbackId, id));

        // 3. 逻辑删除反馈缸自身
        progressFeedbackMapper.deleteById(id);
    }

}
