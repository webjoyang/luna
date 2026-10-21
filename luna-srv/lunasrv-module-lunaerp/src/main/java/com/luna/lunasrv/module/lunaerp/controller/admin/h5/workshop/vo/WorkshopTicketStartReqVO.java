package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车间移动 H5 - 布票开工 Request VO
 *
 * @author luna
 */
@Schema(description = "车间移动 H5 - 布票开工 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTicketStartReqVO {

    @Schema(description = "布票编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BT-MTZZPB09190010101-0001")
    @NotBlank(message = "布票编号不能为空")
    private String code;

    @Schema(description = "实际上机机台号（可选，缺省继承任务机台号）", example = "LM01")
    private String actualMachine;

}
