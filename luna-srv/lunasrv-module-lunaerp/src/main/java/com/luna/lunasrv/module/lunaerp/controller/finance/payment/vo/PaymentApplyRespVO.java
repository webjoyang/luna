package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 付款申请 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApplyRespVO {

    @Schema(description = "付款申请ID", example = "100")
    private Long id;

    @Schema(description = "付款申请单号", example = "FK20261011001")
    private String applyNo;

    @Schema(description = "申请类型（RECON 对账付款 / PREPAY 预付付款）", example = "RECON")
    private String applyType;

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "浙江某某织造有限公司")
    private String supplierName;

    @Schema(description = "关联对账单ID", example = "200")
    private Long reconId;

    @Schema(description = "关联对账单号", example = "SR20261011001")
    private String reconNo;

    @Schema(description = "本次申请付款金额（元）", example = "10000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "付款用途（PREPAY 类型必填）", example = "纱线采购定金")
    private String purpose;

    @Schema(description = "开户行名称快照", example = "中国工商银行")
    private String bankName;

    @Schema(description = "银行账号快照", example = "6222021000000000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.BANK_CARD)
    private String bankAccount;

    @Schema(description = "审批状态（PENDING / APPROVING / APPROVED / REJECTED / CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "备注", example = "第一批付款申请")
    private String remark;

    @Schema(description = "已核销金额（元，实时聚合，只读）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocatedAmount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
