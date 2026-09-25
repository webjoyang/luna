package com.luna.lunasrv.module.lunaerp.controller.stock.warehouse;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehousePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.stock.warehouse.WarehouseService;
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

@Tag(name = "库存管理 - 仓库")
@RestController
@RequestMapping("/lunaerp/stock/warehouse")
@Validated
public class WarehouseController {

    @Resource
    private WarehouseService warehouseService;

    @PostMapping("/create")
    @Operation(summary = "创建仓库")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:create')")
    public CommonResult<Long> createWarehouse(@Valid @RequestBody WarehouseSaveReqVO createReqVO) {
        return success(warehouseService.createWarehouse(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仓库分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query')")
    public CommonResult<PageResult<WarehouseRespVO>> getWarehousePage(@Validated WarehousePageReqVO pageReqVO) {
        return success(warehouseService.getWarehousePage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得仓库详情")
    @Parameter(name = "id", description = "仓库ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query')")
    public CommonResult<WarehouseRespVO> getWarehouse(@RequestParam("id") Long id) {
        return success(warehouseService.getWarehouse(id));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个仓库编号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query')")
    public CommonResult<String> getNextWarehouseNo() {
        return success(warehouseService.getNextWarehouseNo());
    }

    @GetMapping("/list-all")
    @Operation(summary = "获得所有仓库简表（供下拉选择使用）")
    @Parameter(name = "status", description = "状态，可空（默认查 ACTIVE）", example = "ACTIVE")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:query') or @ss.hasPermission('lunaerp:stock:stock-in:query') or @ss.hasPermission('lunaerp:stock:stock-in:create') or @ss.hasPermission('lunaerp:stock:inventory-balance:query')")
    public CommonResult<List<WarehouseRespVO>> getWarehouseListAll(@RequestParam(value = "status", required = false) String status) {
        return success(warehouseService.getWarehouseListAll(status));
    }

    @PutMapping("/update")
    @Operation(summary = "修改仓库")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:update')")
    public CommonResult<Boolean> updateWarehouse(@Valid @RequestBody WarehouseSaveReqVO updateReqVO) {
        warehouseService.updateWarehouse(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仓库")
    @Parameter(name = "id", description = "仓库ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:warehouse:delete')")
    public CommonResult<Boolean> deleteWarehouse(@RequestParam("id") Long id) {
        warehouseService.deleteWarehouse(id);
        return success(true);
    }

}
