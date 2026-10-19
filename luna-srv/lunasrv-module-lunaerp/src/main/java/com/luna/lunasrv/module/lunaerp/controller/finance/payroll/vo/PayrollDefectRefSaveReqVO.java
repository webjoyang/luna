package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工资单次品引用明细保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDefectRefSaveReqVO {

    @Schema(description = "责任工人用户ID", example = "1001")
    private Long userId;

    @Schema(description = "责任工人姓名快照", example = "李四")
    private String nickname;

    @Schema(description = "质检确认记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "质检确认记录ID不能为空")
    private Long confirmationId;

    @Schema(description = "质检确认单号快照", example = "QC20261019001")
    private String confirmationNo;

    @Schema(description = "次品布卷号快照", example = "ROLL-001")
    private String rollNo;

    @Schema(description = "次品布卷等级快照", example = "C")
    private String grade;

    @Schema(description = "质检确认时间快照")
    private LocalDateTime confirmedAt;

    @Schema(description = "扣款单价快照（元/卷，必填且 > 0）", example = "50.00")
    private BigDecimal deductPrice;

}
