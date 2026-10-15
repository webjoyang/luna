package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 付款申请单 Response VO")
@Data
public class PaymentApplyRespVO {

    @Schema(description = "付款申请单ID", example = "1024")
    private Long id;

    @Schema(description = "申请单号（FK+YYYYMMDD+3位日流水）", example = "FK20261015001")
    private String applyNo;

    @Schema(description = "申请类型（RECON 对账付款 / PREPAY 预付付款）", example = "RECON")
    private String applyType;

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "供应商公司名快照", example = "某某织造有限公司")
    private String supplierName;

    @Schema(description = "关联合同对账单ID", example = "2001")
    private Long reconId;

    @Schema(description = "对账单号快照", example = "SR20261015001")
    private String reconNo;

    @Schema(description = "本次申请付款金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "已核销金额（元，只读实时聚合）", example = "2000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocatedAmount;

    @Schema(description = "剩余未核销金额（元，只读实时计算）", example = "3000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal remainAmount;

    @Schema(description = "预付用途备注", example = "采购纱线定金")
    private String purpose;

    @Schema(description = "收款银行名称快照", example = "中国工商银行")
    private String bankName;

    @Schema(description = "收款银行账号快照（脱敏）", example = "622202******3344")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.BANK_CARD)
    private String bankAccount;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "备注", example = "首期预付款")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-10-15 10:30:00")
    private LocalDateTime createTime;

}
