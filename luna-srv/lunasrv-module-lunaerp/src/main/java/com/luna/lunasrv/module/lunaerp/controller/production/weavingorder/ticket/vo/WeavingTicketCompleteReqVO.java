package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 布票完工 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketCompleteReqVO {

    @Schema(description = "布票ID（单票操作）", example = "1024")
    private Long id;

    @Schema(description = "布票ID列表（批量操作）", example = "[1024, 1025]")
    private List<Long> ids;

    @Schema(description = "完工时间（可空，缺省为当前时间）")
    private LocalDateTime completedAt;

}
