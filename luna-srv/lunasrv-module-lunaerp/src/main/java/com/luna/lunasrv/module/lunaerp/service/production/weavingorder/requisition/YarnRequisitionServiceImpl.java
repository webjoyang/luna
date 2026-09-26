package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item.YarnRequisitionItemService;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - 领纱单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnRequisitionServiceImpl implements YarnRequisitionService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final int MAX_GENERATE_RETRY = 5;

    @Resource
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Resource
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Resource
    private YarnRequisitionItemService yarnRequisitionItemService;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private YarnRecipeBomService yarnRecipeBomService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createYarnRequisition(YarnRequisitionSaveReqVO createReqVO) {
        // 1. 校验织造单存在性与状态
        WeavingOrderDO weavingOrder = validateWeavingOrderForRequisition(createReqVO.getWeavingOrderId());

        // 2. 编号生成或查重
        String code;
        if (StringUtils.hasText(createReqVO.getCode())) {
            code = createReqVO.getCode().trim();
            validateCodeUnique(code, null);
        } else {
            code = generateCodeWithRetry(weavingOrder.getCode());
        }

        // 3. 构建 DO 并持久化
        YarnRequisitionDO requisition = BeanUtils.toBean(createReqVO, YarnRequisitionDO.class);
        requisition.setCode(code);
        requisition.setStatus(STATUS_DRAFT);
        yarnRequisitionMapper.insert(requisition);
        return requisition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnRequisition(YarnRequisitionSaveReqVO updateReqVO) {
        // 1. 存在性校验
        YarnRequisitionDO exist = validateRequisitionExists(updateReqVO.getId());

        // 2. 状态守卫：仅 DRAFT 态允许更新主表
        if (!STATUS_DRAFT.equals(exist.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单状态不允许该操作，仅草稿（DRAFT）状态允许修改");
        }

        // 3. 编号修改查重
        if (StringUtils.hasText(updateReqVO.getCode())) {
            String newCode = updateReqVO.getCode().trim();
            validateCodeUnique(newCode, exist.getId());
            exist.setCode(newCode);
        }

        // 4. 更新备注
        exist.setRemark(updateReqVO.getRemark());
        yarnRequisitionMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnRequisition(Long id) {
        // 1. 存在性校验
        YarnRequisitionDO exist = validateRequisitionExists(id);

        // 2. 状态守卫：仅 DRAFT 态允许删除
        if (!STATUS_DRAFT.equals(exist.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单状态不允许该操作，仅草稿（DRAFT）状态允许删除");
        }

        // 3. 级联逻辑删除明细
        yarnRequisitionItemMapper.deleteByRequisitionId(id);

        // 4. 逻辑删除主表
        yarnRequisitionMapper.deleteById(id);
    }

    @Override
    public YarnRequisitionRespVO getYarnRequisition(Long id) {
        YarnRequisitionDO requisition = validateRequisitionExists(id);
        YarnRequisitionRespVO respVO = BeanUtils.toBean(requisition, YarnRequisitionRespVO.class);

        // 补充织造单快照信息
        WeavingOrderDO weavingOrder = weavingOrderMapper.selectById(requisition.getWeavingOrderId());
        if (weavingOrder != null) {
            respVO.setWeavingOrderCode(weavingOrder.getCode());
            respVO.setProductId(weavingOrder.getProductId());
            respVO.setProductCode(weavingOrder.getProductCode());
            respVO.setProductName(weavingOrder.getProductName());
            respVO.setPlannedQty(weavingOrder.getPlannedQty());
            respVO.setPlannedWeight(weavingOrder.getPlannedWeight());
        }

        // 补充明细列表与汇总统计
        List<YarnRequisitionItemRespVO> items = yarnRequisitionItemService.getItemListByRequisitionId(id);
        respVO.setItems(items);
        respVO.setItemCount(items.size());

        BigDecimal totalRequired = BigDecimal.ZERO;
        BigDecimal totalDispatched = BigDecimal.ZERO;
        for (YarnRequisitionItemRespVO item : items) {
            if (item.getRequiredQty() != null) {
                totalRequired = totalRequired.add(item.getRequiredQty());
            }
            if (item.getDispatchedQty() != null) {
                totalDispatched = totalDispatched.add(item.getDispatchedQty());
            }
        }
        respVO.setTotalRequiredQty(totalRequired);
        respVO.setTotalDispatchedQty(totalDispatched);

        return respVO;
    }

    @Override
    public PageResult<YarnRequisitionRespVO> getYarnRequisitionPage(YarnRequisitionPageReqVO pageReqVO) {
        PageResult<YarnRequisitionDO> pageResult = yarnRequisitionMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询关联织造单
        Set<Long> weavingOrderIds = pageResult.getList().stream()
                .map(YarnRequisitionDO::getWeavingOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeavingOrderDO> orderMap = weavingOrderIds.isEmpty() ? Collections.emptyMap() :
                weavingOrderMapper.selectBatchIds(weavingOrderIds).stream().collect(Collectors.toMap(WeavingOrderDO::getId, o -> o));

        // 批量查询明细做统计
        Set<Long> reqIds = pageResult.getList().stream().map(YarnRequisitionDO::getId).collect(Collectors.toSet());
        List<YarnRequisitionItemDO> allItems = yarnRequisitionItemMapper.selectListByRequisitionIds(reqIds);
        Map<Long, List<YarnRequisitionItemDO>> itemsGrouped = allItems.stream()
                .collect(Collectors.groupingBy(YarnRequisitionItemDO::getRequisitionId));

        List<YarnRequisitionRespVO> voList = pageResult.getList().stream().map(item -> {
            YarnRequisitionRespVO vo = BeanUtils.toBean(item, YarnRequisitionRespVO.class);
            if (item.getWeavingOrderId() != null && orderMap.containsKey(item.getWeavingOrderId())) {
                WeavingOrderDO order = orderMap.get(item.getWeavingOrderId());
                vo.setWeavingOrderCode(order.getCode());
                vo.setProductId(order.getProductId());
                vo.setProductCode(order.getProductCode());
                vo.setProductName(order.getProductName());
                vo.setPlannedQty(order.getPlannedQty());
                vo.setPlannedWeight(order.getPlannedWeight());
            }

            List<YarnRequisitionItemDO> items = itemsGrouped.getOrDefault(item.getId(), Collections.emptyList());
            vo.setItemCount(items.size());
            BigDecimal reqTotal = BigDecimal.ZERO;
            BigDecimal dispTotal = BigDecimal.ZERO;
            for (YarnRequisitionItemDO it : items) {
                if (it.getRequiredQty() != null) {
                    reqTotal = reqTotal.add(it.getRequiredQty());
                }
                if (it.getDispatchedQty() != null) {
                    dispTotal = dispTotal.add(it.getDispatchedQty());
                }
            }
            vo.setTotalRequiredQty(reqTotal);
            vo.setTotalDispatchedQty(dispTotal);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public String getNextYarnRequisitionCode(Long weavingOrderId) {
        WeavingOrderDO weavingOrder = validateWeavingOrderForRequisition(weavingOrderId);
        return generateCode(weavingOrder.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitYarnRequisition(Long id) {
        // 1. 存在性校验
        YarnRequisitionDO requisition = validateRequisitionExists(id);

        // 2. 状态守卫：仅 DRAFT 态允许提交
        if (!STATUS_DRAFT.equals(requisition.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单状态不允许该操作，仅草稿（DRAFT）状态允许提交");
        }

        // 3. 校验关联织造单未取消
        WeavingOrderDO weavingOrder = weavingOrderMapper.selectById(requisition.getWeavingOrderId());
        if (weavingOrder == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        if ("CANCELLED".equals(weavingOrder.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "织造单已取消，不允许提交领纱单");
        }

        // 4. 明细非空校验与用量校验
        List<YarnRequisitionItemDO> items = yarnRequisitionItemMapper.selectListByRequisitionId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "提交失败：领纱单没有明细，无法提交");
        }
        for (YarnRequisitionItemDO item : items) {
            if (item.getRequiredQty() == null || item.getRequiredQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(YARN_REQUISITION_ITEM_QTY_INVALID);
            }
        }

        // 5. 流转到待发纱 ACTIVE 态
        requisition.setStatus(STATUS_ACTIVE);
        yarnRequisitionMapper.updateById(requisition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeYarnRequisition(Long id) {
        YarnRequisitionDO requisition = validateRequisitionExists(id);
        if (STATUS_CLOSED.equals(requisition.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单已关闭，无需重复操作");
        }
        requisition.setStatus(STATUS_CLOSED);
        yarnRequisitionMapper.updateById(requisition);
    }

    @Override
    public YarnRequisitionCalculateRespVO calculate(YarnRequisitionCalculateReqVO calculateReqVO) {
        if (calculateReqVO == null || calculateReqVO.getWeavingOrderId() == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        WeavingOrderDO weavingOrder = weavingOrderMapper.selectById(calculateReqVO.getWeavingOrderId());
        if (weavingOrder == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }

        YarnRequisitionCalculateRespVO resp = new YarnRequisitionCalculateRespVO();
        resp.setWeavingOrderId(weavingOrder.getId());
        resp.setWeavingOrderCode(weavingOrder.getCode());
        resp.setProductId(weavingOrder.getProductId());
        resp.setProductCode(weavingOrder.getProductCode());
        resp.setProductName(weavingOrder.getProductName());
        resp.setPlannedQty(weavingOrder.getPlannedQty());
        resp.setPlannedWeight(weavingOrder.getPlannedWeight());
        resp.setItems(new ArrayList<>());

        Integer plannedQty = weavingOrder.getPlannedQty();
        BigDecimal plannedWeight = weavingOrder.getPlannedWeight();

        // 1. 计划匹数与重量检查（缺重量或计划匹数返回提示，结果可手工修改）
        if (plannedQty == null || plannedQty <= 0 || plannedWeight == null || plannedWeight.compareTo(BigDecimal.ZERO) <= 0) {
            resp.setTip("织造单缺少计划重量或计划匹数，无法自动测算，请手工填写用纱需求");
            return resp;
        }

        BigDecimal piecePlannedWeight = plannedWeight.divide(BigDecimal.valueOf(plannedQty), 4, RoundingMode.HALF_UP);
        resp.setPiecePlannedWeight(piecePlannedWeight);

        // 2. 配比 BOM 检查
        if (weavingOrder.getProductId() == null) {
            resp.setTip("织造单未关联产品档案，无法获取配比BOM，请手工填写用纱需求");
            return resp;
        }
        YarnRecipeBomRespVO bom = yarnRecipeBomService.getYarnRecipeBomByProductId(weavingOrder.getProductId());
        if (bom == null || CollUtil.isEmpty(bom.getItems())) {
            resp.setTip("该产品尚未配置纱线配比BOM，无法自动测算，请手工填写用纱需求");
            return resp;
        }

        // 3. 按 yarnSpecId 合并该产品配比 BOM
        // 某纱线规格需求 KG = 计划匹数 × 单匹计划重量 × ratio / 100
        Map<Long, BigDecimal> ratioMap = new LinkedHashMap<>();
        Map<Long, Long> typeIdMap = new LinkedHashMap<>();
        Map<Long, String> specNameMap = new LinkedHashMap<>();
        Map<Long, String> typeNameMap = new LinkedHashMap<>();

        for (YarnRecipeBomItemRespVO item : bom.getItems()) {
            Long specId = item.getYarnSpecId();
            BigDecimal ratio = item.getRatio() != null ? item.getRatio() : BigDecimal.ZERO;
            ratioMap.merge(specId, ratio, BigDecimal::add);
            typeIdMap.put(specId, item.getYarnTypeId());
            if (StringUtils.hasText(item.getYarnSpecName())) {
                specNameMap.put(specId, item.getYarnSpecName());
            }
            if (StringUtils.hasText(item.getYarnTypeName())) {
                typeNameMap.put(specId, item.getYarnTypeName());
            }
        }

        List<YarnRequisitionCalculateItemRespVO> calcItems = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : ratioMap.entrySet()) {
            Long specId = entry.getKey();
            BigDecimal totalRatio = entry.getValue();
            // 需求KG = plannedQty × piecePlannedWeight × totalRatio / 100
            BigDecimal requiredQty = BigDecimal.valueOf(plannedQty)
                    .multiply(piecePlannedWeight)
                    .multiply(totalRatio)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            YarnRequisitionCalculateItemRespVO itemVO = new YarnRequisitionCalculateItemRespVO();
            itemVO.setYarnSpecId(specId);
            itemVO.setYarnSpecName(specNameMap.get(specId));
            itemVO.setYarnTypeId(typeIdMap.get(specId));
            itemVO.setYarnTypeName(typeNameMap.get(specId));
            itemVO.setRatio(totalRatio);
            itemVO.setRequiredQty(requiredQty);
            itemVO.setUnit("KG");
            itemVO.setRemark("BOM 自动测算");
            calcItems.add(itemVO);
        }

        resp.setItems(calcItems);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWeavingOrderDeleted(Long weavingOrderId) {
        if (weavingOrderId == null) {
            return;
        }
        List<YarnRequisitionDO> requisitions = yarnRequisitionMapper.selectListByWeavingOrderId(weavingOrderId);
        if (CollUtil.isNotEmpty(requisitions)) {
            Set<Long> reqIds = requisitions.stream().map(YarnRequisitionDO::getId).collect(Collectors.toSet());
            yarnRequisitionItemMapper.deleteByRequisitionIds(reqIds);
            yarnRequisitionMapper.deleteByWeavingOrderId(weavingOrderId);
        }
    }

    // ==================== 内部私有方法 ====================

    private YarnRequisitionDO validateRequisitionExists(Long id) {
        if (id == null) {
            throw exception(YARN_REQUISITION_NOT_EXISTS);
        }
        YarnRequisitionDO requisition = yarnRequisitionMapper.selectById(id);
        if (requisition == null) {
            throw exception(YARN_REQUISITION_NOT_EXISTS);
        }
        return requisition;
    }

    private WeavingOrderDO validateWeavingOrderForRequisition(Long weavingOrderId) {
        if (weavingOrderId == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        WeavingOrderDO order = weavingOrderMapper.selectById(weavingOrderId);
        if (order == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "织造单已取消，不允许创建领纱单");
        }
        return order;
    }

    private void validateCodeUnique(String code, Long excludeId) {
        YarnRequisitionDO duplicate = yarnRequisitionMapper.selectByCodeIncludingDeleted(code);
        if (duplicate != null && (excludeId == null || !duplicate.getId().equals(excludeId))) {
            throw exception(YARN_REQUISITION_CODE_DUPLICATE, code);
        }
    }

    private String buildCodePrefix(String weavingOrderCode) {
        String cleanCode = weavingOrderCode == null ? "" : weavingOrderCode.replaceAll("[^a-zA-Z0-9]", "");
        return "YS-" + cleanCode + "-";
    }

    private int getMaxSequence(String prefix) {
        List<YarnRequisitionDO> candidates = yarnRequisitionMapper.selectListByCodePrefixIncludingDeleted(prefix);
        int maxSeq = 0;
        if (CollUtil.isNotEmpty(candidates)) {
            for (YarnRequisitionDO candidate : candidates) {
                String code = candidate.getCode();
                if (code != null && code.startsWith(prefix)) {
                    String seqStr = code.substring(prefix.length());
                    try {
                        int seq = Integer.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return maxSeq;
    }

    private String generateCode(String weavingOrderCode) {
        String prefix = buildCodePrefix(weavingOrderCode);
        int nextSeq = getMaxSequence(prefix) + 1;
        return String.format("%s%02d", prefix, nextSeq);
    }

    private String generateCodeWithRetry(String weavingOrderCode) {
        String prefix = buildCodePrefix(weavingOrderCode);
        int maxSeq = getMaxSequence(prefix);
        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            int seq = maxSeq + i;
            String candidate = String.format("%s%02d", prefix, seq);
            if (yarnRequisitionMapper.selectByCodeIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(YARN_REQUISITION_CODE_DUPLICATE, "生成领纱单号并发冲突，请稍后重试");
    }

}
