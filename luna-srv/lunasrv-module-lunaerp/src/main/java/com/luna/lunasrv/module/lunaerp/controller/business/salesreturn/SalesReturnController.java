package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.*;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.SalesReturnService;
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

@Tag(name = "业务管理 - 客户销售退货")
@RestController
@RequestMapping("/lunaerp/business/sales-return")
@Validated
public class SalesReturnController {

    @Resource
    private SalesReturnService salesReturnService;

    @GetMapping("/shipment-candidates")
    @Operation(summary = "获得可选发货单候选列表（仅 COMPLETED 且含实时可退量>0 行）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:query') or @ss.hasPermission('lunaerp:business:sales-return:create')")
    public CommonResult<List<SalesReturnCandidateRespVO>> getShipmentCandidates(
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "shipmentNo", required = false) String shipmentNo) {
        return success(salesReturnService.getShipmentCandidates(customerId, shipmentNo));
    }

    @GetMapping("/shipment-detail")
    @Operation(summary = "发起退货取数（获得发货单快照、明细行带可退量及卷清单）")
    @Parameter(name = "shipmentId", description = "发货单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:query') or @ss.hasPermission('lunaerp:business:sales-return:create')")
    public CommonResult<SalesReturnShipmentDetailRespVO> getShipmentDetail(@RequestParam("shipmentId") Long shipmentId) {
        return success(salesReturnService.getShipmentDetail(shipmentId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建销售退货草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:create')")
    public CommonResult<Long> createSalesReturn(@Valid @RequestBody SalesReturnCreateReqVO createReqVO) {
        return success(salesReturnService.createSalesReturn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改销售退货草稿（仅草稿态可编辑，行/卷两级差异替换保存）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:update')")
    public CommonResult<Boolean> updateSalesReturn(@Valid @RequestBody SalesReturnUpdateReqVO updateReqVO) {
        salesReturnService.updateSalesReturn(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售退货草稿（仅草稿态可删除，级联软删行与卷）")
    @Parameter(name = "id", description = "退货单ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:delete')")
    public CommonResult<Boolean> deleteSalesReturn(@RequestParam("id") Long id) {
        salesReturnService.deleteSalesReturn(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售退货单详情")
    @Parameter(name = "id", description = "退货单ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:query')")
    public CommonResult<SalesReturnRespVO> getSalesReturn(@RequestParam("id") Long id) {
        return success(salesReturnService.getSalesReturn(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售退货单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:query')")
    public CommonResult<PageResult<SalesReturnRespVO>> getSalesReturnPage(@Validated SalesReturnPageReqVO pageReqVO) {
        return success(salesReturnService.getSalesReturnPage(pageReqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交销售退货审批")
    @Parameter(name = "id", description = "退货单ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:submit')")
    public CommonResult<Boolean> submitSalesReturn(@RequestParam("id") Long id) {
        salesReturnService.submitSalesReturn(id);
        return success(true);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回销售退货审批")
    @Parameter(name = "id", description = "退货单ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:submit')")
    public CommonResult<Boolean> withdrawSalesReturn(@RequestParam("id") Long id) {
        salesReturnService.withdrawSalesReturn(id);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "作废销售退货单（草稿分支与已审批分支）")
    @Parameter(name = "id", description = "退货单ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:cancel')")
    public CommonResult<Boolean> cancelSalesReturn(
            @RequestParam("id") Long id,
            @Valid @RequestBody SalesReturnCancelReqVO reqVO) {
        salesReturnService.cancelSalesReturn(id, reqVO.getCancelReason());
        return success(true);
    }

    @PostMapping("/receive")
    @Operation(summary = "到货质检（逐卷判定 RESTOCK/SCRAP 并入库/审核/全废取消）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-return:receive')")
    public CommonResult<Boolean> receiveSalesReturn(@Valid @RequestBody SalesReturnReceiveReqVO reqVO) {
        salesReturnService.receiveSalesReturn(reqVO);
        return success(true);
    }

}
