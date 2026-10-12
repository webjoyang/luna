package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
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
 * 财务管理 - 资金账户 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 资金账户")
@RestController
@RequestMapping("/lunaerp/finance/account")
@Validated
public class AccountController {

    @Resource
    private AccountService accountService;

    @GetMapping("/simple-list")
    @Operation(summary = "获得资金账户精简列表（仅启用状态，供下拉选择）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<List<AccountSimpleRespVO>> getSimpleAccountList() {
        List<AccountDO> list = accountService.getSimpleAccountList();
        return success(BeanUtils.toBean(list, AccountSimpleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资金账户分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<PageResult<AccountRespVO>> getAccountPage(@Valid AccountPageReqVO pageReqVO) {
        PageResult<AccountDO> pageResult = accountService.getAccountPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AccountRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得资金账户详情")
    @Parameter(name = "id", description = "资金账户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<AccountRespVO> getAccount(@RequestParam("id") Long id) {
        AccountDO accountDO = accountService.getAccount(id);
        return success(BeanUtils.toBean(accountDO, AccountRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建资金账户")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Long> createAccount(@Valid @RequestBody AccountSaveReqVO createReqVO) {
        return success(accountService.createAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资金账户")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:register')")
    public CommonResult<Boolean> updateAccount(@Valid @RequestBody AccountSaveReqVO updateReqVO) {
        accountService.updateAccount(updateReqVO);
        return success(true);
    }

}
