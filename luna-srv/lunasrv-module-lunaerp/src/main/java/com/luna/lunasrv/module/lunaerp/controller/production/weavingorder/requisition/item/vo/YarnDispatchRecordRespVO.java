package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 领纱明细发纱记录 Response VO")
@Data
public class YarnDispatchRecordRespVO {

    @Schema(description = "发纱流水ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "领纱明细ID", example = "1024")
    private Long itemId;

    @Schema(description = "纱线规格ID", example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线规格名称", example = "32S 棉")
    private String yarnSpecName;

    @Schema(description = "仓库ID", example = "1")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "原料仓")
    private String warehouseName;

    @Schema(description = "库位ID", example = "10")
    private Long locationId;

    @Schema(description = "库位编码", example = "A-01-01")
    private String locationCode;

    @Schema(description = "发纱数量(KG)", example = "50.00")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "交易类型", example = "YARN_OUT")
    private String txnType;

    @Schema(description = "是否已撤销红冲", example = "false")
    private Boolean reversed;

    @Schema(description = "撤销时间")
    private LocalDateTime reversalTime;

    @Schema(description = "发纱时间")
    private LocalDateTime createTime;

    @Schema(description = "操作人")
    private String creator;

    @Schema(description = "备注说明")
    private String remark;

    @Schema(description = "领纱单号")
    private String relatedDocNo;

}
