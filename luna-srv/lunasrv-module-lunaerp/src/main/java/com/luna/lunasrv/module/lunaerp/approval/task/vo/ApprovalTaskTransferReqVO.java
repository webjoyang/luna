package com.luna.lunasrv.module.lunaerp.approval.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批转交 Request VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 审批转交 Request VO")
@Data
public class ApprovalTaskTransferReqVO {

    @Schema(description = "转交接收人用户ID（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "转交接收人不能为空")
    private Long toUserId;

    @Schema(description = "转交说明/审批意见（可选）", example = "出差转交同事处理")
    private String comment;

}
