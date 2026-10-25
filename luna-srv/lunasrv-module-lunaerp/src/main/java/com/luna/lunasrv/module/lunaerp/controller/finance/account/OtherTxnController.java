package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.account.OtherTxnService;
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
 * 财务管理 - 其他收支单控制器（QT 单）
 *
 * <p>两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。不提供 update/delete 端点。</p>
 *
 * @author luna
 */
@Tag(name = "财务管理 - 其他收支单")
@RestController
@RequestMapping("/lunaerp/finance/other-txn")
@Validated
public class OtherTxnController {

    @Resource
    private OtherTxnService otherTxnService;

    @PostMapping("/create")
    @Operation(summary = "登记其他收支单")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:account:other-txn')")
    public CommonResult<Long> createOtherTxn(@Valid @RequestBody OtherTxnCreateReqVO createReqVO) {
        return success(otherTxnService.createOtherTxn(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得其他收支单详情")
    @Parameter(name = "id", description = "其他收支单编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:finance:account:other-txn', 'lunaerp:finance:account:query')")
    public CommonResult<OtherTxnRespVO> getOtherTxn(@RequestParam("id") Long id) {
        return success(otherTxnService.getOtherTxn(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得其他收支单分页")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:finance:account:other-txn', 'lunaerp:finance:account:query')")
    public CommonResult<PageResult<OtherTxnRespVO>> getOtherTxnPage(@Valid OtherTxnPageReqVO pageReqVO) {
        return success(otherTxnService.getOtherTxnPage(pageReqVO));
    }

}
