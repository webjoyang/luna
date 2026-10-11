package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 销售合同下达生产 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseProductionRespVO {

    @Schema(description = "生成的胚布单列表")
    @Builder.Default
    private List<ReleaseOrderSimpleVO> greigeOrders = new ArrayList<>();

    @Schema(description = "生成的染色单列表")
    @Builder.Default
    private List<ReleaseOrderSimpleVO> dyeingOrders = new ArrayList<>();

    @Schema(description = "生成的检验单列表")
    @Builder.Default
    private List<ReleaseInspectionSimpleVO> inspectionOrders = new ArrayList<>();

    @Schema(description = "生成的打色指示单列表（NEW 模式新建）")
    @Builder.Default
    private List<ReleaseOrderSimpleVO> colorInstructions = new ArrayList<>();

}
