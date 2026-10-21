package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 车间移动 H5 - 我的机台任务精简出参 VO
 *
 * 只包含现场需要字段，严格不输出成本/单价/内部备注等非现场信息
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 我的机台任务精简出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopMyTaskRespVO {

    @Schema(description = "机台任务ID", example = "101")
    private Long id;

    @Schema(description = "机台任务编号", example = "MT-ZZ2026092601-01")
    private String code;

    @Schema(description = "关联织造单ID", example = "201")
    private Long weavingOrderId;

    @Schema(description = "织造单号快照", example = "ZZ-PB2026092601-01")
    private String weavingOrderCode;

    @Schema(description = "产品品名快照", example = "40S 纯棉单面汗布")
    private String productName;

    @Schema(description = "产品编号快照", example = "PRD-2026-001")
    private String productCode;

    @Schema(description = "关联织机ID", example = "1")
    private Long loomId;

    @Schema(description = "机台号快照", example = "LM01")
    private String loomNo;

    @Schema(description = "挡车工用户ID", example = "1001")
    private Long workerUserId;

    @Schema(description = "挡车工姓名快照", example = "张三")
    private String workerName;

    @Schema(description = "计划匹数", example = "20")
    private Integer plannedQty;

    @Schema(description = "完工/实织匹数", example = "8")
    private Integer actualQty;

    @Schema(description = "任务状态（PENDING/IN_PROGRESS/HALTED/COMPLETED）", example = "IN_PROGRESS")
    private String status;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "布票各状态计数统计")
    private WorkshopTicketStatusCountsVO ticketCounts;

}
