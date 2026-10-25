package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 员工费用报销单 Response VO")
@Data
public class ExpenseRespVO {

    @Schema(description = "报销单ID", example = "100")
    private Long id;

    @Schema(description = "报销单号", example = "BX20261019001")
    private String expenseNo;

    @Schema(description = "报销人用户ID", example = "10")
    private Long applicantUserId;

    @Schema(description = "报销人姓名快照", example = "张三")
    private String applicantName;

    @Schema(description = "付款资金账户ID", example = "1001")
    private Long accountId;

    @Schema(description = "付款资金账户名称快照", example = "基本户")
    private String accountName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "原币总金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal originalTotal;

    @Schema(description = "汇率快照", example = "1.00000000")
    private BigDecimal exchangeRate;

    @Schema(description = "本位币总金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal baseTotal;

    @Schema(description = "报销总金额（元）", example = "1000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal totalAmount;

    @Schema(description = "报销业务日期（明细最大发生日）", example = "2026-10-18")
    private LocalDate expenseDate;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "提交审批时间")
    private LocalDateTime submitTime;

    @Schema(description = "审批通过时间")
    private LocalDateTime approveTime;

    @Schema(description = "附件列表（有序URL数组）")
    private List<String> attachments;

    @Schema(description = "备注说明", example = "出差上海报销")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "报销明细行列表")
    private List<ExpenseItemRespVO> items;

    @Schema(description = "最新审批实例进度摘要")
    private ApprovalInstanceRespVO approvalProgress;

}
