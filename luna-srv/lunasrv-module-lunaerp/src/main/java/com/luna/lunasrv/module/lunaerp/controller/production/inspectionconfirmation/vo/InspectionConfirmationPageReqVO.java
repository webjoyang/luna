package com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 生产管理-质检确认分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionConfirmationPageReqVO extends PageParam {

    @Schema(description = "关键词（命中 roll_no/vat_no/product_name/inspection_order_code 之一）", example = "P0309")
    private String keyword;

    @Schema(description = "状态（PENDING 待确认 / CONFIRMED 已认定 / REVOKED 已撤销，缺省为待确认+已认定）", example = "PENDING")
    private String status;

    @Schema(description = "来源（INSPECTION 检验单引入 / MANUAL 手工登记）", example = "INSPECTION")
    private String source;

    @Schema(description = "责任类型（EQUIPMENT 设备故障 / OPERATOR 操作失误 / WAIVED 免扣）", example = "OPERATOR")
    private String responsibility;

    @Schema(description = "挡车工用户ID", example = "1024")
    private Long workerUserId;

    @Schema(description = "来源检验单ID", example = "2048")
    private Long inspectionOrderId;

    @Schema(description = "创建时间/发现日区间", example = "[\"2026-09-01 00:00:00\", \"2026-09-27 23:59:59\"]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
