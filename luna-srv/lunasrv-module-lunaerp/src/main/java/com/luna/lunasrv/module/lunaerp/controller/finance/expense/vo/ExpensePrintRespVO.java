package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 员工费用报销单 A4 套打数据 Response VO（豁免金额脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePrintRespVO {

    @Schema(description = "业务类型（固定 EXPENSE，契约对齐 04 号通用件）", example = "EXPENSE")
    private String bizType;

    @Schema(description = "打印标题", example = "员工费用报销单")
    private String title;

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

    @Schema(description = "付款资金账户名称快照", example = "工商银行基本户")
    private String accountName;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "原币总金额（元，套打豁免脱敏）", example = "1000.00")
    private BigDecimal originalTotal;

    @Schema(description = "入账汇率", example = "1.00000000")
    private BigDecimal exchangeRate;

    @Schema(description = "本位币总金额（元，套打豁免脱敏）", example = "1000.00")
    private BigDecimal baseTotal;

    @Schema(description = "报销总金额（元，套打豁免脱敏）", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "报销业务日期", example = "2026-10-18")
    private LocalDate expenseDate;

    @Schema(description = "审批状态（固定 APPROVED）", example = "APPROVED")
    private String approvalStatus;

    @Schema(description = "提交审批时间")
    private LocalDateTime submitTime;

    @Schema(description = "审批通过时间")
    private LocalDateTime approveTime;

    @Schema(description = "备注说明", example = "出差上海报销")
    private String remark;

    @Schema(description = "制单人 / 创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "报销费用分项明细行列表")
    private List<ExpensePrintItemRespVO> items;

    @Schema(description = "附件名清单", example = "[\"发票1.pdf\", \"行程单.jpg\"]")
    private List<String> attachmentNames;

    @Schema(description = "报销人签章（快照报销人姓名）", example = "张三")
    private String applicantSign;

    @Schema(description = "部门主管签章", example = "李经理")
    private String departmentSign;

    @Schema(description = "财务审核签章", example = "王财务")
    private String financeSign;

    @Schema(description = "总经理/审批人签章", example = "赵总")
    private String managerSign;

    @Schema(description = "出纳付款签章", example = "孙出纳")
    private String cashierSign;

}
