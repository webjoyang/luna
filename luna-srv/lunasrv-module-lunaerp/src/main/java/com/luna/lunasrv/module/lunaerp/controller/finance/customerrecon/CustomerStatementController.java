package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 客户对账")
@RestController
@RequestMapping({"/lunaerp/finance/statement", "/lunaerp/finance/customer-recon"})
@Validated
public class CustomerStatementController {

    @Resource
    private CustomerStatementService customerStatementService;

    @GetMapping("/page")
    @Operation(summary = "获得客户对账单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:query')")
    public CommonResult<PageResult<CustomerStatementRespVO>> getStatementPage(@Valid CustomerStatementPageReqVO pageReqVO) {
        return success(customerStatementService.getStatementPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户对账单详情")
    @Parameter(name = "id", description = "编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:query')")
    public CommonResult<CustomerStatementDetailRespVO> getStatement(@RequestParam("id") Long id) {
        return success(customerStatementService.getStatement(id));
    }

    @GetMapping("/prefetch-lines")
    @Operation(summary = "对账期间候选分录取数预览（含试算与追加选择器）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:query')")
    public CommonResult<CustomerStatementPrefetchRespVO> prefetchLines(@Valid CustomerStatementPrefetchReqVO reqVO) {
        return success(customerStatementService.prefetchLines(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建客户对账单草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:create')")
    public CommonResult<Long> createStatement(@Valid @RequestBody CustomerStatementCreateReqVO reqVO) {
        return success(customerStatementService.createStatement(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户对账单草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:update')")
    public CommonResult<Boolean> updateStatement(@Valid @RequestBody CustomerStatementUpdateReqVO reqVO) {
        customerStatementService.updateStatement(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户对账单草稿（移入回收站）")
    @Parameter(name = "id", description = "编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:delete')")
    public CommonResult<Boolean> deleteStatement(@RequestParam("id") Long id) {
        customerStatementService.deleteStatement(id);
        return success(true);
    }

    @PostMapping("/add-entry")
    @Operation(summary = "草稿对账单手工追加未占用分录")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customer-reconciliation:update')")
    public CommonResult<Boolean> addEntry(@Valid @RequestBody CustomerStatementAddEntryReqVO reqVO) {
        customerStatementService.addEntry(reqVO);
        return success(true);
    }

}
