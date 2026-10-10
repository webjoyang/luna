package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

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
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        wrapper.eqIfPresent(ColorInstructionDO::getStatus, reqVO.getStatus());

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

}
