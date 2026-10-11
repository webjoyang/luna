package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.web.core.util.WebFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionConfirmTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.util.collection.CollectionUtils.convertList;
import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.*;

/**
 * 生产管理 - 打色指示单 Service 实现
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ColorInstructionServiceImpl implements ColorInstructionService {

    /**
     * 打色单号前缀（DS + 8位年月日 + 3位日流水，按天取号，ADR-0006）
     */
    private static final String INSTRUCTION_NO_PREFIX = "DS";
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 3;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> VALID_LIGHT_SOURCES = Set.of("D65", "TL84", "CWF", "A", "UV", "TL83");

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Resource
    private ColorInstructionColorMapper colorInstructionColorMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Override
    public PageResult<ColorInstructionRespVO> getColorInstructionPage(ColorInstructionPageReqVO reqVO) {
        LambdaQueryWrapperX<ColorInstructionDO> wrapper = buildPageWrapper(reqVO);

        // 按客户色号过滤：先子查询命中的打色单 ID 集合（防 N+1 与跨表 JOIN 行乘积），空则直接返回空结果
        if (StringUtils.hasText(reqVO.getCustomerColorNo())) {
            Set<Long> matchingInstructionIds = colorInstructionColorMapper
                    .selectInstructionIdsByCustomerColorNo(reqVO.getCustomerColorNo());
            if (matchingInstructionIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(ColorInstructionDO::getId, matchingInstructionIds);
        }

        // 默认按单据日期降序，次按单据 ID 降序
        wrapper.orderByDesc(ColorInstructionDO::getInstructionDate)
                .orderByDesc(ColorInstructionDO::getId);

        PageResult<ColorInstructionDO> pageResult = colorInstructionMapper.selectPage(reqVO, wrapper);
        if (CollectionUtils.isEmpty(pageResult.getList())) {
            return PageResult.empty();
        }

        // 防 N+1：单次批量加载当前页所有打色单的颜色行
        List<Long> instructionIds = convertList(pageResult.getList(), ColorInstructionDO::getId);
        List<ColorInstructionColorDO> allColors = colorInstructionColorMapper.selectListByInstructionIds(instructionIds);
        Map<Long, List<ColorInstructionColorDO>> colorMap = allColors.stream()
                .collect(Collectors.groupingBy(ColorInstructionColorDO::getInstructionId));

        List<ColorInstructionRespVO> respList = pageResult.getList().stream().map(instruction -> {
            ColorInstructionRespVO respVO = BeanUtils.toBean(instruction, ColorInstructionRespVO.class);
            List<ColorInstructionColorDO> instructionColors = colorMap.getOrDefault(instruction.getId(), Collections.emptyList());
            respVO.setColorSummary(buildColorSummary(instructionColors));
            return respVO;
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public ColorInstructionRespVO getColorInstruction(Long id) {
        if (id == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        ColorInstructionDO instruction = colorInstructionMapper.selectById(id);
        if (instruction == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        ColorInstructionRespVO respVO = BeanUtils.toBean(instruction, ColorInstructionRespVO.class);
        List<ColorInstructionColorDO> colors = colorInstructionColorMapper.selectListByInstructionId(id);
        respVO.setColors(BeanUtils.toBean(colors, ColorInstructionColorRespVO.class));
        respVO.setColorSummary(buildColorSummary(colors));
        return respVO;
    }

    @Override
    public String getNextNo() {
        return generateInstructionNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createColorInstruction(ColorInstructionSaveReqVO createReqVO) {
        // 1. 编号处理：手填查重（含已删除行，撞号报 1_030_032_001）；留空自动生成（有限次顺延重试）
        String instructionNo;
        if (StringUtils.hasText(createReqVO.getInstructionNo())) {
            instructionNo = createReqVO.getInstructionNo().trim();
            validateInstructionNoUnique(instructionNo);
        } else {
            instructionNo = generateInstructionNoWithRetry();
        }

        // 2. 引用校验与快照回填：
        // 客户强关联，不存在抛 1_030_032_003
        CustomerDO customer = customerMapper.selectById(createReqVO.getCustomerId());
        if (customer == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS);
        }

        // 产品强关联，不存在抛 1_030_032_003
        ProductDO product = productMapper.selectById(createReqVO.getProductId());
        if (product == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS);
        }

        // 打色承接方供应商可空（空=本厂打色房自做，ADR-0022 自织先例），存在则校验
        SupplierDO labFactory = null;
        if (createReqVO.getLabFactoryId() != null) {
            labFactory = supplierMapper.selectById(createReqVO.getLabFactoryId());
            if (labFactory == null) {
                throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS);
            }
        }

        // 3. 颜色行校验：客户色号、颜色名必填；主灯源必填且校验字典合法性，辅灯源非空时校验字典合法性
        if (createReqVO.getColors() != null) {
            for (ColorInstructionColorSaveReqVO colorReq : createReqVO.getColors()) {
                if (!StringUtils.hasText(colorReq.getCustomerColorNo()) || !StringUtils.hasText(colorReq.getColorName())) {
                    throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
                }
                validateLightSource(colorReq.getLightPrimary());
                if (StringUtils.hasText(colorReq.getLightSecondary())) {
                    validateLightSource(colorReq.getLightSecondary());
                }
            }
        }

        // 4. 组装主表 DO
        ColorInstructionDO instruction = BeanUtils.toBean(createReqVO, ColorInstructionDO.class);
        instruction.setId(null);
        instruction.setInstructionNo(instructionNo);

        // 回填客户快照
        if (!StringUtils.hasText(instruction.getClientName())) {
            instruction.setClientName(customer.getCompanyName());
        }

        // 回填产品快照
        if (!StringUtils.hasText(instruction.getProductCode())) {
            instruction.setProductCode(product.getCode());
        }
        if (!StringUtils.hasText(instruction.getProductName())) {
            instruction.setProductName(product.getName());
        }
        if (!StringUtils.hasText(instruction.getComposition()) && StringUtils.hasText(product.getComposition())) {
            instruction.setComposition(product.getComposition());
        }
        if (!StringUtils.hasText(instruction.getWidth()) && product.getWidthCm() != null) {
            instruction.setWidth(product.getWidthCm() + "cm");
        }
        if (!StringUtils.hasText(instruction.getGsm()) && product.getWeightGsm() != null) {
            instruction.setGsm(product.getWeightGsm() + "gsm");
        }

        // 回填打色承接方快照
        if (labFactory != null) {
            if (!StringUtils.hasText(instruction.getLabFactoryName())) {
                instruction.setLabFactoryName(labFactory.getCompanyName());
            }
        } else {
            instruction.setLabFactoryId(null);
            instruction.setLabFactoryName(null);
        }

        // 单据日期缺省今天
        if (instruction.getInstructionDate() == null) {
            instruction.setInstructionDate(LocalDate.now());
        }
        // 单据初始状态必为 PENDING
        instruction.setStatus("PENDING");

        colorInstructionMapper.insert(instruction);

        // 5. 颜色行保存（本期按 instruction_id 简单插入 + 重排 lineNo）
        if (createReqVO.getColors() != null && !createReqVO.getColors().isEmpty()) {
            int lineNo = 1;
            for (ColorInstructionColorSaveReqVO colorReq : createReqVO.getColors()) {
                ColorInstructionColorDO colorDO = BeanUtils.toBean(colorReq, ColorInstructionColorDO.class);
                colorDO.setId(null);
                colorDO.setInstructionId(instruction.getId());
                colorDO.setLineNo(lineNo++);
                if (colorDO.getRound() == null) {
                    colorDO.setRound(1);
                }
                if (!StringUtils.hasText(colorDO.getStatus())) {
                    colorDO.setStatus("PENDING");
                }
                colorInstructionColorMapper.insert(colorDO);
            }
        }

        return instruction.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateColorInstruction(ColorInstructionSaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 1. 存在性校验
        ColorInstructionDO existing = colorInstructionMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 2. 编辑守卫：仅 PENDING 和 IN_PROGRESS 可整体编辑；CONFIRMED/IN_PRODUCTION/VOID 报 1_030_032_002
        if (!"PENDING".equals(existing.getStatus()) && !"IN_PROGRESS".equals(existing.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        }

        // 3. 单号查重（手填查重排除自身，含已删除行，ADR-0006）
        if (StringUtils.hasText(updateReqVO.getInstructionNo())) {
            String instructionNo = updateReqVO.getInstructionNo().trim();
            ColorInstructionDO duplicate = colorInstructionMapper.selectByInstructionNoIncludingDeleted(instructionNo);
            if (duplicate != null && !duplicate.getId().equals(existing.getId())) {
                throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, instructionNo);
            }
            existing.setInstructionNo(instructionNo);
        }

        // 4. 引用校验（与创建一致）
        CustomerDO customer = customerMapper.selectById(updateReqVO.getCustomerId());
        if (customer == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS);
        }
        ProductDO product = productMapper.selectById(updateReqVO.getProductId());
        if (product == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS);
        }
        SupplierDO labFactory = null;
        if (updateReqVO.getLabFactoryId() != null) {
            labFactory = supplierMapper.selectById(updateReqVO.getLabFactoryId());
            if (labFactory == null) {
                throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_EXISTS);
            }
        }

        // 5. 更新单据头字段与快照
        existing.setCustomerId(updateReqVO.getCustomerId());
        existing.setClientName(StringUtils.hasText(updateReqVO.getClientName()) ? updateReqVO.getClientName() : customer.getCompanyName());
        existing.setContactPerson(updateReqVO.getContactPerson());

        existing.setProductId(updateReqVO.getProductId());
        existing.setProductCode(StringUtils.hasText(updateReqVO.getProductCode()) ? updateReqVO.getProductCode() : product.getCode());
        existing.setProductName(StringUtils.hasText(updateReqVO.getProductName()) ? updateReqVO.getProductName() : product.getName());
        existing.setComposition(StringUtils.hasText(updateReqVO.getComposition()) ? updateReqVO.getComposition() : product.getComposition());
        existing.setWidth(StringUtils.hasText(updateReqVO.getWidth()) ? updateReqVO.getWidth() : (product.getWidthCm() != null ? product.getWidthCm() + "cm" : null));
        existing.setGsm(StringUtils.hasText(updateReqVO.getGsm()) ? updateReqVO.getGsm() : (product.getWeightGsm() != null ? product.getWeightGsm() + "gsm" : null));

        if (labFactory != null) {
            existing.setLabFactoryId(labFactory.getId());
            existing.setLabFactoryName(StringUtils.hasText(updateReqVO.getLabFactoryName()) ? updateReqVO.getLabFactoryName() : labFactory.getCompanyName());
        } else {
            existing.setLabFactoryId(null);
            existing.setLabFactoryName(null);
        }

        if (updateReqVO.getInstructionDate() != null) {
            existing.setInstructionDate(updateReqVO.getInstructionDate());
        }
        existing.setDeliveryDate(updateReqVO.getDeliveryDate());
        existing.setRemark(updateReqVO.getRemark());

        colorInstructionMapper.updateById(existing);

        // 6. 差异保存颜色明细行（含行状态守卫与 lineNo 服务端重排）
        diffSaveColors(existing.getId(), updateReqVO.getColors(), updateReqVO.getDeletedColorIds());
    }

    /**
     * 对打色指示单颜色行进行差异保存（同事务内三分流）：
     * - 带 id 行：校验属于本单据；APPROVED/REJECTED 行变更报错（1_030_032_006）；PENDING 行更新
     * - 无 id 行：新增（round=1、status=PENDING）
     * - 请求缺失的持久化行：PENDING 行逻辑软删除；APPROVED/REJECTED 冻结行缺失不报错、DB 保持冻结
     * - 显式删除标记：冻结行报 1_030_032_006，PENDING 行逻辑软删除
     * - 服务端按 customerColorNo 分组重排 lineNo（同色号多轮相邻、最新轮在前、从 1 连续编号）
     */
    private void diffSaveColors(Long instructionId, List<ColorInstructionColorSaveReqVO> colors,
                                List<Long> deletedColorIds) {
        // 1. 查询库内存量颜色明细（按 line_no 升序）
        List<ColorInstructionColorDO> dbColors = colorInstructionColorMapper.selectListByInstructionId(instructionId);
        Map<Long, ColorInstructionColorDO> dbColorMap = dbColors.stream()
                .collect(Collectors.toMap(ColorInstructionColorDO::getId, Function.identity()));
        Map<Long, Integer> origLineNoMap = dbColors.stream()
                .collect(Collectors.toMap(ColorInstructionColorDO::getId, c -> c.getLineNo() != null ? c.getLineNo() : -1));
        Set<Long> remainingDbColorIds = new HashSet<>(dbColorMap.keySet());

        // 2. 处理显式删除 ID 集合
        if (deletedColorIds != null) {
            for (Long delId : deletedColorIds) {
                ColorInstructionColorDO dbRow = dbColorMap.get(delId);
                if (dbRow == null) {
                    throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
                }
                if ("APPROVED".equals(dbRow.getStatus()) || "REJECTED".equals(dbRow.getStatus())) {
                    throw exception(COLOR_INSTRUCTION_COLOR_STATUS_NOT_ALLOWED);
                }
                colorInstructionColorMapper.deleteById(delId);
                remainingDbColorIds.remove(delId);
            }
        }

        // 3. 处理请求携带的颜色行
        List<ColorInstructionColorSaveReqVO> reqColors = colors != null ? colors : Collections.emptyList();
        Set<Long> seenIds = new HashSet<>();
        List<ColorInstructionColorDO> survivingRows = new ArrayList<>();
        List<ColorInstructionColorDO> rowsToInsert = new ArrayList<>();
        List<ColorInstructionColorDO> rowsToUpdate = new ArrayList<>();

        for (ColorInstructionColorSaveReqVO colorReq : reqColors) {
            // 显式行级删除标记
            if (Boolean.TRUE.equals(colorReq.getDeleted())) {
                if (colorReq.getId() == null) {
                    continue;
                }
                ColorInstructionColorDO dbRow = dbColorMap.get(colorReq.getId());
                if (dbRow == null) {
                    throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
                }
                if ("APPROVED".equals(dbRow.getStatus()) || "REJECTED".equals(dbRow.getStatus())) {
                    throw exception(COLOR_INSTRUCTION_COLOR_STATUS_NOT_ALLOWED);
                }
                colorInstructionColorMapper.deleteById(colorReq.getId());
                remainingDbColorIds.remove(colorReq.getId());
                continue;
            }

            // 基础必填校验与灯源字典校验
            if (!StringUtils.hasText(colorReq.getCustomerColorNo()) || !StringUtils.hasText(colorReq.getColorName())) {
                throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
            }
            validateLightSource(colorReq.getLightPrimary());
            if (StringUtils.hasText(colorReq.getLightSecondary())) {
                validateLightSource(colorReq.getLightSecondary());
            }

            if (colorReq.getId() != null) {
                Long rowId = colorReq.getId();
                if (!seenIds.add(rowId)) {
                    throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
                }
                ColorInstructionColorDO dbRow = dbColorMap.get(rowId);
                if (dbRow == null) {
                    // 跨父非法 ID（不属于本打色单或不存在）-> 抛 1_030_032_100
                    throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
                }
                remainingDbColorIds.remove(rowId);

                if ("APPROVED".equals(dbRow.getStatus()) || "REJECTED".equals(dbRow.getStatus())) {
                    // 冻结行守卫：提交任何字段变更报 1_030_032_006
                    if (isColorRowChanged(colorReq, dbRow)) {
                        throw exception(COLOR_INSTRUCTION_COLOR_STATUS_NOT_ALLOWED);
                    }
                    // 冻结行未修改，保留在存活列表
                    survivingRows.add(dbRow);
                } else {
                    // PENDING 行：允许修改
                    ColorInstructionColorDO updateDO = BeanUtils.toBean(colorReq, ColorInstructionColorDO.class);
                    updateDO.setId(rowId);
                    updateDO.setInstructionId(instructionId);
                    if (updateDO.getRound() == null) {
                        updateDO.setRound(dbRow.getRound() != null ? dbRow.getRound() : 1);
                    }
                    if (!StringUtils.hasText(updateDO.getStatus())) {
                        updateDO.setStatus(dbRow.getStatus());
                    }
                    survivingRows.add(updateDO);
                    if (isColorRowChanged(colorReq, dbRow)) {
                        rowsToUpdate.add(updateDO);
                    }
                }
            } else {
                // 无 id 行：新增（round=1，status=PENDING）
                ColorInstructionColorDO insertDO = BeanUtils.toBean(colorReq, ColorInstructionColorDO.class);
                insertDO.setId(null);
                insertDO.setInstructionId(instructionId);
                insertDO.setRound(1);
                insertDO.setStatus("PENDING");
                rowsToInsert.add(insertDO);
                survivingRows.add(insertDO);
            }
        }

        // 4. 处理请求缺失的持久化行
        for (Long omittedId : remainingDbColorIds) {
            ColorInstructionColorDO omittedRow = dbColorMap.get(omittedId);
            if ("PENDING".equals(omittedRow.getStatus())) {
                // 请求缺失的持久化 PENDING 行 -> 逻辑软删除
                colorInstructionColorMapper.deleteById(omittedId);
            } else {
                // APPROVED / REJECTED 冻结行缺失不报错，DB 中保持冻结并参与 lineNo 重排
                survivingRows.add(omittedRow);
            }
        }

        // 5. 服务端按 customerColorNo 分组重排 lineNo（同色号多轮相邻、最新轮在前、从 1 连续编号）
        reorderLineNo(survivingRows);

        // 6. 持久化变更
        for (ColorInstructionColorDO insertDO : rowsToInsert) {
            colorInstructionColorMapper.insert(insertDO);
        }

        for (ColorInstructionColorDO row : survivingRows) {
            if (row.getId() != null) {
                Integer origLineNo = origLineNoMap.get(row.getId());
                boolean lineNoChanged = origLineNo != null && !Objects.equals(row.getLineNo(), origLineNo);
                boolean fieldsChanged = rowsToUpdate.contains(row);
                if (fieldsChanged || lineNoChanged) {
                    colorInstructionColorMapper.updateById(row);
                }
            }
        }
    }

    /**
     * 按 customerColorNo 分组重排 lineNo（同色号多轮相邻、最新轮在前、从 1 连续编号）
     */
    private void reorderLineNo(List<ColorInstructionColorDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        Map<String, List<ColorInstructionColorDO>> groups = new LinkedHashMap<>();
        for (ColorInstructionColorDO row : rows) {
            String colorNo = row.getCustomerColorNo() != null ? row.getCustomerColorNo() : "";
            groups.computeIfAbsent(colorNo, k -> new ArrayList<>()).add(row);
        }

        for (List<ColorInstructionColorDO> groupRows : groups.values()) {
            groupRows.sort((r1, r2) -> {
                int round1 = r1.getRound() != null ? r1.getRound() : 1;
                int round2 = r2.getRound() != null ? r2.getRound() : 1;
                return Integer.compare(round2, round1); // 最新轮在前
            });
        }

        int nextLineNo = 1;
        for (List<ColorInstructionColorDO> groupRows : groups.values()) {
            for (ColorInstructionColorDO row : groupRows) {
                row.setLineNo(nextLineNo++);
            }
        }
    }

    /**
     * 判断颜色行是否有任何业务字段变更
     */
    private boolean isColorRowChanged(ColorInstructionColorSaveReqVO req, ColorInstructionColorDO db) {
        if (!Objects.equals(trimToNull(req.getCustomerColorNo()), trimToNull(db.getCustomerColorNo()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getColorName()), trimToNull(db.getColorName()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getFactoryColorNo()), trimToNull(db.getFactoryColorNo()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getColorHex()), trimToNull(db.getColorHex()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getLightPrimary()), trimToNull(db.getLightPrimary()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getLightSecondary()), trimToNull(db.getLightSecondary()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getColorCardRef()), trimToNull(db.getColorCardRef()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getSamplePhotos()), trimToNull(db.getSamplePhotos()))) {
            return true;
        }
        if (req.getCopies() != null && !Objects.equals(req.getCopies(), db.getCopies())) {
            return true;
        }
        if (req.getRound() != null && !Objects.equals(req.getRound(), db.getRound())) {
            return true;
        }
        if (StringUtils.hasText(req.getStatus()) && !Objects.equals(req.getStatus(), db.getStatus())) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getRejectReason()), trimToNull(db.getRejectReason()))) {
            return true;
        }
        if (!Objects.equals(trimToNull(req.getRemark()), trimToNull(db.getRemark()))) {
            return true;
        }
        return false;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteColorInstruction(Long id) {
        if (id == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        ColorInstructionDO instruction = colorInstructionMapper.selectById(id);
        if (instruction == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        if (!"PENDING".equals(instruction.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }

        // 级联软删除主表和颜色明细行
        colorInstructionMapper.deleteById(id);
        colorInstructionColorMapper.delete(new LambdaQueryWrapperX<ColorInstructionColorDO>()
                .eq(ColorInstructionColorDO::getInstructionId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startColorInstruction(Long id) {
        if (id == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        ColorInstructionDO instruction = colorInstructionMapper.selectById(id);
        if (instruction == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        if (!"PENDING".equals(instruction.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }

        // 条件更新 PENDING -> IN_PROGRESS，影响行数 0 抛 1_030_032_002
        int rows = colorInstructionMapper.update(null, new LambdaUpdateWrapper<ColorInstructionDO>()
                .eq(ColorInstructionDO::getId, id)
                .eq(ColorInstructionDO::getStatus, "PENDING")
                .set(ColorInstructionDO::getStatus, "IN_PROGRESS"));
        if (rows == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveColorLine(Long lineId) {
        if (lineId == null) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }
        ColorInstructionColorDO colorDO = colorInstructionColorMapper.selectById(lineId);
        if (colorDO == null) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }
        ColorInstructionDO parent = colorInstructionMapper.selectById(colorDO.getInstructionId());
        if (parent == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        // 打色单须为 PENDING 或 IN_PROGRESS
        if (!"PENDING".equals(parent.getStatus()) && !"IN_PROGRESS".equals(parent.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }
        if (!"PENDING".equals(colorDO.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }

        // 条件更新行状态 PENDING -> APPROVED
        int rows = colorInstructionColorMapper.update(null, new LambdaUpdateWrapper<ColorInstructionColorDO>()
                .eq(ColorInstructionColorDO::getId, lineId)
                .eq(ColorInstructionColorDO::getStatus, "PENDING")
                .set(ColorInstructionColorDO::getStatus, "APPROVED"));
        if (rows == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }

        // 便利规则：若主单仍为 PENDING，同事务自动转为 IN_PROGRESS
        if ("PENDING".equals(parent.getStatus())) {
            colorInstructionMapper.update(null, new LambdaUpdateWrapper<ColorInstructionDO>()
                    .eq(ColorInstructionDO::getId, parent.getId())
                    .eq(ColorInstructionDO::getStatus, "PENDING")
                    .set(ColorInstructionDO::getStatus, "IN_PROGRESS"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long rejectRestrikeColorLine(ColorInstructionColorRejectRestrikeReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }
        ColorInstructionColorDO colorDO = colorInstructionColorMapper.selectById(reqVO.getId());
        if (colorDO == null) {
            throw exception(COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
        }
        ColorInstructionDO parent = colorInstructionMapper.selectById(colorDO.getInstructionId());
        if (parent == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        // 打色单须为 PENDING 或 IN_PROGRESS
        if (!"PENDING".equals(parent.getStatus()) && !"IN_PROGRESS".equals(parent.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }
        // REJECTED 行不可再驳回重打，报 1_030_032_006
        if ("REJECTED".equals(colorDO.getStatus())) {
            throw exception(COLOR_INSTRUCTION_COLOR_STATUS_NOT_ALLOW);
        }
        if (!"PENDING".equals(colorDO.getStatus()) && !"APPROVED".equals(colorDO.getStatus())) {
            throw exception(COLOR_INSTRUCTION_COLOR_STATUS_NOT_ALLOW);
        }

        // 条件更新旧行置 REJECTED 并记录 rejectReason
        int rows = colorInstructionColorMapper.update(null, new LambdaUpdateWrapper<ColorInstructionColorDO>()
                .eq(ColorInstructionColorDO::getId, colorDO.getId())
                .in(ColorInstructionColorDO::getStatus, List.of("PENDING", "APPROVED"))
                .set(ColorInstructionColorDO::getStatus, "REJECTED")
                .set(ColorInstructionColorDO::getRejectReason, reqVO.getRejectReason()));
        if (rows == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }

        // 克隆新行：继承 customerColorNo/colorName/factoryColorNo/colorHex/lightPrimary/lightSecondary/colorCardRef/copies/instructionId
        // round = oldRow.round + 1，status = PENDING，samplePhotos 不继承！
        int nextRound = (colorDO.getRound() != null ? colorDO.getRound() : 1) + 1;
        ColorInstructionColorDO newRow = ColorInstructionColorDO.builder()
                .instructionId(colorDO.getInstructionId())
                .customerColorNo(colorDO.getCustomerColorNo())
                .colorName(colorDO.getColorName())
                .factoryColorNo(colorDO.getFactoryColorNo())
                .colorHex(colorDO.getColorHex())
                .lightPrimary(colorDO.getLightPrimary())
                .lightSecondary(colorDO.getLightSecondary())
                .colorCardRef(colorDO.getColorCardRef())
                .copies(colorDO.getCopies())
                .round(nextRound)
                .status("PENDING")
                .samplePhotos(null)
                .rejectReason(null)
                .remark(colorDO.getRemark())
                .build();

        // 便利规则：若主单仍为 PENDING，同事务自动转为 IN_PROGRESS
        if ("PENDING".equals(parent.getStatus())) {
            colorInstructionMapper.update(null, new LambdaUpdateWrapper<ColorInstructionDO>()
                    .eq(ColorInstructionDO::getId, parent.getId())
                    .eq(ColorInstructionDO::getStatus, "PENDING")
                    .set(ColorInstructionDO::getStatus, "IN_PROGRESS"));
        }

        // 服务端重排 lineNo（同色号多轮相邻、最新轮在前）
        List<ColorInstructionColorDO> existingColors = colorInstructionColorMapper.selectListByInstructionId(parent.getId());
        List<ColorInstructionColorDO> allColors = new ArrayList<>();
        if (CollectionUtils.isEmpty(existingColors)) {
            colorDO.setStatus("REJECTED");
            colorDO.setRejectReason(reqVO.getRejectReason());
            allColors.add(colorDO);
        } else {
            allColors.addAll(existingColors);
            boolean found = false;
            for (ColorInstructionColorDO r : allColors) {
                if (Objects.equals(r.getId(), colorDO.getId())) {
                    r.setStatus("REJECTED");
                    r.setRejectReason(reqVO.getRejectReason());
                    found = true;
                }
            }
            if (!found) {
                colorDO.setStatus("REJECTED");
                colorDO.setRejectReason(reqVO.getRejectReason());
                allColors.add(colorDO);
            }
        }
        allColors.add(newRow);

        Map<Long, Integer> origLineNoMap = allColors.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(ColorInstructionColorDO::getId, c -> c.getLineNo() != null ? c.getLineNo() : -1, (v1, v2) -> v1));

        reorderLineNo(allColors);

        colorInstructionColorMapper.insert(newRow);

        for (ColorInstructionColorDO row : allColors) {
            if (row.getId() != null) {
                Integer origLineNo = origLineNoMap.get(row.getId());
                if (origLineNo != null && !Objects.equals(row.getLineNo(), origLineNo)) {
                    colorInstructionColorMapper.updateById(row);
                }
            }
        }

        return newRow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmColorInstruction(ColorInstructionConfirmReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        // 1. 存在性与守卫 1 校验：单据状态必须为 IN_PROGRESS
        // 若状态为 CONFIRMED（重复确认）或其它非 IN_PROGRESS 态，抛出 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)
        ColorInstructionDO instruction = colorInstructionMapper.selectById(reqVO.getId());
        if (instruction == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        if (!"IN_PROGRESS".equals(instruction.getStatus())) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }

        // 2. 颜色行校验：
        List<ColorInstructionColorDO> colors = colorInstructionColorMapper.selectListByInstructionId(reqVO.getId());
        // 守卫 2：不得存在任何状态为 PENDING 的行（存在未决行报 1_030_032_004 COLOR_INSTRUCTION_PENDING_OR_NO_APPROVED_COLOR / COLOR_INSTRUCTION_CANNOT_CONFIRM）
        boolean hasPending = colors.stream().anyMatch(c -> "PENDING".equals(c.getStatus()));
        if (hasPending) {
            throw exception(COLOR_INSTRUCTION_CANNOT_CONFIRM);
        }
        // 守卫 3：必须至少有 1 行状态为 APPROVED（全部驳回不能确认，报同一错误码 1_030_032_004）
        boolean hasApproved = colors.stream().anyMatch(c -> "APPROVED".equals(c.getStatus()));
        if (!hasApproved) {
            throw exception(COLOR_INSTRUCTION_CANNOT_CONFIRM);
        }

        // 3. confirmType 校验：必填，必须为 CUSTOMER 或 INTERNAL，非法报 1_030_032_007 COLOR_INSTRUCTION_LIGHT_OR_CONFIRM_TYPE_INVALID
        if (!StringUtils.hasText(reqVO.getConfirmType())
                || !ColorInstructionConfirmTypeEnum.isValid(reqVO.getConfirmType().trim())) {
            throw exception(COLOR_INSTRUCTION_LIGHT_OR_CONFIRM_TYPE_INVALID);
        }

        // 4. 获取当前用户 ID
        Long loginUserId = WebFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            loginUserId = SecurityFrameworkUtils.getLoginUserId();
        }

        // 5. 条件更新防并发：WHERE id=? AND status='IN_PROGRESS' AND tenant_id=? AND deleted=0
        // 写入 status='CONFIRMED', confirm_type=reqVO.getConfirmType(), customer_feedback=reqVO.getCustomerFeedback(),
        // confirmed_by=WebFrameworkUtils.getLoginUserId(), confirmed_at=LocalDateTime.now()
        // 影响行数 0 抛出 1_030_032_002
        LocalDateTime now = LocalDateTime.now();
        int rows = colorInstructionMapper.update(null, new LambdaUpdateWrapper<ColorInstructionDO>()
                .eq(ColorInstructionDO::getId, reqVO.getId())
                .eq(ColorInstructionDO::getStatus, "IN_PROGRESS")
                .set(ColorInstructionDO::getStatus, "CONFIRMED")
                .set(ColorInstructionDO::getConfirmType, reqVO.getConfirmType().trim())
                .set(ColorInstructionDO::getCustomerFeedback, reqVO.getCustomerFeedback())
                .set(ColorInstructionDO::getConfirmedBy, loginUserId)
                .set(ColorInstructionDO::getConfirmedAt, now));
        if (rows == 0) {
            throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOW);
        }
    }

    /**
     * 生成打色单号：DS + 8位年月日 + 3位日流水（超 999 自然扩位）。
     * 扫描同前缀全部单据（含已删除行，ADR-0006），按最大后缀 + 1。
     */
    private String generateInstructionNo() {
        LocalDate today = LocalDate.now();
        String prefix = INSTRUCTION_NO_PREFIX + today.format(DATE_FORMATTER);
        List<ColorInstructionDO> candidates = colorInstructionMapper.selectListByInstructionNoPrefixIncludingDeleted(prefix);
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

    /**
     * 自动取号（撞号有限次顺延重试）
     */
    private String generateInstructionNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateInstructionNo();
            if (colorInstructionMapper.selectByInstructionNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, generateInstructionNo());
    }

    /**
     * 校验单号未被占用（扫描含已删除行，ADR-0006）
     */
    private void validateInstructionNoUnique(String instructionNo) {
        if (colorInstructionMapper.selectByInstructionNoIncludingDeleted(instructionNo) != null) {
            throw exception(COLOR_INSTRUCTION_NO_DUPLICATE, instructionNo);
        }
    }

    /**
     * 校验灯源合法性（对齐字典 lunaerp_color_light_source: D65/TL84/CWF/A/UV/TL83）
     */
    private void validateLightSource(String lightSource) {
        if (!StringUtils.hasText(lightSource) || !VALID_LIGHT_SOURCES.contains(lightSource.trim())) {
            throw exception(COLOR_INSTRUCTION_LIGHT_OR_CONFIRM_TYPE_INVALID);
        }
    }

    /**
     * 构造分页主表查询条件
     */
    private LambdaQueryWrapperX<ColorInstructionDO> buildPageWrapper(ColorInstructionPageReqVO reqVO) {
        LambdaQueryWrapperX<ColorInstructionDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.likeIfPresent(ColorInstructionDO::getInstructionNo, reqVO.getInstructionNo());
        wrapper.eqIfPresent(ColorInstructionDO::getCustomerId, reqVO.getCustomerId());
        if (reqVO.getStatuses() != null && !reqVO.getStatuses().isEmpty()) {
            wrapper.in(ColorInstructionDO::getStatus, reqVO.getStatuses());
        } else {
            wrapper.eqIfPresent(ColorInstructionDO::getStatus, reqVO.getStatus());
        }

        // keyword: 单号/客户名/产品名/产品编号模糊匹配
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String like = "%" + reqVO.getKeyword().trim() + "%";
            wrapper.and(w -> w
                    .apply("LOWER(instruction_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like));
        }

        // 产品编号或名称模糊
        if (StringUtils.hasText(reqVO.getProductCodeOrName())) {
            String like = "%" + reqVO.getProductCodeOrName().trim() + "%";
            wrapper.and(w -> w
                    .apply("LOWER(product_code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like));
        }

        // 单据日期区间
        if (reqVO.getInstructionDateRange() != null && reqVO.getInstructionDateRange().length > 0) {
            LocalDate start = reqVO.getInstructionDateRange()[0];
            LocalDate end = reqVO.getInstructionDateRange().length > 1 ? reqVO.getInstructionDateRange()[1] : null;
            wrapper.betweenIfPresent(ColorInstructionDO::getInstructionDate, start, end);
        }

        return wrapper;
    }

    /**
     * 聚合计算颜色摘要 chips（同色号提取最新轮次）
     */
    private List<ColorInstructionSummaryVO> buildColorSummary(List<ColorInstructionColorDO> colors) {
        if (CollectionUtils.isEmpty(colors)) {
            return Collections.emptyList();
        }
        Map<String, ColorInstructionColorDO> latestColorMap = new LinkedHashMap<>();
        for (ColorInstructionColorDO c : colors) {
            if (!StringUtils.hasText(c.getCustomerColorNo())) {
                continue;
            }
            ColorInstructionColorDO existing = latestColorMap.get(c.getCustomerColorNo());
            if (existing == null || (c.getRound() != null && existing.getRound() != null && c.getRound() > existing.getRound())) {
                latestColorMap.put(c.getCustomerColorNo(), c);
            }
        }
        return latestColorMap.values().stream().map(c -> ColorInstructionSummaryVO.builder()
                .customerColorNo(c.getCustomerColorNo())
                .colorName(c.getColorName())
                .colorHex(c.getColorHex())
                .round(c.getRound() != null ? c.getRound() : 1)
                .status(c.getStatus())
                .build()).collect(Collectors.toList());
    }

    @Override
    @LogRecord(type = "打印", subType = "PRINT", bizNo = "{{#id}}", success = "打印打色单【{{#id}}】")
    public ColorInstructionPrintRespVO getColorInstructionPrintData(Long id) {
        if (id == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }
        ColorInstructionDO instruction = colorInstructionMapper.selectById(id);
        if (instruction == null) {
            throw exception(COLOR_INSTRUCTION_NOT_EXISTS);
        }

        List<ColorInstructionColorDO> colorDOList = colorInstructionColorMapper.selectListByInstructionId(id);
        if (colorDOList != null && colorDOList.size() > 1) {
            colorDOList.sort(Comparator.comparing(
                    c -> c.getLineNo() != null ? c.getLineNo() : Integer.MAX_VALUE));
        }

        List<ColorInstructionPrintColorRespVO> colors = BeanUtils.toBean(colorDOList, ColorInstructionPrintColorRespVO.class);
        ColorInstructionPrintRespVO respVO = BeanUtils.toBean(instruction, ColorInstructionPrintRespVO.class);
        respVO.setColors(colors != null ? colors : Collections.emptyList());
        respVO.setPrintTime(LocalDateTime.now());
        return respVO;
    }

}
