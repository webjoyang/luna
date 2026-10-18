package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 业务管理 - 销售退货修改 Update Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnUpdateReqVO {

    @Schema(description = "退货单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "退货单ID不能为空")
    private Long id;

    @Schema(description = "退货入库仓库ID（草稿可改）", requiredMode = Schema.RequiredMode.REQUIRED, example = "400")
    @NotNull(message = "退货仓库不能为空")
    private Long warehouseId;

    @Schema(description = "退货单据日期", example = "2026-10-18")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate returnDate;

    @Schema(description = "总备注", example = "客户整车退回")
    private String remark;

    @Schema(description = "退货明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "退货明细行不能为空")
    @Valid
    private List<SalesReturnItemSaveReqVO> items;

}
