package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 收款单 Response VO")
@Data
public class ReceiptRespVO {

    @Schema(description = "收款单ID", example = "1024")
    private Long id;

    @Schema(description = "收款单号（RC-{yyyyMMdd}-{3位日流水}）", example = "RC-20261016-001")
    private String paymentNo;

    @Schema(description = "收付款方向（IN 固定）", example = "IN")
    private String direction;

    @Schema(description = "收款类型（RECEIPT 货款 / ADVANCE 预收）", example = "RECEIPT")
    private String receiptType;

    @Schema(description = "客户ID", example = "2001")
    private Long customerId;

    @Schema(description = "客户名称", example = "杭州某某服装进出口公司")
    private String customerName;

    @Schema(description = "资金账户ID", example = "3001")
    private Long accountId;

    @Schema(description = "资金账户名称", example = "招商银行一般户")
    private String accountName;

    @Schema(description = "收款币种", example = "RMB")
    private String currency;

    @Schema(description = "过账日汇率", example = "1.00000000")
    private BigDecimal exchangeRate;

    @Schema(description = "本次实收金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal payAmount;

    @Schema(description = "预收抵扣金额（元）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal prepayUsedAmount;

    @Schema(description = "已核销/分配合计金额（元）", example = "5000.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal allocatedTotalAmount;

    @Schema(description = "未核销金额（元）", example = "0.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unallocatedAmount;

    @Schema(description = "收款日期", example = "2026-10-16")
    private LocalDate receiptDate;

    @Schema(description = "银行回单号/交易流水号", example = "BK202610160001")
    private String bankRefNo;

    @Schema(description = "经手人姓名", example = "张会计")
    private String handlerName;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "单据状态（COMPLETED 登记完成 / REVERSED 红冲取消）", example = "COMPLETED")
    private String status;

    @Schema(description = "备注", example = "首期销售回款")
    private String remark;

    @Schema(description = "创建人/登记人", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-10-16 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "拟核销分配明细列表")
    private List<ReceiptItemRespVO> items;

}
