package com.luna.lunasrv.module.lunaerp.approval.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 审批流程配置创建/更新 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigSaveReqVO {

    @Schema(description = "流程配置ID（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "单据类型（sales_contract/payment/expense/payroll/sales_return）", requiredMode = Schema.RequiredMode.REQUIRED, example = "sales_contract")
    @NotBlank(message = "单据类型不能为空")
    private String bizType;

    @Schema(description = "流程名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "标准销售合同审批流程")
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @Schema(description = "状态（ENABLED 启用 / DISABLED 停用，默认 ENABLED）", example = "ENABLED")
    private String status;

    @Schema(description = "备注说明", example = "合同金额审批链")
    private String remark;

    @Schema(description = "节点列表（整体替换）")
    @Valid
    private List<ApprovalConfigNodeVO> nodes;

}
