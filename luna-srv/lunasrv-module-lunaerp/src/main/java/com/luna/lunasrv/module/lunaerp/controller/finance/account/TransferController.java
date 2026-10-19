package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.account.TransferService;
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
 * 财务管理 - 资金账户间转账控制器（ZZ 单）
 *
 * <p>两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。不提供 update/delete 端点。</p>
 *
 * @author luna
 */
@Tag(name = "财务管理 - 账户间转账")
@RestController
@RequestMapping("/lunaerp/finance/transfer")
@Validated
public class TransferController {

    @Resource
    private TransferService transferService;

    @PostMapping("/create")
    @Operation(summary = "登记账户间转账")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:transfer')")
    public CommonResult<Long> createTransfer(@Valid @RequestBody TransferCreateReqVO createReqVO) {
        return success(transferService.createTransfer(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得账户间转账单详情")
    @Parameter(name = "id", description = "转账单编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:finance:account:transfer', 'lunaerp:finance:account:query')")
    public CommonResult<TransferRespVO> getTransfer(@RequestParam("id") Long id) {
        return success(transferService.getTransfer(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得账户间转账单分页")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:finance:account:transfer', 'lunaerp:finance:account:query')")
    public CommonResult<PageResult<TransferRespVO>> getTransferPage(@Valid TransferPageReqVO pageReqVO) {
        return success(transferService.getTransferPage(pageReqVO));
    }

}
