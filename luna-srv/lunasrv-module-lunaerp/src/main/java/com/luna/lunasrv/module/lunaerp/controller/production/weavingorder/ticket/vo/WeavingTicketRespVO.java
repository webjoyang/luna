package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 布票 Response VO")
@Data
public class WeavingTicketRespVO {

    @Schema(description = "布票ID", example = "1024")
    private Long id;

    @Schema(description = "布票编号（BT-{机台任务号去符号}-{4 位流水}）", example = "BT-MTZZPB09190010101-0001")
    private String code;

    @Schema(description = "关联织造单ID", example = "100")
    private Long weavingOrderId;

    @Schema(description = "关联机台任务ID", example = "200")
    private Long machineTaskId;

    @Schema(description = "预排机台号快照", example = "LM20260001")
    private String plannedMachine;

    @Schema(description = "实际上机机台号", example = "LM20260001")
    private String actualMachine;

    @Schema(description = "单匹计划重量 KG", example = "25.00")
    private BigDecimal plannedWeight;

    @Schema(description = "挡车工用户ID", example = "12")
    private Long workerUserId;

    @Schema(description = "挡车工姓名", example = "张三")
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

    @Schema(description = "作废原因", example = "打印破损")
    private String voidReason;

    @Schema(description = "作废时间")
    private LocalDateTime voidedAt;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
