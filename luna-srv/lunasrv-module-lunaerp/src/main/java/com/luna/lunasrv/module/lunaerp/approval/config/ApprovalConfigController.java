package com.luna.lunasrv.module.lunaerp.approval.config;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 审批中心 - 审批链配置 Controller
 *
 * @author luna
 */
@Tag(name = "审批中心 - 审批链配置")
@RestController
@RequestMapping("/lunaerp/approval/config")
@Validated
public class ApprovalConfigController {

    @Resource
    private ApprovalConfigService approvalConfigService;

    @PostMapping
    @Operation(summary = "新建审批流程配置（含节点）")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:config:create')")
    public CommonResult<Long> createApprovalConfig(@Valid @RequestBody ApprovalConfigSaveReqVO createReqVO) {
        return success(approvalConfigService.createApprovalConfig(createReqVO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新审批流程配置（节点整体替换）")
    @Parameter(name = "id", description = "配置ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:config:update')")
    public CommonResult<Boolean> updateApprovalConfig(@PathVariable("id") Long id,
                                                      @Valid @RequestBody ApprovalConfigSaveReqVO updateReqVO) {
        approvalConfigService.updateApprovalConfig(id, updateReqVO);
        return success(true);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用审批流程配置")
    @Parameter(name = "id", description = "配置ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:config:update')")
    public CommonResult<Boolean> updateApprovalConfigStatus(@PathVariable("id") Long id,
                                                            @RequestBody(required = false) ApprovalConfigUpdateStatusReqVO bodyVO,
                                                            @RequestParam(value = "status", required = false) String paramStatus) {
        String status = (bodyVO != null && StrUtil.isNotBlank(bodyVO.getStatus())) ? bodyVO.getStatus() : paramStatus;
        if (StrUtil.isBlank(status)) {
            throw exception(GlobalErrorCodeConstants.BAD_REQUEST, "状态不能为空");
        }
        approvalConfigService.updateApprovalConfigStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除审批流程配置（仅未被实例引用）")
    @Parameter(name = "id", description = "配置ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:config:delete')")
    public CommonResult<Boolean> deleteApprovalConfig(@PathVariable("id") Long id) {
        approvalConfigService.deleteApprovalConfig(id);
        return success(true);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获得审批流程配置详情（含节点列表按 sort 升序）")
    @Parameter(name = "id", description = "配置ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:config:query')")
    public CommonResult<ApprovalConfigRespVO> getApprovalConfig(@PathVariable("id") Long id) {
        return success(approvalConfigService.getApprovalConfig(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得审批流程配置分页（按 biz_type 筛选）")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:config:query')")
    public CommonResult<PageResult<ApprovalConfigRespVO>> getApprovalConfigPage(@Valid ApprovalConfigPageReqVO pageReqVO) {
        return success(approvalConfigService.getApprovalConfigPage(pageReqVO));
    }

}
