package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 我发起的审批实例分页 Response VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 我发起的审批实例分页 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalInstanceMyRespVO {

    @Schema(description = "审批实例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "业务单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "sales_contract")
    private String bizType;

    @Schema(description = "单据类型中文名称", example = "销售合同")
    private String bizTypeName;

    @Schema(description = "业务单据ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    private Long bizId;

    @Schema(description = "业务单号", example = "HT20261010001")
    private String bizNo;

    @Schema(description = "实例状态（APPROVING / APPROVED / REJECTED / CANCELLED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVING")
    private String status;

    @Schema(description = "发起人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long submitterId;

    @Schema(description = "发起人姓名", example = "张三")
    private String submitterName;

    @Schema(description = "当前节点序号（-1表示已结束）", example = "1")
    private Integer currentNodeSort;

    @Schema(description = "当前节点名称", example = "部门主管审批")
    private String currentNodeName;

    @Schema(description = "发起时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "审批结束时间")
    private LocalDateTime endTime;

}
