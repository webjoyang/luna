package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理-检验单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionOrderPageReqVO extends PageParam {

    @Schema(description = "关键词（命中 code/product_code/product_name/inspection_factory_name 之一）", example = "INS-2026")
    private String keyword;

    @Schema(description = "检验类型：GREIGE 胚布检验 / FINISHED 成品检验", example = "GREIGE")
    private String type;

    @Schema(description = "检验单状态：DRAFT/ACTIVE/COMPLETED/CANCELLED", example = "ACTIVE")
    private String status;

    @Schema(description = "承检方供应商ID", example = "2048")
    private Long inspectionFactoryId;

    @Schema(description = "单据日期起", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate orderDateStart;

    @Schema(description = "单据日期止", example = "2026-09-21")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate orderDateEnd;

    @Schema(description = "单据日期区间", example = "[2026-09-01, 2026-09-21]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] orderDate;

}
