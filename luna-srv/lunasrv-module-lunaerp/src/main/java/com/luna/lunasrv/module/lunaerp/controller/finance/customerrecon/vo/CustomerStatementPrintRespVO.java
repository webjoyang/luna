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

@Schema(description = "管理后台 - 客户对账单打印数据 Response VO（豁免金额脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatementPrintRespVO {

    @Schema(description = "业务类型（固定 FINANCE_STATEMENT，契约对齐 04 号通用件）", example = "FINANCE_STATEMENT")
    private String bizType;

    @Schema(description = "打印标题", example = "客户对账单")
    private String title;

    @Schema(description = "对账单ID", example = "100")
    private Long id;

    @Schema(description = "对账单号", example = "CS20260001")
    private String statementNo;

    @Schema(description = "客户ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称（抬头）", example = "杭州某某服装有限公司")
    private String customerName;

    @Schema(description = "统一社会信用代码/纳税人识别号", example = "91330100MA2XXXXX1")
    private String taxNo;

    @Schema(description = "公司/开票地址", example = "浙江省杭州市余杭区临平大道100号")
    private String address;

    @Schema(description = "开户银行", example = "中国工商银行杭州临平支行")
    private String bankName;

    @Schema(description = "开户行账号", example = "1202020209200000000")
    private String bankAccountNo;

    @Schema(description = "联系电话", example = "0571-88888888")
    private String contactPhone;

    @Schema(description = "对账币种", example = "CNY")
    private String currency;

    @Schema(description = "对账区间起始日期", example = "2026-09-01")
    private LocalDate periodStart;

    @Schema(description = "对账区间截止日期", example = "2026-09-30")
    private LocalDate periodEnd;

    @Schema(description = "期初欠款余额（元，豁免脱敏）", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "本期应收发生额/借方合计（元，豁免脱敏）", example = "25000.00")
    private BigDecimal periodDebit;

    @Schema(description = "本期收款核销额/贷方合计（元，豁免脱敏）", example = "15000.00")
    private BigDecimal periodCredit;

    @Schema(description = "期末应收余额（元，豁免脱敏）", example = "20000.00")
    private BigDecimal closingBalance;

    @Schema(description = "状态（DRAFT/SENT/CONFIRMED/REJECTED/VOID）", example = "CONFIRMED")
    private String status;

    @Schema(description = "确认方式（ONLINE_PORTAL/ON_BEHALF）", example = "ON_BEHALF")
    private String confirmType;

    @Schema(description = "确认人姓名", example = "张客户")
    private String confirmByName;

    @Schema(description = "确认时间", example = "2026-10-15 12:00:00")
    private LocalDateTime confirmTime;

    @Schema(description = "代客确认方式", example = "微信")
    private String confirmMethod;

    @Schema(description = "二维码完整URL（{luna.portal.base-url}/portal/statement?no={statementNo}）")
    private String qrUrl;

    @Schema(description = "备注", example = "9月份销售对账")
    private String remark;

    @Schema(description = "制单人 / 创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-10-15 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "全量明细行列表")
    private List<ItemVO> items;

    @Schema(description = "管理后台 - 客户对账单打印明细行 Response VO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVO {
        @Schema(description = "行号", example = "1")
        private Integer lineNo;

        @Schema(description = "业务日期", example = "2026-09-10")
        private LocalDate bizDate;

        @Schema(description = "往来方向（DEBIT 借 / CREDIT 贷）", example = "DEBIT")
        private String direction;

        @Schema(description = "来源单据类型快照", example = "SHIPMENT_ITEM")
        private String refType;

        @Schema(description = "来源业务单号", example = "SH2026090001")
        private String refNo;

        @Schema(description = "摘要（品名或事由）", example = "纯棉斜纹面料 32S")
        private String summary;

        @Schema(description = "借方金额（元，豁免脱敏）", example = "1000.00")
        private BigDecimal debitAmount;

        @Schema(description = "贷方金额（元，豁免脱敏）", example = "0.00")
        private BigDecimal creditAmount;

        @Schema(description = "签收状态标记", example = "SIGNED")
        private String signStatus;

        @Schema(description = "行备注说明")
        private String remark;
    }
}
