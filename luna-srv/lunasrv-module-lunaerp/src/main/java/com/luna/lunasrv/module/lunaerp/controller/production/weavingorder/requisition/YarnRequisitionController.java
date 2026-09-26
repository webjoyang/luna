package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.YarnRequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-织造领纱单")
@RestController
@RequestMapping("/lunaerp/production/weaving-order-requisition")
@Validated
public class YarnRequisitionController {

    @Resource
    private YarnRequisitionService yarnRequisitionService;

    @PostMapping("/create")
    @Operation(summary = "创建领纱单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:create') or @ss.hasPermission('lunaerp:production:weaving-order:update') or @ss.hasPermission('lunaerp:production:weaving-order:create')")
    public CommonResult<Long> createYarnRequisition(@Valid @RequestBody YarnRequisitionSaveReqVO createReqVO) {
        return success(yarnRequisitionService.createYarnRequisition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新领纱单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> updateYarnRequisition(@Valid @RequestBody YarnRequisitionSaveReqVO updateReqVO) {
        yarnRequisitionService.updateYarnRequisition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除领纱单（仅草稿态可删）")
    @Parameter(name = "id", description = "领纱单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:delete') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> deleteYarnRequisition(@RequestParam("id") Long id) {
        yarnRequisitionService.deleteYarnRequisition(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得领纱单详情（含明细）")
    @Parameter(name = "id", description = "领纱单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<YarnRequisitionRespVO> getYarnRequisition(@RequestParam("id") Long id) {
        return success(yarnRequisitionService.getYarnRequisition(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得领纱单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<PageResult<YarnRequisitionRespVO>> getYarnRequisitionPage(@Validated YarnRequisitionPageReqVO pageReqVO) {
        return success(yarnRequisitionService.getYarnRequisitionPage(pageReqVO));
    }

    @GetMapping("/get-next-code")
    @Operation(summary = "预览下一个领纱单号（只读、不占号）")
    @Parameter(name = "weavingOrderId", description = "织造单ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<String> getNextYarnRequisitionCode(@RequestParam("weavingOrderId") Long weavingOrderId) {
        return success(yarnRequisitionService.getNextYarnRequisitionCode(weavingOrderId));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交领纱单（DRAFT → ACTIVE）")
    @Parameter(name = "id", description = "领纱单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:submit') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> submitYarnRequisition(@RequestParam("id") Long id) {
        yarnRequisitionService.submitYarnRequisition(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭领纱单（转 CLOSED 手动终态）")
    @Parameter(name = "id", description = "领纱单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:close') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> closeYarnRequisition(@RequestParam("id") Long id) {
        yarnRequisitionService.closeYarnRequisition(id);
        return success(true);
    }

    @PostMapping("/calculate")
    @Operation(summary = "领纱用量 BOM 权威测算接口")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-requisition:create') or @ss.hasPermission('lunaerp:production:weaving-order-requisition:update') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<YarnRequisitionCalculateRespVO> calculate(@Valid @RequestBody YarnRequisitionCalculateReqVO calculateReqVO) {
        return success(yarnRequisitionService.calculate(calculateReqVO));
    }

}
