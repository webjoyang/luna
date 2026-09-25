package com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag.vo.ProductTagRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag.vo.ProductTagSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagDO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.producttag.ProductTagService;
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

@Tag(name = "基本信息-产品标签")
@RestController
@RequestMapping("/lunaerp/baseinfo/product-tag")
@Validated
public class ProductTagController {

    @Resource
    private ProductTagService productTagService;

    @GetMapping("/list")
    @Operation(summary = "获得当前租户的全部产品标签")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:tag:query')")
    public CommonResult<List<ProductTagRespVO>> getTagList() {
        List<ProductTagDO> list = productTagService.getTagList();
        return success(BeanUtils.toBean(list, ProductTagRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建产品标签")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:tag:create')")
    public CommonResult<Long> createTag(@Valid @RequestBody ProductTagSaveReqVO createReqVO) {
        return success(productTagService.createTag(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改产品标签")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:tag:update')")
    public CommonResult<Boolean> updateTag(@Valid @RequestBody ProductTagSaveReqVO updateReqVO) {
        productTagService.updateTag(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品标签")
    @Parameter(name = "id", description = "标签ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:tag:delete')")
    public CommonResult<Boolean> deleteTag(@RequestParam("id") Long id) {
        productTagService.deleteTag(id);
        return success(true);
    }

}
