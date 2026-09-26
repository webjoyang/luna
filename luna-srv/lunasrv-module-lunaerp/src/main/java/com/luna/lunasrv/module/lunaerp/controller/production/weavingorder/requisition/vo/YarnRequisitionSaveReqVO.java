package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 生产管理-领纱单创建/修改 Request VO")
@Data
public class YarnRequisitionSaveReqVO {

    @Schema(description = "领纱单ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "领纱单号（为空由系统自动生成 YS-{织造单号去符号}-{2位流水}）", example = "YS-ZZPB091900101-01")
    private String code;

    @Schema(description = "关联织造单ID（新建时必填）", example = "100")
    private Long weavingOrderId;

    @Schema(description = "备注", example = "领纱加急")
    private String remark;

}
