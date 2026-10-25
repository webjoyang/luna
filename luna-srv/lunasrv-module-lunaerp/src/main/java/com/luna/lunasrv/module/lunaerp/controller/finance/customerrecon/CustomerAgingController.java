package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 客户账龄分析控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 客户账龄分析")
@RestController
@RequestMapping("/lunaerp/finance/customerrecon/aging")
@Validated
public class CustomerAgingController {

    @Resource
    private CustomerAgingService customerAgingService;

    @GetMapping("/summary")
    @Operation(summary = "获得客户账龄汇总列表（按客户一行）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:query')")
    public CommonResult<List<CustomerAgingSummaryRespVO>> getAgingSummary(@Valid CustomerAgingSummaryReqVO reqVO) {
        return success(customerAgingService.getAgingSummary(reqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得客户未核销明细分录（下钻）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:query')")
    public CommonResult<List<CustomerAgingDetailRespVO>> getAgingDetail(@Valid CustomerAgingDetailReqVO reqVO) {
        return success(customerAgingService.getAgingDetail(reqVO));
    }
}
