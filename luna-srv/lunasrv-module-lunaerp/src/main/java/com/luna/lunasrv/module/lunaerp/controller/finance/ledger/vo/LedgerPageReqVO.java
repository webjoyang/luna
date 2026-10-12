package com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 经营分录账分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LedgerPageReqVO extends PageParam {

    @Schema(description = "往来主体类型（CUSTOMER/SUPPLIER）", example = "CUSTOMER")
    private String partyType;

    @Schema(description = "往来主体ID", example = "1001")
    private Long partyId;

    @Schema(description = "资金账户ID", example = "1")
    private Long accountId;

    @Schema(description = "分录类别（AR_INC, RECEIPT, RECEIPT_WRITE_OFF等）", example = "AR_INC")
    private String category;

    @Schema(description = "借贷方向（DEBIT/CREDIT）", example = "DEBIT")
    private String direction;

    @Schema(description = "币种（CNY/USD等）", example = "USD")
    private String currency;

    @Schema(description = "起始业务日期", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDate;

    @Schema(description = "截止业务日期", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDate;

}
