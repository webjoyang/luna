package com.luna.lunasrv.module.lunaerp.approval.instance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审批节点进度 Response VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 审批节点进度 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNodeProgressRespVO {

    @Schema(description = "节点序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "部门主管审批")
    private String name;

    @Schema(description = "节点类型（SEQUENTIAL / OR_SIGN / AND_SIGN）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SEQUENTIAL")
    private String nodeType;

    @Schema(description = "审批人来源类型", example = "USER")
    private String approverType;

    @Schema(description = "审批人用户ID列表")
    private List<Long> approverUserIds;

    @Schema(description = "审批人姓名列表")
    private List<String> approverNames;

    @Schema(description = "节点状态（PASSED / CURRENT / PENDING / REJECTED / CANCELLED）", example = "CURRENT")
    private String status;

    @Schema(description = "节点状态文本描述", example = "审批中")
    private String statusDesc;

}
