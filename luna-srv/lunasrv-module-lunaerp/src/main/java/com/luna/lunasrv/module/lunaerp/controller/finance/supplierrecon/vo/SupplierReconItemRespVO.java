package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应商对账单行 Response VO")
@Data
public class SupplierReconItemRespVO {

    @Schema(description = "行ID", example = "1")
    private Long id;

    @Schema(description = "所属对账单ID", example = "100")
    private Long reconId;

    @Schema(description = "行类型（PROCESS / PURCHASE / ADJUST）", example = "PROCESS")
    private String lineType;

    @Schema(description = "来源单据类型", example = "INSPECTION")
    private String sourceRefType;

    @Schema(description = "来源单据主表ID", example = "100")
    private Long sourceRefId;

    @Schema(description = "来源单据明细ID", example = "200")
    private Long sourceRefItemId;

    @Schema(description = "加工源检验单ID", example = "100")
    private Long inspectionOrderId;

    @Schema(description = "加工源检验单号快照", example = "INS-20261015-001")
    private String inspectionOrderNo;

    @Schema(description = "检验类型（GREIGE / FINISHED）", example = "GREIGE")
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

    @Schema(description = "千克数量", example = "125.50")
    private BigDecimal qtyKg;

    @Schema(description = "米数数量", example = "250.00")
    private BigDecimal qtyM;

    @Schema(description = "数量单位（KG / M）", example = "M")
    private String qtyUnit;

    @Schema(description = "计价基准（KG 按千克 / METER 按米）", example = "METER")
    private String pricingBasis;

    @Schema(description = "结算计费数量", example = "250.00")
    private BigDecimal billQty;

    @Schema(description = "结算单价（元）", example = "3.5000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unitPrice;

    @Schema(description = "行金额（元）", example = "875.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal lineAmount;

    @Schema(description = "调整原因（ADJUST 行必填）", example = "质量扣款")
    private String adjustReason;

    @Schema(description = "备注", example = "整体验收合格")
    private String remark;

}
