package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 财务管理 - 预收转核销 Request VO
 *
 * <p>对齐 ADR-0017 / Issue 05：在原已过账 ADVANCE 单上追加核销行，不建新单，不走审批。</p>
 *
 * @author luna
 */
@Schema(description = "管理后台 - 预收转核销 Request VO")
@Data
public class AdvanceWriteoffReqVO {

    @Schema(description = "收款单ID（必须为已过账 ADVANCE 单）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收款单ID不能为空")
    private Long paymentId;

    @Schema(description = "拟核销分配数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "核销分配明细不能为空")
    @Valid
    private List<ReceiptItemReqVO> items;

}
