package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;

import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRollbackReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionVoidReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionConfirmTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionLineStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionStatusEnum;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.framework.common.util.collection.CollectionUtils.convertList;
import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.*;

/**
 * 生产管理 - 打色指示单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ColorInstructionServiceImpl implements ColorInstructionService {

    /**
     * 打色指示单号前缀
     */
    private static final String INSTRUCTION_NO_PREFIX = "DS";
    /**
     * 编号数字部分最小补零位数（超 999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 3;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 编号前缀（DS+年月日）后的数字部分
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    /**
     * 年月日格式化器（8位年月日，无连字符，仿 ADR-0006）
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 下游单据（胚布单/染色单）已取消状态（排除 CANCELLED）
     */
    public static final String DOWNSTREAM_STATUS_CANCELLED = "CANCELLED";

    /**
     * 合法灯源字典集合（lunaerp_color_light_source: D65/TL84/CWF/A/UV/TL83）
     */
    private static final Set<String> VALID_LIGHT_SOURCES = Set.of(
            "D65", "TL84", "CWF", "A", "UV", "TL83"
    );

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Resource
    private ColorInstructionColorMapper colorInstructionColorMapper;

    @Resource
    private com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper customerMapper;

    @Resource
    private com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper productMapper;

    @Resource
    private com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper supplierMapper;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Override
    public PageResult<ColorInstructionRespVO> getColorInstructionPage(ColorInstructionPageReqVO pageReqVO) {
        PageResult<ColorInstructionDO> pageResult = colorInstructionMapper.selectPage(pageReqVO);
        if (pageResult.getList() == null || pageResult.getList().isEmpty()) {
            return PageResult.empty(pageResult.getTotal());
        }

        // 防 N+1：收集当前页主单 ID，一次性批量查询颜色行摘要
        List<Long> instructionIds = convertList(pageResult.getList(), ColorInstructionDO::getId);
        List<ColorInstructionColorDO> colors = colorInstructionColorMapper.selectListByInstructionIds(instructionIds);
        Map<Long, List<ColorInstructionColorDO>> colorMap = colors.stream()
                .collect(Collectors.groupingBy(ColorInstructionColorDO::getInstructionId));

        PageResult<ColorInstructionRespVO> respResult = BeanUtils.toBean(pageResult, ColorInstructionRespVO.class);
        for (ColorInstructionRespVO resp : respResult.getList()) {
            List<ColorInstructionColorDO> instructionColors = colorMap.getOrDefault(resp.getId(), Collections.emptyList());
            resp.setColorSummaries(instructionColors.stream().map(c -> {
                ColorInstructionRespVO.ColorSummaryVO summary = new ColorInstructionRespVO.ColorSummaryVO();
                summary.setCustomerColorNo(c.getCustomerColorNo());
                summary.setColorName(c.getColorName());
                summary.setFactoryColorNo(c.getFactoryColorNo());
                summary.setColorHex(c.getColorHex());
                summary.setRound(c.getRound());
                summary.setStatus(c.getStatus());
                return summary;
            }).collect(Collectors.toList()));
        }

        return respResult;
    }

    @Override
    public ColorInstructionRespVO getColorInstruction(Long id) {
        ColorInstructionDO instruction = colorInstructionMapper.selectById(id);
        if (instruction == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        ColorInstructionRespVO resp = BeanUtils.toBean(instruction, ColorInstructionRespVO.class);
        List<ColorInstructionColorDO> colors = colorInstructionColorMapper.selectListByInstructionId(id);
        resp.setColors(BeanUtils.toBean(colors, com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorRespVO.class));
        resp.setColorSummaries(colors.stream().map(c -> {
            ColorInstructionRespVO.ColorSummaryVO summary = new ColorInstructionRespVO.ColorSummaryVO();
            summary.setCustomerColorNo(c.getCustomerColorNo());
            summary.setColorName(c.getColorName());
            summary.setFactoryColorNo(c.getFactoryColorNo());
            summary.setColorHex(c.getColorHex());
            summary.setRound(c.getRound());
            summary.setStatus(c.getStatus());
            return summary;
        }).collect(Collectors.toList()));
        return resp;
    }

    @Override
    public String getNextInstructionNo() {
        return generateInstructionNo(LocalDate.now());
    }

    private String generateInstructionNo(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = INSTRUCTION_NO_PREFIX + DATE_FORMATTER.format(targetDate);
        List<ColorInstructionDO> candidates = colorInstructionMapper.selectListByNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (ColorInstructionDO candidate : candidates) {
            String no = candidate.getInstructionNo();
            if (no == null || no.length() <= prefix.length()) {
                continue;
            }
            String suffix = no.substring(prefix.length());
            Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
            if (!matcher.find()) {
                continue;
            }
            String digits = matcher.group();
            long num;
            try {
                num = Long.parseLong(digits);
            } catch (NumberFormatException ignore) {
                continue;
            }
            if (num > maxNum) {
                maxNum = num;
                maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
            }
        }
        if (maxNum == 0L) {
            return prefix + String.format("%0" + NO_MIN_DIGITS + "d", 1);
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    private String generateInstructionNoWithRetry(LocalDate date) {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateInstructionNo(date);
            if (colorInstructionMapper.selectByInstructionNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, generateInstructionNo(date));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "CREATE", bizNo = "{{#result}}",
            success = "创建打色单【{{#instructionNo}}】")
    public Long createColorInstruction(ColorInstructionSaveReqVO createReqVO) {
        // 1. 颜色行 1~N 领域守卫（打色单至少需要 1 行颜色明细）
        if (createReqVO.getColors() == null || createReqVO.getColors().isEmpty()) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }

        // 2. 手填编号查重（含已删除行，撞号直接报错不顺延）
        if (StringUtils.hasText(createReqVO.getInstructionNo())) {
            validateInstructionNoUnique(createReqVO.getInstructionNo().trim());
        }

        // 3. 组装主表 DO 与快照回填、缺省值
        ColorInstructionDO order = BeanUtils.toBean(createReqVO, ColorInstructionDO.class);
        if (StringUtils.hasText(order.getInstructionNo())) {
            order.setInstructionNo(order.getInstructionNo().trim());
        } else {
            // 自动取号统一以服务器当天日期为基准（ADR-0006）
            order.setInstructionNo(generateInstructionNoWithRetry(LocalDate.now()));
        }
        if (order.getInstructionDate() == null) {
            order.setInstructionDate(LocalDate.now());
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("PENDING");
        }

        // 引用校验与快照填充
        validateHeaderReferences(createReqVO, order);

        // 4. 颜色行灯源合法性校验（主灯源必填且合法，辅灯源可空、若有必合法）
        validateColorLightSources(createReqVO.getColors());

        // 6. 插入主表（捕获数据库并发唯一冲突，映射为业务错误码）
        try {
            colorInstructionMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, order.getInstructionNo());
        }

        // 7. 简单保存颜色行（按 instruction_id 重排 lineNo 插入）
        int lineNo = 1;
        for (ColorInstructionColorSaveVO item : createReqVO.getColors()) {
            ColorInstructionColorDO colorDO = BeanUtils.toBean(item, ColorInstructionColorDO.class);
            colorDO.setId(null);
            colorDO.setInstructionId(order.getId());
            colorDO.setLineNo(lineNo++);
            if (colorDO.getRound() == null) {
                colorDO.setRound(1);
            }
            if (!StringUtils.hasText(colorDO.getStatus())) {
                colorDO.setStatus("PENDING");
            }
            colorInstructionColorMapper.insert(colorDO);
        }

        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
        return order.getId();
    }

    private void validateInstructionNoUnique(String instructionNo) {
        if (colorInstructionMapper.selectByInstructionNoIncludingDeleted(instructionNo) != null) {
            throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, instructionNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "DELETE", bizNo = "{{#id}}",
            success = "删除打色单【{{#instructionNo}}】")
    public void deleteColorInstruction(Long id) {
        // 1. 存在性校验：不存在抛 COLOR_INSTRUCTION_NOT_EXISTS (1_030_032_000)
        ColorInstructionDO order = colorInstructionMapper.selectById(id);
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 2. 状态守卫：仅 PENDING 状态可软删进回收站，其他状态报 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)
        if (!"PENDING".equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 下游守卫：若存在下游有效引用，报 COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE (1_030_032_005)
        if (hasActiveDownstreamRef(id)) {
            throw exception(COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE);
        }

        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());

        // 4. 级联逻辑删除：主表 + 颜色行（单号不释放）
        colorInstructionMapper.deleteById(id);
        colorInstructionColorMapper.deleteByInstructionId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}",
            success = "更新打色单【{{#instructionNo}}】")
    public void updateColorInstruction(ColorInstructionSaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 1. 存在性校验：不存在报 COLOR_INSTRUCTION_NOT_EXISTS (1_030_032_000)
        ColorInstructionDO order = colorInstructionMapper.selectById(updateReqVO.getId());
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 2. 状态守卫：仅 PENDING / IN_PROGRESS 可整体编辑；CONFIRMED / IN_PRODUCTION / VOID 报 1_030_032_002
        if (!"PENDING".equals(order.getStatus()) && !"IN_PROGRESS".equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 颜色行非空守卫（打色单至少需要 1 行颜色明细）
        if (updateReqVO.getColors() == null || updateReqVO.getColors().isEmpty()) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }

        // 4. 手填编号查重（排除自身，包含已删除单据）
        if (StringUtils.hasText(updateReqVO.getInstructionNo())) {
            String trimmedNo = updateReqVO.getInstructionNo().trim();
            ColorInstructionDO duplicate = colorInstructionMapper.selectByInstructionNoIncludingDeleted(trimmedNo);
            if (duplicate != null && !duplicate.getId().equals(order.getId())) {
                throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, trimmedNo);
            }
            order.setInstructionNo(trimmedNo);
        }

        // 5. 引用校验与更新主表快照与字段
        validateHeaderReferences(updateReqVO, order);

        // 6. 颜色行灯源合法性校验（主灯源必填且合法，辅灯源可空、若有必合法）
        validateColorLightSources(updateReqVO.getColors());
        if (updateReqVO.getInstructionDate() != null) {
            order.setInstructionDate(updateReqVO.getInstructionDate());
        }
        order.setDeliveryDate(updateReqVO.getDeliveryDate());
        order.setRemark(updateReqVO.getRemark());

        colorInstructionMapper.updateById(order);
        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());

        // 7. 颜色行差异保存（含行状态守卫与 lineNo 重排）
        diffSaveColors(order, updateReqVO.getColors(), updateReqVO.getDeletedColorIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "START", bizNo = "{{#id}}",
            success = "开始打色【{{#instructionNo}}】")
    public void startColorInstruction(Long id) {
        if (id == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        // 1. 存在性校验
        ColorInstructionDO order = colorInstructionMapper.selectById(id);
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        // 2. 状态守卫：仅 PENDING 状态允许开始打色
        if (!ColorInstructionStatusEnum.PENDING.getValue().equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 条件更新防并发（带 status='PENDING' AND tenant_id=? AND deleted=0）
        ColorInstructionDO updateDO = new ColorInstructionDO();
        updateDO.setStatus(ColorInstructionStatusEnum.IN_PROGRESS.getValue());
        int updated = colorInstructionMapper.update(updateDO,
                new LambdaUpdateWrapper<ColorInstructionDO>()
                        .eq(ColorInstructionDO::getId, id)
                        .eq(ColorInstructionDO::getStatus, ColorInstructionStatusEnum.PENDING.getValue()));
        if (updated == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
    }

    private record ColorLineAndOrder(ColorInstructionColorDO line, ColorInstructionDO order) {}

    private ColorLineAndOrder validateAndGetLineAndOrder(Long lineId) {
        if (lineId == null) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }
        // 1. 颜色行存在性校验
        ColorInstructionColorDO color = colorInstructionColorMapper.selectById(lineId);
        if (color == null) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }
        // 2. 主单校验：存在性与状态守卫（须为 PENDING 或 IN_PROGRESS）
        ColorInstructionDO order = colorInstructionMapper.selectById(color.getInstructionId());
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        if (!ColorInstructionStatusEnum.PENDING.getValue().equals(order.getStatus())
                && !ColorInstructionStatusEnum.IN_PROGRESS.getValue().equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }
        return new ColorLineAndOrder(color, order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "APPROVE_LINE", bizNo = "{{#orderId}}",
            success = "通过打色行【{{#customerColorNo}}】")
    public void approveColorLine(Long lineId) {
        ColorLineAndOrder pair = validateAndGetLineAndOrder(lineId);
        ColorInstructionColorDO color = pair.line();
        ColorInstructionDO order = pair.order();

        // 3. 行状态守卫：仅 PENDING 行允许通过
        if (!ColorInstructionLineStatusEnum.PENDING.getValue().equals(color.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 4. 条件更新行状态 PENDING -> APPROVED
        ColorInstructionColorDO updateColor = new ColorInstructionColorDO();
        updateColor.setStatus(ColorInstructionLineStatusEnum.APPROVED.getValue());
        int updated = colorInstructionColorMapper.update(updateColor,
                new LambdaUpdateWrapper<ColorInstructionColorDO>()
                        .eq(ColorInstructionColorDO::getId, lineId)
                        .eq(ColorInstructionColorDO::getStatus, ColorInstructionLineStatusEnum.PENDING.getValue()));
        if (updated == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 5. 便利规则：单据仍 PENDING 时自动转 IN_PROGRESS（条件更新）
        cascadeOrderPendingToInProgress(order);

        LogRecordContext.putVariable("orderId", order.getId());
        LogRecordContext.putVariable("customerColorNo", color.getCustomerColorNo());
        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
        LogRecordContext.putVariable("colorName", color.getColorName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "REJECT_RESTRIKE_LINE", bizNo = "{{#orderId}}",
            success = "驳回重打打色行【{{#customerColorNo}}】，生成第{{#newRound}}轮")
    public void rejectRestrikeColorLine(Long lineId, String rejectReason) {
        ColorLineAndOrder pair = validateAndGetLineAndOrder(lineId);
        ColorInstructionColorDO oldColor = pair.line();
        ColorInstructionDO order = pair.order();

        // 3. 行状态守卫：
        // REJECTED 行不可再"驳回重打"（报 1_030_032_006 行状态不允许 / 行已冻结）
        if (ColorInstructionLineStatusEnum.REJECTED.getValue().equals(oldColor.getStatus())) {
            throw exception(COLOR_INSTRUCTION_LINE_FROZEN);
        }
        // 仅 PENDING 或 APPROVED 状态允许驳回重打
        if (!ColorInstructionLineStatusEnum.PENDING.getValue().equals(oldColor.getStatus())
                && !ColorInstructionLineStatusEnum.APPROVED.getValue().equals(oldColor.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 4. 条件更新旧行置 REJECTED 冻结 + 记录 rejectReason
        ColorInstructionColorDO updateOld = new ColorInstructionColorDO();
        updateOld.setStatus(ColorInstructionLineStatusEnum.REJECTED.getValue());
        updateOld.setRejectReason(rejectReason != null ? rejectReason.trim() : null);
        int updated = colorInstructionColorMapper.update(updateOld,
                new LambdaUpdateWrapper<ColorInstructionColorDO>()
                        .eq(ColorInstructionColorDO::getId, lineId)
                        .in(ColorInstructionColorDO::getStatus, List.of(
                                ColorInstructionLineStatusEnum.PENDING.getValue(),
                                ColorInstructionLineStatusEnum.APPROVED.getValue()
                        )));
        if (updated == 0) {
            ColorInstructionColorDO current = colorInstructionColorMapper.selectById(lineId);
            if (current != null && ColorInstructionLineStatusEnum.REJECTED.getValue().equals(current.getStatus())) {
                throw exception(COLOR_INSTRUCTION_LINE_FROZEN);
            }
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 5. 同事务克隆新行：
        // 继承 customerColorNo/colorName/factoryColorNo/colorHex/lightPrimary/lightSecondary/colorCardRef/copies
        // round = 旧行 round + 1, status = PENDING, samplePhotos 不继承
        int oldRound = oldColor.getRound() != null ? oldColor.getRound() : 1;
        int newRound = oldRound + 1;

        ColorInstructionColorDO cloned = new ColorInstructionColorDO();
        cloned.setInstructionId(order.getId());
        cloned.setCustomerColorNo(oldColor.getCustomerColorNo());
        cloned.setColorName(oldColor.getColorName());
        cloned.setFactoryColorNo(oldColor.getFactoryColorNo());
        cloned.setColorHex(oldColor.getColorHex());
        cloned.setLightPrimary(oldColor.getLightPrimary());
        cloned.setLightSecondary(oldColor.getLightSecondary());
        cloned.setColorCardRef(oldColor.getColorCardRef());
        cloned.setCopies(oldColor.getCopies());
        cloned.setRound(newRound);
        cloned.setStatus(ColorInstructionLineStatusEnum.PENDING.getValue());
        // samplePhotos 不继承，rejectReason、remark 不继承
        cloned.setSamplePhotos(null);
        cloned.setRejectReason(null);
        cloned.setRemark(null);

        // 6. 服务端重排 lineNo 并插入克隆新行（确保 lineNo 绝不为 null，满足 NOT NULL 约束，同色号多轮相邻、最新轮在前）
        insertAndReorderColorLines(order.getId(), cloned);

        // 7. 便利规则：单据仍 PENDING 时自动转 IN_PROGRESS（条件更新）
        cascadeOrderPendingToInProgress(order);

        LogRecordContext.putVariable("orderId", order.getId());
        LogRecordContext.putVariable("customerColorNo", oldColor.getCustomerColorNo());
        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
        LogRecordContext.putVariable("newRound", newRound);
        LogRecordContext.putVariable("rejectReason", rejectReason);
    }

    private void cascadeOrderPendingToInProgress(ColorInstructionDO order) {
        if (ColorInstructionStatusEnum.PENDING.getValue().equals(order.getStatus())) {
            ColorInstructionDO updateOrder = new ColorInstructionDO();
            updateOrder.setStatus(ColorInstructionStatusEnum.IN_PROGRESS.getValue());
            colorInstructionMapper.update(updateOrder,
                    new LambdaUpdateWrapper<ColorInstructionDO>()
                            .eq(ColorInstructionDO::getId, order.getId())
                            .eq(ColorInstructionDO::getStatus, ColorInstructionStatusEnum.PENDING.getValue()));
        }
    }

    /**
     * 按客户色号分组（同色号多轮相邻）、最新轮在前（round 降序）、原行号在前排序
     *
     * @param lines 待排序颜色行集合
     * @param preferredColorNoOrder 优先的色号出现次序（可为空，为空则依 lines 自身出现顺序）
     */
    private static void sortColorLines(List<ColorInstructionColorDO> lines, Collection<String> preferredColorNoOrder) {
        if (lines == null || lines.size() <= 1) {
            return;
        }
        LinkedHashSet<String> colorNoGroupOrder = new LinkedHashSet<>();
        if (preferredColorNoOrder != null) {
            for (String colorNo : preferredColorNoOrder) {
                if (StringUtils.hasText(colorNo)) {
                    colorNoGroupOrder.add(colorNo.trim());
                }
            }
        }
        for (ColorInstructionColorDO row : lines) {
            if (StringUtils.hasText(row.getCustomerColorNo())) {
                colorNoGroupOrder.add(row.getCustomerColorNo().trim());
            }
        }

        Map<String, Integer> groupRankMap = new HashMap<>();
        int rank = 0;
        for (String colorNo : colorNoGroupOrder) {
            groupRankMap.put(colorNo, rank++);
        }

        lines.sort((a, b) -> {
            String noA = a.getCustomerColorNo() != null ? a.getCustomerColorNo().trim() : "";
            String noB = b.getCustomerColorNo() != null ? b.getCustomerColorNo().trim() : "";
            int rankA = groupRankMap.getOrDefault(noA, Integer.MAX_VALUE);
            int rankB = groupRankMap.getOrDefault(noB, Integer.MAX_VALUE);
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }
            int roundA = a.getRound() != null ? a.getRound() : 1;
            int roundB = b.getRound() != null ? b.getRound() : 1;
            if (roundA != roundB) {
                return Integer.compare(roundB, roundA); // 最新轮在前
            }
            int lineNoA = a.getLineNo() != null ? a.getLineNo() : Integer.MAX_VALUE;
            int lineNoB = b.getLineNo() != null ? b.getLineNo() : Integer.MAX_VALUE;
            return Integer.compare(lineNoA, lineNoB);
        });
    }

    private void insertAndReorderColorLines(Long instructionId, ColorInstructionColorDO newLine) {
        List<ColorInstructionColorDO> existingRows = colorInstructionColorMapper.selectListByInstructionId(instructionId);
        List<ColorInstructionColorDO> allRows = new ArrayList<>(existingRows != null ? existingRows : Collections.emptyList());
        if (newLine != null) {
            allRows.add(newLine);
        }
        if (allRows.isEmpty()) {
            return;
        }

        sortColorLines(allRows, Collections.emptyList());

        int newLineNo = 1;
        for (ColorInstructionColorDO row : allRows) {
            if (row == newLine) {
                row.setLineNo(newLineNo);
                colorInstructionColorMapper.insert(row);
            } else if (!Objects.equals(row.getLineNo(), newLineNo)) {
                ColorInstructionColorDO lineNoUpdate = new ColorInstructionColorDO();
                lineNoUpdate.setId(row.getId());
                lineNoUpdate.setLineNo(newLineNo);
                colorInstructionColorMapper.updateById(lineNoUpdate);
                row.setLineNo(newLineNo);
            }
            newLineNo++;
        }
    }


    private void diffSaveColors(ColorInstructionDO order, List<ColorInstructionColorSaveVO> submittedColors, List<Long> explicitDeleteIds) {
        Long instructionId = order.getId();
        // 1. 查询库内当前打色单现存颜色行
        List<ColorInstructionColorDO> dbColors = colorInstructionColorMapper.selectListByInstructionId(instructionId);
        Map<Long, ColorInstructionColorDO> dbColorMap = dbColors.stream()
                .collect(Collectors.toMap(ColorInstructionColorDO::getId, Function.identity()));

        // 2. 收集显式删除的 ID（来自 explicitDeleteIds）
        Set<Long> explicitDeletes = new HashSet<>();
        if (explicitDeleteIds != null) {
            explicitDeletes.addAll(explicitDeleteIds);
        }

        // 校验显式删除行：跨父非法 ID 校验与冻结行守卫
        for (Long delId : explicitDeletes) {
            ColorInstructionColorDO dbColor = dbColorMap.get(delId);
            if (dbColor == null) {
                throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
            }
            if ("APPROVED".equals(dbColor.getStatus()) || "REJECTED".equals(dbColor.getStatus())) {
                throw exception(COLOR_INSTRUCTION_LINE_FROZEN);
            }
        }

        // 3. 本次提交的颜色行列表
        List<ColorInstructionColorSaveVO> activeSubmitted = submittedColors != null
                ? submittedColors
                : Collections.emptyList();

        // 校验提交行的 ID 合法性：不能与显式删除冲突、跨父非法 ID 与重复提交
        Set<Long> seenIds = new HashSet<>();
        for (ColorInstructionColorSaveVO c : activeSubmitted) {
            if (c.getId() != null) {
                if (explicitDeletes.contains(c.getId())) {
                    throw invalidParamException("打色单颜色明细行 ID【{}】不能同时在更新列表与删除列表中提交", c.getId());
                }
                if (!seenIds.add(c.getId())) {
                    throw invalidParamException("打色单颜色明细行 ID【{}】重复提交", c.getId());
                }
                if (!dbColorMap.containsKey(c.getId())) {
                    throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
                }
            }
        }

        // 4. 行状态守卫：带 id 的更新行中，APPROVED/REJECTED 冻结行提交任何字段变更报 1_030_032_006
        List<ColorInstructionColorDO> toInsert = new ArrayList<>();
        Map<Long, ColorInstructionColorSaveVO> updateVOMap = new HashMap<>();

        for (ColorInstructionColorSaveVO vo : activeSubmitted) {
            if (vo.getId() != null) {
                ColorInstructionColorDO dbRow = dbColorMap.get(vo.getId());
                if ("APPROVED".equals(dbRow.getStatus()) || "REJECTED".equals(dbRow.getStatus())) {
                    if (isColorFieldChanged(vo, dbRow)) {
                        throw exception(COLOR_INSTRUCTION_LINE_FROZEN);
                    }
                }
                updateVOMap.put(vo.getId(), vo);
            } else {
                ColorInstructionColorDO newDO = BeanUtils.toBean(vo, ColorInstructionColorDO.class);
                newDO.setId(null);
                newDO.setInstructionId(instructionId);
                newDO.setRound(1);
                newDO.setStatus("PENDING");
                toInsert.add(newDO);
            }
        }

        // 5. 删除处理：
        // 请求缺失的持久化行——仅 PENDING 行可删；
        // 冻结行缺失不处理（不报错、保留在库中）。
        // 另外 explicitDeletes 里的 PENDING 行也一并删除。
        List<Long> toDeleteIds = new ArrayList<>();
        List<ColorInstructionColorDO> retainedFrozenMissingRows = new ArrayList<>();

        for (ColorInstructionColorDO dbRow : dbColors) {
            boolean isInSubmitted = seenIds.contains(dbRow.getId());
            boolean isExplicitDelete = explicitDeletes.contains(dbRow.getId());

            if (isExplicitDelete) {
                toDeleteIds.add(dbRow.getId());
            } else if (!isInSubmitted) {
                if ("PENDING".equals(dbRow.getStatus())) {
                    toDeleteIds.add(dbRow.getId());
                } else {
                    retainedFrozenMissingRows.add(dbRow);
                }
            }
        }

        if (!toDeleteIds.isEmpty()) {
            colorInstructionColorMapper.deleteBatchIds(toDeleteIds);
        }

        int totalSurvivingCount = toInsert.size() + updateVOMap.size() + retainedFrozenMissingRows.size();
        if (totalSurvivingCount == 0) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }

        // 6. 收集所有存活行，进行 lineNo 重排
        // 按 customerColorNo 分组、同色号多轮相邻、最新轮在前、从 1 连续编号
        List<String> preferredColorNoOrder = new ArrayList<>();
        for (ColorInstructionColorSaveVO vo : activeSubmitted) {
            if (StringUtils.hasText(vo.getCustomerColorNo())) {
                preferredColorNoOrder.add(vo.getCustomerColorNo().trim());
            }
        }
        for (ColorInstructionColorDO missingFrozen : retainedFrozenMissingRows) {
            if (StringUtils.hasText(missingFrozen.getCustomerColorNo())) {
                preferredColorNoOrder.add(missingFrozen.getCustomerColorNo().trim());
            }
        }

        List<ColorInstructionColorDO> survivingList = new ArrayList<>();
        survivingList.addAll(toInsert);
        for (Map.Entry<Long, ColorInstructionColorSaveVO> entry : updateVOMap.entrySet()) {
            Long id = entry.getKey();
            ColorInstructionColorSaveVO vo = entry.getValue();
            ColorInstructionColorDO dbRow = dbColorMap.get(id);
            if (ColorInstructionLineStatusEnum.PENDING.getValue().equals(dbRow.getStatus())) {
                ColorInstructionColorDO updatedDO = BeanUtils.toBean(vo, ColorInstructionColorDO.class);
                updatedDO.setId(id);
                updatedDO.setInstructionId(instructionId);
                updatedDO.setRound(dbRow.getRound() != null ? dbRow.getRound() : 1);
                updatedDO.setStatus(dbRow.getStatus());
                survivingList.add(updatedDO);
            } else {
                survivingList.add(dbRow);
            }
        }
        survivingList.addAll(retainedFrozenMissingRows);

        sortColorLines(survivingList, preferredColorNoOrder);

        int newLineNo = 1;
        for (ColorInstructionColorDO colorDO : survivingList) {
            if (colorDO.getId() == null) {
                colorDO.setLineNo(newLineNo);
                colorInstructionColorMapper.insert(colorDO);
            } else {
                ColorInstructionColorDO originalDB = dbColorMap.get(colorDO.getId());
                boolean lineNoChanged = !Objects.equals(originalDB.getLineNo(), newLineNo);
                if ("PENDING".equals(originalDB.getStatus())) {
                    boolean contentChanged = isColorFieldChanged(updateVOMap.get(colorDO.getId()), originalDB);
                    if (contentChanged || lineNoChanged) {
                        colorDO.setLineNo(newLineNo);
                        colorInstructionColorMapper.updateById(colorDO);
                    }
                } else {
                    if (lineNoChanged) {
                        ColorInstructionColorDO lineNoOnlyUpdate = new ColorInstructionColorDO();
                        lineNoOnlyUpdate.setId(colorDO.getId());
                        lineNoOnlyUpdate.setLineNo(newLineNo);
                        colorInstructionColorMapper.updateById(lineNoOnlyUpdate);
                    }
                }
            }
            newLineNo++;
        }
    }

    private boolean isColorFieldChanged(ColorInstructionColorSaveVO vo, ColorInstructionColorDO db) {
        if (vo == null || db == null) {
            return false;
        }
        if (!Objects.equals(trimToNull(vo.getCustomerColorNo()), trimToNull(db.getCustomerColorNo()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getColorName()), trimToNull(db.getColorName()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getFactoryColorNo()), trimToNull(db.getFactoryColorNo()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getColorHex()), trimToNull(db.getColorHex()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getLightPrimary()), trimToNull(db.getLightPrimary()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getLightSecondary()), trimToNull(db.getLightSecondary()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getColorCardRef()), trimToNull(db.getColorCardRef()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getRejectReason()), trimToNull(db.getRejectReason()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(vo.getRemark()), trimToNull(db.getRemark()))) {
            return true;
        }
        if (!Objects.equals(vo.getCopies(), db.getCopies())) {
            return true;
        }
        if (vo.getRound() != null && !Objects.equals(vo.getRound(), db.getRound())) {
            return true;
        }
        if (vo.getStatus() != null && !Objects.equals(vo.getStatus(), db.getStatus())) {
            return true;
        }
        List<String> voPhotos = vo.getSamplePhotos() != null ? vo.getSamplePhotos() : Collections.emptyList();
        List<String> dbPhotos = db.getSamplePhotos() != null ? db.getSamplePhotos() : Collections.emptyList();
        return !voPhotos.equals(dbPhotos);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateHeaderReferences(ColorInstructionSaveReqVO reqVO, ColorInstructionDO order) {
        CustomerDO customer = customerMapper.selectById(reqVO.getCustomerId());
        if (customer == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
        }

        ProductDO product = productMapper.selectById(reqVO.getProductId());
        if (product == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
        }

        SupplierDO labFactory = null;
        if (reqVO.getLabFactoryId() != null) {
            labFactory = supplierMapper.selectById(reqVO.getLabFactoryId());
            if (labFactory == null) {
                throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
            }
        }

        order.setCustomerId(reqVO.getCustomerId());
        order.setClientName(customer.getCompanyName());
        order.setContactPerson(reqVO.getContactPerson());
        order.setProductId(reqVO.getProductId());
        order.setProductCode(product.getCode());
        order.setProductName(product.getName());
        order.setLabFactoryId(reqVO.getLabFactoryId());
        order.setLabFactoryName(labFactory != null ? labFactory.getCompanyName() : null);

        if (StringUtils.hasText(reqVO.getComposition())) {
            order.setComposition(reqVO.getComposition());
        } else if (!StringUtils.hasText(order.getComposition())) {
            order.setComposition(product.getComposition());
        }
        if (StringUtils.hasText(reqVO.getWidth())) {
            order.setWidth(reqVO.getWidth());
        } else if (!StringUtils.hasText(order.getWidth()) && product.getWidthCm() != null) {
            order.setWidth(product.getWidthCm().stripTrailingZeros().toPlainString() + "cm");
        }
        if (StringUtils.hasText(reqVO.getGsm())) {
            order.setGsm(reqVO.getGsm());
        } else if (!StringUtils.hasText(order.getGsm()) && product.getWeightGsm() != null) {
            order.setGsm(product.getWeightGsm().stripTrailingZeros().toPlainString() + "g/m²");
        }
    }

    private void validateColorLightSources(List<ColorInstructionColorSaveVO> colors) {
        if (colors == null) {
            return;
        }
        for (ColorInstructionColorSaveVO color : colors) {
            if (!VALID_LIGHT_SOURCES.contains(color.getLightPrimary())) {
                throw exception(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT);
            }
            if (StringUtils.hasText(color.getLightSecondary()) && !VALID_LIGHT_SOURCES.contains(color.getLightSecondary().trim())) {
                throw exception(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "CONFIRM", bizNo = "{{#reqVO.id}}",
            success = "确认打色指示单 {{#instructionNo}}")
    public void confirmColorInstruction(ColorInstructionConfirmReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 1. 存在性校验
        ColorInstructionDO order = colorInstructionMapper.selectById(reqVO.getId());
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 2. 守卫一：单据状态必须为 IN_PROGRESS（CONFIRMED 重复确认报 1_030_032_002）
        if (!ColorInstructionStatusEnum.IN_PROGRESS.getValue().equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 守卫二与守卫三：查询打色颜色行，校验无 PENDING 行且至少 1 行 APPROVED
        List<ColorInstructionColorDO> colors = colorInstructionColorMapper.selectList(
                new LambdaQueryWrapperX<ColorInstructionColorDO>()
                        .eq(ColorInstructionColorDO::getInstructionId, reqVO.getId())
        );
        boolean hasPending = colors.stream()
                .anyMatch(c -> ColorInstructionLineStatusEnum.PENDING.getValue().equals(c.getStatus()));
        if (hasPending) {
            throw exception(COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET);
        }
        boolean hasApproved = colors.stream()
                .anyMatch(c -> ColorInstructionLineStatusEnum.APPROVED.getValue().equals(c.getStatus()));
        if (!hasApproved) {
            throw exception(COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET);
        }

        // 4. 枚举校验 confirmType（内部枚举：CUSTOMER 客户确认 / INTERNAL 内部确认）
        if (!ColorInstructionConfirmTypeEnum.isValid(reqVO.getConfirmType())) {
            throw exception(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT);
        }

        // 5. 条件更新：status='IN_PROGRESS'，影响行数 0 报 1_030_032_002（防并发覆盖与重复确认）
        ColorInstructionDO updateDO = new ColorInstructionDO();
        updateDO.setStatus(ColorInstructionStatusEnum.CONFIRMED.getValue());
        updateDO.setConfirmType(reqVO.getConfirmType());
        updateDO.setCustomerFeedback(StringUtils.hasText(reqVO.getCustomerFeedback()) ? reqVO.getCustomerFeedback().trim() : null);
        updateDO.setConfirmedBy(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setConfirmedAt(java.time.LocalDateTime.now());

        int updated = colorInstructionMapper.update(updateDO,
                new LambdaUpdateWrapper<ColorInstructionDO>()
                        .eq(ColorInstructionDO::getId, reqVO.getId())
                        .eq(ColorInstructionDO::getStatus, ColorInstructionStatusEnum.IN_PROGRESS.getValue()));
        if (updated == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 6. 日志变量填充
        LogRecordContext.putVariable("orderId", order.getId());
        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
    }

    @Override
    public ColorInstructionDO validateColorInstructionForDownstream(Long colorInstructionId,
                                                                     Long customerId,
                                                                     Long productId,
                                                                     String productCode,
                                                                     com.luna.lunasrv.framework.common.exception.ErrorCode notFoundErrorCode) {
        if (colorInstructionId == null) {
            return null;
        }
        ColorInstructionDO colorInstruction = colorInstructionMapper.selectById(colorInstructionId);
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (colorInstruction == null
                || (currentTenantId != null && colorInstruction.getTenantId() != null && !Objects.equals(colorInstruction.getTenantId(), currentTenantId))
                || Boolean.TRUE.equals(colorInstruction.getDeleted())
                || (!"CONFIRMED".equals(colorInstruction.getStatus()) && !"IN_PRODUCTION".equals(colorInstruction.getStatus()))) {
            throw exception(notFoundErrorCode, "打色指示单");
        }
        // 口径一致校验：客户
        if (customerId != null && colorInstruction.getCustomerId() != null
                && !Objects.equals(colorInstruction.getCustomerId(), customerId)) {
            throw exception(notFoundErrorCode, "打色指示单");
        }
        // 口径一致校验：产品（优先按 productId 比对，若无则按 productCode 文本比对）
        if (productId != null && colorInstruction.getProductId() != null
                && !Objects.equals(colorInstruction.getProductId(), productId)) {
            throw exception(notFoundErrorCode, "打色指示单");
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(productCode) && org.apache.commons.lang3.StringUtils.isNotBlank(colorInstruction.getProductCode())
                && !Objects.equals(colorInstruction.getProductCode().trim(), productCode.trim())) {
            throw exception(notFoundErrorCode, "打色指示单");
        }
        return colorInstruction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void markInProduction(Long id) {
        if (id == null) {
            return;
        }
        LambdaUpdateWrapper<ColorInstructionDO> updateWrapper = new LambdaUpdateWrapper<ColorInstructionDO>()
                .eq(ColorInstructionDO::getId, id)
                .eq(ColorInstructionDO::getStatus, ColorInstructionStatusEnum.CONFIRMED.getValue());
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            updateWrapper.eq(ColorInstructionDO::getTenantId, tenantId);
        }

        ColorInstructionDO updateDO = new ColorInstructionDO();
        updateDO.setStatus(ColorInstructionStatusEnum.IN_PRODUCTION.getValue());

        int updated = colorInstructionMapper.update(updateDO, updateWrapper);
        if (updated > 0) {
            log.info("[markInProduction] 打色指示单投产成功, id={}", id);
        } else {
            log.debug("[markInProduction] 打色指示单状态非 CONFIRMED 或不存在，幂等跳过, id={}", id);
        }
    }

    @Override
    public boolean hasActiveDownstreamRef(Long id) {
        if (id == null) {
            return false;
        }
        Long greigeCount = greigeOrderMapper.selectCount(new LambdaQueryWrapperX<GreigeOrderDO>()
                .eq(GreigeOrderDO::getColorInstructionId, id)
                .ne(GreigeOrderDO::getStatus, DOWNSTREAM_STATUS_CANCELLED));
        if (greigeCount != null && greigeCount > 0) {
            return true;
        }
        Long dyeingCount = dyeingOrderMapper.selectCount(new LambdaQueryWrapperX<DyeingOrderDO>()
                .eq(DyeingOrderDO::getColorInstructionId, id)
                .ne(DyeingOrderDO::getStatus, DOWNSTREAM_STATUS_CANCELLED));
        return dyeingCount != null && dyeingCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "ROLLBACK", bizNo = "{{#id}}",
            success = "回退打色指示单【{{#instructionNo}}】")
    public void rollbackColorInstruction(Long id) {
        if (id == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 1. 存在性校验
        ColorInstructionDO order = colorInstructionMapper.selectById(id);
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 2. 状态守卫：仅 CONFIRMED 态单据允许回退
        if (!ColorInstructionStatusEnum.CONFIRMED.getValue().equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 守卫：无任何下游有效引用（status ≠ CANCELLED 且未删除）
        if (hasActiveDownstreamRef(id)) {
            throw exception(COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE);
        }

        // 4. 条件更新防并发：CONFIRMED -> IN_PROGRESS，清空确认信息（行状态保持不回退）
        ColorInstructionDO updateDO = new ColorInstructionDO();
        updateDO.setStatus(ColorInstructionStatusEnum.IN_PROGRESS.getValue());
        updateDO.setConfirmType(null);
        updateDO.setCustomerFeedback(null);
        updateDO.setConfirmedBy(null);
        updateDO.setConfirmedAt(null);

        int updated = colorInstructionMapper.update(updateDO,
                new LambdaUpdateWrapper<ColorInstructionDO>()
                        .eq(ColorInstructionDO::getId, id)
                        .eq(ColorInstructionDO::getStatus, ColorInstructionStatusEnum.CONFIRMED.getValue()));
        if (updated == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 5. 日志变量填充
        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "VOID", bizNo = "{{#reqVO.id}}",
            success = "作废打色指示单【{{#instructionNo}}】，原因：{{#reqVO.voidReason}}")
    public void voidColorInstruction(ColorInstructionVoidReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 1. 存在性校验
        ColorInstructionDO order = colorInstructionMapper.selectById(reqVO.getId());
        if (order == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 2. 状态守卫：仅 PENDING / IN_PROGRESS / CONFIRMED 允许作废；IN_PRODUCTION 禁作废（报 1_030_032_002），已作废等其他状态同报
        if (!ColorInstructionStatusEnum.PENDING.getValue().equals(order.getStatus())
                && !ColorInstructionStatusEnum.IN_PROGRESS.getValue().equals(order.getStatus())
                && !ColorInstructionStatusEnum.CONFIRMED.getValue().equals(order.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 守卫：无任何下游有效引用（status ≠ CANCELLED 且未删除，含下游 DRAFT 染色单）
        if (hasActiveDownstreamRef(reqVO.getId())) {
            throw exception(COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE);
        }

        // 4. 条件更新防并发：写入 voidReason 与 status=VOID
        ColorInstructionDO updateDO = new ColorInstructionDO();
        updateDO.setStatus(ColorInstructionStatusEnum.VOID.getValue());
        updateDO.setVoidReason(StringUtils.hasText(reqVO.getVoidReason()) ? reqVO.getVoidReason().trim() : null);

        int updated = colorInstructionMapper.update(updateDO,
                new LambdaUpdateWrapper<ColorInstructionDO>()
                        .eq(ColorInstructionDO::getId, reqVO.getId())
                        .in(ColorInstructionDO::getStatus, List.of(
                                ColorInstructionStatusEnum.PENDING.getValue(),
                                ColorInstructionStatusEnum.IN_PROGRESS.getValue(),
                                ColorInstructionStatusEnum.CONFIRMED.getValue()
                        )));
        if (updated == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 5. 日志变量填充
        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());
    }

}
