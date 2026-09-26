package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 布票分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WeavingTicketPageReqVO extends PageParam {

    @Schema(description = "织造单ID", example = "100")
    private Long weavingOrderId;

    @Schema(description = "机台任务ID", example = "200")
    private Long machineTaskId;

    @Schema(description = "布票状态（GENERATED/STARTED/COMPLETED/INSPECTED/STOCKED/EXCEPTION/VOID）", example = "GENERATED")
    private String status;

    @Schema(description = "布票编号（模糊匹配）", example = "BT-")
    private String code;

    @Schema(description = "计划机台号", example = "LM20260001")
    private String plannedMachine;

    @Schema(description = "实际机台号", example = "LM20260002")
    private String actualMachine;

    @Schema(description = "挡车工姓名", example = "张三")
    private String workerName;

}
