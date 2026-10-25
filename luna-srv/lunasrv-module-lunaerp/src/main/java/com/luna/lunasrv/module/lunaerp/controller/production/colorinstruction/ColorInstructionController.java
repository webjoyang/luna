package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.*;
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

    @PutMapping("/start")
    @Operation(summary = "开始打色（PENDING -> IN_PROGRESS）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:update')")
    public CommonResult<Boolean> startColorInstruction(@Valid @RequestBody ColorInstructionStartReqVO reqVO) {
        colorInstructionService.startColorInstruction(reqVO.getId());
        return success(true);
    }

    @PutMapping("/color/approve")
    @Operation(summary = "通过打色颜色行（PENDING -> APPROVED）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:confirm')")
    public CommonResult<Boolean> approveColorLine(@Valid @RequestBody ColorInstructionColorApproveReqVO reqVO) {
        colorInstructionService.approveColorLine(reqVO.getId());
        return success(true);
    }

    @PutMapping("/color/reject-restrike")
    @Operation(summary = "驳回重打打色颜色行（PENDING/APPROVED -> REJECTED + 克隆新行）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:confirm')")
    public CommonResult<Boolean> rejectRestrikeColorLine(@Valid @RequestBody ColorInstructionColorRejectReqVO reqVO) {
        colorInstructionService.rejectRestrikeColorLine(reqVO.getId(), reqVO.getRejectReason());
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认打色指示单（三道守卫校验）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:confirm')")
    public CommonResult<Boolean> confirmColorInstruction(@Valid @RequestBody ColorInstructionConfirmReqVO reqVO) {
        colorInstructionService.confirmColorInstruction(reqVO);
        return success(true);
    }

    @PutMapping("/rollback")
    @Operation(summary = "回退打色指示单（CONFIRMED -> IN_PROGRESS）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:confirm')")
    public CommonResult<Boolean> rollbackColorInstruction(@Valid @RequestBody ColorInstructionRollbackReqVO reqVO) {
        colorInstructionService.rollbackColorInstruction(reqVO.getId());
        return success(true);
    }

    @PutMapping("/void")
    @Operation(summary = "作废打色指示单（PENDING/IN_PROGRESS/CONFIRMED -> VOID）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:confirm')")
    public CommonResult<Boolean> voidColorInstruction(@Valid @RequestBody ColorInstructionVoidReqVO reqVO) {
        colorInstructionService.voidColorInstruction(reqVO);
        return success(true);
    }

    @GetMapping("/{id}/print-data")
    @Operation(summary = "获得打色指示单打印数据")
    @Parameter(name = "id", description = "打色指示单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:color-instruction:print')")
    public CommonResult<ColorInstructionPrintRespVO> getColorInstructionPrintData(@PathVariable("id") Long id) {
        return success(colorInstructionService.getColorInstructionPrintData(id));
    }
}

