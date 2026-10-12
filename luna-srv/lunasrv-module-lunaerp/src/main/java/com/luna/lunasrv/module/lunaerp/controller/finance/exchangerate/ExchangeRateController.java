package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
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
 * 财务管理 - 汇率主档 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 汇率主档")
@RestController
@RequestMapping("/lunaerp/finance/exchange-rate")
@Validated
public class ExchangeRateController {

    @Resource
    private ExchangeRateService exchangeRateService;

    @GetMapping("/page")
    @Operation(summary = "获得汇率主档分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchange-rate:query')")
    public CommonResult<PageResult<ExchangeRateRespVO>> getExchangeRatePage(@Valid ExchangeRatePageReqVO pageReqVO) {
        PageResult<ExchangeRateDO> pageResult = exchangeRateService.getExchangeRatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExchangeRateRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得汇率主档详情")
    @Parameter(name = "id", description = "汇率编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchange-rate:query')")
    public CommonResult<ExchangeRateRespVO> getExchangeRate(@RequestParam("id") Long id) {
        ExchangeRateDO rateDO = exchangeRateService.getExchangeRate(id);
        return success(BeanUtils.toBean(rateDO, ExchangeRateRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建汇率主档")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchange-rate:create')")
    public CommonResult<Long> createExchangeRate(@Valid @RequestBody ExchangeRateSaveReqVO createReqVO) {
        return success(exchangeRateService.createExchangeRate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新汇率主档（仅允许停用/启用及修改备注）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:exchange-rate:update')")
    public CommonResult<Boolean> updateExchangeRate(@Valid @RequestBody ExchangeRateSaveReqVO updateReqVO) {
        exchangeRateService.updateExchangeRate(updateReqVO);
        return success(true);
    }

}
