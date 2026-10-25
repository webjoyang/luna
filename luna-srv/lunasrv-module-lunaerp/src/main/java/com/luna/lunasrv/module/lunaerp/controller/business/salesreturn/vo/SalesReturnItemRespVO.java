package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货明细行 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnItemRespVO {

    @Schema(description = "退货明细ID", example = "101")
    private Long id;

    @Schema(description = "所属退货单ID", example = "10")
    private Long returnId;

    @Schema(description = "发货明细ID", example = "201")
    private Long shipmentItemId;

    @Schema(description = "产品ID", example = "301")
    private Long productId;

    @Schema(description = "产品编码快照", example = "PRD-001")
    private String productCode;

    @Schema(description = "产品名称快照", example = "纯棉斜纹")
    private String productName;

    @Schema(description = "客户色号快照", example = "WH-01")
    private String customerColorNo;

    @Schema(description = "颜色名称快照", example = "本白")
    private String colorName;

    @Schema(description = "缸号快照", example = "LOT-202609-01")
    private String vatNo;

    @Schema(description = "退货计费数量", example = "50.00")
    private BigDecimal returnQty;

    @Schema(description = "匹数", example = "1")
    private Integer pieces;

    @Schema(description = "单位（KG/M）", example = "KG")
    private String unit;

    @Schema(description = "单价（元）", example = "25.0000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal unitPrice;

    @Schema(description = "行金额（元）", example = "1250.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal amount;

    @Schema(description = "退货原因（字典 lunaerp_sales_return_reason）", example = "QUALITY")
    private String returnReason;

    @Schema(description = "可二次入库数量", example = "50.00")
    private BigDecimal restockQty;

    @Schema(description = "废品数量", example = "0.00")
    private BigDecimal scrapQty;

    @Schema(description = "备注", example = "色差退货")
    private String remark;

    @Schema(description = "退货卷清单列表")
    private List<SalesReturnRollRespVO> rolls;

}
