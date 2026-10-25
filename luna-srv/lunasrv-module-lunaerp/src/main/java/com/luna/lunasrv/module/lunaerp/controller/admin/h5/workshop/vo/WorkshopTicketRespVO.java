package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车间移动 H5 - 布票精简卡片出参 VO
 *
 * 仅包含现场需要字段：票号/织造单/机台/品名颜色/状态/任务 + claimable 标志
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 布票精简卡片出参 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTicketRespVO {

    @Schema(description = "布票ID", example = "1024")
    private Long id;

    @Schema(description = "布票编号（BT-{机台任务号去符号}-{4 位流水}）", example = "BT-MTZZPB09190010101-0001")
    private String code;

    @Schema(description = "关联织造单ID", example = "201")
    private Long weavingOrderId;

    @Schema(description = "织造单号快照", example = "ZZ-PB2026092601-01")
    private String weavingOrderCode;

    @Schema(description = "关联机台任务ID", example = "301")
    private Long machineTaskId;

    @Schema(description = "机台任务编号快照", example = "MT-ZZ2026092601-01")
    private String machineTaskCode;

    @Schema(description = "关联织机ID", example = "1")
    private Long loomId;

    @Schema(description = "机台号快照", example = "LM01")
    private String loomNo;

    @Schema(description = "预排机台号快照", example = "LM01")
    private String plannedMachine;

    @Schema(description = "实际上机机台号", example = "LM01")
    private String actualMachine;

    @Schema(description = "产品品名快照", example = "40S 纯棉单面汗布")
    private String productName;

    @Schema(description = "产品编号快照", example = "PRD-2026-001")
    private String productCode;

    @Schema(description = "成分快照", example = "100% 棉")
    private String composition;

    @Schema(description = "克重快照", example = "180g/m²")
    private String gsm;

    @Schema(description = "门幅快照", example = "185cm")
    private String width;

    @Schema(description = "单匹计划重量 KG", example = "25.00")
    private BigDecimal plannedWeight;

    @Schema(description = "挡车工用户ID（未派工时为 null）", example = "1001")
    private Long workerUserId;

    @Schema(description = "挡车工姓名快照（未派工时为 null）", example = "张三")
    private String workerName;

    @Schema(description = "原料批号", example = "YB20260901-01")
    private String materialBatchNo;

    @Schema(description = "布票状态（GENERATED/STARTED/COMPLETED/INSPECTED/STOCKED/EXCEPTION/VOID）", example = "GENERATED")
    private String status;

    @Schema(description = "异常类型", example = "YARN_BREAK")
    private String exceptionType;

    @Schema(description = "异常说明", example = "断纱")
    private String exceptionNote;

    @Schema(description = "异常现场照片 URL 列表（≤6 张）")
    private List<String> exceptionPhotos;

    @Schema(description = "开工时间")
    private LocalDateTime startedAt;

    @Schema(description = "完工时间")
    private LocalDateTime completedAt;

    @Schema(description = "是否可认领/作业标志（worker 为空或=登录人 时为 true）", example = "true")
    private Boolean claimable;

}
