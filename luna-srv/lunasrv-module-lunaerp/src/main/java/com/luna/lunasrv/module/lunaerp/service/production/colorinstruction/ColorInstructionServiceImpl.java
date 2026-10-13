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
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.util.collection.CollectionUtils.convertList;
import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS;

/**
 * 生产管理 - 打色指示单 Service 实现类
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

}
