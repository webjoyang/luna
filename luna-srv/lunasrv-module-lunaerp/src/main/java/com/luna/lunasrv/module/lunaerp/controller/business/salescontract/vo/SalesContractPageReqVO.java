package com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 业务管理-销售合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesContractPageReqVO extends PageParam {

    @Schema(description = "关键词（命中 contractNo/clientName/clientContractNo 之一）", example = "CH2026")
    private String keyword;

    @Schema(description = "合同状态：ACTIVE/COMPLETED/CANCELLED", example = "ACTIVE")
    private String status;

    @Schema(description = "审批状态：DRAFT/APPROVING/APPROVED/REJECTED/CANCELLED", example = "APPROVING")
    private String approvalStatus;

    @Schema(description = "贸易类型：DOMESTIC/EXPORT", example = "DOMESTIC")
    private String tradeType;

    @Schema(description = "客户ID（预留，透传到查询构造）", example = "1024")
    private Long customerId;

}
