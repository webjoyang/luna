package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车间移动 H5 - 机台任务布票状态计数统计 VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 布票状态统计 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTicketStatusCountsVO {

    @Schema(description = "布票总数", example = "10")
    @Builder.Default
    private Integer total = 0;

    @Schema(description = "已生成/待开工数", example = "2")
    @Builder.Default
    private Integer generated = 0;

    @Schema(description = "已开工/进行中数", example = "1")
    @Builder.Default
    private Integer started = 0;

    @Schema(description = "已完工数", example = "5")
    @Builder.Default
    private Integer completed = 0;

    @Schema(description = "已检验数", example = "2")
    @Builder.Default
    private Integer inspected = 0;

    @Schema(description = "已入库数", example = "0")
    @Builder.Default
    private Integer stocked = 0;

    @Schema(description = "异常数", example = "0")
    @Builder.Default
    private Integer exception = 0;

    @Schema(description = "已作废数", example = "0")
    @Builder.Default
    private Integer voided = 0;

}
