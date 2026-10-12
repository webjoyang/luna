package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "客户门户 - 发货单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalShipmentPageReqVO extends PageParam {

    @Schema(description = "签收状态（PENDING 待签收 / SIGNED 已签收 / REFUSED 拒签）", example = "PENDING")
    private String signStatus;

}
