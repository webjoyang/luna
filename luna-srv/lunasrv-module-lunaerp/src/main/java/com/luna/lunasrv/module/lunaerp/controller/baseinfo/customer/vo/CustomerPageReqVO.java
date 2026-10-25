package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 基本信息-客户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerPageReqVO extends PageParam {

    @Schema(description = "智能搜索词（命中 companyName/shortName/phone/unifiedSocialCreditCode 之一）", example = "杭州")
    private String search;

    @Schema(description = "状态：ACTIVE/INACTIVE", example = "ACTIVE")
    private String status;

    @Schema(description = "归属业务员", example = "1")
    private Long salespersonUserId;
}
