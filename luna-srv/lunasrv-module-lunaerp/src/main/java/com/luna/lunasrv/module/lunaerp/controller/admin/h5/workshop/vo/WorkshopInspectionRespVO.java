package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车间移动 H5 - 移动质检单精简出参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 移动质检单精简出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopInspectionRespVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INS-20261021-001")
    private String code;

    @Schema(description = "检验类型（GREIGE 胚布检验 / FINISHED 成品检验）", requiredMode = Schema.RequiredMode.REQUIRED, example = "GREIGE")
    private String type;

    @Schema(description = "包装模式（ROLL 卷装 / PIECE 匹装）", example = "PIECE")
    private String packageMode;

    @Schema(description = "检验单状态（DRAFT/ACTIVE/COMPLETED/CANCELLED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    private String status;

    @Schema(description = "关联产品ID", example = "101")
    private Long productId;

    @Schema(description = "产品编码快照", example = "P20261001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "关联胚布单ID", example = "201")
    private Long greigeOrderId;

    @Schema(description = "关联染色单ID", example = "301")
    private Long dyeingOrderId;

    @Schema(description = "承检方供应商ID", example = "401")
    private Long inspectionFactoryId;

    @Schema(description = "承检方公司名快照", example = "一号质检车间")
    private String inspectionFactoryName;

    @Schema(description = "关联销售合同ID", example = "501")
    private Long salesContractId;

    @Schema(description = "合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "目标重量 KG", example = "1200.50")
    private BigDecimal targetWeight;

    @Schema(description = "单据日期", example = "2026-10-21")
    private LocalDate orderDate;

    @Schema(description = "备注", example = "现场优先检验")
    private String remark;

    @Schema(description = "录检进度（已录卷数/件数）", example = "5")
    private Integer rollCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
