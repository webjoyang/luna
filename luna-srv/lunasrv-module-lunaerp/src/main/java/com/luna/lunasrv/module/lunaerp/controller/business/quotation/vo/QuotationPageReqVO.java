package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "报价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuotationPageReqVO extends PageParam {

    @Schema(description = "搜索关键词（报价单号 / 客户名称 / 备注）", example = "QT2026")
    private String keyword;

    @Schema(description = "状态（DRAFT / CONFIRMED / SUPERSEDED / CANCELLED / EXPIRED）", example = "CONFIRMED")
    private String status;

    @Schema(description = "客户档案ID", example = "1")
    private Long customerId;

    @Schema(description = "仅看最新版本（缺省 true）", example = "true")
    private Boolean latestOnly;

}
