package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 员工工资单 Response VO")
@Data
public class PayrollRespVO {

    @Schema(description = "工资单ID", example = "100")
    private Long id;

    @Schema(description = "工资单号", example = "GZ202610001")
    private String payrollNo;

    @Schema(description = "工资期间（YYYY-MM）", example = "2026-10")
    private String period;

    @Schema(description = "工资单标题", example = "2026-10 工资表")
    private String title;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "派生发放状态（UNPAID 未发放 / PARTIAL 部分发放 / PAID 已发清）", example = "UNPAID")
    private String paymentStatus;

    @Schema(description = "应发总计（元）", example = "50000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal earningTotal;

    @Schema(description = "扣款总计（元）", example = "2000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal deductionTotal;

    @Schema(description = "实发总计（元）", example = "48000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal netAmount;

    @Schema(description = "已发放聚合金额（元，Σ批次 original_amount）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal paidAmount;

    @Schema(description = "提交审批时间")
    private LocalDateTime submitTime;

    @Schema(description = "审批通过时间")
    private LocalDateTime approveTime;

    @Schema(description = "作废操作人", example = "1")
    private String cancelledBy;

    @Schema(description = "作废时间")
    private LocalDateTime cancelledAt;

    @Schema(description = "作废原因", example = "计件统计重算")
    private String cancelReason;

    @Schema(description = "备注说明", example = "10月工资单")
    private String remark;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "员工汇总行列表")
    private List<PayrollEmployeeRespVO> employees;

    @Schema(description = "审批进度摘要（02号引擎标准视图）")
    private ApprovalInstanceRespVO approvalProgress;

    @Schema(description = "发放批次列表")
    private List<PayrollPaymentRespVO> payments;

}
