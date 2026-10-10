package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "报价单明细保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemSaveReqVO {

    @Schema(description = "报价明细ID", example = "100")
    private Long id;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "产品档案ID（手工录入时可空）", example = "10")
    private Long productId;

    @Schema(description = "产品编号快照", example = "P-1001")
    private String productCode;

    @Schema(description = "品名快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "高弹汗布")
    @NotBlank(message = "品名不能为空")
    private String productName;

    @Schema(description = "客户色号快照", example = "BLK-01")
    private String customerColor;

    @Schema(description = "成分快照", example = "95%棉 5%氨纶")
    private String composition;

    @Schema(description = "门幅快照", example = "180CM")
    private String width;

    @Schema(description = "克重快照", example = "200GSM")
    private String weight;

    @Schema(description = "单位（KG / M / Y，缺省 KG）", example = "KG")
    private String unit;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "来源成本卡ID", example = "200")
    private Long costCardId;

    @Schema(description = "来源成本卡号快照", example = "CC20260001")
    private String costCardNo;

    @Schema(description = "成本来源（CARD / MANUAL）", example = "CARD")
    private String costSource;

    @Schema(description = "成本卡整份快照（JSON）")
    private String costSnapshot;

    @Schema(description = "RMB 单位成本（无 amount 权限位时提交值会被忽略）", example = "25.5000")
    private BigDecimal costUnitPriceRmb;

    @Schema(description = "目标毛利率（反算单价输入，范围 (-100%, 100%)）", example = "0.150000")
    private BigDecimal targetMarginRate;

    @Schema(description = "报价单价（若传目标毛利率则反算；直接录入单价则反算实际毛利率）", example = "30.0000")
    private BigDecimal unitPrice;

    @Schema(description = "行备注", example = "急单需空运")
    private String remark;

}
