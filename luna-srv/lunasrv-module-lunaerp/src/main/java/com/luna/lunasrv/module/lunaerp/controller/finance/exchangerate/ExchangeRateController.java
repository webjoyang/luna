package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 汇率主档 控制器（SPOT 即期牌价）
 *
 * @author luna
 */
@Tag(name = "财务管理 - 汇率主档")
@RestController
@RequestMapping("/lunaerp/finance/exchange-rate")
@Validated
public class ExchangeRateController {

    @Resource
    private ExchangeRateService exchangeRateService;

    @PostMapping("/create")
    @Operation(summary = "创建汇率主档")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchangerate:create')")
    public CommonResult<Long> createExchangeRate(@Valid @RequestBody ExchangeRateSaveReqVO createReqVO) {
        return success(exchangeRateService.createExchangeRate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新汇率主档（停用/维护）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchangerate:update')")
    public CommonResult<Boolean> updateExchangeRate(@Valid @RequestBody ExchangeRateUpdateReqVO updateReqVO) {
        exchangeRateService.updateExchangeRate(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得汇率主档详情")
    @Parameter(name = "id", description = "汇率记录ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchangerate:query')")
    public CommonResult<ExchangeRateRespVO> getExchangeRate(@RequestParam("id") Long id) {
        return success(exchangeRateService.getExchangeRate(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得汇率主档分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchangerate:query')")
    public CommonResult<PageResult<ExchangeRateRespVO>> getExchangeRatePage(@Valid ExchangeRatePageReqVO pageReqVO) {
        return success(exchangeRateService.getExchangeRatePage(pageReqVO));
    }

}
