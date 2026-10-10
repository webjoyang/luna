package com.luna.lunasrv.module.lunaerp.approval.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待我审批任务分页 Response VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 待我审批任务分页 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskTodoRespVO {

    @Schema(description = "审批任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "关联审批实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long instanceId;

    @Schema(description = "节点序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer nodeSort;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "部门主管审批")
    private String nodeName;

    @Schema(description = "节点类型（SEQUENTIAL / OR_SIGN / AND_SIGN）", requiredMode = Schema.RequiredMode.REQUIRED, example = "OR_SIGN")
    private String nodeType;

    @Schema(description = "审批人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long approverId;

    @Schema(description = "审批人姓名", example = "张三")
    private String approverName;

    @Schema(description = "单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "sales_contract")
    private String bizType;

    @Schema(description = "单据类型中文名称", example = "销售合同")
    private String bizTypeName;

    @Schema(description = "单据ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    private Long bizId;

    @Schema(description = "单号", example = "HT20261010001")
    private String bizNo;

    @Schema(description = "发起人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long submitterId;

    @Schema(description = "发起人姓名", example = "李四")
    private String submitterName;

    @Schema(description = "任务到达时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "发起时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime instanceStartTime;

    @Schema(description = "超时/等待时长（毫秒）", example = "3600000")
    private Long timeoutDuration;

    @Schema(description = "超时/等待时长可读描述", example = "1小时0分")
    private String timeoutDurationDesc;

    @Schema(description = "任务状态（PENDING / DONE）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING")
    private String status;

}
