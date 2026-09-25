package com.luna.lunasrv.module.lunaerp.service.stock.stockledger;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalancePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerRespVO;

/**
 * 库存管理 - 库存台账与余额 Service 接口（ADR-0017）
 *
 * @author luna
 */
public interface StockLedgerService {

    /**
     * 获得库存余额分页（实时聚合 SUM(quantity) GROUP BY product_id, warehouse_id, unit）
     *
     * @param pageReqVO 筛选与分页条件
     * @return 库存余额分页结果
     */
    PageResult<InventoryBalanceRespVO> getInventoryBalancePage(InventoryBalancePageReqVO pageReqVO);

    /**
     * 获得库存流水台账分页（只读明细）
     *
     * @param pageReqVO 筛选与分页条件
     * @return 库存流水分页结果
     */
    PageResult<StockLedgerRespVO> getStockLedgerPage(StockLedgerPageReqVO pageReqVO);

}
