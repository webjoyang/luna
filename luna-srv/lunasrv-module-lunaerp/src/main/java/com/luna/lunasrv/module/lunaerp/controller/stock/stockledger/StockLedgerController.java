package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerRespVO;
import com.luna.lunasrv.module.lunaerp.service.stock.stockledger.StockLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 库存流水台账 Controller（ADR-0017）
 *
 * 归属 stock.stockledger 子包，映射 URL /lunaerp/stock/stock-ledger；
 * 权限码按 Spec / Issue 06 要求与库存余额共用 lunaerp:stock:inventory-balance:query 统一查询权限。
 *
 * @author luna
 */
@Tag(name = "库存管理 - 库存流水台账")
@RestController
@RequestMapping("/lunaerp/stock/stock-ledger")
@Validated
public class StockLedgerController {

    @Resource
    private StockLedgerService stockLedgerService;

    @GetMapping("/page")
    @Operation(summary = "获得库存流水台账分页（只读）")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:inventory-balance:query')")
    public CommonResult<PageResult<StockLedgerRespVO>> getStockLedgerPage(@Validated StockLedgerPageReqVO pageReqVO) {
        return success(stockLedgerService.getStockLedgerPage(pageReqVO));
    }

}
