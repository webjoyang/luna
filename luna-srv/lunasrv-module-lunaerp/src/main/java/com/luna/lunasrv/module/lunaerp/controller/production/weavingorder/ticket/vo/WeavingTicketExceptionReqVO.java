package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 布票异常登记 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketExceptionReqVO {

    @Schema(description = "布票ID（单票操作）", example = "1024")
    private Long id;

    @Schema(description = "布票ID列表（批量操作）", example = "[1024, 1025]")
    private List<Long> ids;

    @Schema(description = "异常类型（字典 lunaerp_weaving_exception_type：YARN_BREAK/CLOTH_DAMAGE/YARN_SHORTAGE/MACHINE_CHANGE/WORKER_CHANGE）", requiredMode = Schema.RequiredMode.REQUIRED, example = "YARN_BREAK")
    @NotBlank(message = "异常类型不能为空")
    private String exceptionType;

    @Schema(description = "异常说明", example = "3号引纱杆断纱，停机接纱")
    private String exceptionNote;

    @Schema(description = "异常现场照片 URL 列表（≤6 张，可空）", example = "[\"https://oss.example.com/photo1.jpg\"]")
    @Size(max = 6, message = "异常现场照片最多上传 6 张")
    private List<String> exceptionPhotos;

}
