package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务管理 - 销售退货单作废 Request VO
 *
 * @author luna
 */
@Schema(description = "业务管理 - 销售退货单作废 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnCancelReqVO {

    @Schema(description = "退货单ID", example = "10")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户取消退货")
    @NotBlank(message = "作废原因不能为空")
    private String cancelReason;

}
