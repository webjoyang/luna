package com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-打色指示单创建/修改 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionSaveReqVO {

    @Schema(description = "打色指示单ID（修改时必填，新建为空）", example = "1024")
    private Long id;

    @Schema(description = "打色单号（留空时后端自动生成 DS+yyyyMMdd+3位日流水，按天取号见 ADR-0006）", example = "DS20261010001")
    @Size(max = 32, message = "打色单号长度不能超过 32 位")
    private String instructionNo;

    @Schema(description = "客户档案ID（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "客户档案不能为空")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "优衣库")
    @Size(max = 128, message = "客户名称长度不能超过 128 位")
    private String clientName;

    @Schema(description = "客户联系人（文本，可含电话）", example = "张三 13800138000")
    @Size(max = 64, message = "联系人长度不能超过 64 位")
    private String contactPerson;

    @Schema(description = "产品档案ID（必填，强关联）", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "产品档案不能为空")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P001")
    @Size(max = 64, message = "产品编号长度不能超过 64 位")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    @Size(max = 128, message = "产品名称长度不能超过 128 位")
    private String productName;

    @Schema(description = "打色承接方供应商ID（可空=本厂打色房自做，ADR-0022 自织先例）", example = "3072")
    private Long labFactoryId;

    @Schema(description = "打色承接方公司名快照", example = "盛泽打色房")
    @Size(max = 128, message = "打色承接方公司名长度不能超过 128 位")
    private String labFactoryName;

    @Schema(description = "成分快照", example = "95%棉 5%氨纶")
    @Size(max = 128, message = "成分长度不能超过 128 位")
    private String composition;

    @Schema(description = "幅宽快照", example = "180cm")
    @Size(max = 64, message = "幅宽长度不能超过 64 位")
    private String width;

    @Schema(description = "克重快照", example = "200gsm")
    @Size(max = 64, message = "克重长度不能超过 64 位")
    private String gsm;

    @Schema(description = "单据日期（缺省今天）", example = "2026-10-10")
    private LocalDate instructionDate;

    @Schema(description = "要求完成日", example = "2026-10-20")
    private LocalDate deliveryDate;

    @Schema(description = "备注", example = "急单打色")
    private String remark;

    @Schema(description = "颜色明细行列表")
    @Valid
    private List<ColorInstructionColorSaveReqVO> colors;

    @Schema(description = "显式删除的颜色行ID集合（可选）")
    private List<Long> deletedColorIds;

}
