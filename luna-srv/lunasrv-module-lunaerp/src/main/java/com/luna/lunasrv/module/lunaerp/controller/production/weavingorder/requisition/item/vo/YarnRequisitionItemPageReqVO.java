package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理-领纱明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class YarnRequisitionItemPageReqVO extends PageParam {

    @Schema(description = "所属领纱单ID", example = "1024")
    private Long requisitionId;

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "纱线规格ID", example = "10")
    private Long yarnSpecId;

}
