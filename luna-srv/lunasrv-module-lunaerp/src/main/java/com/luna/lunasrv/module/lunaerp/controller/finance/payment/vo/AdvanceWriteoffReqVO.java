package com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 预收转核销请求 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceWriteoffReqVO {

    @Schema(description = "预收款单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "预收款单 ID 不能为空")
    private Long paymentId;

    @Schema(description = "拟核销分录列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "核销分录项列表不能为空")
    @Valid
    private List<ReceiptRefSaveVO> items;

    @Schema(description = "核销说明/备注", example = "客户预收货款转核销发货应收")
    private String remark;

}
