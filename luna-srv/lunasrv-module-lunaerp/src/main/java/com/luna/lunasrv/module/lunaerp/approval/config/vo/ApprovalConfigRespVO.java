package com.luna.lunasrv.module.lunaerp.approval.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 审批流程配置 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigRespVO {

    @Schema(description = "流程配置ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "sales_contract")
    private String bizType;

    @Schema(description = "流程名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "标准销售合同审批流程")
    private String name;

    @Schema(description = "状态（ENABLED 启用 / DISABLED 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENABLED")
    private String status;

    @Schema(description = "备注说明", example = "合同金额审批链")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "节点列表（按 sort 升序）")
    private List<ApprovalConfigNodeVO> nodes;

}
