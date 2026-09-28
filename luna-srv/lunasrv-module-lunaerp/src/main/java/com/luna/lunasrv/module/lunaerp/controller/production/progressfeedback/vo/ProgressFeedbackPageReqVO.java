package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理-进度反馈覆盖层分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProgressFeedbackPageReqVO extends PageParam {

    @Schema(description = "染色单ID", example = "100")
    private Long dyeingOrderId;

    @Schema(description = "销售合同号（支持模糊查询）", example = "HT2026-001")
    private String contractNo;

    @Schema(description = "承染方供应商ID", example = "200")
    private Long dyeingFactoryId;

    @Schema(description = "进度状态（NOT_SCHEDULED/SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）", example = "SCHEDULED")
    private String status;

    @Schema(description = "综合关键词（匹配颜色名称、客户色号、缸号、合同号）", example = "藏青")
    private String keyword;

    @Schema(description = "是否仅看延期", example = "true")
    private Boolean delayedOnly;

    @Schema(description = "是否仅看未决问题（存在未闭环反馈记录的缸）", example = "true")
    private Boolean unresolvedOnly;

    @Schema(description = "是否包含全缸已完成的颜色行（默认 false，开启后显示）", example = "false")
    private Boolean includeCompleted;

    @Schema(description = "是否包含已失效/已删除染色单（默认 false）", example = "false")
    private Boolean includeDeleted;

}
