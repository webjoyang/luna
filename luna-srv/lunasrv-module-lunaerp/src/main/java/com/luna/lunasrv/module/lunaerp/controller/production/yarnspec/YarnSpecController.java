package com.luna.lunasrv.module.lunaerp.controller.production.yarnspec;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecCascadeOptionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.yarnspec.YarnSpecService;
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

@Tag(name = "生产管理 - 纱线规格")
@RestController
@RequestMapping("/lunaerp/production/yarn-spec")
@Validated
public class YarnSpecController {

    @Resource
    private YarnSpecService yarnSpecService;

    @PostMapping("/create")
    @Operation(summary = "创建纱线规格")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:production:yarn-spec:create', 'lunaerp:production:yarn-type:create')")
    public CommonResult<Long> createYarnSpec(@Valid @RequestBody YarnSpecSaveReqVO createReqVO) {
        return success(yarnSpecService.createYarnSpec(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改纱线规格")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:production:yarn-spec:update', 'lunaerp:production:yarn-type:update')")
    public CommonResult<Boolean> updateYarnSpec(@Valid @RequestBody YarnSpecSaveReqVO updateReqVO) {
        yarnSpecService.updateYarnSpec(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除纱线规格")
    @Parameter(name = "id", description = "规格编号", required = true, example = "10")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:production:yarn-spec:delete', 'lunaerp:production:yarn-type:delete')")
    public CommonResult<Boolean> deleteYarnSpec(@RequestParam("id") Long id) {
        yarnSpecService.deleteYarnSpec(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得纱线规格")
    @Parameter(name = "id", description = "规格编号", required = true, example = "10")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:production:yarn-spec:query', 'lunaerp:production:yarn-type:query')")
    public CommonResult<YarnSpecRespVO> getYarnSpec(@RequestParam("id") Long id) {
        return success(yarnSpecService.getYarnSpec(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得纱线规格分页")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:production:yarn-spec:query', 'lunaerp:production:yarn-type:query')")
    public CommonResult<PageResult<YarnSpecRespVO>> getYarnSpecPage(@Validated YarnSpecPageReqVO pageReqVO) {
        return success(yarnSpecService.getYarnSpecPage(pageReqVO));
    }

    @GetMapping("/cascade-options")
    @Operation(summary = "获得两级级联规格数据源")
    @Parameter(name = "keyword", description = "规格名称过滤关键词", example = "32S")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:production:yarn-spec:query', 'lunaerp:production:yarn-type:query')")
    public CommonResult<List<YarnSpecCascadeOptionVO>> getCascadeOptions(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(yarnSpecService.getCascadeOptions(keyword));
    }

}
