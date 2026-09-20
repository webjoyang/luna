package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.yarnorder.YarnOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 生产管理-纱线采购单
 *
 * @author luna
 */
@Tag(name = "生产管理-纱线采购单")
@RestController
@RequestMapping("/admin-api/lunaerp/production/yarn-order")
@Validated
public class YarnOrderController {

    @Resource
    private YarnOrderService yarnOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建纱线采购单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:create')")
    public CommonResult<Long> createYarnOrder(@Valid @RequestBody YarnOrderSaveReqVO createReqVO) {
        return success(yarnOrderService.createYarnOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新纱线采购单（仅草稿态可更新，明细整体替换）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:update')")
    public CommonResult<Boolean> updateYarnOrder(@Valid @RequestBody YarnOrderSaveReqVO updateReqVO) {
        yarnOrderService.updateYarnOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除纱线采购单（仅草稿态可删除，级联逻辑删除明细）")
    @Parameter(name = "id", description = "采购单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:delete')")
    public CommonResult<Boolean> deleteYarnOrder(@RequestParam("id") Long id) {
        yarnOrderService.deleteYarnOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得纱线采购单详情")
    @Parameter(name = "id", description = "采购单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:query')")
    public CommonResult<YarnOrderRespVO> getYarnOrder(@RequestParam("id") Long id) {
        return success(yarnOrderService.getYarnOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得纱线采购单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:query')")
    public CommonResult<PageResult<YarnOrderRespVO>> getYarnOrderPage(@Validated YarnOrderPageReqVO pageReqVO) {
        return success(yarnOrderService.getYarnOrderPage(pageReqVO));
    }

    @PutMapping("/effect")
    @Operation(summary = "下达采购单（DRAFT -> ACTIVE）")
    @Parameter(name = "id", description = "采购单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:update')")
    public CommonResult<Boolean> effectYarnOrder(@RequestParam("id") Long id) {
        yarnOrderService.effectYarnOrder(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成采购单（ACTIVE -> COMPLETED）")
    @Parameter(name = "id", description = "采购单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:update')")
    public CommonResult<Boolean> completeYarnOrder(@RequestParam("id") Long id) {
        yarnOrderService.completeYarnOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消采购单（ACTIVE -> CANCELLED）")
    @Parameter(name = "id", description = "采购单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:update')")
    public CommonResult<Boolean> cancelYarnOrder(@RequestParam("id") Long id) {
        yarnOrderService.cancelYarnOrder(id);
        return success(true);
    }

    @PostMapping("/calculate")
    @Operation(summary = "核心测算接口（根据勾选的胚布单及损耗率测算需求量）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:create')")
    public CommonResult<YarnOrderCalculateRespVO> calculateYarnOrder(@Valid @RequestBody YarnOrderCalculateReqVO calculateReqVO) {
        return success(yarnOrderService.calculateYarnOrder(calculateReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个采购单号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:query')")
    public CommonResult<String> getNextYarnOrderNo() {
        return success(yarnOrderService.getNextYarnOrderNo());
    }

}
