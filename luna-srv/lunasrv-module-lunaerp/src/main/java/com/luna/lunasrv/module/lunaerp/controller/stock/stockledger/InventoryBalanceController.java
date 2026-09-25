package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalancePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
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
 * 管理后台 - 库存余额 Controller（ADR-0017）
 *
 * 归属 stock.stockledger 子包，映射 URL /lunaerp/stock/inventory-balance（CONTEXT.md 架构约束）
 *
 * @author luna
 */
@Tag(name = "库存管理 - 库存余额")
@RestController
@RequestMapping("/lunaerp/stock/inventory-balance")
@Validated
public class InventoryBalanceController {

    @Resource
    private StockLedgerService stockLedgerService;

    @GetMapping("/page")
    @Operation(summary = "获得库存余额分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:inventory-balance:query')")
    public CommonResult<PageResult<InventoryBalanceRespVO>> getInventoryBalancePage(@Validated InventoryBalancePageReqVO pageReqVO) {
        return success(stockLedgerService.getInventoryBalancePage(pageReqVO));
    }

}
