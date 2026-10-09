package com.luna.lunasrv.module.lunaerp.service.production.yarnspec;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportHelper;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ImportRow;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecCascadeOptionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecImportVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Resource
    private SecurityFrameworkService securityFrameworkService;

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

    @Override
    public List<YarnSpecImportVO> getImportTemplate() {
        return Collections.singletonList(
                YarnSpecImportVO.builder()
                        .yarnTypeName("棉")
                        .specName("32S 棉")
                        .standardName("32S-COTTON")
                        .spinMethod("环锭纺")
                        .luster("半光")
                        .refPrice(new BigDecimal("28.50"))
                        .remark("标准纱线规格示例")
                        .build()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importYarnSpec(MultipartFile file) throws IOException {
        boolean hasAmountPermission = securityFrameworkService != null
                && securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT);

        return ExcelImportHelper.importExcel(
                file,
                YarnSpecImportVO.class,
                rows -> validateYarnSpecImportRows(rows, hasAmountPermission),
                validList -> {
                    Map<String, Long> activeTypeMap = queryActiveYarnTypeMap();
                    List<YarnSpecDO> doList = new ArrayList<>(validList.size());
                    for (YarnSpecImportVO vo : validList) {
                        YarnSpecDO spec = BeanUtils.toBean(vo, YarnSpecDO.class);
                        Long yarnTypeId = activeTypeMap.get(vo.getYarnTypeName().trim());
                        spec.setYarnTypeId(yarnTypeId);
                        spec.setStatus(DEFAULT_STATUS);
                        doList.add(spec);
                    }
                    yarnSpecMapper.insertBatch(doList);
                }
        );
    }

    /**
     * 逐行校验纱线规格导入数据：
     * 1. 材质名称必填与启用状态存在性精确匹配
     * 2. 规格名称必填
     * 3. 标准名称必填
     * 4. 参考单价受 lunaerp:sensitive:amount 管控（无权限按行报错 1_030_031_102；有权限则金额非负校验）
     * 5. 文件内同材质标准名称重复排查
     * 6. 数据库内同材质标准名称查重（含软删行，冲突报 1_030_008_003 规格已存在）
     */
    private void validateYarnSpecImportRows(List<ImportRow<YarnSpecImportVO>> rows, boolean hasAmountPermission) {
        Map<String, Long> activeTypeMap = queryActiveYarnTypeMap();
        Set<String> seenTypeAndStandardNames = new HashSet<>();
        Set<String> fileDuplicateTypeAndStandardNames = new HashSet<>();

        for (ImportRow<YarnSpecImportVO> row : rows) {
            YarnSpecImportVO vo = row.getData();

            // 材质名称必填与存在性校验
            String typeName = vo.getYarnTypeName() != null ? vo.getYarnTypeName().trim() : "";
            Long yarnTypeId = null;
            if (StrUtil.isBlank(typeName)) {
                row.addError("材质名称不能为空");
            } else {
                yarnTypeId = activeTypeMap.get(typeName);
                if (yarnTypeId == null) {
                    row.addError("未找到启用的纱线材质【" + typeName + "】");
                }
            }

            // 规格名称必填
            String specName = vo.getSpecName() != null ? vo.getSpecName().trim() : "";
            if (StrUtil.isBlank(specName)) {
                row.addError("规格名称不能为空");
            }

            // 标准名称必填
            String standardName = vo.getStandardName() != null ? vo.getStandardName().trim() : "";
            if (StrUtil.isBlank(standardName)) {
                row.addError("标准名称不能为空");
            } else if (yarnTypeId != null) {
                String pairKey = yarnTypeId + "::" + standardName;
                if (!seenTypeAndStandardNames.add(pairKey)) {
                    fileDuplicateTypeAndStandardNames.add(pairKey);
                }
            }

            // 参考单价敏感权限与非负金额校验
            if (vo.getRefPrice() != null) {
                if (!hasAmountPermission) {
                    row.addError(StrUtil.format(CommonErrorCodeConstants.EXCEL_IMPORT_PERMISSION_DENIED.getMsg(),
                            SensitiveConstants.PERMISSION_AMOUNT));
                } else if (vo.getRefPrice().compareTo(BigDecimal.ZERO) < 0) {
                    row.addError("参考价不能为负数");
                }
            }
        }

        // 文件内同材质标准名称重复
        if (!fileDuplicateTypeAndStandardNames.isEmpty()) {
            for (ImportRow<YarnSpecImportVO> row : rows) {
                YarnSpecImportVO vo = row.getData();
                String typeName = vo.getYarnTypeName() != null ? vo.getYarnTypeName().trim() : "";
                String standardName = vo.getStandardName() != null ? vo.getStandardName().trim() : "";
                Long yarnTypeId = activeTypeMap.get(typeName);
                if (yarnTypeId != null && StrUtil.isNotBlank(standardName)) {
                    String pairKey = yarnTypeId + "::" + standardName;
                    if (fileDuplicateTypeAndStandardNames.contains(pairKey)) {
                        row.addError("标准名称在文件中重复");
                    }
                }
            }
        }

        // 数据库内冲突校验（租户+材质内唯一，含软删行，报 1_030_008_003 规格已存在）
        for (ImportRow<YarnSpecImportVO> row : rows) {
            YarnSpecImportVO vo = row.getData();
            String typeName = vo.getYarnTypeName() != null ? vo.getYarnTypeName().trim() : "";
            String standardName = vo.getStandardName() != null ? vo.getStandardName().trim() : "";
            Long yarnTypeId = activeTypeMap.get(typeName);
            if (yarnTypeId != null && StrUtil.isNotBlank(standardName)) {
                YarnSpecDO existing = yarnSpecMapper.selectByYarnTypeIdAndStandardNameIncludingDeleted(yarnTypeId, standardName);
                if (existing != null) {
                    row.addError(StrUtil.format(YARN_SPEC_NAME_DUPLICATE.getMsg(), standardName));
                }
            }
        }
    }

    private Map<String, Long> queryActiveYarnTypeMap() {
        List<YarnTypeDO> typeList = yarnTypeMapper.selectList(
                new LambdaQueryWrapperX<YarnTypeDO>().eq(YarnTypeDO::getStatus, "ACTIVE"));
        if (CollUtil.isEmpty(typeList)) {
            return Collections.emptyMap();
        }
        return typeList.stream()
                .filter(t -> StrUtil.isNotBlank(t.getName()))
                .collect(Collectors.toMap(t -> t.getName().trim(), YarnTypeDO::getId, (k1, k2) -> k1));
    }

}
