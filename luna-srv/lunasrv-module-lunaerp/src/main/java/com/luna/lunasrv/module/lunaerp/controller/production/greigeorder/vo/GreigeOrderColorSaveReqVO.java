package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 生产管理-胚布单颜色投胚分配行创建/修改 Request VO")
@Data
public class GreigeOrderColorSaveReqVO {

    @Schema(description = "颜色", example = "大红")
    @Size(max = 64, message = "颜色长度不能超过 64 位")
    private String color;

    @Schema(description = "成品下单量 KG（null 按 0 处理）", example = "500.00")
    private BigDecimal finishedQty;

    @Schema(description = "损耗率（小数，0.05 = 5%；为空时取主单预设损耗、再缺省 0）", example = "0.05")
    private BigDecimal lossRate;

    @Schema(description = "投胚需求 KG（服务端权威重算：成品量÷(1−损耗率)，前端传值被覆盖）", example = "526.32")
    private BigDecimal demandQty;

    @Schema(description = "行备注", example = "加急")
    @Size(max = 256, message = "行备注长度不能超过 256 位")
    private String remark;

}
