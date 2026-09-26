package com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 库存管理 - 库存流水台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockLedgerPageReqVO extends PageParam {

    @Schema(description = "物品类型（PRODUCT / YARN_SPEC）", example = "PRODUCT")
    private String itemType;

    @Schema(description = "物品关键词（编码/名称/纱线规格）", example = "棉纱")
    private String keyword;

    @Schema(description = "产品关键词（产品编码或产品名称，兼容旧字段）", example = "棉纱")
    private String productKeyword;

    @Schema(description = "仓库ID", example = "1")
    private Long warehouseId;

    @Schema(description = "库位ID", example = "5")
    private Long locationId;

    @Schema(description = "业务类型（STOCK_IN 入库 / STOCK_IN_REVERSAL 红冲）", example = "STOCK_IN")
    private String txnType;

    @Schema(description = "关联单号（如入库单号）", example = "IN-20260925-001")
    private String relatedDocNo;

    @Schema(description = "流水时间区间", example = "[\"2026-09-01 00:00:00\", \"2026-09-25 23:59:59\"]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
