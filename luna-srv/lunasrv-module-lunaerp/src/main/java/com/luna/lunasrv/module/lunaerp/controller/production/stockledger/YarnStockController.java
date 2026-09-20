package com.luna.lunasrv.module.lunaerp.controller.production.stockledger;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.stockledger.vo.YarnStockBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.stockledger.StockLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 纱线库存")
@RestController
@RequestMapping("/admin-api/lunaerp/production/yarn-stock")
@Validated
public class YarnStockController {

    @Resource
    private StockLedgerService stockLedgerService;

    @GetMapping("/balance")
    @Operation(summary = "获得单规格纱线库存余额")
    @Parameter(name = "yarnSpecId", description = "纱线规格编号", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-stock:query')")
    public CommonResult<YarnStockBalanceRespVO> getBalance(@RequestParam("yarnSpecId") Long yarnSpecId) {
        BigDecimal balance = stockLedgerService.getBalance(yarnSpecId);
        return success(new YarnStockBalanceRespVO(yarnSpecId, balance));
    }

    @GetMapping("/balances")
    @Operation(summary = "批量获得纱线库存余额")
    @Parameter(name = "yarnSpecIds", description = "纱线规格编号列表", required = true, example = "1,2,3")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-stock:query')")
    public CommonResult<List<YarnStockBalanceRespVO>> getBalances(@RequestParam("yarnSpecIds") List<Long> yarnSpecIds) {
        if (CollUtil.isEmpty(yarnSpecIds)) {
            return success(Collections.emptyList());
        }
        Map<Long, BigDecimal> balances = stockLedgerService.getBalances(yarnSpecIds);
        List<YarnStockBalanceRespVO> list = yarnSpecIds.stream()
                .map(id -> new YarnStockBalanceRespVO(id, balances.getOrDefault(id, BigDecimal.ZERO)))
                .collect(Collectors.toList());
        return success(list);
    }

}
