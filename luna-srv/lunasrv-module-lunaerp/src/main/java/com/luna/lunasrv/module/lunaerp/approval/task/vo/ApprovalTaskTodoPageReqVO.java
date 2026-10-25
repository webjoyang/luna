package com.luna.lunasrv.module.lunaerp.approval.task.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 待我审批分页 Request VO
 *
 * @author luna
 */
@Schema(description = "审批中心 - 待我审批分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApprovalTaskTodoPageReqVO extends PageParam {

    @Schema(description = "单据类型（如 sales_contract 等）", example = "sales_contract")
    private String bizType;

    @Schema(description = "单号模糊搜索", example = "HT2026")
    private String bizNo;

}
