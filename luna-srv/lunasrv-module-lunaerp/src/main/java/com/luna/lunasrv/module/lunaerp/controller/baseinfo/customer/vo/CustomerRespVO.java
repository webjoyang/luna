package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 基本信息-客户信息 Response VO")
@Data
public class CustomerRespVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "杭州织造有限公司")
    private String company;

    @Schema(description = "简称", example = "杭州织造")
    private String shortName;

    @Schema(description = "联系电话", example = "0571-12345678")
    private String phone;

    @Schema(description = "公司地址", example = "浙江省杭州市余杭区")
    private String address;

    @Schema(description = "客户类型（字典 lunaerp_customer_client_type）", requiredMode = Schema.RequiredMode.REQUIRED, example = "FORMAL")
    private String clientType;

    @Schema(description = "所属行业（字典 lunaerp_customer_industry）", example = "TEXTILE")
    private String industry;

    @Schema(description = "企业规模（字典 lunaerp_customer_scale）", example = "MEDIUM")
    private String scale;

    @Schema(description = "客户来源（字典 lunaerp_customer_source）", example = "SELF_DEV")
    private String source;

    @Schema(description = "合作状态（字典 lunaerp_customer_cooperation_status）", example = "COOPERATING")
    private String cooperationStatus;

    @Schema(description = "统一社会信用代码", example = "91330100MA12345678")
    private String unifiedSocialCreditCode;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
