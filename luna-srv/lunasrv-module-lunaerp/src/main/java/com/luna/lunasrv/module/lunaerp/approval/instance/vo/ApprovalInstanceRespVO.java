package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 审批实例 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalInstanceRespVO {

    @Schema(description = "审批实例ID", example = "101")
    private Long id;

    @Schema(description = "业务单据类型", example = "sales_contract")
    private String bizType;

    @Schema(description = "业务单据类型中文名称", example = "销售合同")
    private String bizTypeName;

    @Schema(description = "业务单据ID", example = "1024")
    private Long bizId;

    @Schema(description = "业务单号快照", example = "CH20260001")
    private String bizNo;

    @Schema(description = "流程定义全量快照JSON")
    private String processSnapshot;

    @Schema(description = "实例状态（APPROVING / APPROVED / REJECTED / CANCELLED）", example = "APPROVING")
    private String status;

    @Schema(description = "发起人用户ID", example = "1")
    private Long submitterId;

    @Schema(description = "发起人姓名", example = "张三")
    private String submitterName;

    @Schema(description = "当前节点序号（-1表示已结束）", example = "1")
    private Integer currentNodeSort;

    @Schema(description = "预留 Flowable 流程实例ID")
    private String flowableProcInstId;

    @Schema(description = "发起时间")
    private LocalDateTime startTime;

    @Schema(description = "审批结束时间")
    private LocalDateTime endTime;

    @Schema(description = "审批流程节点进度列表")
    private List<ApprovalNodeProgressRespVO> nodeProgress;

    @Schema(description = "审批任务列表")
    private List<ApprovalTaskRespVO> tasks;

    @Schema(description = "审批操作记录列表")
    private List<ApprovalRecordRespVO> records;

}
