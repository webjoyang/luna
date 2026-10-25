package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 付款单分页查询 Request VO（OUT 侧）")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentPageReqVO extends PageParam {

    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    @Schema(description = "资金账户ID", example = "2001")
    private Long accountId;

    @Schema(description = "时间区间", example = "[\"2026-10-01 00:00:00\", \"2026-10-15 23:59:59\"]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "关键词（单号或备注模糊查询）", example = "PAY2026")
    private String keyword;

}
