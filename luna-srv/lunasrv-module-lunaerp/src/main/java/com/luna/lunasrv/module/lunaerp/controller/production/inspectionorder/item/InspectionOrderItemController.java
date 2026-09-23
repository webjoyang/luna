package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.item.InspectionOrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-检验单录检明细")
@RestController
@RequestMapping("/lunaerp/production/inspection-order-item")
@Validated
public class InspectionOrderItemController {

    @Resource
    private InspectionOrderItemService inspectionOrderItemService;

    @PostMapping("/create")
    @Operation(summary = "创建录检明细")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order-item:create')")
    public CommonResult<Long> createInspectionOrderItem(@Valid @RequestBody InspectionOrderItemSaveReqVO createReqVO) {
        return success(inspectionOrderItemService.createInspectionOrderItem(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得录检明细分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order-item:query')")
    public CommonResult<PageResult<InspectionOrderItemRespVO>> getInspectionOrderItemPage(
            @Validated InspectionOrderItemPageReqVO pageReqVO) {
        return success(inspectionOrderItemService.getInspectionOrderItemPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得录检明细")
    @Parameter(name = "id", description = "明细ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order-item:query')")
    public CommonResult<InspectionOrderItemRespVO> getInspectionOrderItem(@RequestParam("id") Long id) {
        return success(inspectionOrderItemService.getInspectionOrderItem(id));
    }

    @PutMapping("/update")
    @Operation(summary = "修改录检明细")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order-item:update')")
    public CommonResult<Boolean> updateInspectionOrderItem(@Valid @RequestBody InspectionOrderItemSaveReqVO updateReqVO) {
        inspectionOrderItemService.updateInspectionOrderItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除录检明细")
    @Parameter(name = "id", description = "明细ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order-item:delete')")
    public CommonResult<Boolean> deleteInspectionOrderItem(@RequestParam("id") Long id) {
        inspectionOrderItemService.deleteInspectionOrderItem(id);
        return success(true);
    }

}
