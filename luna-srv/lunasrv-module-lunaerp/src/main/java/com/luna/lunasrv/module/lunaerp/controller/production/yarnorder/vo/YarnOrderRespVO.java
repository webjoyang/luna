package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线采购单 Response VO")
@Data
public class YarnOrderRespVO {

    @Schema(description = "采购单ID", example = "1024")
    private Long id;

    @Schema(description = "采购单号", example = "YP20260920001")
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

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "明细行列表")
    private List<YarnOrderItemRespVO> items;

    @Schema(description = "关联胚布单展示快照列表")
    private List<YarnOrderCalculateGreigeOrderRespVO> sourceGreigeOrders;

    @Schema(description = "关联胚布单号拼接（列表展示用）", example = "PB0920001, PB0920002")
    private String sourceGreigeOrderNos;

    @Schema(description = "明细行数", example = "3")
    private Integer itemCount;

    @Schema(description = "总金额", example = "6500.00")
    private BigDecimal totalAmount;

}
