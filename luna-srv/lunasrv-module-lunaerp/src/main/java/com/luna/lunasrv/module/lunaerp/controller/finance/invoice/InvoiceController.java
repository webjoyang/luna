package com.luna.lunasrv.module.lunaerp.controller.finance.invoice;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceService;
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
 * 财务管理 - 发票申请 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 发票申请")
@RestController
@RequestMapping("/lunaerp/finance/invoice")
@Validated
public class InvoiceController {

    @Resource
    private InvoiceService invoiceService;

    @GetMapping("/page")
    @Operation(summary = "获得发票申请分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:query')")
    public CommonResult<PageResult<InvoiceRespVO>> getInvoicePage(@Valid InvoicePageReqVO pageReqVO) {
        return success(invoiceService.getInvoicePage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得发票申请详情")
    @Parameter(name = "id", description = "发票申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:query')")
    public CommonResult<InvoiceRespVO> getInvoice(@RequestParam("id") Long id) {
        return success(invoiceService.getInvoice(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建发票申请（草稿）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:create')")
    public CommonResult<Long> createInvoice(@Valid @RequestBody InvoiceSaveReqVO createReqVO) {
        return success(invoiceService.createInvoice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新发票申请（草稿/已驳回状态允许修改）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:update')")
    public CommonResult<Boolean> updateInvoice(@Valid @RequestBody InvoiceSaveReqVO updateReqVO) {
        invoiceService.updateInvoice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发票申请（仅草稿状态允许删除）")
    @Parameter(name = "id", description = "发票申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:delete')")
    public CommonResult<Boolean> deleteInvoice(@RequestParam("id") Long id) {
        invoiceService.deleteInvoice(id);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交发票申请（DRAFT/REJECTED -> SUBMITTED）")
    @Parameter(name = "id", description = "发票申请编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:update')")
    public CommonResult<Boolean> submitInvoice(@RequestParam("id") Long id) {
        invoiceService.submitInvoice(id);
        return success(true);
    }

    @PutMapping("/issue")
    @Operation(summary = "发票登记回填（SUBMITTED -> ISSUED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:issue')")
    public CommonResult<Boolean> issueInvoice(@Valid @RequestBody(required = false) InvoiceIssueReqVO reqVO,
                                              @RequestParam(value = "id", required = false) Long idParam) {
        if (reqVO == null) {
            reqVO = new InvoiceIssueReqVO();
        }
        if (reqVO.getId() == null && idParam != null) {
            reqVO.setId(idParam);
        }
        invoiceService.issueInvoice(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回发票申请（SUBMITTED -> REJECTED，释放发货单开票占用）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:issue')")
    public CommonResult<Boolean> rejectInvoice(@RequestParam("id") Long id,
                                               @RequestParam(value = "reason", required = false) String reason) {
        invoiceService.rejectInvoice(id, reason);
        return success(true);
    }

    @GetMapping("/candidate-shipments")
    @Operation(summary = "获得客户可开票发货单候选列表")
    @Parameter(name = "customerId", description = "客户编号", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:invoice:query')")
    public CommonResult<List<InvoiceCandidateShipmentRespVO>> getCandidateShipments(@RequestParam("customerId") Long customerId) {
        return success(invoiceService.getCandidateShipments(customerId));
    }

}
