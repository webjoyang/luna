package com.luna.lunasrv.module.lunaerp.approval.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 审批流程节点 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigNodeVO {

    @Schema(description = "节点定义ID", example = "101")
    private Long id;

    @Schema(description = "关联流程配置ID", example = "10")
    private Long configId;

    @Schema(description = "节点顺序（从1开始）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "节点顺序不能为空")
    private Integer sort;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "部门主管审批")
    @NotBlank(message = "节点名称不能为空")
    private String name;

    @Schema(description = "节点类型（SEQUENTIAL 逐级 / OR_SIGN 或签 / AND_SIGN 会签）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SEQUENTIAL")
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @Schema(description = "审批人来源（USER 指定用户 / POST 指定岗位 / ROLE 指定角色 / DEPT_LEADER 部门负责人 / SUBMITTER_LEADER 提交人直属上级）", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER")
    @NotBlank(message = "审批人来源不能为空")
    private String approverType;

    @Schema(description = "审批人ID标识（USER时存用户ID列表JSON；POST/ROLE时存岗位或角色ID）", example = "[1, 2]")
    private String approverIds;

}
