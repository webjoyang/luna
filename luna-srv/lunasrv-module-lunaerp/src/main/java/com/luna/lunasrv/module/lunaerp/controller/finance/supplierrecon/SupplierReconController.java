package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon.SupplierReconService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 供应商对账")
@RestController
@RequestMapping("/lunaerp/finance/supplier-recon")
@Validated
public class SupplierReconController {

    @Resource
    private SupplierReconService supplierReconService;

    @GetMapping("/page")
    @Operation(summary = "获得供应商对账单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:query')")
    public CommonResult<PageResult<SupplierReconRespVO>> getReconPage(@Valid SupplierReconPageReqVO pageReqVO) {
        return success(supplierReconService.getReconPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商对账单详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:query')")
    public CommonResult<SupplierReconRespVO> getRecon(@RequestParam("id") Long id) {
        return success(supplierReconService.getRecon(id));
    }

    @PostMapping("/preview")
    @Operation(summary = "取数预览候选明细")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:query')")
    public CommonResult<SupplierReconCandidatesRespVO> previewRecon(@Valid @RequestBody SupplierReconPreviewReqVO previewReqVO) {
        return success(supplierReconService.previewRecon(previewReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建供应商对账单")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:create')")
    public CommonResult<Long> createRecon(@Valid @RequestBody SupplierReconSaveReqVO createReqVO) {
        return success(supplierReconService.createRecon(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新草稿供应商对账单")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:update')")
    public CommonResult<Boolean> updateRecon(@Valid @RequestBody SupplierReconSaveReqVO updateReqVO) {
        supplierReconService.updateRecon(updateReqVO);
        return success(true);
    }

    @PutMapping("/re-fetch")
    @Operation(summary = "重取草稿供应商对账单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:update')")
    public CommonResult<Boolean> reFetchRecon(@RequestParam("id") Long id) {
        supplierReconService.reFetchRecon(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除草稿供应商对账单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:delete')")
    public CommonResult<Boolean> deleteRecon(@RequestParam("id") Long id) {
        supplierReconService.deleteRecon(id);
        return success(true);
    }

}
