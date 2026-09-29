package com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产管理 - 发染记录 Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理-发染记录 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreigeDispatchRecordRespVO {

    @Schema(description = "发染记录ID", example = "1001")
    private Long id;

    @Schema(description = "胚布单ID", example = "100")
    private Long greigeOrderId;

    @Schema(description = "染色单ID", example = "2001")
    private Long dyeingOrderId;

    @Schema(description = "承染染厂供应商ID", example = "3001")
    private Long dyeingFactoryId;

    @Schema(description = "染厂公司名快照", example = "XX染厂")
    private String dyeingFactoryName;

    @Schema(description = "出库仓库ID", example = "5001")
    private Long warehouseId;

    @Schema(description = "仓库名称快照", example = "主仓")
    private String warehouseName;

    @Schema(description = "产品ID", example = "6001")
    private Long productId;

    @Schema(description = "产品编码快照", example = "P001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉府绸")
    private String productName;

    @Schema(description = "发染日期", example = "2026-09-28")
    private LocalDate dispatchDate;

    @Schema(description = "发染数量 KG", example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "关联库存流水ID", example = "9001")
    private Long stockLedgerId;

    @Schema(description = "备注", example = "急单优先")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
