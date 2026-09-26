package com.luna.lunasrv.module.lunaerp.controller.production.loom;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.loom.LoomService;
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

@Tag(name = "生产管理 - 织机档案")
@RestController
@RequestMapping("/lunaerp/production/loom")
@Validated
public class LoomController {

    @Resource
    private LoomService loomService;

    @PostMapping("/create")
    @Operation(summary = "创建织机")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:create')")
    public CommonResult<Long> createLoom(@Valid @RequestBody LoomSaveReqVO createReqVO) {
        return success(loomService.createLoom(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得织机分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:query')")
    public CommonResult<PageResult<LoomRespVO>> getLoomPage(@Validated LoomPageReqVO pageReqVO) {
        return success(loomService.getLoomPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得织机详情")
    @Parameter(name = "id", description = "织机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:query')")
    public CommonResult<LoomRespVO> getLoom(@RequestParam("id") Long id) {
        return success(loomService.getLoom(id));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个织机机台号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:query')")
    public CommonResult<String> getNextLoomNo() {
        return success(loomService.getNextLoomNo());
    }

    @GetMapping("/list-all")
    @Operation(summary = "获得所有织机简表（供下拉选择使用）")
    @Parameter(name = "status", description = "状态，可空（默认查 ACTIVE）", example = "ACTIVE")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:query') or @ss.hasPermission('lunaerp:production:weaving-order:query') or @ss.hasPermission('lunaerp:production:weaving-order:create')")
    public CommonResult<List<LoomRespVO>> getLoomListAll(@RequestParam(value = "status", required = false) String status) {
        return success(loomService.getLoomListAll(status));
    }

    @PutMapping("/update")
    @Operation(summary = "修改织机")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:update')")
    public CommonResult<Boolean> updateLoom(@Valid @RequestBody LoomSaveReqVO updateReqVO) {
        loomService.updateLoom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除织机")
    @Parameter(name = "id", description = "织机ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:loom:delete')")
    public CommonResult<Boolean> deleteLoom(@RequestParam("id") Long id) {
        loomService.deleteLoom(id);
        return success(true);
    }

}
