package com.luna.lunasrv.module.lunaerp.controller.finance.ledger;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerCustomerBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 经营分录账 Controller（只读视图查询）
 *
 * @author luna
 */
@Tag(name = "管理后台 - 经营分录账")
@RestController
@RequestMapping("/lunaerp/finance/ledger")
@Validated
public class LedgerController {

    @Resource
    private LedgerService ledgerService;

    @GetMapping("/page")
    @Operation(summary = "获得经营分录账分页（只读流水视图）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:ledger:query') || @ss.hasPermission('lunaerp:finance:customer-reconciliation:query')")
    public CommonResult<PageResult<LedgerRespVO>> getLedgerPage(@Valid LedgerPageReqVO pageReqVO) {
        PageResult<LedgerRespVO> pageResult = ledgerService.getLedgerPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/customer-balance")
    @Operation(summary = "按客户维度查询经营分录账余额与汇率差异列表（只读往来视图）")
    @Parameter(name = "customerId", description = "客户ID（可选）", example = "1001")
    @Parameter(name = "currency", description = "币种（可选）", example = "USD")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:ledger:query') || @ss.hasPermission('lunaerp:finance:customer-reconciliation:query')")
    public CommonResult<List<LedgerCustomerBalanceRespVO>> getCustomerBalanceList(
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "currency", required = false) String currency) {
        List<LedgerCustomerBalanceRespVO> list = ledgerService.getCustomerBalanceList(customerId, currency);
        return success(list);
    }

}
