package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import com.luna.lunasrv.module.lunaerp.service.stock.stockledger.StockLedgerService;
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
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String DEFAULT_UNIT = "KG";

    private static final String TXN_TYPE_YARN_OUT = "YARN_OUT";
    private static final String TXN_TYPE_YARN_OUT_REVERSAL = "YARN_OUT_REVERSAL";
    private static final String DOC_TYPE_YARN_REQUISITION_ITEM = "YARN_REQUISITION_ITEM";
    private static final String REVERSE_PREFIX = "REVERSE_LEDGER_";

    @Resource
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Resource
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Resource
    private StockLedgerService stockLedgerService;

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private WarehouseLocationMapper warehouseLocationMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchYarn(YarnRequisitionDispatchReqVO dispatchReqVO) {
        if (dispatchReqVO == null || dispatchReqVO.getItemId() == null) {
            throw exception(YARN_REQUISITION_ITEM_NOT_EXISTS);
        }

        // 1. 校验领纱明细存在
        YarnRequisitionItemDO item = validateItemExists(dispatchReqVO.getItemId());

        // 2. 校验领纱单存在与状态
        YarnRequisitionDO requisition = yarnRequisitionMapper.selectById(item.getRequisitionId());
        if (requisition == null) {
            throw exception(YARN_REQUISITION_NOT_EXISTS);
        }
        // 领纱单状态守卫：仅 ACTIVE / PARTIAL 态允许发纱 (312 错误码)
        if (!STATUS_ACTIVE.equals(requisition.getStatus()) && !STATUS_PARTIAL.equals(requisition.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单当前状态不允许发纱，仅待发纱（ACTIVE）或部分发纱（PARTIAL）状态允许发纱");
        }

        // 3. 校验仓库与库位
        if (dispatchReqVO.getWarehouseId() == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
        WarehouseDO warehouse = warehouseMapper.selectById(dispatchReqVO.getWarehouseId());
        if (warehouse == null || !"ACTIVE".equals(warehouse.getStatus())) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }

        WarehouseLocationDO location = null;
        if (dispatchReqVO.getLocationId() != null) {
            location = warehouseLocationMapper.selectById(dispatchReqVO.getLocationId());
            if (location == null || !"ACTIVE".equals(location.getStatus())) {
                throw exception(WAREHOUSE_LOCATION_NOT_EXISTS);
            }
            if (!warehouse.getId().equals(location.getWarehouseId())) {
                throw exception(STOCK_IN_LOCATION_NOT_IN_WAREHOUSE, location.getCode(), warehouse.getName());
            }
        }

        // 4. 发放数量校验
        BigDecimal dispatchQty = dispatchReqVO.getDispatchQty();
        if (dispatchQty == null || dispatchQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(YARN_REQUISITION_ITEM_QTY_INVALID);
        }

        // 5. 库存不足拦截（1-030-015-311，返回 required / available）
        BigDecimal availableBalance = stockLedgerService.getYarnSpecBalance(warehouse.getId(), item.getYarnSpecId());
        if (availableBalance == null || availableBalance.compareTo(dispatchQty) < 0) {
            BigDecimal av = availableBalance != null ? availableBalance : BigDecimal.ZERO;
            throw exception(YARN_REQUISITION_STOCK_NOT_ENOUGH, dispatchQty, av);
        }

        // 6. 写入库存流水 YARN_OUT（负数，物品=yarn_spec_id，关联领纱单/织造单）
        StockLedgerDO ledger = new StockLedgerDO();
        ledger.setItemType("YARN_SPEC");
        ledger.setYarnSpecId(item.getYarnSpecId());
        ledger.setYarnSpecName(item.getYarnSpecName());
        ledger.setWarehouseId(warehouse.getId());
        ledger.setWarehouseName(warehouse.getName());
        ledger.setLocationId(location != null ? location.getId() : null);
        ledger.setLocationCode(location != null ? location.getCode() : null);
        ledger.setBatchNo(item.getId().toString());
        ledger.setTxnType(TXN_TYPE_YARN_OUT);
        ledger.setQuantity(dispatchQty.negate()); // 负数
        ledger.setUnit(DEFAULT_UNIT);
        ledger.setRelatedDocType(DOC_TYPE_YARN_REQUISITION_ITEM);
        ledger.setRelatedDocId(item.getId());
        ledger.setRelatedDocNo(requisition.getCode());
        ledger.setRemark(dispatchReqVO.getRemark());
        stockLedgerMapper.insert(ledger);

        // 7. 回加明细 dispatchedQty
        BigDecimal currentDispatched = item.getDispatchedQty() != null ? item.getDispatchedQty() : BigDecimal.ZERO;
        item.setDispatchedQty(currentDispatched.add(dispatchQty));
        yarnRequisitionItemMapper.updateById(item);

        // 8. 状态推导：首次发→PARTIAL、各行 dispatchedQty≥requiredQty→DISPATCHED
        List<YarnRequisitionItemDO> allItems = yarnRequisitionItemMapper.selectListByRequisitionId(requisition.getId());
        boolean allDispatched = true;
        for (YarnRequisitionItemDO it : allItems) {
            BigDecimal reqQty = it.getRequiredQty() != null ? it.getRequiredQty() : BigDecimal.ZERO;
            BigDecimal dispQty = it.getDispatchedQty() != null ? it.getDispatchedQty() : BigDecimal.ZERO;
            if (dispQty.compareTo(reqQty) < 0) {
                allDispatched = false;
                break;
            }
        }

        String targetStatus = allDispatched ? STATUS_DISPATCHED : STATUS_PARTIAL;
        if (!targetStatus.equals(requisition.getStatus())) {
            requisition.setStatus(targetStatus);
            yarnRequisitionMapper.updateById(requisition);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseDispatch(YarnRequisitionDispatchReversalReqVO reversalReqVO) {
        if (reversalReqVO == null || reversalReqVO.getStockLedgerId() == null) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "撤销发纱记录ID不能为空");
        }

        // 1. 校验发纱流水记录存在
        StockLedgerDO orig = stockLedgerMapper.selectById(reversalReqVO.getStockLedgerId());
        if (orig == null) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "未找到对应的发纱流水记录");
        }

        // 2. 校验仅允许冲本系统登记的发纱记录
        if (!TXN_TYPE_YARN_OUT.equals(orig.getTxnType()) || !DOC_TYPE_YARN_REQUISITION_ITEM.equals(orig.getRelatedDocType())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "只能对本系统登记的领纱发纱记录进行撤销红冲");
        }

        // 3. 校验该记录是否已被撤销过（防重复撤销红冲）
        List<StockLedgerDO> existingReversals = stockLedgerMapper.selectList(new LambdaQueryWrapperX<StockLedgerDO>()
                .eq(StockLedgerDO::getRelatedDocType, DOC_TYPE_YARN_REQUISITION_ITEM)
                .eq(StockLedgerDO::getRelatedDocId, orig.getRelatedDocId())
                .eq(StockLedgerDO::getTxnType, TXN_TYPE_YARN_OUT_REVERSAL)
                .like(StockLedgerDO::getRemark, REVERSE_PREFIX + orig.getId()));
        if (CollUtil.isNotEmpty(existingReversals)) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "该发纱记录已被撤销，不可重复撤销");
        }

        // 4. 校验对应领纱明细存在
        YarnRequisitionItemDO item = yarnRequisitionItemMapper.selectById(orig.getRelatedDocId());
        if (item == null) {
            throw exception(YARN_REQUISITION_ITEM_NOT_EXISTS);
        }

        // 5. 校验领纱单存在与状态守卫
        YarnRequisitionDO requisition = yarnRequisitionMapper.selectById(item.getRequisitionId());
        if (requisition == null) {
            throw exception(YARN_REQUISITION_NOT_EXISTS);
        }
        if (STATUS_CLOSED.equals(requisition.getStatus()) || STATUS_DRAFT.equals(requisition.getStatus())) {
            throw exception(YARN_REQUISITION_STATUS_NOT_ALLOW, "领纱单已关闭或处于草稿态，不允许撤销发纱");
        }

        // 6. 写入红冲流水 YARN_OUT_REVERSAL（正数流水）
        BigDecimal reversalQty = orig.getQuantity() != null ? orig.getQuantity().abs() : BigDecimal.ZERO;
        StockLedgerDO reversal = new StockLedgerDO();
        reversal.setItemType("YARN_SPEC");
        reversal.setYarnSpecId(orig.getYarnSpecId());
        reversal.setYarnSpecName(orig.getYarnSpecName());
        reversal.setWarehouseId(orig.getWarehouseId());
        reversal.setWarehouseName(orig.getWarehouseName());
        reversal.setLocationId(orig.getLocationId());
        reversal.setLocationCode(orig.getLocationCode());
        reversal.setBatchNo(orig.getBatchNo());
        reversal.setTxnType(TXN_TYPE_YARN_OUT_REVERSAL);
        reversal.setQuantity(reversalQty); // 正数
        reversal.setUnit(orig.getUnit());
        reversal.setRelatedDocType(orig.getRelatedDocType());
        reversal.setRelatedDocId(orig.getRelatedDocId());
        reversal.setRelatedDocNo(orig.getRelatedDocNo());
        String customRemark = StringUtils.hasText(reversalReqVO.getRemark()) ? ": " + reversalReqVO.getRemark().trim() : "";
        reversal.setRemark(REVERSE_PREFIX + orig.getId() + customRemark);
        stockLedgerMapper.insert(reversal);

        // 7. 回减 dispatchedQty
        BigDecimal currentDispatched = item.getDispatchedQty() != null ? item.getDispatchedQty() : BigDecimal.ZERO;
        BigDecimal newDispatched = currentDispatched.subtract(reversalQty);
        if (newDispatched.compareTo(BigDecimal.ZERO) < 0) {
            newDispatched = BigDecimal.ZERO;
        }
        item.setDispatchedQty(newDispatched);
        yarnRequisitionItemMapper.updateById(item);

        // 8. 状态回退（DISPATCHED→PARTIAL/ACTIVE）
        List<YarnRequisitionItemDO> allItems = yarnRequisitionItemMapper.selectListByRequisitionId(requisition.getId());
        boolean anyDispatched = false;
        boolean allDispatched = true;
        for (YarnRequisitionItemDO it : allItems) {
            BigDecimal dispQty = it.getDispatchedQty() != null ? it.getDispatchedQty() : BigDecimal.ZERO;
            BigDecimal reqQty = it.getRequiredQty() != null ? it.getRequiredQty() : BigDecimal.ZERO;
            if (dispQty.compareTo(BigDecimal.ZERO) > 0) {
                anyDispatched = true;
            }
            if (dispQty.compareTo(reqQty) < 0) {
                allDispatched = false;
            }
        }

        String targetStatus;
        if (!anyDispatched) {
            targetStatus = STATUS_ACTIVE; // 彻底无已发纱，回退到待发纱 ACTIVE
        } else if (allDispatched) {
            targetStatus = STATUS_DISPATCHED;
        } else {
            targetStatus = STATUS_PARTIAL; // 部分已发，回退/保持 PARTIAL
        }

        if (!targetStatus.equals(requisition.getStatus())) {
            requisition.setStatus(targetStatus);
            yarnRequisitionMapper.updateById(requisition);
        }
    }

    @Override
    public List<YarnDispatchRecordRespVO> getDispatchRecordList(Long itemId) {
        if (itemId == null) {
            return Collections.emptyList();
        }
        // 查询该明细关联的所有流水（包括 YARN_OUT 和 YARN_OUT_REVERSAL）
        List<StockLedgerDO> list = stockLedgerMapper.selectList(new LambdaQueryWrapperX<StockLedgerDO>()
                .eq(StockLedgerDO::getRelatedDocType, DOC_TYPE_YARN_REQUISITION_ITEM)
                .eq(StockLedgerDO::getRelatedDocId, itemId)
                .in(StockLedgerDO::getTxnType, Arrays.asList(TXN_TYPE_YARN_OUT, TXN_TYPE_YARN_OUT_REVERSAL))
                .orderByDesc(StockLedgerDO::getId));

        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        // 统计哪些 YARN_OUT 已被撤销
        Map<Long, StockLedgerDO> reversedOutMap = new HashMap<>();
        for (StockLedgerDO ledger : list) {
            if (TXN_TYPE_YARN_OUT_REVERSAL.equals(ledger.getTxnType()) && StringUtils.hasText(ledger.getRemark())) {
                if (ledger.getRemark().startsWith(REVERSE_PREFIX)) {
                    String part = ledger.getRemark().substring(REVERSE_PREFIX.length());
                    int colonIdx = part.indexOf(':');
                    String idStr = colonIdx > 0 ? part.substring(0, colonIdx) : part;
                    try {
                        Long origId = Long.parseLong(idStr.trim());
                        reversedOutMap.put(origId, ledger);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // 构建返回列表：仅展示 YARN_OUT 记录，带 reversed 状态和撤销时间
        List<YarnDispatchRecordRespVO> respList = new ArrayList<>();
        for (StockLedgerDO ledger : list) {
            if (TXN_TYPE_YARN_OUT.equals(ledger.getTxnType())) {
                YarnDispatchRecordRespVO vo = new YarnDispatchRecordRespVO();
                vo.setId(ledger.getId());
                vo.setItemId(ledger.getRelatedDocId());
                vo.setYarnSpecId(ledger.getYarnSpecId());
                vo.setYarnSpecName(ledger.getYarnSpecName());
                vo.setWarehouseId(ledger.getWarehouseId());
                vo.setWarehouseName(ledger.getWarehouseName());
                vo.setLocationId(ledger.getLocationId());
                vo.setLocationCode(ledger.getLocationCode());
                vo.setQuantity(ledger.getQuantity() != null ? ledger.getQuantity().abs() : BigDecimal.ZERO);
                vo.setUnit(ledger.getUnit());
                vo.setTxnType(ledger.getTxnType());
                vo.setCreateTime(ledger.getCreateTime());
                vo.setCreator(ledger.getCreator());
                vo.setRemark(ledger.getRemark());
                vo.setRelatedDocNo(ledger.getRelatedDocNo());

                StockLedgerDO rev = reversedOutMap.get(ledger.getId());
                if (rev != null) {
                    vo.setReversed(true);
                    vo.setReversalTime(rev.getCreateTime());
                } else {
                    vo.setReversed(false);
                }
                respList.add(vo);
            }
        }
        return respList;
    }

    @Override
    public BigDecimal getYarnBalance(Long warehouseId, Long yarnSpecId) {
        return stockLedgerService.getYarnSpecBalance(warehouseId, yarnSpecId);
    }

}
