package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 审批实例撤回 Request VO")
@Data
public class ApprovalInstanceWithdrawReqVO {

    @Schema(description = "撤回原因", example = "单据信息有误，撤回修改")
    private String reason;

}
