package com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.dyeingorder.DyeingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-染色单")
@RestController
@RequestMapping("/lunaerp/production/dyeing-order")
@Validated
public class DyeingOrderController {

    @Resource
    private DyeingOrderService dyeingOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建染色单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:create')")
    public CommonResult<Long> createDyeingOrder(@Valid @RequestBody DyeingOrderSaveReqVO createReqVO) {
        return success(dyeingOrderService.createDyeingOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新染色单（整体保存）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:update')")
    public CommonResult<Boolean> updateDyeingOrder(@Valid @RequestBody DyeingOrderSaveReqVO updateReqVO) {
        dyeingOrderService.updateDyeingOrder(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得染色单详情")
    @Parameter(name = "id", description = "染色单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:query')")
    public CommonResult<DyeingOrderRespVO> getDyeingOrder(@RequestParam("id") Long id) {
        return success(dyeingOrderService.getDyeingOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得染色单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:query')")
    public CommonResult<PageResult<DyeingOrderRespVO>> getDyeingOrderPage(@Validated DyeingOrderPageReqVO pageReqVO) {
        return success(dyeingOrderService.getDyeingOrderPage(pageReqVO));
    }

    @PutMapping("/effect")
    @Operation(summary = "生效/下达染色单")
    @Parameter(name = "id", description = "染色单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:update')")
    public CommonResult<Boolean> effectDyeingOrder(@RequestParam("id") Long id) {
        dyeingOrderService.effectDyeingOrder(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成染色单")
    @Parameter(name = "id", description = "染色单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:update')")
    public CommonResult<Boolean> completeDyeingOrder(@RequestParam("id") Long id) {
        dyeingOrderService.completeDyeingOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消染色单")
    @Parameter(name = "id", description = "染色单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:update')")
    public CommonResult<Boolean> cancelDyeingOrder(@RequestParam("id") Long id) {
        dyeingOrderService.cancelDyeingOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除染色单")
    @Parameter(name = "id", description = "染色单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:delete')")
    public CommonResult<Boolean> deleteDyeingOrder(@RequestParam("id") Long id) {
        dyeingOrderService.deleteDyeingOrder(id);
        return success(true);
    }

}
