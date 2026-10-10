package com.luna.lunasrv.module.lunaerp.approval.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批通过 Request VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 审批通过 Request VO")
@Data
public class ApprovalTaskApproveReqVO {

    @Schema(description = "审批意见（可选）", example = "同意")
    private String comment;

}
