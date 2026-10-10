package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理-打色指示单颜色行 Response VO")
@Data
public class ColorInstructionColorRespVO {

    @Schema(description = "颜色行ID", example = "1024")
    private Long id;

    @Schema(description = "打色指示单ID", example = "100")
    private Long instructionId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "客户色号", example = "C01")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "大红")
    private String colorName;

    @Schema(description = "厂内色号", example = "F001")
    private String factoryColorNo;

    @Schema(description = "色值十六进制", example = "#FF0000")
    private String colorHex;

    @Schema(description = "主灯源", example = "D65")
    private String lightPrimary;

    @Schema(description = "辅灯源", example = "TL84")
    private String lightSecondary;

    @Schema(description = "色卡参考", example = "Pantone TPX 19-4052")
    private String colorCardRef;

    @Schema(description = "样布照片 JSON 数组")
    private String samplePhotos;

    @Schema(description = "份数", example = "2")
    private Integer copies;

    @Schema(description = "打色轮次", example = "1")
    private Integer round;

    @Schema(description = "行状态（PENDING/APPROVED/REJECTED）", example = "PENDING")
    private String status;

    @Schema(description = "驳回意见")
    private String rejectReason;

    @Schema(description = "行备注")
    private String remark;

}
