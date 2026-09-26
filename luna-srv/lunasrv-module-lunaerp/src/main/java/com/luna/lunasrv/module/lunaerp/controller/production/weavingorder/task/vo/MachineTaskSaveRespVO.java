package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 生产管理 - 机台任务新建/修改 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineTaskSaveRespVO {

    @Schema(description = "机台任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "机台任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MT-ZZPB091900101-01")
    private String code;

    @Schema(description = "超量软警告信息（超织造单计划匹数时返回，不拦截保存）", example = "单内机台任务计划匹数合计(120匹)超出织造单计划匹数(100匹)")
    private String warning;

}
