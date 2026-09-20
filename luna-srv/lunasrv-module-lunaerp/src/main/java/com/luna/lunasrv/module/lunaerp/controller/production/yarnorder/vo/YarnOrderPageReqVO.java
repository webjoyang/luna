package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理-纱线采购单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnOrderPageReqVO extends PageParam {

    @Schema(description = "关键词（命中 orderNo 或反查关联胚布单号）", example = "YP2026")
    private String keyword;

    @Schema(description = "采购单状态：DRAFT/ACTIVE/COMPLETED/CANCELLED", example = "ACTIVE")
    private String status;

    @Schema(description = "单据日期起", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate orderDateStart;

    @Schema(description = "单据日期止", example = "2026-09-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate orderDateEnd;

    @Schema(description = "交货日期起", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate deliveryDateStart;

    @Schema(description = "交货日期止", example = "2026-09-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate deliveryDateEnd;

    @Schema(description = "单据日期区间", example = "[2026-09-01, 2026-09-30]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] orderDate;

    @Schema(description = "交货日期区间", example = "[2026-09-01, 2026-09-30]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] deliveryDate;

}
