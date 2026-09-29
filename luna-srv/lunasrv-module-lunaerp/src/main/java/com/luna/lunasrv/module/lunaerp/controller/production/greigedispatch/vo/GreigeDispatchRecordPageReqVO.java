package com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产管理 - 发染记录分页 Request VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 生产管理-发染记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeDispatchRecordPageReqVO extends PageParam {

    @Schema(description = "胚布单ID", example = "1001")
    private Long greigeOrderId;

    @Schema(description = "染色单ID", example = "2001")
    private Long dyeingOrderId;

}
