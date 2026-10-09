package com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier.SupplierService;
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

@Tag(name = "基本信息-供应商信息")
@RestController
@RequestMapping("/lunaerp/baseinfo/supplier")
@Validated
public class SupplierController {

    @Resource
    private SupplierService supplierService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:create')")
    public CommonResult<Long> createSupplier(@Valid @RequestBody SupplierSaveReqVO createReqVO) {
        return success(supplierService.createSupplier(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:query')")
    public CommonResult<PageResult<SupplierRespVO>> getSupplierPage(@Validated SupplierPageReqVO pageReqVO) {
        return success(supplierService.getSupplierPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商")
    @Parameter(name = "id", description = "供应商ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:query')")
    public CommonResult<SupplierRespVO> getSupplier(@RequestParam("id") Long id) {
        return success(supplierService.getSupplier(id));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个供应商编号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:query')")
    public CommonResult<String> getNextSupplierNo() {
        return success(supplierService.getNextSupplierNo());
    }

    @PutMapping("/update")
    @Operation(summary = "修改供应商")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:update')")
    public CommonResult<Boolean> updateSupplier(@Valid @RequestBody SupplierSaveReqVO updateReqVO) {
        supplierService.updateSupplier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商")
    @Parameter(name = "id", description = "供应商ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:delete')")
    public CommonResult<Boolean> deleteSupplier(@RequestParam("id") Long id) {
        supplierService.deleteSupplier(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除供应商")
    @Parameter(name = "ids", description = "供应商ID列表", required = true, example = "1,2,3")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:delete')")
    public CommonResult<Boolean> deleteSupplierList(@RequestParam("ids") List<Long> ids) {
        supplierService.deleteSupplierList(ids);
        return success(true);
    }

    @GetMapping("/import-template")
    @Operation(summary = "获得供应商导入模板")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:import')")
    public void importSupplierTemplate(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierImportVO> list = supplierService.getImportTemplate();
        com.luna.lunasrv.framework.excel.core.util.ExcelUtils.write(response, "供应商导入模板.xlsx", "供应商列表",
                com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierImportVO.class, list);
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入供应商档案")
    @io.swagger.v3.oas.annotations.Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true)
    })
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier:import')")
    public CommonResult<Integer> importSupplier(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult result = supplierService.importSupplier(file);
        if (result.hasError()) {
            result.writeResponse(response, "供应商导入错误明细.xlsx");
            return null;
        }
        return success(result.getSuccessCount());
    }

}
