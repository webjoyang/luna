package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 打色指示单创建/保存 Request VO")
@Data
public class ColorInstructionSaveReqVO {

    @Schema(description = "打色指示单ID（新建时空，修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "打色单号（选填，留空由系统自动按天生成 DS+yyyyMMdd+3位流水）", example = "DS20261013001")
    private String instructionNo;

    @Schema(description = "客户档案ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "客户联系人（文本，可含电话）", example = "张经理 13800138000")
    private String contactPerson;

    @Schema(description = "产品档案ID（强关联）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "产品不能为空")
    private Long productId;

    @Schema(description = "打色承接方供应商ID（空=本厂打色房自做）", example = "3001")
    private Long labFactoryId;

    @Schema(description = "成分快照", example = "100% 棉")
    private String composition;

    @Schema(description = "幅宽快照", example = "180cm")
    private String width;

    @Schema(description = "克重快照", example = "200g/m²")
    private String gsm;

    @Schema(description = "单据日期（缺省今天）", example = "2026-10-13")
    private LocalDate instructionDate;

    @Schema(description = "要求完成日", example = "2026-10-20")
    private LocalDate deliveryDate;

    @Schema(description = "备注", example = "客户急需，优先安排")
    private String remark;

    @Schema(description = "打色颜色行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "打色单颜色明细不能为空")
    @Valid
    private List<ColorInstructionColorSaveVO> colors;

}

