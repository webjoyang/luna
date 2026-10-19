package com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工资单次品引用追溯 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDefectRefRespVO {

    @Schema(description = "次品引用ID", example = "1")
    private Long id;

    @Schema(description = "款项明细行ID", example = "10")
    private Long lineItemId;

    @Schema(description = "责任工人用户ID", example = "1001")
    private Long userId;

    @Schema(description = "责任工人姓名快照", example = "李四")
    private String nickname;

    @Schema(description = "质检确认记录ID", example = "2001")
    private Long confirmationId;

    @Schema(description = "质检确认单号快照", example = "QC20261019001")
    private String confirmationNo;

    @Schema(description = "次品布卷号快照", example = "ROLL-001")
    private String rollNo;

    @Schema(description = "次品布卷等级快照", example = "C")
    private String grade;

    @Schema(description = "质检确认时间快照")
    private LocalDateTime confirmedAt;

    @Schema(description = "扣款单价快照（元/卷）", example = "50.00")
    @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
    private BigDecimal deductPrice;

}
