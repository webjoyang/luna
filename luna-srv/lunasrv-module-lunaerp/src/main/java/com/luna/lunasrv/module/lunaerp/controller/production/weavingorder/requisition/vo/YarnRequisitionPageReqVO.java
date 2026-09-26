package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 生产管理-领纱单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnRequisitionPageReqVO extends PageParam {

    @Schema(description = "关联织造单ID", example = "100")
    private Long weavingOrderId;

    @Schema(description = "领纱单号", example = "YS-ZZPB091900101-01")
    private String code;

    @Schema(description = "领纱单状态（DRAFT/ACTIVE/PARTIAL/DISPATCHED/CLOSED）", example = "DRAFT")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
