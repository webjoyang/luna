package com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理 - 发染记录创建 Request VO
 *
 * @author luna
 */
@Schema(description = "生产管理 - 发染记录创建 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreigeDispatchRecordCreateReqVO {

    @Schema(description = "胚布单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "胚布单不能为空")
    private Long greigeOrderId;

    @Schema(description = "染色单ID（可空，可后续补填）", example = "2001")
    private Long dyeingOrderId;

    @Schema(description = "承染染厂供应商ID（可空，可后续补填）", example = "3001")
    private Long dyeingFactoryId;

    @Schema(description = "染厂公司名快照", example = "XX染厂")
    private String dyeingFactoryName;

    @Schema(description = "出库仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    @NotNull(message = "出库仓库不能为空")
    private Long warehouseId;

    @Schema(description = "发染日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-28")
    @NotNull(message = "发染日期不能为空")
    private LocalDate dispatchDate;

    @Schema(description = "发染数量 KG", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    @NotNull(message = "发染数量不能为空")
    @DecimalMin(value = "0.01", message = "发染数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "备注", example = "急单优先")
    @Size(max = 1000, message = "备注长度不能超过 1000 个字符")
    private String remark;

}
