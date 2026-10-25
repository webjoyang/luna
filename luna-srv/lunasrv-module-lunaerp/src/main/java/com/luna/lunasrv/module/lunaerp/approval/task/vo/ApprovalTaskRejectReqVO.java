package com.luna.lunasrv.module.lunaerp.approval.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批驳回 Request VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 审批驳回 Request VO")
@Data
public class ApprovalTaskRejectReqVO {

    @Schema(description = "驳回意见（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "单价不符合政策，请重新核算")
    @NotBlank(message = "驳回意见不能为空")
    private String comment;

}
