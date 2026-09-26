package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 布票作废 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketVoidReqVO {

    @Schema(description = "布票ID（单票操作）", example = "1024")
    private Long id;

    @Schema(description = "布票ID列表（批量操作）", example = "[1024, 1025]")
    private List<Long> ids;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "打印破损重打，此票作废")
    @NotBlank(message = "作废原因不能为空")
    private String voidReason;

}
