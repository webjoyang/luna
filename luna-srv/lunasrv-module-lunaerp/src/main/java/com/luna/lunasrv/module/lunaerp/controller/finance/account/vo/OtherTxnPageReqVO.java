package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 其他收支单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class OtherTxnPageReqVO extends PageParam {

    @Schema(description = "单号", example = "QT20261019001")
    private String txnNo;

    @Schema(description = "资金账户ID", example = "1001")
    private Long accountId;

    @Schema(description = "收支方向（DEBIT / CREDIT）", example = "DEBIT")
    private String direction;

    @Schema(description = "收支类别", example = "INTEREST_IN")
    private String category;

    @Schema(description = "业务日期起", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate txnDateStart;

    @Schema(description = "业务日期止", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate txnDateEnd;

}
