package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产管理-打色指示单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ColorInstructionPageReqVO extends PageParam {

    @Schema(description = "打色单号（模糊匹配）", example = "DS20261010")
    private String instructionNo;

    @Schema(description = "客户档案ID", example = "1001")
    private Long customerId;

    @Schema(description = "产品编号或名称（模糊匹配）", example = "纯棉")
    private String productCodeOrName;

    @Schema(description = "客户色号（模糊匹配颜色行）", example = "C01")
    private String customerColorNo;

    @Schema(description = "单据状态（PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）", example = "PENDING")
    private String status;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    @Schema(description = "单据日期范围（起止日期）")
    private LocalDate[] instructionDateRange;

}
