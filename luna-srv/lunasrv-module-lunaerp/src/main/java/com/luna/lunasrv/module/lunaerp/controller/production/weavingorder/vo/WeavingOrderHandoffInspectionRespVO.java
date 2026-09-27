package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 生产管理 - 织造单完工交接检验 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingOrderHandoffInspectionRespVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long inspectionOrderId;

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INS-20260927-001")
    private String inspectionOrderCode;

    @Schema(description = "检验单号（前端兼容别名）", example = "INS-20260927-001")
    private String inspectionOrderNo;

    @Schema(description = "是否新创建表头（true 新创建，false 幂等复用已有）", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean created;

    @Schema(description = "是否复用已有检验单（!created）", example = "false")
    private Boolean reused;

    @Schema(description = "本单有效完工票数量（COMPLETED/INSPECTED/STOCKED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer completedTicketCount;

    @Schema(description = "起始完工布票号", example = "BT-TASK01-0001")
    private String minTicketCode;

    @Schema(description = "截止完工布票号", example = "BT-TASK01-0020")
    private String maxTicketCode;

    @Schema(description = "完工布票范围文本", example = "BT-TASK01-0001 ~ BT-TASK01-0020")
    private String ticketCodeRange;

    @Schema(description = "交接完工布票号列表")
    private List<String> ticketCodes;

}
