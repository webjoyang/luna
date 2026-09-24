package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 库存管理 - 入库单创建/修改 Request VO")
@Data
public class StockInSaveReqVO {

    @Schema(description = "入库单ID（修改时必填，新建为空）", example = "1024")
    private Long id;

    @Schema(description = "入库单号（IN-YYYYMMDD-NNN，为空自动按天生成，也可手填查重）", example = "IN-20260924-001")
    private String inNo;

    @Schema(description = "入库类型（字典 lunaerp_stock_in_type：PURCHASE/PRODUCTION/RETURN/OTHER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PURCHASE")
    @NotBlank(message = "入库类型不能为空")
    private String inType;

    @Schema(description = "入库仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "入库仓库不能为空")
    private Long warehouseId;

    @Schema(description = "供应商ID（可选）", example = "200")
    private Long supplierId;

    @Schema(description = "关联合同ID（可选）", example = "300")
    private Long salesContractId;

    @Schema(description = "关联胚布单ID（可选）", example = "400")
    private Long greigeOrderId;

    @Schema(description = "关联染色单ID（可选）", example = "500")
    private Long dyeingOrderId;

    @Schema(description = "关联胚布品种ID（可选）", example = "600")
    private Long greigeTypeId;

    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-24")
    @NotNull(message = "单据日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate inDate;

    @Schema(description = "备注", example = "采购大货入库")
    private String remark;

    @Schema(description = "入库明细列表（随整单聚合保存替换；零明细允许保存草稿）")
    @Valid
    private List<StockInItemSaveReqVO> items;

}
