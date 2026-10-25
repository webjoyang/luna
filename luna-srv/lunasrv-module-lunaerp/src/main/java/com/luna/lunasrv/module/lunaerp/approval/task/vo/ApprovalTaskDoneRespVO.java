package com.luna.lunasrv.module.lunaerp.approval.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 我已审批任务分页 Response VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 我已审批任务分页 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskDoneRespVO {

    @Schema(description = "操作记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "301")
    private Long id;

    @Schema(description = "关联审批实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long instanceId;

    @Schema(description = "关联审批任务ID", example = "1024")
    private Long taskId;

    @Schema(description = "单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "sales_contract")
    private String bizType;

    @Schema(description = "单据类型中文名称", example = "销售合同")
    private String bizTypeName;

    @Schema(description = "单据ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    private Long bizId;

    @Schema(description = "单号", example = "HT20261010001")
    private String bizNo;

    @Schema(description = "实例当前状态（APPROVING / APPROVED / REJECTED / CANCELLED）", example = "APPROVING")
    private String instanceStatus;

    @Schema(description = "实例发起时间")
    private LocalDateTime instanceStartTime;

    @Schema(description = "实例结束时间")
    private LocalDateTime instanceEndTime;

    @Schema(description = "发起人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long submitterId;

    @Schema(description = "发起人姓名", example = "李四")
    private String submitterName;

    @Schema(description = "操作人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    @Schema(description = "操作动作（APPROVE / REJECT / TRANSFER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVE")
    private String action;

    @Schema(description = "操作动作中文描述", example = "通过")
    private String actionName;

    @Schema(description = "审批意见", example = "同意通过")
    private String comment;

    @Schema(description = "转交接收人ID", example = "102")
    private Long transferToId;

    @Schema(description = "转交接收人姓名", example = "王五")
    private String transferToName;

    @Schema(description = "操作时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime operateTime;

    @Schema(description = "节点名称", example = "部门主管审批")
    private String nodeName;

}
