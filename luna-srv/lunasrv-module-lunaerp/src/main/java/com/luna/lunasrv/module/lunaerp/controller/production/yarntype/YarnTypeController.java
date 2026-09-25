package com.luna.lunasrv.module.lunaerp.controller.production.yarntype;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.yarntype.YarnTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 纱线材质")
@RestController
@RequestMapping("/lunaerp/production/yarn-type")
@Validated
public class YarnTypeController {

    @Resource
    private YarnTypeService yarnTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建纱线材质")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-type:create')")
    public CommonResult<Long> createYarnType(@Valid @RequestBody YarnTypeSaveReqVO createReqVO) {
        return success(yarnTypeService.createYarnType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改纱线材质")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-type:update')")
    public CommonResult<Boolean> updateYarnType(@Valid @RequestBody YarnTypeSaveReqVO updateReqVO) {
        yarnTypeService.updateYarnType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除纱线材质")
    @Parameter(name = "id", description = "材质编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-type:delete')")
    public CommonResult<Boolean> deleteYarnType(@RequestParam("id") Long id) {
        yarnTypeService.deleteYarnType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得纱线材质")
    @Parameter(name = "id", description = "材质编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-type:query')")
    public CommonResult<YarnTypeRespVO> getYarnType(@RequestParam("id") Long id) {
        return success(yarnTypeService.getYarnType(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得纱线材质分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-type:query')")
    public CommonResult<PageResult<YarnTypeRespVO>> getYarnTypePage(@Validated YarnTypePageReqVO pageReqVO) {
        return success(yarnTypeService.getYarnTypePage(pageReqVO));
    }

}
