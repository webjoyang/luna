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

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 财务管理 - 供应商对账 控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 供应商对账")
@RestController
@RequestMapping("/lunaerp/finance/supplier-recon")
@Validated
public class SupplierReconController {

    @Resource
    private SupplierReconService supplierReconService;

    @PostMapping("/preview")
    @Operation(summary = "取数预览（候选行不落库）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:create')")
    public CommonResult<List<SupplierReconSourceItemRespVO>> preview(@Valid @RequestBody SupplierReconPreviewReqVO reqVO) {
        return success(supplierReconService.preview(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建供应商对账单（草稿）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:create')")
    public CommonResult<Long> createSupplierRecon(@Valid @RequestBody SupplierReconCreateReqVO createReqVO) {
        return success(supplierReconService.createSupplierRecon(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商对账单（仅草稿）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:update')")
    public CommonResult<Boolean> updateSupplierRecon(@Valid @RequestBody SupplierReconUpdateReqVO updateReqVO) {
        supplierReconService.updateSupplierRecon(updateReqVO);
        return success(true);
    }

    @PutMapping("/re-fetch")
    @Operation(summary = "重新取数（仅草稿，替换全部 PROCESS/PURCHASE 行，保留 ADJUST 行）")
    @Parameter(name = "id", description = "对账单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:update')")
    public CommonResult<Boolean> reFetchSupplierRecon(@RequestParam("id") Long id) {
        supplierReconService.reFetchSupplierRecon(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商对账单（仅草稿，软删进回收站）")
    @Parameter(name = "id", description = "对账单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:delete')")
    public CommonResult<Boolean> deleteSupplierRecon(@RequestParam("id") Long id) {
        supplierReconService.deleteSupplierRecon(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商对账单详情（内嵌 items + 只读实时聚合已申请金额）")
    @Parameter(name = "id", description = "对账单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:query')")
    public CommonResult<SupplierReconRespVO> getSupplierRecon(@RequestParam("id") Long id) {
        return success(supplierReconService.getSupplierRecon(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商对账单分页（实时聚合各单已申请金额）")
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:query')")
    public CommonResult<PageResult<SupplierReconRespVO>> getSupplierReconPage(@Valid SupplierReconPageReqVO pageReqVO) {
        return success(supplierReconService.getSupplierReconPage(pageReqVO));
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认供应商对账单（仅草稿，冻结快照）")
    @Parameter(name = "id", description = "对账单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:confirm')")
    public CommonResult<Boolean> confirmSupplierRecon(@RequestParam("id") Long id) {
        supplierReconService.confirmSupplierRecon(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "作废供应商对账单（仅已确认，释放指纹）")
    @Parameter(name = "id", description = "对账单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:confirm')")
    public CommonResult<Boolean> cancelSupplierRecon(@RequestParam("id") Long id) {
        supplierReconService.cancelSupplierRecon(id);
        return success(true);
    }

    @GetMapping({"/print-data", "/{id}/print-data"})
    @Operation(summary = "获得供应商对账单打印数据（豁免金额脱敏）")
    @Parameter(name = "id", description = "对账单编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:finance:supplierrecon:print')")
    public CommonResult<SupplierReconPrintRespVO> getSupplierReconPrintData(
            @PathVariable(value = "id", required = false) Long pathId,
            @RequestParam(value = "id", required = false) Long queryId) {
        Long id = pathId != null ? pathId : queryId;
        return success(supplierReconService.getSupplierReconPrintData(id));
    }

}
