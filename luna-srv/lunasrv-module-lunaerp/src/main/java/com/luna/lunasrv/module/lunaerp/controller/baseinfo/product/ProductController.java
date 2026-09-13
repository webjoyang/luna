package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductTagRefVO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.product.ProductService;
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

// 注意：本模块包名是 controller.baseinfo（不含 .controller.admin. 段），框架不会自动追加
// /admin-api 前缀，因此类级 @RequestMapping 必须显式写全 /admin-api 路径（前端 axios baseURL 为 /admin-api）。
@Tag(name = "基本信息-产品信息")
@RestController
@RequestMapping("/admin-api/lunaerp/baseinfo/product")
@Validated
public class ProductController {

    @Resource
    private ProductService productService;

    @GetMapping("/page")
    @Operation(summary = "获得产品分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:query')")
    public CommonResult<PageResult<ProductRespVO>> getProductPage(@Validated ProductPageReqVO pageReqVO) {
        return success(productService.getProductPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得产品")
    @Parameter(name = "id", description = "产品ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:query')")
    public CommonResult<ProductRespVO> getProduct(@RequestParam("id") Long id) {
        return success(productService.getProduct(id));
    }

    @GetMapping("/get-by-code")
    @Operation(summary = "按产品编号获得产品")
    @Parameter(name = "code", description = "产品编号", required = true, example = "P0001")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:query')")
    public CommonResult<ProductRespVO> getProductByCode(@RequestParam("code") String code) {
        return success(productService.getProductByCode(code));
    }

    @GetMapping("/next-code")
    @Operation(summary = "获取下一个产品编号")
    @Parameter(name = "prefix", description = "编号前缀，默认 P", example = "P")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:query')")
    public CommonResult<String> getNextCode(
            @RequestParam(value = "prefix", required = false) String prefix) {
        return success(productService.getNextCode(prefix));
    }

    @PostMapping("/create")
    @Operation(summary = "创建产品")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:create')")
    public CommonResult<Long> createProduct(@Valid @RequestBody ProductSaveReqVO createReqVO) {
        return success(productService.createProduct(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改产品")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:update')")
    public CommonResult<Boolean> updateProduct(@Valid @RequestBody ProductSaveReqVO updateReqVO) {
        productService.updateProduct(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品")
    @Parameter(name = "id", description = "产品ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:delete')")
    public CommonResult<Boolean> deleteProduct(@RequestParam("id") Long id) {
        productService.deleteProduct(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除产品")
    @Parameter(name = "ids", description = "产品ID列表", required = true, example = "1,2,3")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:delete')")
    public CommonResult<Boolean> deleteProductList(@RequestParam("ids") List<Long> ids) {
        productService.deleteProductList(ids);
        return success(true);
    }

    @PutMapping("/update-tags")
    @Operation(summary = "更新产品标签（仅做标签 co-update）")
    @Parameter(name = "id", description = "产品ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:update')")
    public CommonResult<List<ProductTagRefVO>> updateProductTags(
            @RequestParam("id") Long id,
            @Valid @RequestBody List<@Valid ProductTagRefVO> tags) {
        return success(productService.updateProductTags(id, tags));
    }

}
