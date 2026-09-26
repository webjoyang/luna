package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 布票开工 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketStartReqVO {

    @Schema(description = "布票ID（单票操作）", example = "1024")
    private Long id;

    @Schema(description = "布票ID列表（批量操作）", example = "[1024, 1025]")
    private List<Long> ids;

    @Schema(description = "实际上机机台号（可空，缺省继承任务机台号）", example = "LM20260001")
    private String actualMachine;

    @Schema(description = "挡车工用户ID（可空，缺省继承任务挡车工）", example = "12")
    private Long workerUserId;

    @Schema(description = "挡车工姓名（可空，缺省继承任务挡车工）", example = "张三")
    private String workerName;

    @Schema(description = "开工时间（可空，缺省为当前时间）")
    private LocalDateTime startedAt;

}
