package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 车间移动 H5 - 领纱单明细精简出参 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 领纱单明细精简出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopRequisitionItemRespVO {

    @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "领纱单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long requisitionId;

    @Schema(description = "纱线材质ID", example = "11")
    private Long yarnTypeId;

    @Schema(description = "纱线规格ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "40S/1 纯棉精梳纱")
    private String yarnSpecName;

    @Schema(description = "需求量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal requiredQty;

    @Schema(description = "已发量", requiredMode = Schema.RequiredMode.REQUIRED, example = "30.00")
    private BigDecimal dispatchedQty;

    @Schema(description = "计量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "KG")
    private String unit;

    @Schema(description = "备注", example = "首批领用")
    private String remark;

}
