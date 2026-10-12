package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "客户门户 - 发货单签收登记 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalShipmentSignReqVO {

    @Schema(description = "发货单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "发货单 ID 不能为空")
    private Long id;

    @Schema(description = "签收状态（SIGNED 已签收 / REFUSED 拒签）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SIGNED")
    @NotBlank(message = "签收状态不能为空")
    private String signStatus;

    @Schema(description = "签收人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "签收人姓名不能为空")
    private String signedByName;

    @Schema(description = "签收/拒收备注", example = "货物外包装完好，实收全部卷数")
    private String signRemark;

}
