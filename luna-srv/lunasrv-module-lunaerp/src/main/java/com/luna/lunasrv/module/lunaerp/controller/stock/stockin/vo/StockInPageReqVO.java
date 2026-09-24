package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 库存管理 - 入库单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInPageReqVO extends PageParam {

    @Schema(description = "关键词（命中入库单号 / 供应商名称 / 关联合同号 / 明细产品编码 / 明细产品名称 之一）", example = "IN-2026")
    private String keyword;

    @Schema(description = "入库类型（PURCHASE 采购入库 / PRODUCTION 生产入库 / RETURN 退货入库 / OTHER 其他入库）", example = "PURCHASE")
    private String inType;

    @Schema(description = "入库单状态（DRAFT 草稿 / COMPLETED 已审核 / CANCELLED 已取消）", example = "DRAFT")
    private String status;

    @Schema(description = "仓库ID", example = "100")
    private Long warehouseId;

    @Schema(description = "供应商ID", example = "200")
    private Long supplierId;

    @Schema(description = "单据日期起", example = "2026-09-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate inDateStart;

    @Schema(description = "单据日期止", example = "2026-09-24")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate inDateEnd;

    @Schema(description = "单据日期区间", example = "[\"2026-09-01\", \"2026-09-24\"]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] inDate;

}
