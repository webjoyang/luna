package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 资金账户控制器（ADR-0043 / Issue 02）
 *
 * @author luna
 */
@Tag(name = "财务管理 - 资金账户")
@RestController
@RequestMapping("/lunaerp/finance/account")
@Validated
public class AccountController {

    @Resource
    private AccountService accountService;

    @PostMapping("/create")
    @Operation(summary = "创建资金账户")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:create')")
    public CommonResult<Long> createAccount(@Valid @RequestBody AccountSaveReqVO createReqVO) {
        return success(accountService.createAccount(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得资金账户详情")
    @Parameter(name = "id", description = "账户编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:query')")
    public CommonResult<AccountRespVO> getAccount(@RequestParam("id") Long id) {
        return success(accountService.getAccount(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资金账户分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:query')")
    public CommonResult<PageResult<AccountRespVO>> getAccountPage(@Valid AccountPageReqVO pageReqVO) {
        return success(accountService.getAccountPage(pageReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改资金账户")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:update')")
    public CommonResult<Boolean> updateAccount(@Valid @RequestBody AccountUpdateReqVO updateReqVO) {
        accountService.updateAccount(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资金账户（进入回收站）")
    @Parameter(name = "id", description = "账户编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:delete')")
    public CommonResult<Boolean> deleteAccount(@RequestParam("id") Long id) {
        accountService.deleteAccount(id);
        return success(true);
    }


    @GetMapping("/simple-list")
    @Operation(summary = "获得资金账户精简下拉列表（仅 ENABLE 状态）")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:finance:account:query', 'lunaerp:finance:payment:query', 'lunaerp:finance:expense:query', 'lunaerp:finance:payroll:query')")
    public CommonResult<List<FinanceAccountSimpleRespVO>> getSimpleAccountList() {
        return success(accountService.getSimpleAccountList());
    }

}
