package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationManualCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRevokeReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRevokeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation.InspectionConfirmationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-质检确认")
@RestController
@RequestMapping("/lunaerp/production/inspection-confirmation")
@Validated
public class InspectionConfirmationController {

    @Resource
    private InspectionConfirmationService inspectionConfirmationService;

    @GetMapping("/page")
    @Operation(summary = "获得质检确认分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:query')")
    public CommonResult<PageResult<InspectionConfirmationRespVO>> getInspectionConfirmationPage(
            @Validated InspectionConfirmationPageReqVO pageReqVO) {
        return success(inspectionConfirmationService.getInspectionConfirmationPage(pageReqVO));
    }

    @GetMapping("/worker-options")
    @Operation(summary = "获得挡车工候选列表")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:query')")
    public CommonResult<List<WorkerOptionRespVO>> getWorkerOptions() {
        return success(inspectionConfirmationService.getWorkerOptions());
    }

    @PostMapping("/manual-create")
    @Operation(summary = "手工登记质检确认")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:create')")
    public CommonResult<Long> manualCreateInspectionConfirmation(
            @Valid @RequestBody InspectionConfirmationManualCreateReqVO reqVO) {
        return success(inspectionConfirmationService.manualCreateInspectionConfirmation(reqVO));
    }

    @PostMapping("/generate-from-order")
    @Operation(summary = "从检验单生成质检确认")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:generate')")
    public CommonResult<InspectionConfirmationGenerateRespVO> generateFromOrder(
            @Valid @RequestBody InspectionConfirmationGenerateReqVO reqVO) {
        return success(inspectionConfirmationService.generateFromOrder(reqVO));
    }

    @PostMapping("/confirm")
    @Operation(summary = "责任认定")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:confirm')")
    public CommonResult<Long> confirmInspectionConfirmation(
            @Valid @RequestBody InspectionConfirmationConfirmReqVO reqVO) {
        return success(inspectionConfirmationService.confirmInspectionConfirmation(reqVO));
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销重录")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:confirm')")
    public CommonResult<InspectionConfirmationRevokeRespVO> revokeInspectionConfirmation(
            @Valid @RequestBody InspectionConfirmationRevokeReqVO reqVO) {
        return success(inspectionConfirmationService.revokeInspectionConfirmation(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得质检确认详情（含认定链）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-confirmation:query')")
    public CommonResult<InspectionConfirmationRespVO> getInspectionConfirmation(
            @RequestParam("id") Long id) {
        return success(inspectionConfirmationService.getInspectionConfirmation(id));
    }

}
