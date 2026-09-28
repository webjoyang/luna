package com.luna.lunasrv.module.lunaerp.service.stock.stockledger;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalancePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 库存管理 - 库存台账与余额 Service 实现类（ADR-0017）
 *
 * @author luna
 */
@Service
public class StockLedgerServiceImpl implements StockLedgerService {

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Override
    public PageResult<InventoryBalanceRespVO> getInventoryBalancePage(InventoryBalancePageReqVO pageReqVO) {
        return stockLedgerMapper.selectBalancePage(pageReqVO);
    }

    @Override
    public PageResult<StockLedgerRespVO> getStockLedgerPage(StockLedgerPageReqVO pageReqVO) {
        PageResult<StockLedgerDO> pageResult = stockLedgerMapper.selectLedgerPage(pageReqVO);
        return BeanUtils.toBean(pageResult, StockLedgerRespVO.class);
    }

    @Override
    public BigDecimal getYarnSpecBalance(Long warehouseId, Long yarnSpecId) {
        if (warehouseId == null || yarnSpecId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal balance = stockLedgerMapper.sumQuantityByWarehouseAndYarnSpec(warehouseId, yarnSpecId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getProductBalance(Long warehouseId, Long productId, String unit) {
        if (warehouseId == null || productId == null || unit == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal balance = stockLedgerMapper.sumQuantityByWarehouseAndProductAndUnit(warehouseId, productId, unit);
        return balance != null ? balance : BigDecimal.ZERO;
    }

}
