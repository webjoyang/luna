package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 打色指示单")
@RestController
@RequestMapping("/lunaerp/production/color-instruction")
@Validated
public class ColorInstructionController {

    @Resource
    private ColorInstructionService colorInstructionService;

    @GetMapping("/page")
    @Operation(summary = "获得打色指示单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:query')")
    public CommonResult<PageResult<ColorInstructionRespVO>> getColorInstructionPage(@Validated ColorInstructionPageReqVO pageReqVO) {
        return success(colorInstructionService.getColorInstructionPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得打色指示单详情")
    @Parameter(name = "id", description = "打色指示单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:query')")
    public CommonResult<ColorInstructionRespVO> getColorInstruction(@RequestParam("id") Long id) {
        return success(colorInstructionService.getColorInstruction(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建打色指示单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:create')")
    public CommonResult<Long> createColorInstruction(@Valid @RequestBody ColorInstructionSaveReqVO createReqVO) {
        return success(colorInstructionService.createColorInstruction(createReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个打色单号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:query')")
    public CommonResult<String> getNextInstructionNo() {
        return success(colorInstructionService.getNextInstructionNo());
    }

    @DeleteMapping("/delete")
    @Operation(summary = "软删除打色指示单（仅 PENDING 状态）")
    @Parameter(name = "id", description = "打色指示单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:delete')")
    public CommonResult<Boolean> deleteColorInstruction(@RequestParam("id") Long id) {
        colorInstructionService.deleteColorInstruction(id);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新打色指示单（差异保存）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:update')")
    public CommonResult<Boolean> updateColorInstruction(@Valid @RequestBody ColorInstructionSaveReqVO updateReqVO) {
        colorInstructionService.updateColorInstruction(updateReqVO);
        return success(true);
    }

}
