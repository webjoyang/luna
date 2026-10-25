package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.util.servlet.ServletUtils;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS;

/**
 * 客户门户 - 客户对账单免登控制器
 *
 * 提供客户门户免登录查看对账单、确认与驳回接口。
 *
 * @author luna
 */
@Tag(name = "客户门户 - 客户对账单")
@RestController
@RequestMapping("/app-api/lunaerp/portal/finance/statement")
@Validated
@PermitAll
public class CustomerStatementPortalController {

    @Resource
    private CustomerStatementService customerStatementService;

    @GetMapping("/list")
    @Operation(summary = "获得客户对账单列表（仅 SENT/CONFIRMED 状态）")
    @Parameter(name = "customerId", description = "客户ID（内部代客场景传参，门户免登由 PortalContext 注入）", required = false, example = "1001")
    public CommonResult<List<CustomerStatementRespVO>> getCustomerStatementList(
            @RequestParam(value = "customerId", required = false) Long customerId) {
        return success(customerStatementService.getPortalCustomerStatementList(customerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获得客户对账单详情（含全部明细行）")
    @Parameter(name = "id", description = "对账单ID", required = true, example = "1024")
    @Parameter(name = "customerId", description = "客户ID（内部代客场景传参，门户免登由 PortalContext 注入）", required = false, example = "1001")
    public CommonResult<CustomerStatementDetailRespVO> getCustomerStatement(
            @PathVariable("id") Long id,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        return success(customerStatementService.getPortalCustomerStatement(id, customerId));
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户对账单详情（支持按 id 或 no 查询）")
    @Parameters({
            @Parameter(name = "id", description = "对账单ID", example = "1024"),
            @Parameter(name = "no", description = "对账单号（二维码冻结契约）", example = "CS-20260927-001"),
            @Parameter(name = "statementNo", description = "对账单号别名", example = "CS-20260927-001"),
            @Parameter(name = "customerId", description = "客户ID", example = "1001")
    })
    public CommonResult<CustomerStatementDetailRespVO> getCustomerStatementByParam(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "no", required = false) String no,
            @RequestParam(value = "statementNo", required = false) String statementNo,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        String finalNo = StrUtil.isNotBlank(no) ? no.trim() : (StrUtil.isNotBlank(statementNo) ? statementNo.trim() : null);
        return success(customerStatementService.getPortalCustomerStatement(id, finalNo, customerId));
    }

    @PutMapping("/confirm")
    @Operation(summary = "在线确认客户对账单（SENT -> CONFIRMED）")
    public CommonResult<Boolean> confirmCustomerStatement(
            @Valid @RequestBody CustomerStatementPortalConfirmReqVO reqVO,
            HttpServletRequest request) {
        if (request != null) {
            String clientIp = ServletUtils.getClientIP(request);
            if (StrUtil.isNotBlank(clientIp)) {
                reqVO.setConfirmIp(clientIp);
            }
        }
        customerStatementService.confirmPortalCustomerStatement(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回客户对账单（SENT -> REJECTED）")
    public CommonResult<Boolean> rejectCustomerStatement(
            @Valid @RequestBody CustomerStatementPortalRejectReqVO reqVO) {
        customerStatementService.rejectPortalCustomerStatement(reqVO);
        return success(true);
    }

}
