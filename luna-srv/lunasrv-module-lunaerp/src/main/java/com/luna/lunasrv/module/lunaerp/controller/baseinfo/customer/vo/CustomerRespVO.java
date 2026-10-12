package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 基本信息-客户信息 Response VO")
@Data
public class CustomerRespVO {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "杭州织造有限公司")
    private String companyName;

    @Schema(description = "简称", example = "杭州织造")
    private String shortName;

    @Schema(description = "联系电话", example = "0571-12345678")
    @Sensitive(seePermission = "lunaerp:sensitive:contact", type = SensitiveType.FIXED_PHONE)
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

    @Schema(description = "英文名称", example = "Hangzhou Weaving Co., Ltd.")
    private String nameEn;

    @Schema(description = "英文地址", example = "Yuhang District, Hangzhou, Zhejiang")
    private String addressEn;

    @Schema(description = "归属业务员")
    private Long salespersonUserId;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）")
    private String status;

    @Schema(description = "默认毛利率（小数，如 0.150000 表示 15%，可空）", example = "0.150000")
    private BigDecimal defaultMarginRate;

    @Schema(description = "开票抬头", example = "杭州织造有限公司")
    private String invoiceTitle;

    @Schema(description = "开票地址", example = "浙江省杭州市余杭区临平大道100号")
    private String invoiceAddress;

    @Schema(description = "开户银行", example = "中国工商银行杭州临平支行")
    private String bankName;

    @Schema(description = "开户行账号", example = "1202021109900012345")
    private String bankAccountNo;

    @Schema(description = "信用账期天数（空=不设账期）", example = "30")
    private Integer creditDays;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
