package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 生产管理 - 织造单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WeavingOrderPageReqVO extends PageParam {

    @Schema(description = "关键词（命中 code / greige_order_no / contract_no / product_name / product_code 之一）", example = "ZZ-PB")
    private String keyword;

    @Schema(description = "关联胚布单ID", example = "1024")
    private Long greigeOrderId;

    @Schema(description = "织造单状态（字典 lunaerp_weaving_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "创建时间", example = "[\"2026-09-01 00:00:00\", \"2026-09-26 23:59:59\"]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
