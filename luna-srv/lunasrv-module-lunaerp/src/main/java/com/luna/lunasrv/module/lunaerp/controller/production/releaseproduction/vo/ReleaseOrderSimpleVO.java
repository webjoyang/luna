package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 销售合同下达生产单据精简 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseOrderSimpleVO {

    @Schema(description = "单据ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "单号", example = "PB20261011001")
    private String orderNo;

    public ReleaseOrderSimpleVO(Long id) {
        this.id = id;
    }

}
