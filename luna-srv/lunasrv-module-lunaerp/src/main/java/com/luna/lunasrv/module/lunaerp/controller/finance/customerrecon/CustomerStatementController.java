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

/**
 * 财务管理 - 客户对账单控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 客户对账单")
@RestController
@RequestMapping("/lunaerp/finance/customerrecon/statement")
@Validated
public class CustomerStatementController {

    @Resource
    private CustomerStatementService customerStatementService;

    @GetMapping("/prefetch-lines")
    @Operation(summary = "期间对账分录取数预览与候选计算")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:query')")
    public CommonResult<CustomerStatementPrefetchRespVO> prefetchLines(@Valid CustomerStatementPrefetchReqVO reqVO) {
        return success(customerStatementService.prefetchLines(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "生成客户对账单（草稿）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:create')")
    public CommonResult<Long> createCustomerStatement(@Valid @RequestBody CustomerStatementCreateReqVO createReqVO) {
        return success(customerStatementService.createCustomerStatement(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户对账单详情")
    @Parameter(name = "id", description = "对账单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:query')")
    public CommonResult<CustomerStatementDetailRespVO> getCustomerStatement(@RequestParam("id") Long id) {
        return success(customerStatementService.getCustomerStatement(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户对账单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:query')")
    public CommonResult<PageResult<CustomerStatementRespVO>> getCustomerStatementPage(@Valid CustomerStatementPageReqVO pageReqVO) {
        return success(customerStatementService.getCustomerStatementPage(pageReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户对账单（仅 DRAFT 允许）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:update')")
    public CommonResult<Boolean> updateCustomerStatement(@Valid @RequestBody CustomerStatementUpdateReqVO updateReqVO) {
        customerStatementService.updateCustomerStatement(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户对账单（仅 DRAFT 允许软删除）")
    @Parameter(name = "id", description = "对账单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:delete')")
    public CommonResult<Boolean> deleteCustomerStatement(@RequestParam("id") Long id) {
        customerStatementService.deleteCustomerStatement(id);
        return success(true);
    }

    @PostMapping("/add-entry")
    @Operation(summary = "草稿对账单手工追加未占用分录")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:update')")
    public CommonResult<Long> addStatementEntry(@Valid @RequestBody CustomerStatementAddEntryReqVO addEntryReqVO) {
        return success(customerStatementService.addStatementEntry(addEntryReqVO));
    }

}
