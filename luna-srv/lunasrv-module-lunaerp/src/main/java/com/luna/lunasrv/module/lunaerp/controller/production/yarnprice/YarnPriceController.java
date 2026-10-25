package com.luna.lunasrv.module.lunaerp.controller.production.yarnprice;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.excel.core.util.ExcelUtils;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.yarnprice.YarnPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 纱价主档")
@RestController
@RequestMapping("/lunaerp/production/yarn-price")
@Validated
public class YarnPriceController {

    @Resource
    private YarnPriceService yarnPriceService;

    @PostMapping("/create")
    @Operation(summary = "创建纱价记录")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:create')")
    public CommonResult<Long> createYarnPrice(@Valid @RequestBody YarnPriceSaveReqVO createReqVO) {
        return success(yarnPriceService.createYarnPrice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改纱价记录")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:update')")
    public CommonResult<Boolean> updateYarnPrice(@Valid @RequestBody YarnPriceSaveReqVO updateReqVO) {
        yarnPriceService.updateYarnPrice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除纱价记录")
    @Parameter(name = "id", description = "纱价记录ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:delete')")
    public CommonResult<Boolean> deleteYarnPrice(@RequestParam("id") Long id) {
        yarnPriceService.deleteYarnPrice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得纱价记录")
    @Parameter(name = "id", description = "纱价记录ID", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:query')")
    public CommonResult<YarnPriceRespVO> getYarnPrice(@RequestParam("id") Long id) {
        return success(yarnPriceService.getYarnPrice(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得纱价分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:query')")
    public CommonResult<PageResult<YarnPriceRespVO>> getYarnPricePage(@Validated YarnPricePageReqVO pageReqVO) {
        return success(yarnPriceService.getYarnPricePage(pageReqVO));
    }

    @GetMapping("/list-by-spec")
    @Operation(summary = "获得指定规格的历史价格列表（按日期倒序）")
    @Parameters({
            @Parameter(name = "yarnSpecId", description = "规格ID", required = true, example = "10"),
            @Parameter(name = "priceType", description = "价类（可选）", example = "PURCHASE")
    })
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:query')")
    public CommonResult<List<YarnPriceRespVO>> listBySpec(
            @RequestParam("yarnSpecId") Long yarnSpecId,
            @RequestParam(value = "priceType", required = false) String priceType) {
        return success(yarnPriceService.listBySpec(yarnSpecId, priceType));
    }

    @GetMapping("/latest-purchased-price")
    @Operation(summary = "获得指定规格的最近采购单价（只读聚合）")
    @Parameter(name = "yarnSpecId", description = "规格ID", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:query')")
    public CommonResult<BigDecimal> getLatestPurchasedPrice(@RequestParam("yarnSpecId") Long yarnSpecId) {
        return success(yarnPriceService.getLatestPurchasedPrice(yarnSpecId));
    }

    @GetMapping("/import-template")
    @Operation(summary = "获得纱价导入模板")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:create')")
    public void importYarnPriceTemplate(HttpServletResponse response) throws IOException {
        List<YarnPriceImportVO> list = yarnPriceService.getImportTemplate();
        ExcelUtils.write(response, "纱价导入模板.xlsx", "纱价列表", YarnPriceImportVO.class, list);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入纱价")
    @Parameter(name = "file", description = "Excel 文件", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-price:create')")
    public CommonResult<Integer> importYarnPrice(@RequestParam("file") MultipartFile file,
                                                 HttpServletResponse response) throws IOException {
        ExcelImportResult result = yarnPriceService.importYarnPrice(file);
        if (result.hasError()) {
            result.writeResponse(response, "纱价导入错误明细.xlsx");
            return null;
        }
        return success(result.getSuccessCount());
    }

}
