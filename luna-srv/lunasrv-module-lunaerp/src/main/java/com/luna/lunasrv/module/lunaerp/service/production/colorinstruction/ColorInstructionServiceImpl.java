package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSummaryVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.util.collection.CollectionUtils.convertList;
import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS;

/**
 * 生产管理 - 打色指示单 Service 实现
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ColorInstructionServiceImpl implements ColorInstructionService {

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Resource
    private ColorInstructionColorMapper colorInstructionColorMapper;

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
