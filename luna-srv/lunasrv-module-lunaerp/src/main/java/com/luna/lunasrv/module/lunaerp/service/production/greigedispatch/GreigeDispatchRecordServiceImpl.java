package com.luna.lunasrv.module.lunaerp.service.production.greigedispatch;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch.GreigeDispatchRecordDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigedispatch.GreigeDispatchRecordMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.service.stock.stockledger.StockLedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-发染记录 Service 实现类（ADR-0033）
 *
 * 核心逻辑：
 * 1. 创建：校验胚布单存在且未取消、仓库启用、库存余额充足，同事务写 PROCESS_OUT 负流水
 * 2. 删除：写 PROCESS_OUT_REVERSAL 正红冲流水，恢复库存，软删记录
 * 3. 受限编辑：仅允许改染色单/染厂/备注，数量/日期/匹数/仓库锁定
 * 4. 补填染色单时校验与胚布单同合同
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class GreigeDispatchRecordServiceImpl implements GreigeDispatchRecordService {

    private static final String TXN_TYPE_PROCESS_OUT = "PROCESS_OUT";
    private static final String TXN_TYPE_PROCESS_OUT_REVERSAL = "PROCESS_OUT_REVERSAL";
    private static final String DOC_TYPE_GREIGE_DISPATCH = "GREIGE_DISPATCH";
    private static final String REVERSE_PREFIX = "REVERSE_LEDGER_";
    private static final String DEFAULT_UNIT = "KG";

    @Resource
    private GreigeDispatchRecordMapper greigeDispatchRecordMapper;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Resource
    private StockLedgerService stockLedgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGreigeDispatchRecord(GreigeDispatchRecordCreateReqVO reqVO) {
        // 1. 校验胚布单存在且未取消
        GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(reqVO.getGreigeOrderId());
        if (greigeOrder == null || Boolean.TRUE.equals(greigeOrder.getDeleted())
                || "CANCELLED".equals(greigeOrder.getStatus())) {
            throw exception(GREIGE_DISPATCH_GREIGE_ORDER_NOT_EXISTS);
        }

        // 2. 校验仓库存在且启用
        WarehouseDO warehouse = warehouseMapper.selectById(reqVO.getWarehouseId());
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())
                || !"ACTIVE".equals(warehouse.getStatus())) {
            throw exception(GREIGE_DISPATCH_WAREHOUSE_NOT_ENABLED);
        }

        // 3. 校验数量 > 0
        BigDecimal quantity = reqVO.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(GREIGE_DISPATCH_STOCK_NOT_ENOUGH, quantity, BigDecimal.ZERO);
        }

        // 4. 库存不足拦截（同仓同产品 KG 余额）
        BigDecimal availableBalance = stockLedgerService.getProductBalance(
                warehouse.getId(), greigeOrder.getProductId(), DEFAULT_UNIT);
        if (availableBalance == null || availableBalance.compareTo(quantity) < 0) {
            BigDecimal av = availableBalance != null ? availableBalance : BigDecimal.ZERO;
            throw exception(GREIGE_DISPATCH_STOCK_NOT_ENOUGH, quantity, av);
        }

        // 5. 若指定了染色单，校验其存在且与胚布单同合同
        if (reqVO.getDyeingOrderId() != null) {
            validateDyeingOrderContract(reqVO.getDyeingOrderId(), greigeOrder);
        }

        // 6. 写入 PROCESS_OUT 负流水
        StockLedgerDO ledger = new StockLedgerDO();
        ledger.setItemType("PRODUCT");
        ledger.setProductId(greigeOrder.getProductId());
        ledger.setProductCode(greigeOrder.getProductCode());
        ledger.setProductName(greigeOrder.getProductName());
        ledger.setWarehouseId(warehouse.getId());
        ledger.setWarehouseName(warehouse.getName());
        ledger.setTxnType(TXN_TYPE_PROCESS_OUT);
        ledger.setQuantity(quantity.negate()); // 负数
        ledger.setUnit(DEFAULT_UNIT);
        ledger.setRelatedDocType(DOC_TYPE_GREIGE_DISPATCH);
        // relatedDocId 需要记录插入后才能拿到，先插入记录再回填
        // 流水备注含合同信息（合同ID+合同号），便于反查
        String contractInfo = greigeOrder.getSalesContractId() != null
                ? "合同[" + greigeOrder.getSalesContractId() + ":" + greigeOrder.getContractNo() + "] "
                : "";
        String userRemark = reqVO.getRemark() != null ? reqVO.getRemark() : "";
        ledger.setRemark(contractInfo + userRemark);
        stockLedgerMapper.insert(ledger);

        // 7. 组装并插入发染记录
        GreigeDispatchRecordDO record = new GreigeDispatchRecordDO();
        record.setGreigeOrderId(greigeOrder.getId());
        record.setDyeingOrderId(reqVO.getDyeingOrderId());
        record.setDyeingFactoryId(reqVO.getDyeingFactoryId());
        record.setDyeingFactoryName(reqVO.getDyeingFactoryName());
        record.setWarehouseId(warehouse.getId());
        record.setWarehouseName(warehouse.getName());
        record.setProductId(greigeOrder.getProductId());
        record.setProductCode(greigeOrder.getProductCode());
        record.setProductName(greigeOrder.getProductName());
        record.setDispatchDate(reqVO.getDispatchDate());
        record.setQuantity(quantity);
        record.setPieces(reqVO.getPieces());
        record.setStockLedgerId(ledger.getId());
        record.setRemark(reqVO.getRemark());
        greigeDispatchRecordMapper.insert(record);

        // 8. 回填流水的 relatedDocId
        ledger.setRelatedDocId(record.getId());
        stockLedgerMapper.updateById(ledger);

        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGreigeDispatchRecord(GreigeDispatchRecordUpdateReqVO reqVO) {
        // 1. 校验记录存在
        GreigeDispatchRecordDO record = greigeDispatchRecordMapper.selectById(reqVO.getId());
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw exception(GREIGE_DISPATCH_NOT_EXISTS);
        }

        // 2. 若修改了染色单，校验同合同
        if (reqVO.getDyeingOrderId() != null) {
            GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(record.getGreigeOrderId());
            if (greigeOrder == null) {
                throw exception(GREIGE_DISPATCH_GREIGE_ORDER_NOT_EXISTS);
            }
            validateDyeingOrderContract(reqVO.getDyeingOrderId(), greigeOrder);
        }

        // 3. 仅允许改染色单/染厂/备注（数量/日期/匹数/仓库锁定，VO 中不含这些字段）
        record.setDyeingOrderId(reqVO.getDyeingOrderId());
        record.setDyeingFactoryId(reqVO.getDyeingFactoryId());
        record.setDyeingFactoryName(reqVO.getDyeingFactoryName());
        record.setRemark(reqVO.getRemark());
        greigeDispatchRecordMapper.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGreigeDispatchRecord(Long id) {
        // 1. 校验记录存在
        GreigeDispatchRecordDO record = greigeDispatchRecordMapper.selectById(id);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw exception(GREIGE_DISPATCH_NOT_EXISTS);
        }

        // 2. 校验原流水存在
        StockLedgerDO origLedger = stockLedgerMapper.selectById(record.getStockLedgerId());
        if (origLedger == null) {
            throw exception(GREIGE_DISPATCH_NOT_EXISTS);
        }

        // 3. 校验是否已被撤销过（防重复撤销）
        List<StockLedgerDO> existingReversals = stockLedgerMapper.selectList(
                new LambdaQueryWrapperX<StockLedgerDO>()
                        .eq(StockLedgerDO::getRelatedDocType, DOC_TYPE_GREIGE_DISPATCH)
                        .eq(StockLedgerDO::getRelatedDocId, record.getId())
                        .eq(StockLedgerDO::getTxnType, TXN_TYPE_PROCESS_OUT_REVERSAL)
                        .like(StockLedgerDO::getRemark, REVERSE_PREFIX + origLedger.getId()));
        if (CollUtil.isNotEmpty(existingReversals)) {
            throw exception(GREIGE_DISPATCH_ALREADY_REVERSED);
        }

        // 4. 写入 PROCESS_OUT_REVERSAL 正红冲流水
        BigDecimal reversalQty = origLedger.getQuantity() != null ? origLedger.getQuantity().abs() : BigDecimal.ZERO;
        StockLedgerDO reversal = new StockLedgerDO();
        reversal.setItemType(origLedger.getItemType());
        reversal.setProductId(origLedger.getProductId());
        reversal.setProductCode(origLedger.getProductCode());
        reversal.setProductName(origLedger.getProductName());
        reversal.setWarehouseId(origLedger.getWarehouseId());
        reversal.setWarehouseName(origLedger.getWarehouseName());
        reversal.setTxnType(TXN_TYPE_PROCESS_OUT_REVERSAL);
        reversal.setQuantity(reversalQty); // 正数
        reversal.setUnit(origLedger.getUnit());
        reversal.setRelatedDocType(origLedger.getRelatedDocType());
        reversal.setRelatedDocId(record.getId());
        reversal.setRemark(REVERSE_PREFIX + origLedger.getId());
        stockLedgerMapper.insert(reversal);

        // 5. 软删发染记录
        greigeDispatchRecordMapper.deleteById(id);
    }

    @Override
    public GreigeDispatchRecordDO getGreigeDispatchRecord(Long id) {
        return greigeDispatchRecordMapper.selectById(id);
    }

    @Override
    public PageResult<GreigeDispatchRecordDO> getGreigeDispatchRecordPage(GreigeDispatchRecordPageReqVO reqVO) {
        return greigeDispatchRecordMapper.selectPage(reqVO);
    }

    @Override
    public List<GreigeDispatchRecordDO> getGreigeDispatchRecordListByGreigeOrderId(Long greigeOrderId) {
        return greigeDispatchRecordMapper.selectListByGreigeOrderId(greigeOrderId);
    }

    @Override
    public List<GreigeDispatchRecordDO> getGreigeDispatchRecordListByDyeingOrderId(Long dyeingOrderId) {
        return greigeDispatchRecordMapper.selectListByDyeingOrderId(dyeingOrderId);
    }

    @Override
    public Map<Long, BigDecimal> sumDispatchedQtyByGreigeOrderIds(Collection<Long> greigeOrderIds) {
        if (CollUtil.isEmpty(greigeOrderIds)) {
            return Collections.emptyMap();
        }
        List<GreigeDispatchRecordDO> records = greigeDispatchRecordMapper.selectListByGreigeOrderIds(greigeOrderIds);
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (GreigeDispatchRecordDO r : records) {
            if (r.getQuantity() != null) {
                result.merge(r.getGreigeOrderId(), r.getQuantity(), BigDecimal::add);
            }
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验染色单存在且与胚布单同合同
     */
    private void validateDyeingOrderContract(Long dyeingOrderId, GreigeOrderDO greigeOrder) {
        DyeingOrderDO dyeingOrder = dyeingOrderMapper.selectById(dyeingOrderId);
        if (dyeingOrder == null || Boolean.TRUE.equals(dyeingOrder.getDeleted())) {
            throw exception(GREIGE_DISPATCH_DYEING_ORDER_NOT_EXISTS);
        }
        // 校验同合同：两者 salesContractId 一致（含两者均为 null 的情况放行）
        if (!Objects.equals(dyeingOrder.getSalesContractId(), greigeOrder.getSalesContractId())) {
            throw exception(GREIGE_DISPATCH_DYEING_ORDER_CONTRACT_MISMATCH);
        }
    }

}
