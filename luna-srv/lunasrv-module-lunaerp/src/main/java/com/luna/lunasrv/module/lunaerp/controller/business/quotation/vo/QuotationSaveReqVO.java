package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "报价单保存 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationSaveReqVO {

    @Schema(description = "报价单ID（新增为空，修改必填）", example = "100")
    private Long id;

    @Schema(description = "报价单号（留空自动生成 QT{yyyy}NNNN）", example = "QT20260001")
    private String quotationNo;

    @Schema(description = "客户档案ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "客户名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "杭州某某服装有限公司")
    @NotBlank(message = "客户名称不能为空")
    private String clientName;

    @Schema(description = "客户联系人快照", example = "张经理")
    private String clientContact;

    @Schema(description = "联系电话快照", example = "13800138000")
    private String phone;

    @Schema(description = "归属业务员（留空缺省当前登录用户）", example = "1")
    private Long salespersonUserId;

    @Schema(description = "报价日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-10")
    @NotNull(message = "报价日期不能为空")
    private LocalDate quotationDate;

    @Schema(description = "有效期自", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-10-10")
    @NotNull(message = "有效期自不能为空")
    private LocalDate validFrom;

    @Schema(description = "有效期至（空表示长期有效）", example = "2026-11-10")
    private LocalDate validTo;

    @Schema(description = "报价币种（缺省 RMB）", example = "RMB")
    private String currency;

    @Schema(description = "汇率（1 外币兑人民币；非 RMB 必填且 > 0，RMB 为 1）", example = "1.000000")
    private BigDecimal exchangeRate;

    @Schema(description = "备注", example = "大客户特惠报价")
    private String remarks;

    @Schema(description = "报价明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "报价明细不能为空")
    @Valid
    private List<QuotationItemSaveReqVO> items;

}
