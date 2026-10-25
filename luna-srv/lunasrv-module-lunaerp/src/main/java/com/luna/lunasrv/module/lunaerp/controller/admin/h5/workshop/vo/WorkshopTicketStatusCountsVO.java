package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    @Schema(description = "有效布票总数（排除作废票）", example = "10")
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

    @Schema(description = "已作废数（仅留痕，不计入有效总数）", example = "0")
    @Builder.Default
    private Integer voided = 0;

    /**
     * 根据状态计数 Map 构建布票状态统计对象
     *
     * 业务规约：有效布票总数 (total) 严格排除作废票 (VOID)，与生产域有效布票统计口径一致
     *
     * @param counts 状态与计数的映射
     * @return 统计 VO
     */
    public static WorkshopTicketStatusCountsVO fromStatusCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return new WorkshopTicketStatusCountsVO();
        }
        int generated = counts.getOrDefault("GENERATED", 0);
        int started = counts.getOrDefault("STARTED", 0);
        int completed = counts.getOrDefault("COMPLETED", 0);
        int inspected = counts.getOrDefault("INSPECTED", 0);
        int stocked = counts.getOrDefault("STOCKED", 0);
        int exception = counts.getOrDefault("EXCEPTION", 0);
        int voided = counts.getOrDefault("VOID", 0);
        int total = generated + started + completed + inspected + stocked + exception;

        return WorkshopTicketStatusCountsVO.builder()
                .generated(generated)
                .started(started)
                .completed(completed)
                .inspected(inspected)
                .stocked(stocked)
                .exception(exception)
                .voided(voided)
                .total(total)
                .build();
    }

}
