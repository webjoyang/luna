package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理 - 织造单新建/修改 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeavingOrderSaveRespVO {

    @Schema(description = "织造单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "织造单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZZ-PB0919001-01")
    private String code;

    @Schema(description = "超量软警告信息（超胚布单匹数时提示，不拦截保存）", example = "织造单计划匹数合计(120匹)超出胚布单总匹数(100匹)")
    private String warning;

}
