package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.mzt.logapi.starter.annotation.LogRecord;
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

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个打色单号")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:query')")
    public CommonResult<String> getNextNo() {
        return success(colorInstructionService.getNextNo());
    }

    @PostMapping("/create")
    @Operation(summary = "创建打色指示单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:create')")
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "CREATE", bizNo = "{{#_ret.data}}",
            success = "创建打色指示单【{{#_ret.data}}】")
    public CommonResult<Long> createColorInstruction(@Valid @RequestBody ColorInstructionSaveReqVO createReqVO) {
        return success(colorInstructionService.createColorInstruction(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新打色指示单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:update')")
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}",
            success = "更新打色指示单【{{#updateReqVO.id}}】")
    public CommonResult<Boolean> updateColorInstruction(@Valid @RequestBody ColorInstructionSaveReqVO updateReqVO) {
        colorInstructionService.updateColorInstruction(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除打色指示单")
    @Parameter(name = "id", description = "打色指示单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:delete')")
    @LogRecord(type = "COLOR_INSTRUCTION", subType = "DELETE", bizNo = "{{#id}}",
            success = "删除打色指示单【{{#id}}】")
    public CommonResult<Boolean> deleteColorInstruction(@RequestParam("id") Long id) {
        colorInstructionService.deleteColorInstruction(id);
        return success(true);
    }

}
