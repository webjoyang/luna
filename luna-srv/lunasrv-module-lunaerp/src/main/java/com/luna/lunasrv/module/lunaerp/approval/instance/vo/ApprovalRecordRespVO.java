package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审批操作记录 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRecordRespVO {

    @Schema(description = "操作记录ID", example = "301")
    private Long id;

    @Schema(description = "关联审批实例ID", example = "101")
    private Long instanceId;

    @Schema(description = "关联审批任务ID", example = "201")
    private Long taskId;

    @Schema(description = "操作人用户ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    @Schema(description = "操作动作（SUBMIT / APPROVE / REJECT / TRANSFER / WITHDRAW）", example = "SUBMIT")
    private String action;

    @Schema(description = "审批意见/说明", example = "同意")
    private String comment;

    @Schema(description = "转交接收人用户ID")
    private Long transferToId;

    @Schema(description = "转交接收人姓名")
    private String transferToName;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

}
