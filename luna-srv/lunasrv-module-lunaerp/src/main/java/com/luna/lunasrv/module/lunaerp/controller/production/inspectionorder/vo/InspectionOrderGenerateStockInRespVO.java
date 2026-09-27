package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 检验单生成胚布入库单 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionOrderGenerateStockInRespVO {

    @Schema(description = "入库单ID", example = "2048")
    private Long stockInId;

    @Schema(description = "入库单号", example = "IN-20260927-001")
    private String stockInNo;

    @Schema(description = "本次生成入库明细条数", example = "10")
    private Integer generatedCount;

    @Schema(description = "跳过（已生成或不合格）明细条数", example = "2")
    private Integer skippedCount;

}
