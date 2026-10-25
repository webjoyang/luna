package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审批任务 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskRespVO {

    @Schema(description = "审批任务ID", example = "201")
    private Long id;

    @Schema(description = "关联审批实例ID", example = "101")
    private Long instanceId;

    @Schema(description = "节点序号", example = "1")
    private Integer nodeSort;

    @Schema(description = "节点名称快照", example = "部门主管审批")
    private String nodeName;

    @Schema(description = "节点类型快照（SEQUENTIAL / OR_SIGN / AND_SIGN）", example = "SEQUENTIAL")
    private String nodeType;

    @Schema(description = "当前审批人用户ID", example = "2")
    private Long approverId;

    @Schema(description = "当前审批人姓名", example = "李四")
    private String approverName;

    @Schema(description = "任务状态（PENDING / DONE）", example = "PENDING")
    private String status;

    @Schema(description = "处理动作（DONE / REJECTED / TRANSFERRED）")
    private String action;

    @Schema(description = "任务到达时间")
    private LocalDateTime startTime;

    @Schema(description = "任务完成时间")
    private LocalDateTime endTime;

}
