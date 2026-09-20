package com.luna.lunasrv.module.lunaerp.service.production.yarnspec;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecCascadeOptionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-纱线规格 Service 实现类
 *
 * @author luna
 */
@Service
public class YarnSpecServiceImpl implements YarnSpecService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    public Long createYarnSpec(YarnSpecSaveReqVO createReqVO) {
        // 校验材质存在（1_030_008_000）
        validateYarnTypeExists(createReqVO.getYarnTypeId());
        // 校验材质内 standard_name 唯一（1_030_008_003）
        validateStandardNameUnique(null, createReqVO.getYarnTypeId(), createReqVO.getStandardName());

        YarnSpecDO yarnSpec = BeanUtils.toBean(createReqVO, YarnSpecDO.class);
        if (!StringUtils.hasText(yarnSpec.getStatus())) {
            yarnSpec.setStatus(DEFAULT_STATUS);
        }
        yarnSpecMapper.insert(yarnSpec);
        return yarnSpec.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnSpec(YarnSpecSaveReqVO updateReqVO) {
        // 校验规格存在（1_030_008_002）
        validateYarnSpecExists(updateReqVO.getId());
        // 校验材质存在（1_030_008_000）
        validateYarnTypeExists(updateReqVO.getYarnTypeId());
        // 校验材质内 standard_name 唯一（1_030_008_003）
        validateStandardNameUnique(updateReqVO.getId(), updateReqVO.getYarnTypeId(), updateReqVO.getStandardName());

        YarnSpecDO updateObj = BeanUtils.toBean(updateReqVO, YarnSpecDO.class);
        yarnSpecMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnSpec(Long id) {
        // 校验存在
        validateYarnSpecExists(id);
        // 先置 status='INACTIVE'
        YarnSpecDO updateObj = new YarnSpecDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_INACTIVE);
        yarnSpecMapper.updateById(updateObj);
        // 逻辑删除
        yarnSpecMapper.deleteById(id);
    }

    @Override
    public YarnSpecRespVO getYarnSpec(Long id) {
        YarnSpecDO yarnSpec = yarnSpecMapper.selectById(id);
        if (yarnSpec == null) {
            throw exception(YARN_SPEC_NOT_EXISTS);
        }
        YarnSpecRespVO respVO = BeanUtils.toBean(yarnSpec, YarnSpecRespVO.class);
        if (yarnSpec.getYarnTypeId() != null) {
            YarnTypeDO yarnType = yarnTypeMapper.selectById(yarnSpec.getYarnTypeId());
            if (yarnType != null) {
                respVO.setYarnTypeName(yarnType.getName());
            }
        }
        return respVO;
    }

    @Override
    public PageResult<YarnSpecRespVO> getYarnSpecPage(YarnSpecPageReqVO pageReqVO) {
        PageResult<YarnSpecDO> pageResult = yarnSpecMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty();
        }

        // 批量回填材质名称
        Set<Long> typeIds = pageResult.getList().stream()
                .map(YarnSpecDO::getYarnTypeId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> typeNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(typeIds)) {
            List<YarnTypeDO> types = yarnTypeMapper.selectByIds(typeIds);
            if (CollUtil.isNotEmpty(types)) {
                typeNameMap = types.stream().collect(Collectors.toMap(YarnTypeDO::getId, YarnTypeDO::getName));
            }
        }

        Map<Long, String> finalTypeNameMap = typeNameMap;
        List<YarnSpecRespVO> respList = pageResult.getList().stream().map(spec -> {
            YarnSpecRespVO respVO = BeanUtils.toBean(spec, YarnSpecRespVO.class);
            respVO.setYarnTypeName(finalTypeNameMap.get(spec.getYarnTypeId()));
            return respVO;
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<YarnSpecCascadeOptionVO> getCascadeOptions(String keyword) {
        // 1. 查询启用态的材质列表（按 sort 升序、id 升序）
        List<YarnTypeDO> typeList = yarnTypeMapper.selectList(new LambdaQueryWrapperX<YarnTypeDO>()
                .eq(YarnTypeDO::getStatus, DEFAULT_STATUS)
                .orderByAsc(YarnTypeDO::getSort)
                .orderByAsc(YarnTypeDO::getId));
        if (CollUtil.isEmpty(typeList)) {
            return Collections.emptyList();
        }

        // 2. 查询启用态的规格列表，若有 keyword 则对 specName 做模糊过滤
        LambdaQueryWrapperX<YarnSpecDO> specWrapper = new LambdaQueryWrapperX<YarnSpecDO>()
                .eq(YarnSpecDO::getStatus, DEFAULT_STATUS);
        if (StringUtils.hasText(keyword)) {
            specWrapper.like(YarnSpecDO::getSpecName, keyword.trim());
        }
        specWrapper.orderByAsc(YarnSpecDO::getId);
        List<YarnSpecDO> specList = yarnSpecMapper.selectList(specWrapper);

        // 3. 按 yarnTypeId 分组
        Map<Long, List<YarnSpecDO>> specGroupMap = specList.stream()
                .collect(Collectors.groupingBy(YarnSpecDO::getYarnTypeId));

        // 4. 组装两级级联结构：若传了 keyword 过滤且某材质下无匹配规格，则跳过该材质
        List<YarnSpecCascadeOptionVO> result = new ArrayList<>();
        for (YarnTypeDO type : typeList) {
            List<YarnSpecDO> specs = specGroupMap.getOrDefault(type.getId(), Collections.emptyList());
            if (StringUtils.hasText(keyword) && CollUtil.isEmpty(specs)) {
                continue;
            }
            List<YarnSpecCascadeOptionVO.SpecOption> specOptions = specs.stream().map(s ->
                    new YarnSpecCascadeOptionVO.SpecOption(
                            s.getId(),
                            s.getSpecName(),
                            s.getStandardName(),
                            s.getRefPrice(),
                            s.getSpinMethod(),
                            s.getLuster()
                    )
            ).collect(Collectors.toList());

            result.add(new YarnSpecCascadeOptionVO(type.getId(), type.getName(), specOptions));
        }

        return result;
    }

    // ==================== 私有方法 ====================

    private void validateYarnSpecExists(Long id) {
        if (id == null || yarnSpecMapper.selectById(id) == null) {
            throw exception(YARN_SPEC_NOT_EXISTS);
        }
    }

    private void validateYarnTypeExists(Long yarnTypeId) {
        if (yarnTypeId == null || yarnTypeMapper.selectById(yarnTypeId) == null) {
            throw exception(YARN_TYPE_NOT_EXISTS);
        }
    }

    private void validateStandardNameUnique(Long id, Long yarnTypeId, String standardName) {
        if (!StringUtils.hasText(standardName) || yarnTypeId == null) {
            return;
        }
        YarnSpecDO existing = yarnSpecMapper.selectByYarnTypeIdAndStandardNameIncludingDeleted(yarnTypeId, standardName);
        if (existing == null) {
            return;
        }
        // 如果存在且不是当前被更新的记录，则报错
        if (id == null || !existing.getId().equals(id)) {
            throw exception(YARN_SPEC_NAME_DUPLICATE, standardName);
        }
    }

}
