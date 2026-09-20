package com.luna.lunasrv.module.lunaerp.service.production.yarnorder;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.service.production.stockledger.StockLedgerService;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-纱线采购单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnOrderServiceImpl implements YarnOrderService {

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String ORDER_NO_PREFIX = "YP";
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 5;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private YarnOrderMapper yarnOrderMapper;

    @Resource
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private YarnRecipeBomService yarnRecipeBomService;

    @Resource
    private StockLedgerService stockLedgerService;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createYarnOrder(YarnOrderSaveReqVO createReqVO) {
        // 1. 编号生成与查重（含软删行，按天取号）
        String orderNo = createReqVO.getOrderNo();
        if (StringUtils.hasText(orderNo)) {
            YarnOrderDO duplicate = yarnOrderMapper.selectByOrderNoIncludingDeleted(orderNo.trim());
            if (duplicate != null) {
                throw exception(YARN_ORDER_NO_DUPLICATE, orderNo.trim());
            }
            orderNo = orderNo.trim();
        } else {
            orderNo = generateOrderNoWithRetry(createReqVO.getOrderDate());
        }

        // 2. 关联引用校验
        validateSourceInstructions(createReqVO.getSourceInstructions());
        validateItemsReferences(createReqVO.getItems());

        // 3. 组装主表 DO
        YarnOrderDO order = BeanUtils.toBean(createReqVO, YarnOrderDO.class);
        order.setOrderNo(orderNo);
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        if (order.getLossRate() == null) {
            order.setLossRate(BigDecimal.ZERO);
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus(DEFAULT_STATUS);
        }
        yarnOrderMapper.insert(order);

        // 4. 保存明细（服务端权威重算金额）
        saveOrderItems(order.getId(), createReqVO.getItems(), order.getLossRate());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnOrder(YarnOrderSaveReqVO updateReqVO) {
        // 1. 校验存在
        YarnOrderDO existing = yarnOrderMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw exception(YARN_ORDER_NOT_EXISTS);
        }

        // 2. 状态校验：仅草稿态可更新
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            throw exception(YARN_ORDER_STATUS_NOT_ALLOWED);
        }

        // 3. 编号查重
        String orderNo = updateReqVO.getOrderNo();
        if (StringUtils.hasText(orderNo)) {
            YarnOrderDO duplicate = yarnOrderMapper.selectByOrderNoIncludingDeleted(orderNo.trim());
            if (duplicate != null && !duplicate.getId().equals(existing.getId())) {
                throw exception(YARN_ORDER_NO_DUPLICATE, orderNo.trim());
            }
            orderNo = orderNo.trim();
        } else {
            orderNo = existing.getOrderNo();
        }

        // 4. 关联引用校验
        validateSourceInstructions(updateReqVO.getSourceInstructions());
        validateItemsReferences(updateReqVO.getItems());

        // 5. 更新主表
        YarnOrderDO order = BeanUtils.toBean(updateReqVO, YarnOrderDO.class);
        order.setOrderNo(orderNo);
        yarnOrderMapper.updateById(order);

        // 6. 明细整体替换
        yarnOrderItemMapper.deleteByOrderId(order.getId());
        saveOrderItems(order.getId(), updateReqVO.getItems(), order.getLossRate() != null ? order.getLossRate() : existing.getLossRate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnOrder(Long id) {
        YarnOrderDO existing = yarnOrderMapper.selectById(id);
        if (existing == null) {
            throw exception(YARN_ORDER_NOT_EXISTS);
        }
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            throw exception(YARN_ORDER_STATUS_NOT_ALLOWED);
        }
        yarnOrderMapper.deleteById(id);
        yarnOrderItemMapper.deleteByOrderId(id);
    }

    @Override
    public YarnOrderRespVO getYarnOrder(Long id) {
        YarnOrderDO order = yarnOrderMapper.selectById(id);
        if (order == null) {
            throw exception(YARN_ORDER_NOT_EXISTS);
        }
        YarnOrderRespVO resp = BeanUtils.toBean(order, YarnOrderRespVO.class);

        // 明细列表及名称补齐
        List<YarnOrderItemDO> itemDOs = yarnOrderItemMapper.selectListByOrderId(id);
        List<YarnOrderItemRespVO> itemVOs = buildItemRespVOs(itemDOs);
        resp.setItems(itemVOs);
        resp.setItemCount(itemVOs.size());

        BigDecimal totalAmount = itemVOs.stream()
                .map(YarnOrderItemRespVO::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

        // 胚布单展示快照
        enrichSourceGreigeOrders(resp);
        return resp;
    }

    @Override
    public PageResult<YarnOrderRespVO> getYarnOrderPage(YarnOrderPageReqVO pageReqVO) {
        LambdaQueryWrapperX<YarnOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(YarnOrderDO::getStatus, pageReqVO.getStatus());

        // 单据日期
        if (pageReqVO.getOrderDate() != null && pageReqVO.getOrderDate().length == 2) {
            wrapper.between(YarnOrderDO::getOrderDate, pageReqVO.getOrderDate()[0], pageReqVO.getOrderDate()[1]);
        } else {
            wrapper.geIfPresent(YarnOrderDO::getOrderDate, pageReqVO.getOrderDateStart());
            wrapper.leIfPresent(YarnOrderDO::getOrderDate, pageReqVO.getOrderDateEnd());
        }

        // 交货日期
        if (pageReqVO.getDeliveryDate() != null && pageReqVO.getDeliveryDate().length == 2) {
            wrapper.between(YarnOrderDO::getDeliveryDate, pageReqVO.getDeliveryDate()[0], pageReqVO.getDeliveryDate()[1]);
        } else {
            wrapper.geIfPresent(YarnOrderDO::getDeliveryDate, pageReqVO.getDeliveryDateStart());
            wrapper.leIfPresent(YarnOrderDO::getDeliveryDate, pageReqVO.getDeliveryDateEnd());
        }

        // 关键词：匹配 orderNo 或反查关联胚布单号
        String keyword = pageReqVO.getKeyword() != null ? pageReqVO.getKeyword().trim() : "";
        if (!keyword.isEmpty()) {
            List<GreigeOrderDO> matchingGreige = greigeOrderMapper.selectList(new LambdaQueryWrapperX<GreigeOrderDO>()
                    .like(GreigeOrderDO::getOrderNo, keyword));
            wrapper.and(w -> {
                w.apply("LOWER(order_no) LIKE LOWER({0})", "%" + keyword + "%");
                if (CollUtil.isNotEmpty(matchingGreige)) {
                    for (GreigeOrderDO g : matchingGreige) {
                        w.or().apply("source_instructions LIKE {0}", "%" + g.getId() + "%");
                    }
                }
            });
        }

        wrapper.orderByDesc(YarnOrderDO::getOrderDate).orderByDesc(YarnOrderDO::getId);
        PageResult<YarnOrderDO> page = yarnOrderMapper.selectPage(pageReqVO, wrapper);
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }

        // 转换为 RespVO 并聚合统计
        List<Long> orderIds = page.getList().stream().map(YarnOrderDO::getId).collect(Collectors.toList());
        List<YarnOrderItemDO> allItems = yarnOrderItemMapper.selectList(new LambdaQueryWrapperX<YarnOrderItemDO>()
                .in(YarnOrderItemDO::getOrderId, orderIds));
        Map<Long, List<YarnOrderItemDO>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(YarnOrderItemDO::getOrderId));

        List<YarnOrderRespVO> respList = page.getList().stream().map(order -> {
            YarnOrderRespVO resp = BeanUtils.toBean(order, YarnOrderRespVO.class);
            List<YarnOrderItemDO> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());
            resp.setItemCount(items.size());
            BigDecimal sum = items.stream()
                    .map(YarnOrderItemDO::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resp.setTotalAmount(sum.setScale(2, RoundingMode.HALF_UP));
            enrichSourceGreigeOrders(resp);
            return resp;
        }).collect(Collectors.toList());

        return new PageResult<>(respList, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effectYarnOrder(Long id) {
        YarnOrderDO existing = yarnOrderMapper.selectById(id);
        if (existing == null) {
            throw exception(YARN_ORDER_NOT_EXISTS);
        }
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            throw exception(YARN_ORDER_STATUS_NOT_ALLOWED);
        }
        YarnOrderDO update = new YarnOrderDO();
        update.setId(id);
        update.setStatus(STATUS_ACTIVE);
        yarnOrderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeYarnOrder(Long id) {
        YarnOrderDO existing = yarnOrderMapper.selectById(id);
        if (existing == null) {
            throw exception(YARN_ORDER_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(existing.getStatus())) {
            throw exception(YARN_ORDER_STATUS_NOT_ALLOWED);
        }
        YarnOrderDO update = new YarnOrderDO();
        update.setId(id);
        update.setStatus(STATUS_COMPLETED);
        yarnOrderMapper.updateById(update);
        // Issue 06 将在此处添加自动写入库存台账逻辑
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelYarnOrder(Long id) {
        YarnOrderDO existing = yarnOrderMapper.selectById(id);
        if (existing == null) {
            throw exception(YARN_ORDER_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(existing.getStatus())) {
            throw exception(YARN_ORDER_STATUS_NOT_ALLOWED);
        }
        YarnOrderDO update = new YarnOrderDO();
        update.setId(id);
        update.setStatus(STATUS_CANCELLED);
        yarnOrderMapper.updateById(update);
    }

    @Override
    public YarnOrderCalculateRespVO calculateYarnOrder(YarnOrderCalculateReqVO calculateReqVO) {
        List<Long> greigeOrderIds = calculateReqVO.getGreigeOrderIds();
        if (CollUtil.isEmpty(greigeOrderIds)) {
            throw new IllegalArgumentException("请至少选择一张胚布指示单");
        }

        // 1. 校验胚布单存在、有产品ID、有配比BOM
        List<GreigeOrderDO> greigeOrders = new ArrayList<>();
        Map<Long, YarnRecipeBomRespVO> bomsByProductId = new HashMap<>();

        for (Long greigeOrderId : greigeOrderIds) {
            GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(greigeOrderId);
            if (greigeOrder == null) {
                throw exception(GREIGE_ORDER_NOT_EXISTS);
            }
            if (greigeOrder.getProductId() == null) {
                throw exception(YARN_ORDER_REFERENCE_NOT_FOUND, "产品档案");
            }
            YarnRecipeBomRespVO bom = yarnRecipeBomService.getYarnRecipeBomByProductId(greigeOrder.getProductId());
            if (bom == null || CollUtil.isEmpty(bom.getItems())) {
                throw exception(YARN_RECIPE_BOM_NOT_EXISTS);
            }
            greigeOrders.add(greigeOrder);
            bomsByProductId.put(greigeOrder.getProductId(), bom);
        }

        // 2. 组装选中胚布单快照
        List<YarnOrderCalculateGreigeOrderRespVO> sourceGreigeOrders = greigeOrders.stream().map(g -> {
            YarnOrderCalculateGreigeOrderRespVO vo = new YarnOrderCalculateGreigeOrderRespVO();
            vo.setId(g.getId());
            vo.setOrderNo(g.getOrderNo());
            vo.setProductId(g.getProductId());
            vo.setProductCode(g.getProductCode());
            vo.setProductName(g.getProductName());
            vo.setFinishedQty(g.getFinishedQty());
            vo.setSuggestedLoss(g.getSuggestedLoss());
            return vo;
        }).collect(Collectors.toList());

        // 3. 按 yarn_spec_id 合并多单多 BOM 明细需求 baseQty
        Map<Long, BigDecimal> baseQtyMap = new LinkedHashMap<>();
        Map<Long, Long> yarnTypeIdMap = new HashMap<>();
        Map<Long, BigDecimal> ratioMap = new HashMap<>();
        Map<Long, Set<Long>> sourceGreigeOrderIdsMap = new LinkedHashMap<>();
        Map<Long, Set<String>> sourceGreigeOrderNosMap = new LinkedHashMap<>();

        for (GreigeOrderDO greigeOrder : greigeOrders) {
            BigDecimal finishedQty = greigeOrder.getFinishedQty() != null ? greigeOrder.getFinishedQty() : BigDecimal.ZERO;
            YarnRecipeBomRespVO bom = bomsByProductId.get(greigeOrder.getProductId());
            for (YarnRecipeBomItemRespVO bomItem : bom.getItems()) {
                Long specId = bomItem.getYarnSpecId();
                BigDecimal ratio = bomItem.getRatio() != null ? bomItem.getRatio() : BigDecimal.ZERO;
                BigDecimal itemBaseQty = finishedQty.multiply(ratio).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                baseQtyMap.merge(specId, itemBaseQty, BigDecimal::add);
                yarnTypeIdMap.put(specId, bomItem.getYarnTypeId());
                ratioMap.put(specId, ratio);
                sourceGreigeOrderIdsMap.computeIfAbsent(specId, k -> new LinkedHashSet<>()).add(greigeOrder.getId());
                sourceGreigeOrderNosMap.computeIfAbsent(specId, k -> new LinkedHashSet<>()).add(greigeOrder.getOrderNo());
            }
        }

        // 4. 批量查询在库量
        Map<Long, BigDecimal> balances = stockLedgerService.getBalances(baseQtyMap.keySet());
        if (balances == null) {
            balances = Collections.emptyMap();
        }

        // 5. 算 requiredQty = baseQty × (1 + lossRate/100)、purchaseQty = max(0, requiredQty - stockQty)、历史价回填
        BigDecimal lossRate = calculateReqVO.getLossRate() != null ? calculateReqVO.getLossRate() : BigDecimal.ZERO;
        BigDecimal lossMultiplier = BigDecimal.ONE.add(lossRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        List<YarnOrderCalculateItemRespVO> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : baseQtyMap.entrySet()) {
            Long specId = entry.getKey();
            BigDecimal baseQty = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal requiredQty = baseQty.multiply(lossMultiplier).setScale(2, RoundingMode.HALF_UP);
            BigDecimal stockQty = balances.getOrDefault(specId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal purchaseQty = requiredQty.subtract(stockQty).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            // 历史价：最近已下达单价 > 规格 refPrice 兜底
            BigDecimal recentUnitPrice = yarnOrderItemMapper.selectRecentUnitPriceBySpecId(specId);
            YarnSpecDO specDO = yarnSpecMapper.selectById(specId);
            BigDecimal refUnitPrice = recentUnitPrice != null
                    ? recentUnitPrice : (specDO != null ? specDO.getRefPrice() : null);

            Long typeId = yarnTypeIdMap.get(specId);
            if (typeId == null && specDO != null) {
                typeId = specDO.getYarnTypeId();
            }
            YarnTypeDO typeDO = typeId != null ? yarnTypeMapper.selectById(typeId) : null;

            YarnOrderCalculateItemRespVO itemResp = new YarnOrderCalculateItemRespVO();
            itemResp.setYarnTypeId(typeId);
            itemResp.setYarnTypeName(typeDO != null ? typeDO.getName() : null);
            itemResp.setYarnSpecId(specId);
            itemResp.setYarnSpecName(specDO != null ? specDO.getSpecName() : null);
            itemResp.setSupplierId(null);
            itemResp.setSupplierName(null);
            itemResp.setRatio(ratioMap.get(specId));
            itemResp.setBaseQty(baseQty);
            itemResp.setLossRate(lossRate);
            itemResp.setRequiredQty(requiredQty);
            itemResp.setStockQty(stockQty);
            itemResp.setPurchaseQty(purchaseQty);
            itemResp.setRefUnitPrice(refUnitPrice);
            itemResp.setSourceGreigeOrderIds(new ArrayList<>(sourceGreigeOrderIdsMap.get(specId)));
            itemResp.setSourceGreigeOrderNos(new ArrayList<>(sourceGreigeOrderNosMap.get(specId)));
            items.add(itemResp);
        }

        YarnOrderCalculateRespVO resp = new YarnOrderCalculateRespVO();
        resp.setItems(items);
        resp.setSourceGreigeOrders(sourceGreigeOrders);
        return resp;
    }

    @Override
    public String getNextYarnOrderNo() {
        return generateOrderNo(LocalDate.now());
    }

    // ==================== 私有方法 ====================

    private void validateSourceInstructions(String sourceInstructions) {
        if (!StringUtils.hasText(sourceInstructions)) {
            return;
        }
        List<Long> greigeOrderIds;
        try {
            greigeOrderIds = JsonUtils.parseArray(sourceInstructions, Long.class);
        } catch (Exception e) {
            log.warn("解析 sourceInstructions 失败: {}", sourceInstructions, e);
            return;
        }
        if (CollUtil.isNotEmpty(greigeOrderIds)) {
            for (Long greigeOrderId : greigeOrderIds) {
                if (greigeOrderMapper.selectById(greigeOrderId) == null) {
                    throw exception(YARN_ORDER_REFERENCE_NOT_FOUND, "胚布单");
                }
            }
        }
    }

    private void validateItemsReferences(List<YarnOrderItemSaveReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (YarnOrderItemSaveReqVO item : items) {
            if (yarnSpecMapper.selectById(item.getYarnSpecId()) == null) {
                throw exception(YARN_ORDER_REFERENCE_NOT_FOUND, "纱线规格");
            }
            if (item.getSupplierId() != null) {
                SupplierDO supplier = supplierMapper.selectById(item.getSupplierId());
                if (supplier == null) {
                    throw exception(YARN_ORDER_REFERENCE_NOT_FOUND, "供应商");
                }
                if (!StringUtils.hasText(item.getSupplierName())) {
                    item.setSupplierName(supplier.getCompanyName());
                }
            }
        }
    }

    private void saveOrderItems(Long orderId, List<YarnOrderItemSaveReqVO> itemVOs, BigDecimal mainLossRate) {
        if (CollUtil.isEmpty(itemVOs)) {
            return;
        }
        List<YarnOrderItemDO> items = new ArrayList<>();
        for (YarnOrderItemSaveReqVO vo : itemVOs) {
            YarnOrderItemDO itemDO = BeanUtils.toBean(vo, YarnOrderItemDO.class);
            itemDO.setOrderId(orderId);
            if (itemDO.getLossRate() == null) {
                itemDO.setLossRate(mainLossRate != null ? mainLossRate : BigDecimal.ZERO);
            }
            // 服务端权威重算金额 total_amount = unit_price × purchase_qty
            if (itemDO.getUnitPrice() != null && itemDO.getPurchaseQty() != null) {
                itemDO.setTotalAmount(itemDO.getUnitPrice().multiply(itemDO.getPurchaseQty()).setScale(2, RoundingMode.HALF_UP));
            } else {
                itemDO.setTotalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            items.add(itemDO);
        }
        yarnOrderItemMapper.insertBatch(items);
    }

    private List<YarnOrderItemRespVO> buildItemRespVOs(List<YarnOrderItemDO> itemDOs) {
        if (CollUtil.isEmpty(itemDOs)) {
            return Collections.emptyList();
        }
        // 收集规格及材质信息
        Set<Long> specIds = itemDOs.stream().map(YarnOrderItemDO::getYarnSpecId).collect(Collectors.toSet());
        Map<Long, YarnSpecDO> specMap = specIds.isEmpty() ? Collections.emptyMap() :
                yarnSpecMapper.selectBatchIds(specIds).stream().collect(Collectors.toMap(YarnSpecDO::getId, s -> s));

        Set<Long> typeIds = itemDOs.stream().map(YarnOrderItemDO::getYarnTypeId).collect(Collectors.toSet());
        Map<Long, YarnTypeDO> typeMap = typeIds.isEmpty() ? Collections.emptyMap() :
                yarnTypeMapper.selectBatchIds(typeIds).stream().collect(Collectors.toMap(YarnTypeDO::getId, t -> t));

        return itemDOs.stream().map(item -> {
            YarnOrderItemRespVO vo = BeanUtils.toBean(item, YarnOrderItemRespVO.class);
            YarnSpecDO spec = specMap.get(item.getYarnSpecId());
            if (spec != null) {
                vo.setYarnSpecName(spec.getSpecName());
            }
            YarnTypeDO type = typeMap.get(item.getYarnTypeId());
            if (type != null) {
                vo.setYarnTypeName(type.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private void enrichSourceGreigeOrders(YarnOrderRespVO resp) {
        if (!StringUtils.hasText(resp.getSourceInstructions())) {
            return;
        }
        List<Long> greigeOrderIds;
        try {
            greigeOrderIds = JsonUtils.parseArray(resp.getSourceInstructions(), Long.class);
        } catch (Exception e) {
            log.warn("解析 sourceInstructions 失败: {}", resp.getSourceInstructions(), e);
            return;
        }
        if (CollUtil.isNotEmpty(greigeOrderIds)) {
            List<GreigeOrderDO> greigeOrders = greigeOrderMapper.selectBatchIds(greigeOrderIds);
            if (CollUtil.isNotEmpty(greigeOrders)) {
                List<YarnOrderCalculateGreigeOrderRespVO> snapshots = greigeOrders.stream().map(g -> {
                    YarnOrderCalculateGreigeOrderRespVO vo = new YarnOrderCalculateGreigeOrderRespVO();
                    vo.setId(g.getId());
                    vo.setOrderNo(g.getOrderNo());
                    vo.setProductId(g.getProductId());
                    vo.setProductCode(g.getProductCode());
                    vo.setProductName(g.getProductName());
                    vo.setFinishedQty(g.getFinishedQty());
                    vo.setSuggestedLoss(g.getSuggestedLoss());
                    return vo;
                }).collect(Collectors.toList());
                resp.setSourceGreigeOrders(snapshots);

                String nos = greigeOrders.stream()
                        .map(GreigeOrderDO::getOrderNo)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(", "));
                resp.setSourceGreigeOrderNos(nos);
            }
        }
    }

    private String generateOrderNoWithRetry(LocalDate orderDate) {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateOrderNo(orderDate);
            if (yarnOrderMapper.selectByOrderNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(YARN_ORDER_NO_DUPLICATE, generateOrderNo(orderDate));
    }

    private String generateOrderNo(LocalDate orderDate) {
        LocalDate date = orderDate != null ? orderDate : LocalDate.now();
        String prefix = ORDER_NO_PREFIX + date.format(DATE_FORMATTER);
        List<YarnOrderDO> candidates = yarnOrderMapper.selectListByOrderNoPrefixIncludingDeleted(prefix);

        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (YarnOrderDO candidate : candidates) {
            String no = candidate.getOrderNo();
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

}
