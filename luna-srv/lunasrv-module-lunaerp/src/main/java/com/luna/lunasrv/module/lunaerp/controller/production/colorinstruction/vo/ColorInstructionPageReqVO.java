package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理 - 打色指示单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ColorInstructionPageReqVO extends PageParam {

    @Schema(description = "打色单号（模糊查询）", example = "DS202610")
    private String instructionNo;

    @Schema(description = "客户档案ID", example = "1024")
    private Long customerId;

    @Schema(description = "产品编号或名称（模糊查询）", example = "纯棉")
    private String productCodeOrName;

    @Schema(description = "客户色号（模糊查询）", example = "C01")
    private String customerColorNo;

    @Schema(description = "单据状态：PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID", example = "PENDING")
    private String status;

    @Schema(description = "单据状态列表（如查询可引用的打色单 CONFIRMED,IN_PRODUCTION）", example = "[\"CONFIRMED\", \"IN_PRODUCTION\"]")
    private java.util.List<String> statusList;

    @Schema(description = "关键字（同时匹配单号、客户名称、产品编号/名称）", example = "纯棉")
    private String keyword;

    @Schema(description = "单据日期区间", example = "[2026-10-01, 2026-10-31]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] instructionDateRange;

}
