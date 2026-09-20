package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线采购单新增/修改 Request VO")
@Data
public class YarnOrderSaveReqVO {

    @Schema(description = "采购单ID", example = "1024")
    private Long id;

    @Schema(description = "采购单号（留空服务端自动生成）", example = "YP20260920001")
    private String orderNo;

    @Schema(description = "关联胚布指示单 ID 数组（JSON 字符串，如 \"[123,456]\"）", example = "[123,456]")
    private String sourceInstructions;

    @Schema(description = "单据日期", example = "2026-09-20")
    private LocalDate orderDate;

    @Schema(description = "交货日期", example = "2026-09-30")
    private LocalDate deliveryDate;

    @Schema(description = "损耗率百分比数字（5 = 5%）", example = "5.00")
    private BigDecimal lossRate;

    @Schema(description = "状态：DRAFT/ACTIVE/COMPLETED/CANCELLED", example = "DRAFT")
    private String status;

    @Schema(description = "备注", example = "首批大货纱线")
    private String remark;

    @Schema(description = "采购明细列表")
    @Valid
    private List<YarnOrderItemSaveReqVO> items;

}
