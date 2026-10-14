package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 下达生产 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseProductionRespVO {

    @Schema(description = "生成的胚布单列表")
    @Builder.Default
    private List<GreigeOrderSimpleVO> greigeOrders = new ArrayList<>();

    @Schema(description = "生成的染色单列表")
    @Builder.Default
    private List<DyeingOrderSimpleVO> dyeingOrders = new ArrayList<>();

    @Schema(description = "生成的检验单列表")
    @Builder.Default
    private List<InspectionOrderSimpleVO> inspectionOrders = new ArrayList<>();

    @Schema(description = "新建的打色单列表（NEW 模式）")
    @Builder.Default
    private List<ColorInstructionSimpleVO> colorInstructions = new ArrayList<>();

    @Schema(description = "胚布单简要信息")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GreigeOrderSimpleVO {
        private Long id;
        private String orderNo;
    }

    @Schema(description = "染色单简要信息")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DyeingOrderSimpleVO {
        private Long id;
    }

    @Schema(description = "检验单简要信息")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectionOrderSimpleVO {
        private Long id;
        private String code;
        private String type;
    }

    @Schema(description = "打色指示单简要信息")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorInstructionSimpleVO {
        private Long id;
        private String instructionNo;

        public ColorInstructionSimpleVO(Long id) {
            this.id = id;
        }
    }

}
