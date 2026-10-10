package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "报价单明细 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemRespVO {

    @Schema(description = "报价明细ID", example = "100")
    private Long id;

    @Schema(description = "所属报价单ID", example = "10")
    private Long quotationId;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "产品档案ID", example = "10")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P-1001")
    private String productCode;

    @Schema(description = "品名快照", example = "高弹汗布")
    private String productName;

    @Schema(description = "客户色号快照", example = "BLK-01")
    private String customerColor;

    @Schema(description = "成分快照", example = "95%棉 5%氨纶")
    private String composition;

    @Schema(description = "门幅快照", example = "180CM")
    private String width;

    @Schema(description = "克重快照", example = "200GSM")
    private String weight;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "数量", example = "1000.00")
    private BigDecimal quantity;

    @Schema(description = "来源成本卡ID", example = "200")
    private Long costCardId;

    @Schema(description = "来源成本卡号快照", example = "CC20260001")
    private String costCardNo;

    @Schema(description = "成本来源（CARD / MANUAL）", example = "CARD")
    private String costSource;

    @Schema(description = "成本卡整份快照（JSON，脱敏字段）")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private String costSnapshot;

    @Schema(description = "RMB 单位成本（脱敏字段）", example = "25.5000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal costUnitPriceRmb;

    @Schema(description = "汇率快照", example = "1.000000")
    private BigDecimal exchangeRateSnapshot;

    @Schema(description = "折报价币单位成本（脱敏字段）", example = "25.5000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal costUnitPrice;

    @Schema(description = "目标毛利率（脱敏字段）", example = "0.150000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal targetMarginRate;

    @Schema(description = "报价单价（报价币种）", example = "30.0000")
    private BigDecimal unitPrice;

    @Schema(description = "行成本额（脱敏字段）", example = "25500.0000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal costAmount;

    @Schema(description = "行金额", example = "30000.0000")
    private BigDecimal amount;

    @Schema(description = "实际毛利率（脱敏字段）", example = "0.150000")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal marginRate;

    @Schema(description = "低毛利预警（不脱敏，仅布尔值）", example = "false")
    private Boolean lowMarginWarning;

    @Schema(description = "行备注", example = "急单需空运")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
