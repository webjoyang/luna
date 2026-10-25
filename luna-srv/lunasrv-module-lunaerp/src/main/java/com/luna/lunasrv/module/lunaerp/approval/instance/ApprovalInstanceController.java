package com.luna.lunasrv.module.lunaerp.approval.instance;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 审批中心 - 审批实例 Controller
 *
 * @author luna
 */
@Tag(name = "审批中心 - 审批实例")
@RestController
@RequestMapping("/lunaerp/approval/instance")
@Validated
public class ApprovalInstanceController {

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @PostMapping("/submit")
    @Operation(summary = "发起审批")
    public CommonResult<Long> submitInstance(@Valid @RequestBody ApprovalInstanceSubmitReqVO submitReqVO) {
        return success(approvalInstanceService.submitInstance(submitReqVO));
    }

    @PutMapping("/{id}/withdraw")
    @Operation(summary = "撤回审批（仅发起人本人）")
    @Parameter(name = "id", description = "审批实例ID", required = true, example = "1024")
    public CommonResult<Boolean> withdrawInstance(@PathVariable("id") Long id,
                                                  @RequestBody(required = false) ApprovalInstanceWithdrawReqVO withdrawReqVO) {
        approvalInstanceService.withdrawInstance(id, withdrawReqVO);
        return success(true);
    }

    @GetMapping("/my-page")
    @Operation(summary = "获得我发起的审批实例分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:task:query')")
    public CommonResult<PageResult<ApprovalInstanceMyRespVO>> getMyPage(@Valid ApprovalInstanceMyPageReqVO pageReqVO) {
        return success(approvalInstanceService.getMyPage(pageReqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取审批实例详情")
    @Parameter(name = "id", description = "审批实例ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:task:query')")
    public CommonResult<ApprovalInstanceRespVO> getInstance(@PathVariable("id") Long id) {
        return success(approvalInstanceService.getInstance(id));
    }

    @GetMapping("/get-by-biz")
    @Operation(summary = "根据业务单据获取最新审批实例")
    public CommonResult<ApprovalInstanceRespVO> getLatestInstanceByBiz(@RequestParam("bizType") String bizType,
                                                                      @RequestParam("bizId") Long bizId) {
        return success(approvalInstanceService.getLatestInstanceByBiz(bizType, bizId));
    }

}
