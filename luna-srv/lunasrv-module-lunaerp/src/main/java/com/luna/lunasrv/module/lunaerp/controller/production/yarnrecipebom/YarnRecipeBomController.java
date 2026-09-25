package com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 纱线配比BOM")
@RestController
@RequestMapping("/lunaerp/production/yarn-recipe-bom")
@Validated
public class YarnRecipeBomController {

    @Resource
    private YarnRecipeBomService yarnRecipeBomService;

    @PostMapping("/create")
    @Operation(summary = "创建配比BOM")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:create')")
    public CommonResult<Long> createYarnRecipeBom(@Valid @RequestBody YarnRecipeBomSaveReqVO createReqVO) {
        return success(yarnRecipeBomService.createYarnRecipeBom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改配比BOM")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:update')")
    public CommonResult<Boolean> updateYarnRecipeBom(@Valid @RequestBody YarnRecipeBomSaveReqVO updateReqVO) {
        yarnRecipeBomService.updateYarnRecipeBom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除配比BOM")
    @Parameter(name = "id", description = "配比BOM编号", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:delete')")
    public CommonResult<Boolean> deleteYarnRecipeBom(@RequestParam("id") Long id) {
        yarnRecipeBomService.deleteYarnRecipeBom(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得配比BOM")
    @Parameter(name = "id", description = "配比BOM编号", required = true, example = "10")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:query')")
    public CommonResult<YarnRecipeBomRespVO> getYarnRecipeBom(@RequestParam("id") Long id) {
        return success(yarnRecipeBomService.getYarnRecipeBom(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得配比BOM分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:query')")
    public CommonResult<PageResult<YarnRecipeBomRespVO>> getYarnRecipeBomPage(@Validated YarnRecipeBomPageReqVO pageReqVO) {
        return success(yarnRecipeBomService.getYarnRecipeBomPage(pageReqVO));
    }

    @GetMapping("/get-by-product-id")
    @Operation(summary = "按产品档案ID获得配比BOM")
    @Parameter(name = "productId", description = "产品档案ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:query')")
    public CommonResult<YarnRecipeBomRespVO> getYarnRecipeBomByProductId(@RequestParam("productId") Long productId) {
        return success(yarnRecipeBomService.getYarnRecipeBomByProductId(productId));
    }

}
