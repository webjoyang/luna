package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item.YarnRequisitionItemService;
import io.swagger.v3.oas.annotations.Operation;

import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-领纱明细（独立子资源）")
@RestController
@RequestMapping("/lunaerp/production/weaving-order-requisition-item")
@Validated
public class YarnRequisitionItemController {

    @Resource
    private YarnRequisitionItemService yarnRequisitionItemService;

    @PostMapping("/create")
    @Operation(summary = "创建领纱明细")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:create') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:update') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:create')")
    public CommonResult<Long> createYarnRequisitionItem(@Valid @RequestBody YarnRequisitionItemSaveReqVO createReqVO) {
        return success(yarnRequisitionItemService.createYarnRequisitionItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改领纱明细")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:update') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:update')")
    public CommonResult<Boolean> updateYarnRequisitionItem(@Valid @RequestBody YarnRequisitionItemSaveReqVO updateReqVO) {
        yarnRequisitionItemService.updateYarnRequisitionItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除领纱明细")
    @Parameter(name = "id", description = "明细ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:delete') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:update')")
    public CommonResult<Boolean> deleteYarnRequisitionItem(@RequestParam("id") Long id) {
        yarnRequisitionItemService.deleteYarnRequisitionItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得领纱明细详情")
    @Parameter(name = "id", description = "明细ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:query') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:query')")
    public CommonResult<YarnRequisitionItemRespVO> getYarnRequisitionItem(@RequestParam("id") Long id) {
        return success(yarnRequisitionItemService.getYarnRequisitionItem(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得领纱明细分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:query') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:query')")
    public CommonResult<PageResult<YarnRequisitionItemRespVO>> getYarnRequisitionItemPage(@Validated YarnRequisitionItemPageReqVO pageReqVO) {
        return success(yarnRequisitionItemService.getYarnRequisitionItemPage(pageReqVO));
    }

    @PostMapping("/dispatch")
    @Operation(summary = "领纱明细发纱（同事务 YARN_OUT + dispatchedQty + 状态推导）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:dispatch') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> dispatchYarn(@Valid @RequestBody YarnRequisitionDispatchReqVO dispatchReqVO) {
        yarnRequisitionItemService.dispatchYarn(dispatchReqVO);
        return success(true);
    }

    @PostMapping("/reverse-dispatch")
    @Operation(summary = "撤销发纱（按发纱记录红冲 YARN_OUT_REVERSAL + 回减 dispatchedQty + 状态回退）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:dispatch') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> reverseDispatch(@Valid @RequestBody YarnRequisitionDispatchReversalReqVO reversalReqVO) {
        yarnRequisitionItemService.reverseDispatch(reversalReqVO);
        return success(true);
    }

    @GetMapping("/list-dispatch-records")
    @Operation(summary = "获取指定领纱明细的发纱记录列表")
    @Parameter(name = "itemId", description = "领纱明细ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:query') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<List<YarnDispatchRecordRespVO>> getDispatchRecordList(@RequestParam("itemId") Long itemId) {
        return success(yarnRequisitionItemService.getDispatchRecordList(itemId));
    }

    @GetMapping("/get-yarn-balance")
    @Operation(summary = "获取指定仓库下某纱线规格的可用库存余额")
    @Parameter(name = "warehouseId", description = "仓库ID", required = true, example = "1")
    @Parameter(name = "yarnSpecId", description = "纱线规格ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition-item:query') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:query') or @ss.hasPermission('lunaerp:stock:inventory-balance:query')")
    public CommonResult<BigDecimal> getYarnBalance(@RequestParam("warehouseId") Long warehouseId,
                                                   @RequestParam("yarnSpecId") Long yarnSpecId) {
        return success(yarnRequisitionItemService.getYarnBalance(warehouseId, yarnSpecId));
    }

}
