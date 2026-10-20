package com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "客户门户 - 发货单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalShipmentPageReqVO extends PageParam {

    @Schema(description = "签收状态（PENDING/SIGNED/REFUSED/ALL）", example = "PENDING")
    private String signStatus;

    @Schema(description = "状态别名（兼容前端参数 status）", example = "PENDING")
    private String status;

}
