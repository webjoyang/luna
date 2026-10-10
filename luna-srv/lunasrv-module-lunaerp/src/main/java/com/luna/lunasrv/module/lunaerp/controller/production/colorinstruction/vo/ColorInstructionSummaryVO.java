package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 打色指示单颜色摘要 VO（色号 chips + 轮次数）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionSummaryVO {

    @Schema(description = "客户色号", example = "C01")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "大红")
    private String colorName;

    @Schema(description = "色值十六进制", example = "#FF0000")
    private String colorHex;

    @Schema(description = "打色轮次", example = "1")
    private Integer round;

    @Schema(description = "行状态（PENDING/APPROVED/REJECTED）", example = "APPROVED")
    private String status;

}
