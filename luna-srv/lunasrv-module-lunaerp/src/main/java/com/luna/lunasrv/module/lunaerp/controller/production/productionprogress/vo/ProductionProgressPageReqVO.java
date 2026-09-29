package com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 生产管理 - 生产进度分页 Request VO（只读聚合，ADR-0034）
 *
 * <p>硬过滤（关键字/交期范围/状态）下推 Mapper；排序、仅看预警、分页由 Service 内存完成
 * （预警需聚合后才能判定）。默认排除 CANCELLED 合同，可切 includeCancelled 查看。</p>
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理-生产进度分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionProgressPageReqVO extends PageParam {

    @Schema(description = "关键字（合同号/客户名模糊）", example = "CH2026")
    private String keyword;

    @Schema(description = "交期起（含）", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate deliveryDateStart;

    @Schema(description = "交期止（含）", example = "2026-09-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate deliveryDateEnd;

    @Schema(description = "合同状态（ACTIVE/COMPLETED/CANCELLED），为空时默认排除 CANCELLED", example = "ACTIVE")
    private String status;

    @Schema(description = "仅看有预警的合同", example = "false")
    private Boolean alertOnly;

    @Schema(description = "是否包含已取消合同（无 status 时生效）", example = "false")
    private Boolean includeCancelled;

}
