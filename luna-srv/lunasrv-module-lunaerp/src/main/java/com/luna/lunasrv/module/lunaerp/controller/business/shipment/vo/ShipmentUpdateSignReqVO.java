package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 业务管理 - 发货单签收登记 Request VO")
@Data
public class ShipmentUpdateSignReqVO {

    @Schema(description = "发货单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "发货单ID不能为空")
    private Long id;

    @Schema(description = "签收状态（字典 lunaerp_shipment_sign_status：PENDING/SIGNED/REFUSED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SIGNED")
    @NotBlank(message = "签收状态不能为空")
    private String signStatus;

    @Schema(description = "签收人姓名", example = "张收货")
    private String signedByName;

    @Schema(description = "签收时间", example = "2026-09-28 14:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime signedAt;

    @Schema(description = "签收备注", example = "外包装完好，实收10件")
    private String signRemark;

}
