package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackColorSectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackStatisticsRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.progressfeedback.ProgressFeedbackService;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCreateReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

}
