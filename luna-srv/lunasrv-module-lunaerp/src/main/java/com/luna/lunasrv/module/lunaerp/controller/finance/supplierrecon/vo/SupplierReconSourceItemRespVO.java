package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商对账 - 取数候选行响应 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 供应商对账取数候选行 Response VO")
@Data
public class SupplierReconSourceItemRespVO {

    @Schema(description = "行类型（PROCESS 加工费 / PURCHASE 采购货款 / ADJUST 调整项）", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROCESS")
    private String lineType;

    @Schema(description = "来源单据类型（INSPECTION 检验单 / STOCK_IN_ITEM 入库明细）", example = "INSPECTION")
    private String sourceRefType;

    @Schema(description = "来源单据主表ID", example = "100")
    private Long sourceRefId;

    @Schema(description = "来源单据明细ID（检验单汇总行可为空）", example = "200")
    private Long sourceRefItemId;

    @Schema(description = "加工源检验单ID", example = "100")
    private Long inspectionOrderId;

    @Schema(description = "加工源检验单号快照", example = "INS-20261015-001")
    private String inspectionOrderNo;

    @Schema(description = "检验类型（GREIGE 胚布检验=织费 / FINISHED 成品检验=染费）", example = "GREIGE")
    private String inspectionType;

    @Schema(description = "采购源入库单ID", example = "300")
    private Long stockInId;

    @Schema(description = "采购源入库单号快照", example = "IN-20261015-001")
    private String stockInNo;

    @Schema(description = "采购源入库单明细ID", example = "400")
    private Long stockInItemId;

    @Schema(description = "纱线采购单ID快照", example = "500")
    private Long yarnOrderId;

    @Schema(description = "纱线采购单号快照", example = "YO-20261015-001")
    private String yarnOrderNo;

    @Schema(description = "产品ID快照", example = "10")
    private Long productId;

    @Schema(description = "产品编码快照", example = "P-1001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "纯棉斜纹布")
    private String productName;

    @Schema(description = "批次/缸号快照", example = "B20261015")
    private String batchNo;

    @Schema(description = "千克数量（实称重量合计 / KG 单位数量）", example = "125.50")
    private BigDecimal qtyKg;

    @Schema(description = "米数数量（检验米数合计 / M 单位数量）", example = "250.00")
    private BigDecimal qtyM;

    @Schema(description = "卷数/匹数", example = "5")
    private Integer rollCount;

    @Schema(description = "数量单位（KG / M）", example = "M")
    private String qtyUnit;

    @Schema(description = "计价基准（KG 按千克 / METER 按米）", example = "METER")
    private String pricingBasis;

    @Schema(description = "结算计费数量（基准对应量）", example = "250.00")
    private BigDecimal billQty;

    @Schema(description = "结算单价（元）", example = "3.5000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unitPrice;

    @Schema(description = "行金额（元，bill_qty × unit_price）", example = "875.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal lineAmount;

    @Schema(description = "防重指纹（source_ref_type:source_ref_id[:source_ref_item_id]）", example = "INSPECTION:100")
    private String fingerprint;

}
