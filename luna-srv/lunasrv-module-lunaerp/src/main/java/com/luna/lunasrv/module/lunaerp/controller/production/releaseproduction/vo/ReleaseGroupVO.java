package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 销售合同下达生产产品分组 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseGroupVO {

    @Schema(description = "产品档案ID", example = "101")
    private Long productId;

    @Schema(description = "产品编号", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉汗布")
    private String productName;

    @Schema(description = "成分", example = "100%棉")
    private String composition;

    @Schema(description = "幅宽", example = "180cm")
    private String width;

    @Schema(description = "克重", example = "180g")
    private String gsm;

    @Schema(description = "打色单模式（EXISTING 选已有 / NEW 新建 PENDING / NONE 不关联）", example = "EXISTING")
    private String colorInstructionMode;

    @Schema(description = "已有打色单ID（EXISTING 模式时必填）", example = "5")
    private Long existingColorInstructionId;

    @Schema(description = "新建打色单信息（NEW 模式时填）")
    @Valid
    private ReleaseNewColorInstructionVO newColorInstruction;

    @Schema(description = "分组明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "分组明细行列表不能为空")
    @Valid
    private List<ReleaseGroupLineVO> lines;

}
