package com.luna.lunasrv.module.lunaerp.controller.finance.payroll;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.PayrollDefectService;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.PayrollService;
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
 * 管理后台 - 员工工资单
 *
 * @author luna
 */
@Tag(name = "管理后台 - 员工工资单")
@RestController
@RequestMapping("/lunaerp/finance/payroll")
@Validated
public class PayrollController {

    @Resource
    private PayrollService payrollService;

    @Resource
    private PayrollDefectService payrollDefectService;

    @PostMapping("/create")
    @Operation(summary = "创建员工工资单草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:create')")
    public CommonResult<Long> createPayroll(@Valid @RequestBody PayrollCreateReqVO createReqVO) {
        return success(payrollService.createPayroll(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工工资单（整体替换员工行与款项行）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:update')")
    public CommonResult<Boolean> updatePayroll(@Valid @RequestBody PayrollUpdateReqVO updateReqVO) {
        payrollService.updatePayroll(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工工资单草稿（进回收站）")
    @Parameter(name = "id", description = "工资单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:delete')")
    public CommonResult<Boolean> deletePayroll(@RequestParam("id") Long id) {
        payrollService.deletePayroll(id);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交员工工资单审批")
    @Parameter(name = "id", description = "工资单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:submit')")
    public CommonResult<Boolean> submitPayroll(@RequestParam("id") Long id) {
        payrollService.submitPayroll(id);
        return success(true);
    }

    @PostMapping("/cancel-approve")
    @Operation(summary = "撤回员工工资单审批")
    @Parameter(name = "id", description = "工资单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:submit')")
    public CommonResult<Boolean> cancelApprove(@RequestParam("id") Long id) {
        payrollService.cancelApprove(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "作废员工工资单（仅 APPROVED 且零发放）")
    @Parameter(name = "id", description = "工资单ID（可选通过 query param 或 request body 传入）", example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:cancel')")
    public CommonResult<Boolean> cancelPayroll(@RequestParam(value = "id", required = false) Long paramId,
                                               @Valid @RequestBody PayrollCancelReqVO reqVO) {
        Long id = paramId != null ? paramId : reqVO.getId();
        payrollService.cancelPayroll(id, reqVO.getCancelReason());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取员工工资单详情")
    @Parameter(name = "id", description = "工资单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:query')")
    public CommonResult<PayrollRespVO> getPayroll(@RequestParam("id") Long id) {
        return success(payrollService.getPayroll(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询员工工资单")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:query')")
    public CommonResult<PageResult<PayrollRespVO>> getPayrollPage(@Valid PayrollPageReqVO pageReqVO) {
        return success(payrollService.getPayrollPage(pageReqVO));
    }

    @GetMapping("/print-data")
    @Operation(summary = "获取员工工资表 A4 套打数据（仅 APPROVED）")
    @Parameter(name = "id", description = "工资单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:print')")
    public CommonResult<PayrollPrintRespVO> getPayrollPrintData(@RequestParam("id") Long id) {
        return success(payrollService.getPayrollPrintData(id));
    }

    @GetMapping("/defect-preview")
    @Operation(summary = "获得工资单次品扣款预览数据")
    @Parameter(name = "period", description = "工资期间（YYYY-MM）", required = true, example = "2026-10")
    @Parameter(name = "payrollId", description = "当前工资单ID（可选，排除自身占用的指纹）", example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:import-defect')")
    public CommonResult<PayrollDefectPreviewRespVO> getDefectPreview(
            @RequestParam("period") String period,
            @RequestParam(value = "payrollId", required = false) Long payrollId) {
        return success(payrollDefectService.getDefectPreview(period, payrollId));
    }

    @PostMapping("/payment/register")
    @Operation(summary = "登记工资单发放批次（出纳执行·扣款并过账）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:payroll:pay')")
    public CommonResult<Long> registerPayrollPayment(@Valid @RequestBody PayrollPaymentRegisterReqVO reqVO) {
        return success(payrollService.registerPayrollPayment(reqVO));
    }

    @GetMapping("/payment/page")
    @Operation(summary = "分页查询工资单发放批次列表")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:finance:payroll:query', 'lunaerp:finance:payroll:pay')")
    public CommonResult<PageResult<PayrollPaymentRespVO>> getPayrollPaymentPage(@Valid PayrollPaymentPageReqVO pageReqVO) {
        return success(payrollService.getPayrollPaymentPage(pageReqVO));
    }

}
