package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import com.luna.lunasrv.module.lunaerp.service.stock.stockin.StockInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-检验单")
@RestController
@RequestMapping("/lunaerp/production/inspection-order")
@Validated
public class InspectionOrderController {

    @Resource
    private InspectionOrderService inspectionOrderService;

    @Resource
    private StockInService stockInService;

    @GetMapping("/page")
    @Operation(summary = "获得检验单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:query')")
    public CommonResult<PageResult<InspectionOrderRespVO>> getInspectionOrderPage(@Validated InspectionOrderPageReqVO pageReqVO) {
        return success(inspectionOrderService.getInspectionOrderPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得检验单")
    @Parameter(name = "id", description = "检验单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:query')")
    public CommonResult<InspectionOrderRespVO> getInspectionOrder(@RequestParam("id") Long id) {
        return success(inspectionOrderService.getInspectionOrder(id));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个检验单号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:query')")
    public CommonResult<String> getNextInspectionOrderNo() {
        return success(inspectionOrderService.getNextInspectionOrderNo());
    }

    @PostMapping("/create")
    @Operation(summary = "创建检验单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:create')")
    public CommonResult<Long> createInspectionOrder(@Valid @RequestBody InspectionOrderSaveReqVO createReqVO) {
        return success(inspectionOrderService.createInspectionOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改检验单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:update')")
    public CommonResult<Boolean> updateInspectionOrder(@Valid @RequestBody InspectionOrderSaveReqVO updateReqVO) {
        inspectionOrderService.updateInspectionOrder(updateReqVO);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成检验单")
    @Parameter(name = "id", description = "检验单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:update')")
    public CommonResult<Boolean> completeInspectionOrder(@RequestParam("id") Long id) {
        inspectionOrderService.completeInspectionOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消检验单")
    @Parameter(name = "id", description = "检验单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:update')")
    public CommonResult<Boolean> cancelInspectionOrder(@RequestParam("id") Long id) {
        inspectionOrderService.cancelInspectionOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除检验单")
    @Parameter(name = "id", description = "检验单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:delete')")
    public CommonResult<Boolean> deleteInspectionOrder(@RequestParam("id") Long id) {
        inspectionOrderService.deleteInspectionOrder(id);
        return success(true);
    }

    @PostMapping("/generate-stock-in")
    @Operation(summary = "检验单生成胚布入库单草稿")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:create')")
    public CommonResult<InspectionOrderGenerateStockInRespVO> generateStockIn(
            @Valid @RequestBody InspectionOrderGenerateStockInReqVO reqVO) {
        return success(stockInService.createStockInFromInspection(reqVO));
    }

}

