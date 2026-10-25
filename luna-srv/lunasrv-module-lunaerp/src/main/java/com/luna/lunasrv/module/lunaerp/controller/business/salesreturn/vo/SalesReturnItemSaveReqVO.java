package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 销售退货明细行 Save Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnItemSaveReqVO {

    @Schema(description = "退货明细ID（修改已有明细时传递）", example = "101")
    private Long id;

    @Schema(description = "强制发货明细血缘ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "201")
    @NotNull(message = "发货明细不能为空")
    private Long shipmentItemId;

    @Schema(description = "退货计费数量（手填，受可退量守卫）", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    @NotNull(message = "退货数量不能为空")
    @DecimalMin(value = "0.01", message = "退货数量必须大于0")
    private BigDecimal returnQty;

    @Schema(description = "匹数（缺省1）", example = "1")
    private Integer pieces;

    @Schema(description = "退货单价（元，缺省原发货单价，允许改折让价）", example = "25.0000")
    private BigDecimal unitPrice;

    @Schema(description = "退货原因（字典 lunaerp_sales_return_reason：QUALITY/WRONG/EXCESS/CUSTOMER/OTHER）", requiredMode = Schema.RequiredMode.REQUIRED, example = "QUALITY")
    @NotBlank(message = "退货原因不能为空")
    private String returnReason;

    @Schema(description = "备注", example = "经纬密不符")
    private String remark;

    @Schema(description = "逐卷登记列表（每行至少一卷）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "每行至少登记一卷")
    @Valid
    private List<SalesReturnRollSaveReqVO> rolls;

}
