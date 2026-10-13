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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
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

        // 3. 引用校验：客户/产品强关联校验；打色承接方供应商可空，传值则校验
        CustomerDO customer = customerMapper.selectById(createReqVO.getCustomerId());
        if (customer == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
        }

        ProductDO product = productMapper.selectById(createReqVO.getProductId());
        if (product == null) {
            throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
        }

        SupplierDO labFactory = null;
        if (createReqVO.getLabFactoryId() != null) {
            labFactory = supplierMapper.selectById(createReqVO.getLabFactoryId());
            if (labFactory == null) {
                throw exception(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
            }
        }

        // 4. 颜色行灯源合法性校验（主灯源必填且合法，辅灯源可空、若有必合法）
        for (ColorInstructionColorSaveVO color : createReqVO.getColors()) {
            if (!VALID_LIGHT_SOURCES.contains(color.getLightPrimary())) {
                throw exception(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT);
            }
            if (StringUtils.hasText(color.getLightSecondary()) && !VALID_LIGHT_SOURCES.contains(color.getLightSecondary().trim())) {
                throw exception(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT);
            }
        }

        // 5. 组装主表 DO 与快照回填、缺省值
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
        order.setClientName(customer.getCompanyName());
        order.setProductCode(product.getCode());
        order.setProductName(product.getName());
        if (!StringUtils.hasText(order.getComposition())) {
            order.setComposition(product.getComposition());
        }
        if (!StringUtils.hasText(order.getWidth()) && product.getWidthCm() != null) {
            order.setWidth(product.getWidthCm().stripTrailingZeros().toPlainString() + "cm");
        }
        if (!StringUtils.hasText(order.getGsm()) && product.getWeightGsm() != null) {
            order.setGsm(product.getWeightGsm().stripTrailingZeros().toPlainString() + "g/m²");
        }
        if (labFactory != null) {
            order.setLabFactoryName(labFactory.getCompanyName());
        } else {
            order.setLabFactoryId(null);
            order.setLabFactoryName(null);
        }

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

}
