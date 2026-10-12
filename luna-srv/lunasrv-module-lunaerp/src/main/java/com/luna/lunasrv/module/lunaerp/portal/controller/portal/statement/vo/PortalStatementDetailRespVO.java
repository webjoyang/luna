package com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "客户门户 - 对账单详情响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalStatementDetailRespVO {

    @Schema(description = "对账单ID", example = "101")
    private Long id;

    @Schema(description = "对账单号", example = "CS202610110001")
    private String statementNo;

    @Schema(description = "客户ID", example = "1001")
    private Long customerId;

    @Schema(description = "客户名称", example = "广州盛达纺织有限公司")
    private String customerName;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "对账开始日期", example = "2026-10-01")
    private LocalDate periodStart;

    @Schema(description = "对账结束日期", example = "2026-10-31")
    private LocalDate periodEnd;

    @Schema(description = "期初欠款/余额", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "本期借方发生额（本期发货/应收增加）", example = "50000.00")
    private BigDecimal periodDebit;

    @Schema(description = "本期贷方发生额（本期回款/冲减）", example = "40000.00")
    private BigDecimal periodCredit;

    @Schema(description = "期末欠款/余额", example = "20000.00")
    private BigDecimal closingBalance;

    @Schema(description = "状态（SENT / CONFIRMED / REJECTED）", example = "SENT")
    private String status;

    @Schema(description = "确认类型（ONLINE_PORTAL 线上门户 / ON_BEHALF 内部代客）", example = "ONLINE_PORTAL")
    private String confirmType;

    @Schema(description = "确认人姓名", example = "张三")
    private String confirmByName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "驳回原因", example = "明细金额有误")
    private String rejectReason;

    @Schema(description = "备注", example = "10月份对账单")
    private String remark;

    @Schema(description = "对账明细行列表")
    private List<PortalStatementItemRespVO> items;

}
