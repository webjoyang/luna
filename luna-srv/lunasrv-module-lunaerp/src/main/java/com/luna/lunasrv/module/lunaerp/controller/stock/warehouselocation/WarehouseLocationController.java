package com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.stock.warehouselocation.WarehouseLocationService;
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

@Tag(name = "库存管理 - 库位")
@RestController
@RequestMapping("/lunaerp/stock/warehouse-location")
@Validated
public class WarehouseLocationController {

    @Resource
    private WarehouseLocationService warehouseLocationService;

    @PostMapping("/create")
    @Operation(summary = "创建库位")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse-location:create')")
    public CommonResult<Long> createWarehouseLocation(@Valid @RequestBody WarehouseLocationSaveReqVO createReqVO) {
        return success(warehouseLocationService.createWarehouseLocation(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库位分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query') or @ss.hasPermission('lunaerp:stock:warehouse-location:query')")
    public CommonResult<PageResult<WarehouseLocationRespVO>> getWarehouseLocationPage(@Validated WarehouseLocationPageReqVO pageReqVO) {
        return success(warehouseLocationService.getWarehouseLocationPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得库位详情")
    @Parameter(name = "id", description = "库位ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query') or @ss.hasPermission('lunaerp:stock:warehouse-location:query')")
    public CommonResult<WarehouseLocationRespVO> getWarehouseLocation(@RequestParam("id") Long id) {
        return success(warehouseLocationService.getWarehouseLocation(id));
    }

    @GetMapping("/list-by-warehouse")
    @Operation(summary = "获得仓库下的启用库位列表（供下拉选择使用）")
    @Parameter(name = "warehouseId", description = "仓库ID", required = true, example = "1024")
    @Parameter(name = "status", description = "状态，可空（默认查 ACTIVE）", example = "ACTIVE")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query') or @ss.hasPermission('lunaerp:stock:warehouse-location:query') or @ss.hasPermission('lunaerp:stock:stock-in:query') or @ss.hasPermission('lunaerp:stock:stock-in:create') or @ss.hasPermission('lunaerp:stock:inventory-balance:query')")
    public CommonResult<List<WarehouseLocationRespVO>> getWarehouseLocationListByWarehouse(
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam(value = "status", required = false) String status) {
        return success(warehouseLocationService.getWarehouseLocationListByWarehouse(warehouseId, status));
    }

    @PutMapping("/update")
    @Operation(summary = "修改库位")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse-location:update')")
    public CommonResult<Boolean> updateWarehouseLocation(@Valid @RequestBody WarehouseLocationSaveReqVO updateReqVO) {
        warehouseLocationService.updateWarehouseLocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库位")
    @Parameter(name = "id", description = "库位ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse-location:delete')")
    public CommonResult<Boolean> deleteWarehouseLocation(@RequestParam("id") Long id) {
        warehouseLocationService.deleteWarehouseLocation(id);
        return success(true);
    }

}
