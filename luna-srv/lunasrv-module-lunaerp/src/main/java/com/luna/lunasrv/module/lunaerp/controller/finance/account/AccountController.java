package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.FinanceAccountSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 资金账户控制器（一期提供下拉选择端点，完整管理页归 12 号）
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

    @GetMapping("/simple-list")
    @Operation(summary = "获得资金账户精简下拉列表（仅 ENABLE 状态）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payment:query')")
    public CommonResult<List<FinanceAccountSimpleRespVO>> getSimpleAccountList() {
        return success(accountService.getSimpleAccountList());
    }

}
