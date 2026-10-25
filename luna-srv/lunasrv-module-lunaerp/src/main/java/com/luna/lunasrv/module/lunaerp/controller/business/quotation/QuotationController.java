package com.luna.lunasrv.module.lunaerp.controller.business.quotation;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo.*;
import com.luna.lunasrv.module.lunaerp.service.business.quotation.QuotationService;
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
 * 业务管理 - 报价单 Controller
 *
 * @author luna
 */
@Tag(name = "业务管理 - 报价单")
@RestController
@RequestMapping("/lunaerp/business/quotation")
@Validated
public class QuotationController {

    @Resource
    private QuotationService quotationService;

    @PostMapping("/create")
    @Operation(summary = "创建报价单")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:create')")
    public CommonResult<Long> createQuotation(@Valid @RequestBody QuotationSaveReqVO createReqVO) {
        return success(quotationService.createQuotation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报价单（仅草稿）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:update')")
    public CommonResult<Boolean> updateQuotation(@Valid @RequestBody QuotationSaveReqVO updateReqVO) {
        quotationService.updateQuotation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报价单（仅草稿）")
    @Parameter(name = "id", description = "报价单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:delete')")
    public CommonResult<Boolean> deleteQuotation(@RequestParam("id") Long id) {
        quotationService.deleteQuotation(id);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认报价（DRAFT -> CONFIRMED）")
    @Parameter(name = "id", description = "报价单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:update')")
    public CommonResult<Boolean> confirmQuotation(@RequestParam("id") Long id) {
        quotationService.confirmQuotation(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消报价（CONFIRMED -> CANCELLED）")
    @Parameter(name = "id", description = "报价单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:update')")
    public CommonResult<Boolean> cancelQuotation(@RequestParam("id") Long id) {
        quotationService.cancelQuotation(id);
        return success(true);
    }

    @RequestMapping(value = "/revise", method = {RequestMethod.POST, RequestMethod.PUT})
    @Operation(summary = "修订报价单（CONFIRMED -> SUPERSEDED，生成新版本草稿）")
    @Parameter(name = "id", description = "报价单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:update')")
    public CommonResult<Long> reviseQuotation(@RequestParam("id") Long id) {
        return success(quotationService.reviseQuotation(id));
    }

    @GetMapping("/get")
    @Operation(summary = "获得报价单详情")
    @Parameter(name = "id", description = "报价单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:query')")
    public CommonResult<QuotationRespVO> getQuotation(@RequestParam("id") Long id) {
        return success(quotationService.getQuotation(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报价单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:query')")
    public CommonResult<PageResult<QuotationRespVO>> getQuotationPage(@Valid QuotationPageReqVO pageReqVO) {
        return success(quotationService.getQuotationPage(pageReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个自动报价单号（只读不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:create')")
    public CommonResult<String> getNextQuotationNo() {
        return success(quotationService.getNextQuotationNo());
    }

    @GetMapping("/versions")
    @Operation(summary = "查询指定单号的历史版本列表")
    @Parameter(name = "quotationNo", description = "报价单号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:query')")
    public CommonResult<List<QuotationRespVO>> getQuotationVersions(@RequestParam("quotationNo") String quotationNo) {
        return success(quotationService.getQuotationVersions(quotationNo));
    }

    @GetMapping("/contract-prefill")
    @Operation(summary = "获取报价单转合同预填数据")
    @Parameter(name = "id", description = "报价单ID", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:query')")
    public CommonResult<QuotationContractPrefillRespVO> getContractPrefill(@RequestParam("id") Long id) {
        return success(quotationService.getContractPrefill(id));
    }

    @GetMapping("/print-data")
    @Operation(summary = "获取报价单打印数据")
    @Parameter(name = "id", description = "报价单ID", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:quotation:print')")
    public CommonResult<QuotationPrintRespVO> getPrintData(@RequestParam("id") Long id) {
        return success(quotationService.getPrintData(id));
    }

}
