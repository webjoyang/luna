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

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS;

import com.luna.lunasrv.framework.common.util.validation.ValidationUtils;
import jakarta.validation.Validator;

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

    @Resource
    private Validator validator;

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

    @PutMapping("/send")
    @Operation(summary = "发送客户对账单（DRAFT -> SENT，条件更新防并发）")
    @Parameter(name = "id", description = "对账单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:send')")
    public CommonResult<Boolean> sendCustomerStatement(@RequestParam("id") Long id) {
        customerStatementService.sendCustomerStatement(id);
        return success(true);
    }

    @PutMapping("/void")
    @Operation(summary = "作废客户对账单（非终态可作废，释放行占用，不删分录）")
    @Parameter(name = "id", description = "对账单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:void')")
    public CommonResult<Boolean> voidCustomerStatement(@RequestParam("id") Long id) {
        customerStatementService.voidCustomerStatement(id);
        return success(true);
    }

    @PutMapping("/confirm-on-behalf")
    @Operation(summary = "代客确认客户对账单（SENT -> CONFIRMED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:confirm-on-behalf')")
    public CommonResult<Boolean> confirmOnBehalf(
            @RequestBody(required = false) CustomerStatementConfirmOnBehalfReqVO bodyReqVO,
            @RequestParam(value = "id", required = false) Long idParam,
            @RequestParam(value = "confirmMethod", required = false) String confirmMethodParam,
            @RequestParam(value = "remark", required = false) String remarkParam) {
        CustomerStatementConfirmOnBehalfReqVO reqVO = bodyReqVO != null ? bodyReqVO : new CustomerStatementConfirmOnBehalfReqVO();
        if (reqVO.getId() == null && idParam != null) {
            reqVO.setId(idParam);
        }
        if (reqVO.getConfirmMethod() == null && confirmMethodParam != null) {
            reqVO.setConfirmMethod(confirmMethodParam);
        }
        if (reqVO.getRemark() == null && remarkParam != null) {
            reqVO.setRemark(remarkParam);
        }
        ValidationUtils.validate(validator, reqVO);
        customerStatementService.confirmOnBehalf(reqVO);
        return success(true);
    }

    @PutMapping("/reject-reply")
    @Operation(summary = "回复客户对账单异议并重置为草稿（REJECTED -> DRAFT）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:update')")
    public CommonResult<Boolean> rejectReplyCustomerStatement(
            @RequestBody(required = false) CustomerStatementRejectReplyReqVO bodyReqVO,
            @RequestParam(value = "id", required = false) Long idParam,
            @RequestParam(value = "replyRemark", required = false) String replyRemarkParam) {
        CustomerStatementRejectReplyReqVO reqVO = bodyReqVO != null ? bodyReqVO : new CustomerStatementRejectReplyReqVO();
        if (reqVO.getId() == null && idParam != null) {
            reqVO.setId(idParam);
        }
        if (reqVO.getReplyRemark() == null && replyRemarkParam != null) {
            reqVO.setReplyRemark(replyRemarkParam);
        }
        ValidationUtils.validate(validator, reqVO);
        customerStatementService.rejectReplyCustomerStatement(reqVO);
        return success(true);
    }

    @GetMapping({"/print-data", "/{id}/print-data"})
    @Operation(summary = "获得客户对账单打印数据（豁免金额脱敏）")
    @Parameter(name = "id", description = "对账单ID", example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:customerrecon:print')")
    public CommonResult<CustomerStatementPrintRespVO> getCustomerStatementPrintData(
            @RequestParam(value = "id", required = false) Long idParam,
            @PathVariable(value = "id", required = false) Long idPath) {
        Long id = idParam != null ? idParam : idPath;
        if (id == null) {
            throw exception(STATEMENT_NOT_EXISTS);
        }
        return success(customerStatementService.getCustomerStatementPrintData(id));
    }

}
