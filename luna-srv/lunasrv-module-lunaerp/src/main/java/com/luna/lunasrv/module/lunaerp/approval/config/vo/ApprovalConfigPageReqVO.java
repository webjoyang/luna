package com.luna.lunasrv.module.lunaerp.approval.config.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 审批流程配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApprovalConfigPageReqVO extends PageParam {

    @Schema(description = "单据类型（按 biz_type 筛选）", example = "sales_contract")
    private String bizType;

    @Schema(description = "流程名称（模糊匹配）", example = "销售合同")
    private String name;

    @Schema(description = "状态（ENABLED 启用 / DISABLED 停用）", example = "ENABLED")
    private String status;

}
