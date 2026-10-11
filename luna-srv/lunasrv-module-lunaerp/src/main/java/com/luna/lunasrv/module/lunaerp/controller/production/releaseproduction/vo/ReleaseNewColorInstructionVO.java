package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 销售合同下达生产新建打色单 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseNewColorInstructionVO {

    @Schema(description = "打色承接方供应商ID（可空=本厂打色房）", example = "3072")
    private Long labFactoryId;

    @Schema(description = "打色单颜色行列表")
    private List<ReleaseNewColorVO> colors;

}
