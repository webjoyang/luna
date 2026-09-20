package com.luna.lunasrv.module.lunaerp.service.production.stockledger;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger.dto.StockBalanceDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产管理-纱线库存流水台账 Service 实现类
 *
 * @author luna
 */
@Service
public class StockLedgerServiceImpl implements StockLedgerService {

    public static final String TYPE_STOCK_IN = "STOCK_IN";

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Override
    public BigDecimal getBalance(Long yarnSpecId) {
        BigDecimal balance = stockLedgerMapper.selectBalanceByYarnSpecId(yarnSpecId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Override
    public Map<Long, BigDecimal> getBalances(Collection<Long> yarnSpecIds) {
        if (CollUtil.isEmpty(yarnSpecIds)) {
            return Collections.emptyMap();
        }
        List<StockBalanceDTO> dtoList = stockLedgerMapper.selectBalancesByYarnSpecIds(yarnSpecIds);
        Map<Long, BigDecimal> map = new HashMap<>(yarnSpecIds.size());
        if (dtoList != null) {
            for (StockBalanceDTO dto : dtoList) {
                map.put(dto.getYarnSpecId(), dto.getBalance());
            }
        }
        for (Long id : yarnSpecIds) {
            map.putIfAbsent(id, BigDecimal.ZERO);
        }
        return map;
    }

    @Override
    public void writeStockIn(Long yarnSpecId, BigDecimal quantity, String relatedDocType, Long relatedDocId, String batchNo, String remark) {
        StockLedgerDO ledger = new StockLedgerDO();
        ledger.setYarnSpecId(yarnSpecId);
        ledger.setType(TYPE_STOCK_IN);
        ledger.setQuantity(quantity);
        ledger.setRelatedDocType(relatedDocType);
        ledger.setRelatedDocId(relatedDocId);
        ledger.setBatchNo(batchNo);
        ledger.setRemark(remark);
        stockLedgerMapper.insert(ledger);
    }

    @Override
    public boolean existsByRelatedDoc(String relatedDocType, Long relatedDocId) {
        Long count = stockLedgerMapper.selectCountByRelatedDoc(relatedDocType, relatedDocId);
        return count != null && count > 0;
    }

}
