package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 资金账户间转账分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferPageReqVO extends PageParam {

    @Schema(description = "转账单号", example = "ZZ20261019001")
    private String transferNo;

    @Schema(description = "转出资金账户ID", example = "1001")
    private Long fromAccountId;

    @Schema(description = "转入资金账户ID", example = "1002")
    private Long toAccountId;

    @Schema(description = "转账日期起", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate transferDateStart;

    @Schema(description = "转账日期止", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate transferDateEnd;

}
