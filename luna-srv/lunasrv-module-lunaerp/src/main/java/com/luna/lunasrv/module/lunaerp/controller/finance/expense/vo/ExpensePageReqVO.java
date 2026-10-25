package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 员工费用报销单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExpensePageReqVO extends PageParam {

    @Schema(description = "报销单号（模糊匹配）", example = "BX202610")
    private String expenseNo;

    @Schema(description = "报销人用户ID", example = "10")
    private Long applicantUserId;

    @Schema(description = "报销人姓名（模糊匹配）", example = "张三")
    private String applicantName;

    @Schema(description = "付款资金账户ID", example = "1001")
    private Long accountId;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "APPROVED")
    private String approvalStatus;

    @Schema(description = "费用类别（字典 2284）", example = "TRAVEL")
    private String category;

    @Schema(description = "报销业务起始日期", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expenseDateStart;

    @Schema(description = "报销业务截止日期", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expenseDateEnd;

}
