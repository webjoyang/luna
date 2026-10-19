package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 员工工资单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollPageReqVO extends PageParam {

    @Schema(description = "工资单号（模糊匹配）", example = "GZ202610")
    private String payrollNo;

    @Schema(description = "工资期间（精确匹配，YYYY-MM）", example = "2026-10")
    private String period;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "APPROVED")
    private String approvalStatus;

    @Schema(description = "派生发放状态（UNPAID 未发放 / PARTIAL 部分发放 / PAID 已发清）", example = "UNPAID")
    private String paymentStatus;

}
