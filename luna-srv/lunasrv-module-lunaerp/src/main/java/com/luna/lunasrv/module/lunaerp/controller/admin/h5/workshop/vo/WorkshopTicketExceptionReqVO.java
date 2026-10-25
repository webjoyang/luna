package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车间移动 H5 - 布票异常登记 Request VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 布票异常登记 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTicketExceptionReqVO {

    @Schema(description = "布票编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BT-MTZZPB09190010101-0001")
    @NotBlank(message = "布票编号不能为空")
    private String code;

    @Schema(description = "异常类型（字典 lunaerp_weaving_exception_type：YARN_BREAK/CLOTH_DAMAGE/YARN_SHORTAGE/MACHINE_CHANGE/WORKER_CHANGE）", requiredMode = Schema.RequiredMode.REQUIRED, example = "YARN_BREAK")
    @NotBlank(message = "异常类型不能为空")
    private String exceptionType;

    @Schema(description = "异常说明", example = "3号引纱杆断纱，停机接纱")
    private String exceptionNote;

    @Schema(description = "异常现场照片 URL 列表（≤6 张，仅允许图片 URL）", example = "[\"https://oss.example.com/photo1.jpg\"]")
    private List<String> exceptionPhotos;

}
