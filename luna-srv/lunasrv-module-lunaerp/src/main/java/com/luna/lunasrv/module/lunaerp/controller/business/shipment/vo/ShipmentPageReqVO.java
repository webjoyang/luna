package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 业务管理 - 发货单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ShipmentPageReqVO extends PageParam {

    @Schema(description = "关键词（命中发货单号 / 客户名称 / 合同号 / 卷号 之一）", example = "SH-2026")
    private String keyword;

    @Schema(description = "发货单状态（DRAFT 草稿 / COMPLETED 已发货 / CANCELLED 已取消）", example = "DRAFT")
    private String status;

    @Schema(description = "客户ID", example = "100")
    private Long customerId;

    @Schema(description = "发货仓库ID", example = "300")
    private Long warehouseId;

    @Schema(description = "销售合同号", example = "CH20260001")
    private String contractNo;

    @Schema(description = "发货日期起", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate shippingDateStart;

    @Schema(description = "发货日期止", example = "2026-09-27")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate shippingDateEnd;

    @Schema(description = "发货日期区间", example = "[\"2026-09-01\", \"2026-09-27\"]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] shippingDate;

}
