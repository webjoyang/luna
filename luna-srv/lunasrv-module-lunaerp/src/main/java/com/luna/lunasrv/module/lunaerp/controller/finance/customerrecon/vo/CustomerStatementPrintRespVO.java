package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 客户对账单打印数据响应 VO（豁免金额与联系人脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementPrintRespVO {

    @Schema(description = "对账单ID", example = "100")
    private Long id;

    @Schema(description = "对账单号", example = "CS20260001")
    private String statementNo;

    @Schema(description = "客户ID", example = "10")
    private Long customerId;

    @Schema(description = "客户名称", example = "广州盛世纺织有限公司")
    private String customerName;

    @Schema(description = "开票抬头", example = "广州盛世纺织有限公司")
    private String invoiceTitle;

    @Schema(description = "开票地址电话", example = "广州市海珠区新港西路100号 020-88888888")
    private String invoiceAddress;

    @Schema(description = "开户银行", example = "中国建设银行广州中大支行")
    private String bankName;

    @Schema(description = "开户行账号", example = "44050143004100001234")
    private String bankAccountNo;

    @Schema(description = "联系人姓名", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "期间开始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "期间结束日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "期初余额", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "本期借方发生额（本期发货增加）", example = "50000.00")
    private BigDecimal periodDebit;

    @Schema(description = "本期贷方发生额（本期收款/冲减）", example = "30000.00")
    private BigDecimal periodCredit;

    @Schema(description = "期末余额", example = "30000.00")
    private BigDecimal closingBalance;

    @Schema(description = "状态", example = "CONFIRMED")
    private String status;

    @Schema(description = "确认方式（ONLINE_PORTAL/ON_BEHALF）", example = "ON_BEHALF")
    private String confirmType;

    @Schema(description = "确认人姓名", example = "李四")
    private String confirmByName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "线下确认方式/凭证备注", example = "微信确认截图已归档")
    private String confirmMethod;

    @Schema(description = "驳回异议原因", example = "对部分运费有异议")
    private String rejectReason;

    @Schema(description = "驳回复核备注", example = "已扣减争议运费")
    private String replyRemark;

    @Schema(description = "备注说明", example = "9月份结算对账单")
    private String remark;

    @Schema(description = "二维码完整URL（{luna.portal.base-url}/portal/statement?no={statementNo}）")
    private String qrUrl;

    @Schema(description = "二维码基址是否降级（未配置基址时为 true）", example = "false")
    private Boolean qrDegraded;

    @Schema(description = "制单人", example = "admin")
    private String creator;

    @Schema(description = "制单时间")
    private LocalDateTime createTime;

    @Schema(description = "明细分录列表")
    private List<CustomerStatementPrintItemRespVO> items;

}
