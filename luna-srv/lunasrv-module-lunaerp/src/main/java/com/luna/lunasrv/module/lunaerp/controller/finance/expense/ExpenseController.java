package com.luna.lunasrv.module.lunaerp.controller.finance.expense;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.expense.ExpenseService;
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
 * 财务管理 - 员工费用报销 控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 员工费用报销")
@RestController
@RequestMapping("/lunaerp/finance/expense")
@Validated
public class ExpenseController {

    @Resource
    private ExpenseService expenseService;

    @PostMapping("/create")
    @Operation(summary = "新建报销单草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:create')")
    public CommonResult<Long> createExpense(@Valid @RequestBody ExpenseCreateReqVO createReqVO) {
        return success(expenseService.createExpense(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改报销单草稿（仅 PENDING/REJECTED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:update')")
    public CommonResult<Boolean> updateExpense(@Valid @RequestBody ExpenseUpdateReqVO updateReqVO) {
        expenseService.updateExpense(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报销单草稿（仅 PENDING/REJECTED 软删进回收站）")
    @Parameter(name = "id", description = "报销单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:delete')")
    public CommonResult<Boolean> deleteExpense(@RequestParam("id") Long id) {
        expenseService.deleteExpense(id);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交报销单审批（02号审批引擎）")
    @Parameter(name = "id", description = "报销单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:submit')")
    public CommonResult<Boolean> submitExpense(@RequestParam("id") Long id) {
        expenseService.submitExpense(id);
        return success(true);
    }

    @PostMapping("/cancel-approve")
    @Operation(summary = "撤回报销单审批（仅 APPROVING 态，回置为 PENDING）")
    @Parameter(name = "id", description = "报销单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:submit')")
    public CommonResult<Boolean> cancelApprove(@RequestParam("id") Long id) {
        expenseService.cancelApprove(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报销单详情")
    @Parameter(name = "id", description = "报销单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:query')")
    public CommonResult<ExpenseRespVO> getExpense(@RequestParam("id") Long id) {
        return success(expenseService.getExpense(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报销单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:query')")
    public CommonResult<PageResult<ExpenseRespVO>> getExpensePage(@Valid ExpensePageReqVO pageReqVO) {
        return success(expenseService.getExpensePage(pageReqVO));
    }

    @GetMapping("/print-data")
    @Operation(summary = "获得报销单 A4 套打数据（仅 APPROVED 可取，豁免金额脱敏）")
    @Parameter(name = "id", description = "报销单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:expense:print')")
    public CommonResult<ExpensePrintRespVO> getExpensePrintData(@RequestParam("id") Long id) {
        return success(expenseService.getExpensePrintData(id));
    }

}
