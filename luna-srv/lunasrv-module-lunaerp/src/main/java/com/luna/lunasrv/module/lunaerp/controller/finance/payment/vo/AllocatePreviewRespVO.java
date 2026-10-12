package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 自动核销分配试算结果 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocatePreviewRespVO {

    @Schema(description = "收款待分配金额（元）", example = "10000.00")
    private BigDecimal totalPayAmount;

    @Schema(description = "本次建议核销总额（元）", example = "10000.00")
    private BigDecimal totalAllocated;

    @Schema(description = "剩余未分配收款额（元）", example = "0.00")
    private BigDecimal remainingPayAmount;

    @Schema(description = "分配明细行列表")
    private List<AllocateItemRespVO> items;

}
