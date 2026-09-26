package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - 领纱明细 Service 实现类（独立子资源）
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnRequisitionItemServiceImpl implements YarnRequisitionItemService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String DEFAULT_UNIT = "KG";

    @Resource
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Resource
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createYarnRequisitionItem(YarnRequisitionItemSaveReqVO createReqVO) {
        // 1. 校验父级领纱单存在且状态为草稿
        YarnRequisitionDO requisition = validateRequisitionDraft(createReqVO.getRequisitionId());

        // 2. 校验需求量 > 0
        validateRequiredQty(createReqVO.getRequiredQty());

        // 3. 校验纱线规格并补齐快照
        fillSpecAndTypeSnapshot(createReqVO);

        // 4. 构建 DO 并持久化
        YarnRequisitionItemDO itemDO = BeanUtils.toBean(createReqVO, YarnRequisitionItemDO.class);
        itemDO.setDispatchedQty(BigDecimal.ZERO);
        if (!StringUtils.hasText(itemDO.getUnit())) {
            itemDO.setUnit(DEFAULT_UNIT);
        }
        yarnRequisitionItemMapper.insert(itemDO);
        return itemDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnRequisitionItem(YarnRequisitionItemSaveReqVO updateReqVO) {
        // 1. 存在性校验
        YarnRequisitionItemDO exist = validateItemExists(updateReqVO.getId());

        // 2. 校验所属领纱单状态为草稿
        validateRequisitionDraft(exist.getRequisitionId());

        // 3. 校验需求量 > 0
        validateRequiredQty(updateReqVO.getRequiredQty());

        // 4. 校验纱线规格并补齐快照
        fillSpecAndTypeSnapshot(updateReqVO);

        // 5. 更新字段
        exist.setYarnTypeId(updateReqVO.getYarnTypeId());
        exist.setYarnSpecId(updateReqVO.getYarnSpecId());
        exist.setYarnSpecName(updateReqVO.getYarnSpecName());
        exist.setRequiredQty(updateReqVO.getRequiredQty());
        exist.setRemark(updateReqVO.getRemark());
        if (StringUtils.hasText(updateReqVO.getUnit())) {
            exist.setUnit(updateReqVO.getUnit());
        }

        yarnRequisitionItemMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnRequisitionItem(Long id) {
        // 1. 存在性校验
        YarnRequisitionItemDO exist = validateItemExists(id);

        // 2. 校验所属领纱单状态为草稿
        validateRequisitionDraft(exist.getRequisitionId());

        // 3. 逻辑删除
        yarnRequisitionItemMapper.deleteById(id);
    }

    @Override
    public YarnRequisitionItemRespVO getYarnRequisitionItem(Long id) {
        YarnRequisitionItemDO item = validateItemExists(id);
        YarnRequisitionItemRespVO respVO = BeanUtils.toBean(item, YarnRequisitionItemRespVO.class);
        if (item.getYarnTypeId() != null) {
            YarnTypeDO type = yarnTypeMapper.selectById(item.getYarnTypeId());
            if (type != null) {
                respVO.setYarnTypeName(type.getName());
            }
        }
        return respVO;
    }

    @Override
    public PageResult<YarnRequisitionItemRespVO> getYarnRequisitionItemPage(YarnRequisitionItemPageReqVO pageReqVO) {
        PageResult<YarnRequisitionItemDO> pageResult = yarnRequisitionItemMapper.selectPage(pageReqVO);
        return toRespVOPage(pageResult);
    }

    @Override
    public List<YarnRequisitionItemRespVO> getItemListByRequisitionId(Long requisitionId) {
        List<YarnRequisitionItemDO> list = yarnRequisitionItemMapper.selectListByRequisitionId(requisitionId);
        return toRespVOList(list);
    }

    // ==================== 内部校验与辅助方法 ====================

    private YarnRequisitionDO validateRequisitionDraft(Long requisitionId) {
        if (requisitionId == null) {
            throw exception(YARN_REQUISITION_NOT_EXISTS);
        }
        YarnRequisitionDO requisition = yarnRequisitionMapper.selectById(requisitionId);
        if (requisition == null) {
            throw exception(YARN_REQUISITION_NOT_EXISTS);
        }
        if (!STATUS_DRAFT.equals(requisition.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单状态不允许该操作，仅草稿（DRAFT）状态允许编辑明细");
        }
        return requisition;
    }

    private YarnRequisitionItemDO validateItemExists(Long id) {
        if (id == null) {
            throw exception(YARN_REQUISITION_ITEM_NOT_EXISTS);
        }
        YarnRequisitionItemDO item = yarnRequisitionItemMapper.selectById(id);
        if (item == null) {
            throw exception(YARN_REQUISITION_ITEM_NOT_EXISTS);
        }
        return item;
    }

    private void validateRequiredQty(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(YARN_REQUISITION_ITEM_QTY_INVALID);
        }
    }

    private void fillSpecAndTypeSnapshot(YarnRequisitionItemSaveReqVO vo) {
        YarnSpecDO spec = yarnSpecMapper.selectById(vo.getYarnSpecId());
        if (spec == null) {
            throw exception(YARN_SPEC_NOT_EXISTS);
        }
        if (!StringUtils.hasText(vo.getYarnSpecName())) {
            vo.setYarnSpecName(spec.getSpecName());
        }
        if (vo.getYarnTypeId() == null) {
            vo.setYarnTypeId(spec.getYarnTypeId());
        }
    }

    private PageResult<YarnRequisitionItemRespVO> toRespVOPage(PageResult<YarnRequisitionItemDO> pageResult) {
        List<YarnRequisitionItemRespVO> voList = toRespVOList(pageResult.getList());
        return new PageResult<>(voList, pageResult.getTotal());
    }

    private List<YarnRequisitionItemRespVO> toRespVOList(List<YarnRequisitionItemDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Set<Long> typeIds = list.stream()
                .map(YarnRequisitionItemDO::getYarnTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, YarnTypeDO> typeMap = typeIds.isEmpty() ? Collections.emptyMap() :
                yarnTypeMapper.selectBatchIds(typeIds).stream().collect(Collectors.toMap(YarnTypeDO::getId, t -> t));

        return list.stream().map(item -> {
            YarnRequisitionItemRespVO vo = BeanUtils.toBean(item, YarnRequisitionItemRespVO.class);
            if (item.getYarnTypeId() != null && typeMap.containsKey(item.getYarnTypeId())) {
                vo.setYarnTypeName(typeMap.get(item.getYarnTypeId()).getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

}
