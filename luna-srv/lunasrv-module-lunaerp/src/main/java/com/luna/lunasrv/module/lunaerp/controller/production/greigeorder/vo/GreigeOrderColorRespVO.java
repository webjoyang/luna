package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-胚布单颜色投胚分配行 Response VO")
@Data
public class GreigeOrderColorRespVO {

    @Schema(description = "颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "胚布单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long orderId;

    @Schema(description = "行号（按 line_no 升序返回）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

    @Schema(description = "来源合同行 ID", example = "2048")
    private Long contractLineId;

    @Schema(description = "来源合同行号快照", example = "1")
    private Integer contractLineNo;

    @Schema(description = "颜色", example = "大红")
    private String color;

    @Schema(description = "成品下单量 KG", example = "500.00")
    private BigDecimal finishedQty;

    @Schema(description = "损耗率（小数，0.05 = 5%；保存时空值取主单预设损耗、再缺省 0）", example = "0.05")
    private BigDecimal lossRate;

    @Schema(description = "投胚需求 KG（服务端权威重算：成品量÷(1−损耗率)）", example = "526.32")
    private BigDecimal demandQty;

    @Schema(description = "行备注", example = "加急")
    private String remark;

}
