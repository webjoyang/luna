package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 生产管理-检验单创建/修改 Request VO")
@Data
public class InspectionOrderSaveReqVO {

    @Schema(description = "检验单ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "检验单号（INS-YYYYMMDD-NNN，为空由后端自动生成，手填查重）", example = "INS-20260921-001")
    @Size(max = 32, message = "检验单号长度不能超过 32 位")
    private String code;

    @Schema(description = "检验类型（GREIGE 胚布检验 / FINISHED 成品检验）", requiredMode = Schema.RequiredMode.REQUIRED, example = "GREIGE")
    @NotBlank(message = "检验类型不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^(GREIGE|FINISHED)$", message = "检验类型只能为 GREIGE 或 FINISHED")
    private String type;

    @Schema(description = "包装模式（字典 lunaerp_inspection_package_mode：ROLL 卷装 / PIECE 匹装，缺省 ROLL）", example = "ROLL")
    @jakarta.validation.constraints.Pattern(regexp = "^(ROLL|PIECE)$", message = "包装模式只能为 ROLL 或 PIECE")
    private String packageMode;

    @Schema(description = "检验单状态（字典 lunaerp_inspection_order_status，缺省 DRAFT）", example = "DRAFT")
    private String status;

    @Schema(description = "关联产品ID（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "关联产品不能为空")
    private Long productId;

    @Schema(description = "关联胚布单ID（type=GREIGE 时可选）", example = "201")
    private Long greigeOrderId;

    @Schema(description = "关联染色单ID（type=FINISHED 时可选）", example = "301")
    private Long dyeingOrderId;

    @Schema(description = "承检方供应商ID（可选）", example = "401")
    private Long inspectionFactoryId;

    @Schema(description = "关联销售合同ID（可选）", example = "501")
    private Long salesContractId;

    @Schema(description = "目标重量 KG", example = "1200.50")
    private BigDecimal targetWeight;

    @Schema(description = "备注", example = "优先检验 A 等品")
    @Size(max = 1000, message = "备注长度不能超过 1000 位")
    private String remark;

    @Schema(description = "单据日期（缺省当天）", example = "2026-09-21")
    private LocalDate orderDate;

}
