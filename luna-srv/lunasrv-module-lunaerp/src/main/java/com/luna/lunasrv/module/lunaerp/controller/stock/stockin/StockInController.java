package com.luna.lunasrv.module.lunaerp.controller.stock.stockin;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.stock.stockin.StockInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;
import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Tag(name = "库存管理 - 入库单")
@RestController
@RequestMapping("/lunaerp/stock/stock-in")
@Validated
public class StockInController {

    @Resource
    private StockInService stockInService;

    @PostMapping("/create")
    @Operation(summary = "创建入库单")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:create')")
    public CommonResult<Long> createStockIn(@Valid @RequestBody StockInSaveReqVO createReqVO) {
        return success(stockInService.createStockIn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改入库单")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:update')")
    public CommonResult<Boolean> updateStockIn(@Valid @RequestBody StockInSaveReqVO updateReqVO) {
        stockInService.updateStockIn(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除入库单")
    @Parameter(name = "id", description = "入库单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:delete')")
    public CommonResult<Boolean> deleteStockIn(@RequestParam("id") Long id) {
        stockInService.deleteStockIn(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得入库单详情")
    @Parameter(name = "id", description = "入库单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:query')")
    public CommonResult<StockInRespVO> getStockIn(@RequestParam("id") Long id) {
        return success(stockInService.getStockIn(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得入库单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:query')")
    public CommonResult<PageResult<StockInRespVO>> getStockInPage(@Validated StockInPageReqVO pageReqVO) {
        return success(stockInService.getStockInPage(pageReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个入库单号（只读、不占号）")
    @Parameter(name = "inDate", description = "单据日期，可空（默认今天）", example = "2026-09-24")
    @PreAuthorize("@ss.hasPermission('lunaerp:stock:stock-in:query') or @ss.hasPermission('lunaerp:stock:stock-in:create')")
    public CommonResult<String> getNextInNo(
            @RequestParam(value = "inDate", required = false)
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate inDate) {
        return success(stockInService.getNextInNo(inDate));
    }

}
