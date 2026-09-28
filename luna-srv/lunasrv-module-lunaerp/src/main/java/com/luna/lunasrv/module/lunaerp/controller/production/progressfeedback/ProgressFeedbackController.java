package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackAdvanceReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackColorSectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackStatisticsRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.progressfeedback.ProgressFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 生产管理-进度反馈覆盖层 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 生产管理-进度反馈")
@RestController
@RequestMapping("/lunaerp/production/progress-feedback")
@Validated
public class ProgressFeedbackController {

    @Resource
    private ProgressFeedbackService progressFeedbackService;

    @GetMapping("/page")
    @Operation(summary = "获得进度反馈覆盖层分页（以染色颜色明细行为骨架）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:query')")
    public CommonResult<PageResult<ProgressFeedbackColorSectionRespVO>> getProgressFeedbackPage(
            @Valid ProgressFeedbackPageReqVO pageReqVO) {
        return success(progressFeedbackService.getProgressFeedbackPage(pageReqVO));
    }

    @GetMapping("/list-by-order")
    @Operation(summary = "获得指定染色单的进度反馈节列表（供染色单工作台页签使用）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:query')")
    public CommonResult<List<ProgressFeedbackColorSectionRespVO>> getProgressFeedbackListByOrder(
            @RequestParam(value = "dyeingOrderId", required = false) Long dyeingOrderId,
            @RequestParam(value = "id", required = false) Long id) {
        Long orderId = dyeingOrderId != null ? dyeingOrderId : id;
        return success(progressFeedbackService.getProgressFeedbackListByOrder(orderId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获得进度反馈统计条数据（与当前筛选同口径）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:query')")
    public CommonResult<ProgressFeedbackStatisticsRespVO> getProgressFeedbackStatistics(
            ProgressFeedbackPageReqVO reqVO) {
        return success(progressFeedbackService.getProgressFeedbackStatistics(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "新建反馈缸")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:create')")
    public CommonResult<Long> createProgressFeedback(@Valid @RequestBody ProgressFeedbackCreateReqVO reqVO) {
        return success(progressFeedbackService.createProgressFeedback(reqVO));
    }

    @GetMapping({"/get", "/{id}"})
    @Operation(summary = "获得反馈缸详情（缸字段 + 五阶段日期 + 颜色行/染色单实时信息 + 延期/覆盖率）")
    @Parameter(name = "id", description = "反馈缸ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:query')")
    public CommonResult<ProgressFeedbackDetailRespVO> getProgressFeedback(
            @RequestParam(value = "id", required = false) Long queryId,
            @PathVariable(value = "id", required = false) Long pathId) {
        Long vatId = queryId != null ? queryId : pathId;
        return success(progressFeedbackService.getProgressFeedback(vatId));
    }

    @PutMapping("/update")
    @Operation(summary = "编辑反馈缸（修改缸信息、阶段日期或状态回退）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:update')")
    public CommonResult<Boolean> updateProgressFeedback(@Valid @RequestBody ProgressFeedbackUpdateReqVO reqVO) {
        progressFeedbackService.updateProgressFeedback(reqVO);
        return success(true);
    }

    @PutMapping("/advance")
    @Operation(summary = "推进反馈缸工序阶段（严格逐格推进，自动盖当天日期戳）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:advance')")
    public CommonResult<Boolean> advanceProgressFeedback(@Valid @RequestBody ProgressFeedbackAdvanceReqVO reqVO) {
        progressFeedbackService.advanceProgressFeedback(reqVO);
        return success(true);
    }

    @DeleteMapping({"/{id}", "/delete"})
    @Operation(summary = "删除反馈缸（级联软删其下所有反馈记录）")
    @Parameter(name = "id", description = "反馈缸ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:delete')")
    public CommonResult<Boolean> deleteProgressFeedback(
            @PathVariable(value = "id", required = false) Long pathId,
            @RequestParam(value = "id", required = false) Long queryId) {
        Long vatId = pathId != null ? pathId : queryId;
        progressFeedbackService.deleteProgressFeedback(vatId);
        return success(true);
    }

}
