package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

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

        LogRecordContext.putVariable("instructionNo", order.getInstructionNo());

        // 3. 级联逻辑删除：主表 + 颜色行（单号不释放）
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
        LinkedHashSet<String> colorNoGroupOrder = new LinkedHashSet<>();
        for (ColorInstructionColorSaveVO vo : activeSubmitted) {
            if (StringUtils.hasText(vo.getCustomerColorNo())) {
                colorNoGroupOrder.add(vo.getCustomerColorNo().trim());
            }
        }
        for (ColorInstructionColorDO missingFrozen : retainedFrozenMissingRows) {
            if (StringUtils.hasText(missingFrozen.getCustomerColorNo())) {
                colorNoGroupOrder.add(missingFrozen.getCustomerColorNo().trim());
            }
        }

        Map<String, Integer> groupRankMap = new HashMap<>();
        int rank = 0;
        for (String colorNo : colorNoGroupOrder) {
            groupRankMap.put(colorNo, rank++);
        }

        List<ColorInstructionColorDO> survivingList = new ArrayList<>();
        survivingList.addAll(toInsert);
        for (Map.Entry<Long, ColorInstructionColorSaveVO> entry : updateVOMap.entrySet()) {
            Long id = entry.getKey();
            ColorInstructionColorSaveVO vo = entry.getValue();
            ColorInstructionColorDO dbRow = dbColorMap.get(id);
            if ("PENDING".equals(dbRow.getStatus())) {
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

        survivingList.sort((a, b) -> {
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

}
