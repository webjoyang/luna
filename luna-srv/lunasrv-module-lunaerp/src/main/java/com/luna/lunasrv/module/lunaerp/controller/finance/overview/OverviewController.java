package com.luna.lunasrv.module.lunaerp.controller.finance.overview;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.overview.FinanceOverviewService;
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
 * 财务管理 - 收支总览 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 收支总览")
@RestController
@RequestMapping("/lunaerp/finance/overview")
@Validated
public class OverviewController {

    @Resource
    private FinanceOverviewService overviewService;

    @GetMapping("/summary")
    @Operation(summary = "获得收支总览汇总（账户期初/流入/流出/期末 + 类别发生额）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:overview:query')")
    public CommonResult<OverviewSummaryRespVO> getOverviewSummary(@Valid OverviewSummaryReqVO reqVO) {
        OverviewSummaryRespVO respVO = overviewService.getOverviewSummary(reqVO);
        return success(respVO);
    }

    @GetMapping("/gross-margin")
    @Operation(summary = "获得经营毛利参考（管理口径：收入净额 - 供应商确认对账成本）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:overview:query')")
    public CommonResult<GrossMarginRespVO> getGrossMargin(@Valid GrossMarginReqVO reqVO) {
        GrossMarginRespVO respVO = overviewService.getGrossMargin(reqVO);
        return success(respVO);
    }

}
