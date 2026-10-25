package com.luna.lunasrv.module.lunaerp.controller.finance.ledger;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 经营分录账/资金流水 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 资金流水")
@RestController
@RequestMapping("/lunaerp/finance/ledger")
@Validated
public class LedgerController {

    @Resource
    private LedgerService ledgerService;

    @GetMapping("/page")
    @Operation(summary = "获得资金流水分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:query')")
    public CommonResult<PageResult<LedgerRespVO>> getLedgerPage(@Valid LedgerPageReqVO pageReqVO) {
        PageResult<LedgerRespVO> pageResult = ledgerService.getLedgerPage(pageReqVO);
        return success(pageResult);
    }

}
