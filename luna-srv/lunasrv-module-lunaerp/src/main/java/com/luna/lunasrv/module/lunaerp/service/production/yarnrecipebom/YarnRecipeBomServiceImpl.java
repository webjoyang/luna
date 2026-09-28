package com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-纱线配比BOM Service 实现类
 *
 * @author luna
 */
@Service
public class YarnRecipeBomServiceImpl implements YarnRecipeBomService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private YarnRecipeBomMapper yarnRecipeBomMapper;

    @Resource
    private YarnRecipeBomItemMapper yarnRecipeBomItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createYarnRecipeBom(YarnRecipeBomSaveReqVO createReqVO) {
        // 1. 校验产品存在与唯一性
        ProductDO product = validateProductAndUnique(null, createReqVO.getProductId());
        // 2. 校验明细规格存在并组装
        List<YarnRecipeBomItemDO> itemDOList = validateAndBuildItems(null, createReqVO.getItems());

        // 3. 保存主表
        YarnRecipeBomDO bom = BeanUtils.toBean(createReqVO, YarnRecipeBomDO.class);
        bom.setProductCode(product.getCode());
        bom.setProductName(product.getName());
        if (!StringUtils.hasText(bom.getStatus())) {
            bom.setStatus(DEFAULT_STATUS);
        }
        yarnRecipeBomMapper.insert(bom);

        // 4. 保存明细
        saveItems(bom.getId(), itemDOList);
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnRecipeBom(YarnRecipeBomSaveReqVO updateReqVO) {
        // 1. 校验BOM存在（1_030_008_004）
        validateYarnRecipeBomExists(updateReqVO.getId());
        // 2. 校验产品存在与唯一性（排除自身）
        ProductDO product = validateProductAndUnique(updateReqVO.getId(), updateReqVO.getProductId());

        // 3. 更新主表
        YarnRecipeBomDO bom = BeanUtils.toBean(updateReqVO, YarnRecipeBomDO.class);
        bom.setProductCode(product.getCode());
        bom.setProductName(product.getName());
        yarnRecipeBomMapper.updateById(bom);

        // 4. 配比明细差异保存（三分流：update / insert / deleteByIds，ADR-0032）
        diffSaveItems(updateReqVO.getId(), updateReqVO.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnRecipeBom(Long id) {
        // 1. 校验存在
        validateYarnRecipeBomExists(id);
        // 2. 先置 status='INACTIVE'
        YarnRecipeBomDO updateObj = new YarnRecipeBomDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_INACTIVE);
        yarnRecipeBomMapper.updateById(updateObj);
        // 3. 逻辑删除主表
        yarnRecipeBomMapper.deleteById(id);
        // 4. 级联逻辑删除明细
        yarnRecipeBomItemMapper.deleteByRecipeBomId(id);
    }

    @Override
    public YarnRecipeBomRespVO getYarnRecipeBom(Long id) {
        YarnRecipeBomDO bom = yarnRecipeBomMapper.selectById(id);
        if (bom == null) {
            throw exception(YARN_RECIPE_BOM_NOT_EXISTS);
        }
        return buildRespVOWithItems(bom);
    }

    @Override
    public YarnRecipeBomRespVO getYarnRecipeBomByProductId(Long productId) {
        YarnRecipeBomDO bom = yarnRecipeBomMapper.selectByProductId(productId);
        if (bom == null) {
            return null;
        }
        return buildRespVOWithItems(bom);
    }

    @Override
    public PageResult<YarnRecipeBomRespVO> getYarnRecipeBomPage(YarnRecipeBomPageReqVO pageReqVO) {
        PageResult<YarnRecipeBomDO> pageResult = yarnRecipeBomMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty();
        }

        // 批量统计明细行数
        List<Long> bomIds = pageResult.getList().stream().map(YarnRecipeBomDO::getId).collect(Collectors.toList());
        List<YarnRecipeBomItemDO> allItems = yarnRecipeBomItemMapper.selectListByRecipeBomIds(bomIds);
        Map<Long, List<YarnRecipeBomItemDO>> itemGroupMap = allItems.stream()
                .collect(Collectors.groupingBy(YarnRecipeBomItemDO::getRecipeBomId));

        List<YarnRecipeBomRespVO> respList = pageResult.getList().stream().map(bom -> {
            YarnRecipeBomRespVO respVO = BeanUtils.toBean(bom, YarnRecipeBomRespVO.class);
            List<YarnRecipeBomItemDO> items = itemGroupMap.getOrDefault(bom.getId(), Collections.emptyList());
            respVO.setItemCount(items.size());
            return respVO;
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    // ==================== 私有方法 ====================

    private ProductDO validateProductExists(Long productId) {
        if (productId == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        ProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        return product;
    }

    private void validateProductBomUnique(Long id, Long productId) {
        if (productId == null) {
            return;
        }
        YarnRecipeBomDO existing = yarnRecipeBomMapper.selectByProductIdIncludingDeleted(productId);
        if (existing == null) {
            return;
        }
        if (id == null || !existing.getId().equals(id)) {
            throw exception(YARN_RECIPE_BOM_PRODUCT_EXISTS);
        }
    }

    private ProductDO validateProductAndUnique(Long bomId, Long productId) {
        ProductDO product = validateProductExists(productId);
        validateProductBomUnique(bomId, productId);
        return product;
    }

    private void validateYarnRecipeBomExists(Long id) {
        if (id == null || yarnRecipeBomMapper.selectById(id) == null) {
            throw exception(YARN_RECIPE_BOM_NOT_EXISTS);
        }
    }

    private List<YarnRecipeBomItemDO> validateAndBuildItems(Long recipeBomId, List<YarnRecipeBomItemSaveReqVO> reqItems) {
        if (CollUtil.isEmpty(reqItems)) {
            return Collections.emptyList();
        }
        return reqItems.stream().map(itemReq -> {
            YarnSpecDO spec = yarnSpecMapper.selectById(itemReq.getYarnSpecId());
            if (spec == null) {
                throw exception(YARN_SPEC_NOT_EXISTS);
            }
            YarnRecipeBomItemDO itemDO = BeanUtils.toBean(itemReq, YarnRecipeBomItemDO.class);
            itemDO.setId(null);
            itemDO.setRecipeBomId(recipeBomId);
            // 若未填工艺或参考价，取规格默认值兜底
            if (!StringUtils.hasText(itemDO.getSpinMethod())) {
                itemDO.setSpinMethod(spec.getSpinMethod());
            }
            if (itemDO.getRefPrice() == null) {
                itemDO.setRefPrice(spec.getRefPrice());
            }
            return itemDO;
        }).collect(Collectors.toList());
    }

    private void saveItems(Long recipeBomId, List<YarnRecipeBomItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (YarnRecipeBomItemDO item : items) {
            item.setRecipeBomId(recipeBomId);
            yarnRecipeBomItemMapper.insert(item);
        }
    }

    /**
     * 纱线配比 BOM 明细差异保存：将提交的明细行按 id 分流为 update / insert / 逻辑删除三组。
     *
     * <ul>
     *   <li>提交行携带 id 且 id 属于本 BOM → updateById（保留原有行身份与审计信息）</li>
     *   <li>提交行 id 为 null → insert（新增行）</li>
     *   <li>本 BOM 库内存量已有 id 未出现在提交行中 → 逻辑删除（deleteByIds）</li>
     *   <li>提交行携带 id 不属于本 BOM → 报错拦截（防御跨父非法篡改）</li>
     *   <li>提交行中出现重复 id → 报错拦截</li>
     * </ul>
     *
     * @param recipeBomId 配比 BOM ID
     * @param reqItems    提交的明细列表（可为空）
     */
    private void diffSaveItems(Long recipeBomId, List<YarnRecipeBomItemSaveReqVO> reqItems) {
        // 1. 查询库内存量明细
        List<YarnRecipeBomItemDO> dbItems = yarnRecipeBomItemMapper.selectListByRecipeBomId(recipeBomId);
        Map<Long, YarnRecipeBomItemDO> dbItemMap = dbItems.stream()
                .collect(Collectors.toMap(YarnRecipeBomItemDO::getId, Function.identity()));
        Set<Long> remainingDbItemIds = new HashSet<>(dbItemMap.keySet());

        List<YarnRecipeBomItemDO> toInsert = new ArrayList<>();
        List<YarnRecipeBomItemDO> toUpdate = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        // 2. 遍历提交明细，校验规格与合法性并分流
        if (CollUtil.isNotEmpty(reqItems)) {
            for (YarnRecipeBomItemSaveReqVO reqItem : reqItems) {
                YarnSpecDO spec = yarnSpecMapper.selectById(reqItem.getYarnSpecId());
                if (spec == null) {
                    throw exception(YARN_SPEC_NOT_EXISTS);
                }
                YarnRecipeBomItemDO itemDO = BeanUtils.toBean(reqItem, YarnRecipeBomItemDO.class);
                itemDO.setRecipeBomId(recipeBomId);
                // 若未填工艺或参考价，取规格默认值兜底
                if (!StringUtils.hasText(itemDO.getSpinMethod())) {
                    itemDO.setSpinMethod(spec.getSpinMethod());
                }
                if (itemDO.getRefPrice() == null) {
                    itemDO.setRefPrice(spec.getRefPrice());
                }

                if (reqItem.getId() != null) {
                    Long itemId = reqItem.getId();
                    if (!seenIds.add(itemId)) {
                        throw invalidParamException("配比明细行 ID【{}】重复提交", itemId);
                    }
                    if (!remainingDbItemIds.remove(itemId)) {
                        throw invalidParamException("配比明细行 ID【{}】不属于当前配比BOM", itemId);
                    }
                    itemDO.setId(itemId);
                    toUpdate.add(itemDO);
                } else {
                    itemDO.setId(null);
                    toInsert.add(itemDO);
                }
            }
        }

        // 3. 执行删除（存量中未被本次提交命中的 ID）
        if (!remainingDbItemIds.isEmpty()) {
            yarnRecipeBomItemMapper.deleteByIds(remainingDbItemIds);
        }

        // 4. 执行更新
        for (YarnRecipeBomItemDO updateItem : toUpdate) {
            yarnRecipeBomItemMapper.updateById(updateItem);
        }

        // 5. 执行插入
        for (YarnRecipeBomItemDO insertItem : toInsert) {
            yarnRecipeBomItemMapper.insert(insertItem);
        }
    }

    private YarnRecipeBomRespVO buildRespVOWithItems(YarnRecipeBomDO bom) {
        YarnRecipeBomRespVO respVO = BeanUtils.toBean(bom, YarnRecipeBomRespVO.class);
        List<YarnRecipeBomItemDO> itemDOList = yarnRecipeBomItemMapper.selectListByRecipeBomId(bom.getId());
        if (CollUtil.isEmpty(itemDOList)) {
            respVO.setItems(Collections.emptyList());
            respVO.setItemCount(0);
            return respVO;
        }

        // 收集材质ID与规格ID批量回填名称
        Set<Long> typeIds = itemDOList.stream().map(YarnRecipeBomItemDO::getYarnTypeId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> specIds = itemDOList.stream().map(YarnRecipeBomItemDO::getYarnSpecId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> typeNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(typeIds)) {
            List<YarnTypeDO> types = yarnTypeMapper.selectByIds(typeIds);
            if (CollUtil.isNotEmpty(types)) {
                typeNameMap = types.stream().collect(Collectors.toMap(YarnTypeDO::getId, YarnTypeDO::getName));
            }
        }

        Map<Long, String> specNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(specIds)) {
            List<YarnSpecDO> specs = yarnSpecMapper.selectByIds(specIds);
            if (CollUtil.isNotEmpty(specs)) {
                specNameMap = specs.stream().collect(Collectors.toMap(YarnSpecDO::getId, YarnSpecDO::getSpecName));
            }
        }

        Map<Long, String> finalTypeNameMap = typeNameMap;
        Map<Long, String> finalSpecNameMap = specNameMap;
        List<YarnRecipeBomItemRespVO> itemRespList = itemDOList.stream().map(itemDO -> {
            YarnRecipeBomItemRespVO itemResp = BeanUtils.toBean(itemDO, YarnRecipeBomItemRespVO.class);
            itemResp.setYarnTypeName(finalTypeNameMap.get(itemDO.getYarnTypeId()));
            itemResp.setYarnSpecName(finalSpecNameMap.get(itemDO.getYarnSpecId()));
            return itemResp;
        }).collect(Collectors.toList());

        respVO.setItems(itemRespList);
        respVO.setItemCount(itemRespList.size());
        return respVO;
    }

}
