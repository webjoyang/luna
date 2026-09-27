package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.WeavingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 织造单")
@RestController
@RequestMapping("/lunaerp/production/weaving-order")
@Validated
public class WeavingOrderController {

    @Resource
    private WeavingOrderService weavingOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建织造单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:create')")
    public CommonResult<WeavingOrderSaveRespVO> createWeavingOrder(@Valid @RequestBody WeavingOrderSaveReqVO createReqVO) {
        return success(weavingOrderService.createWeavingOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改织造单（仅待排产 DRAFT 态允许修改）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<WeavingOrderSaveRespVO> updateWeavingOrder(@Valid @RequestBody WeavingOrderSaveReqVO updateReqVO) {
        return success(weavingOrderService.updateWeavingOrder(updateReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除织造单（仅待排产 DRAFT 态允许删除，级联逻辑删除下属任务与布票）")
    @Parameter(name = "id", description = "织造单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:delete')")
    public CommonResult<Boolean> deleteWeavingOrder(@RequestParam("id") Long id) {
        weavingOrderService.deleteWeavingOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消织造单（手动终态）")
    @Parameter(name = "id", description = "织造单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:cancel')")
    public CommonResult<Boolean> cancelWeavingOrder(@RequestParam("id") Long id) {
        weavingOrderService.cancelWeavingOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得织造单详情")
    @Parameter(name = "id", description = "织造单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<WeavingOrderRespVO> getWeavingOrder(@RequestParam("id") Long id) {
        return success(weavingOrderService.getWeavingOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得织造单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<PageResult<WeavingOrderRespVO>> getWeavingOrderPage(@Validated WeavingOrderPageReqVO pageReqVO) {
        return success(weavingOrderService.getWeavingOrderPage(pageReqVO));
    }

    @GetMapping("/get-next-code")
    @Operation(summary = "预览下一个织造单号（只读、不占号）")
    @Parameter(name = "greigeOrderId", description = "关联自织胚布单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<String> getNextWeavingOrderCode(@RequestParam("greigeOrderId") Long greigeOrderId) {
        return success(weavingOrderService.getNextWeavingOrderCode(greigeOrderId));
    }

    @GetMapping("/get-statistics")
    @Operation(summary = "获得织造单统计数据（计划匹数、有效票数、作废数、完工数、完成率、各状态计数）")
    @Parameter(name = "id", description = "织造单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderStatsRespVO> getWeavingOrderStats(
            @RequestParam("id") Long id) {
        return success(weavingOrderService.getWeavingOrderStats(id));
    }

    @PostMapping("/handoff-inspection")
    @Operation(summary = "织造完工交接检验（find-or-create PIECE 模式检验单表头，幂等，ADR-0025）")
    @Parameter(name = "id", description = "织造单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order:handoff-inspection') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderHandoffInspectionRespVO> handoffInspection(
            @RequestParam("id") Long id) {
        return success(weavingOrderService.handoffInspection(id));
    }

}

