package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 付款申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentApplyPageReqVO extends PageParam {

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "申请类型（RECON 对账付款 / PREPAY 预付付款）", example = "RECON")
    private String applyType;

    @Schema(description = "审批状态（PENDING / APPROVING / APPROVED / REJECTED / CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "搜索关键字（申请单号/供应商名称/对账单号/用途）", example = "FK20261011001")
    private String keyword;

}
